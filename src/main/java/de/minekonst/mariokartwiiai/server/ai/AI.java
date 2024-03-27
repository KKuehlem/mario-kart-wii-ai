package de.minekonst.mariokartwiiai.server.ai;

import de.minekonst.mariokartwiiai.shared.utils.editortable.EditorValue;
import de.minekonst.mariokartwiiai.shared.utils.charts.ChartUtils;
import de.minekonst.mariokartwiiai.shared.utils.charts.XYChartEntry;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.client.recorder.RecordFrame;
import de.minekonst.mariokartwiiai.main.Constants;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.server.RemoteDriver;
import de.minekonst.mariokartwiiai.server.Telegram;
import de.minekonst.mariokartwiiai.server.ai.properties.AiProperties;
import de.minekonst.mariokartwiiai.server.ai.properties.DataHolder;
import de.minekonst.mariokartwiiai.server.ai.types.ArchivedNetwork;
import de.minekonst.mariokartwiiai.server.ai.types.Replay;
import de.minekonst.mariokartwiiai.server.ai.types.Statistics;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethod;
import de.minekonst.mariokartwiiai.shared.tasks.LearningTask;
import de.minekonst.mariokartwiiai.shared.tasks.Score;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import de.minekonst.mariokartwiiai.shared.utils.IngameTime;
import de.minekonst.mariokartwiiai.tracks.Track;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import lombok.Getter;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

@Getter
public class AI<L extends LearningMethod<N>, N> {

    private final AiProperties<LearningMethod<N>, N> properties;
    private final LearningMethod<N> learningMethod;
    private final Scheduler<LearningMethod<N>, N> scheduler;

    private final List<Double> lastPoints;
    private long generationStart;
    private int failedGen;
    private List<RecordFrame> lastReplay;
    private String state = "Waiting";
    private boolean inEvaluation;

    /**
     * Create a new AI
     *
     * @param learningMethod The learning method to use for this AI
     * @param name           The name for the new AI
     * @param track          The track
     * @param layers         The layers for the NeuralNetwork
     */
    public AI(LearningMethod<N> learningMethod, String name, Track track, int[] layers) {
        this.learningMethod = learningMethod;

        int[] nl = new int[layers.length + 2];
        nl[0] = this.learningMethod.getInputMethod().getNeededNeurons();
        System.arraycopy(layers, 0, nl, 1, layers.length);
        nl[nl.length - 1] = LearningMethod.OUTPUT;

        if (!name.endsWith(".xml")) {
            name += ".xml";
        }

        scheduler = new Scheduler<>();
        lastPoints = new ArrayList<>(100);

        properties = new AiProperties<>(name, new DataHolder<>(learningMethod, nl, null, new ArrayList<>(), new ArrayList<>()));
        learningMethod.init(this);
        properties.onLearningMethodReady();
        properties.getTrack().setValue(track);
    }

    /**
     * Load an AI from file
     *
     * @param name The name of the AI to load
     */
    public AI(String name) {
        scheduler = new Scheduler<>();
        lastPoints = new ArrayList<>(100);

        if (!name.endsWith(".xml")) {
            name += ".xml";
        }

        properties = new AiProperties<>(name);
        learningMethod = properties.getDataHolder().getLearningMethod();
        learningMethod.init(this);
        properties.onLearningMethodReady();
    }

    public void update() {
        scheduler.update();
        
        //<editor-fold defaultstate="collapsed" desc="Check for timeout tasks">
        scheduler.takeBack((LearningTask t) -> {
            if (System.currentTimeMillis() - t.getCreationTime() > Constants.TASK_TIMEOUT) {
                Main.log("Tasked #%d (Client #%d) timed out and got back to open tasks",
                        t.getTaskID(), t.getClientID());
                return true;
            }
            else {
                return false;
            }
        });
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Calc average">
        TaskResponse best = null;
        double avg = 0;
        for (TaskResponse r : scheduler.getResponses()) {
            if (best == null || r.getScore().getScorePoints() > best.getScore().getScorePoints()) {
                best = r;
            }

            avg += r.getScore().getScorePoints();
        }
        if (!scheduler.getResponses().isEmpty()) {
            avg /= scheduler.getResponses().size();
        }
        //</editor-fold>

        final int totalTasks = properties.getGenomesPerGeneration().getValue();
        state = String.format("%d / %d done, %d running (Score: %.2f, Avg: %.2f)%s",
                scheduler.getResponses().size(), totalTasks,
                scheduler.getRunningTasks().size(), best != null ? best.getScore().getScorePoints() : 0, avg,
                (best != null && best.getScore().isAllLaps() ? " | " + best.getScore().getTime() : ""));

        if (scheduler.getResponses().size() == totalTasks && !inEvaluation) {
            LearningTask<LearningMethod<N>, N> eval = learningMethod.evaluationTask(scheduler.getResponses());
            if (eval != null) {
                scheduler.addOpenTask(eval);
                inEvaluation = true;
            }
            else {
                nextGeneration();
            }
        }
        else if (scheduler.getResponses().size() == totalTasks + 1 && inEvaluation) {
            inEvaluation = false;
            nextGeneration();
        }
    }

    @SuppressWarnings("unchecked")
    private void nextGeneration() {
        //<editor-fold defaultstate="collapsed" desc="Get EditorValues, Average and bestTribe">
        EditorValue<Integer> era = properties.getEra();
        EditorValue<Integer> species = properties.getSpecies();
        EditorValue<Integer> generation = properties.getGeneration();
        EditorValue<Double> score = properties.getScore();
        EditorValue<Double> maxScore = properties.getMaxScore();
        List<TaskResponse> responses = scheduler.getResponses();

        double avg = responses.stream().mapToDouble(r -> r.getScore().getScorePoints()).average().getAsDouble();
        TaskResponse best = learningMethod.getBest(responses);
        Score bestScore = best.getScore();
        //</editor-fold>

        boolean success = best.getScore().getScorePoints() > properties.getMaxScore().getValue() - properties.getMaxScoreDrop().getValue();
        if (success) {
            Main.log("§2Era %d, Species %d, Generation %d scored %.2f §0(Parents had %.2f, Max %.2f). ",
                    era.getValue(), species.getValue(), generation.getValue(),
                    bestScore.getScorePoints(), score.getValue(), maxScore.getValue());
            Telegram.broadcast("=== New %s Score: %.2f ===\n"
                    + "Max %.2f, Parents %.2f\n"
                    + "Era %d, Species %d, Generation %d\n"
                    + bestScore.toString()
                    + (bestScore.isAllLaps() ? "\nTime: " + bestScore.getTime() : ""),
                    bestScore.getScorePoints() > maxScore.getValue() ? "Max" : "Best",
                    bestScore.getScorePoints(),
                    maxScore.getValue(), score.getValue(),
                    era.getValue(), species.getValue(), generation.getValue());

            if (bestScore.getScorePoints() > maxScore.getValue()) {
                maxScore.setValue(bestScore.getScorePoints());
            }

            score.setValue(best.getScore().getScorePoints());
            lastReplay = best.getReplay();
            properties.getDataHolder().setBaseNetwork(learningMethod.cloneNetwork(((LearningTask<L, N>) best.getTask()).getNetwork()));

            Statistics s = new Statistics(era.getValue(), species.getValue(), generation.getValue(),
                    score.getValue(), properties.getTotalTime().getValue(), best.getLastPosition(), avg,
                    true, bestScore.isAllLaps() ? bestScore.getTime() : null);
            properties.getStatistics().add(s);

            // Archive
            if (generation.getValue() == 1 || generation.getValue() % properties.getArchiveEveryGen().getValue() == 0) {
                properties.getDataHolder().getReplays().add(new Replay(s, lastReplay));
                properties.getDataHolder().getArchivedNetworks().add(new ArchivedNetwork(s, learningMethod, ((LearningTask<L, N>) best.getTask()).getNetwork()));
            }

            // Next Gneration
            generation.setValue(generation.getValue() + 1);
            if (generation.getValue() > properties.getGenerationsPerSpecies().getValue()) {
                generation.setValue(1);
                species.setValue(species.getValue() + 1);
                if (species.getValue() > properties.getSpeciesPerEra().getValue()) {
                    species.setValue(1);
                    era.setValue(era.getValue() + 1);
                }
            }

            failedGen = 0;
        }
        else {
            Main.log("Score: %.2f §0(Parents had %.2f). ",
                    bestScore.getScorePoints(), properties.getMaxScore().getValue());
            failedGen++;
        }

        lastPoints.add(bestScore.getScorePoints());
        double avgLast = 0;
        for (Double d : lastPoints) {
            avgLast += d;
        }
        avgLast /= lastPoints.size();
        Main.log("Average of last %d: %.2f", lastPoints.size(), avgLast);

        properties.getTotalTime().setValue(properties.getTotalTime().getValue() + (System.currentTimeMillis() - generationStart) / 1_000.0);
        save();

        List<TaskResponse> resp = new ArrayList<>(responses);
        scheduler.reset();
        createTasks(resp, success);
    }

    public void onLoad() {
        scheduler.reset();
        createTasks(List.of(), false);
    }

    public void save() {
        properties.save();
        Main.log("AI saved");
    }

    private void createTasks(List<TaskResponse> responses, boolean success) {
        generationStart = System.currentTimeMillis();

        new Thread(() -> {
            List<LearningTask<LearningMethod<N>, N>> next = learningMethod.nextGeneration(responses, success, properties.getGenomesPerGeneration().getValue());
            scheduler.addOpenTasks(next);
        }).start();
    }

    public void takeBackTasksFor(RemoteDriver s, boolean disconnected) {
        scheduler.takeBack((LearningTask t) -> {
            if (t.getClientID() == s.getServerClient().getID()) {
                String reason = disconnected ? "disconnected" : "refused this task";
                Main.log("§2Task #%d §0got back to open Tasks, because §3Client #%d §0%s",
                        t.getTaskID(), s.getServerClient().getID(), reason);
                return true;
            }
            else {
                return false;
            }
        });
    }

    public String onCommand(String cmd) {
        switch (cmd) {
            case "/list" -> {
                return scheduler.listAnswers();
            }
            case "/avg" -> {
                double avg = 0;
                for (Double d : lastPoints) {
                    avg += d;
                }
                avg /= lastPoints.size();
                return String.format("Average of last %d: %.2f", lastPoints.size(), avg);
            }
            default -> {
                return properties.onCommand(cmd);
            }
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Statistics">
    public void showStatistics() {
        List<Vector2D> data = new ArrayList<>(properties.getStatistics().size());
        List<Vector2D> avg = new ArrayList<>(properties.getStatistics().size());
        List<Vector2D> bestGen = new ArrayList<>(properties.getStatistics().size());
        for (Statistics s : properties.getStatistics()) {
            double time = s.getTotalTimePassed() / (60.0 * 60.0);
            data.add(new Vector2D(time, s.getScore()));
            avg.add(new Vector2D(time, s.getAverage()));
            if (s.isNewBest()) {
                bestGen.add(new Vector2D(time, s.getScore()));
            }
        }

        JFreeChart c = ChartUtils.createXYChart("Statistics", "Passed time in hours", "Score Points",
                new XYChartEntry("Best Score", data), new XYChartEntry("Best Generation", data), new XYChartEntry("Average Score", avg));
        JFrame frame = new JFrame("Statistics for AI " + this.getName());
        ChartPanel panel = new ChartPanel(c, false);
        frame.setContentPane(panel);
        panel.setDisplayToolTips(true);
        panel.setMouseZoomable(true, true);
        panel.setMouseWheelEnabled(true);
        frame.setSize(800, 400);
        frame.setVisible(true);
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Archive -> Replay with Properties">
    public List<Replay> getReplayArchive() {
        List<Replay> list = new ArrayList<>(properties.getDataHolder().getReplays());

        // Add current best
        if (lastReplay != null) {
            list.add(new Replay(new Statistics(properties.getEra().getValue(), properties.getSpecies().getValue(), properties.getGeneration().getValue(),
                    properties.getScore().getValue(), properties.getTotalTime().getValue(), Vector3D.ZERO, 0, false, null), lastReplay));
        }
        return list;
    }

    public List<ArchivedNetwork<LearningMethod<N>, N>> getArchivedNetworks() {
        ArrayList<ArchivedNetwork<LearningMethod<N>, N>> list = new ArrayList<>(properties.getDataHolder().getArchivedNetworks());
        Statistics stats = new Statistics(properties.getEra().getValue(), properties.getSpecies().getValue(), properties.getGeneration().getValue(),
                properties.getScore().getValue(), properties.getTotalTime().getValue(), Vector3D.ZERO, 0, false, null);
        list.add(new ArchivedNetwork<>(stats, learningMethod, properties.getDataHolder().getBaseNetwork()));
        return list;
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Getter / Setter">
    public String getLearnTime() {
        double time = properties.getTotalTime().getValue();
        int minutes = (int) (time / 60) % 60;
        int hours = (int) ((time / 60) / 60);
        return String.format("%d h %d min", hours, minutes);
    }

    @Override
    public String toString() {
        String s = "";
        for (int x = 0; x < properties.getDataHolder().getLayers().length; x++) {
            if (x != 0) {
                s += " - ";
            }

            s += properties.getDataHolder().getLayers()[x];
        }
        return learningMethod.getClass().getSimpleName() + ", Size: " + s;
    }

    public String getName() {
        return properties.getXmlFile().getName().substring(0, properties.getXmlFile().getName().lastIndexOf(".xml"));
    }

    public IngameTime getBestTime() {
        return !properties.getStatistics().isEmpty()
                ? properties.getStatistics().get(properties.getStatistics().size() - 1).getEndTime()
                : null;
    }

    public long getFileSize() {
        return properties.getXmlFile().length();
    }
    //</editor-fold>
}
