package de.minekonst.mariokartwiiai.shared.tasks;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.shared.utils.profiler.Profiler;
import de.minekonst.mariokartwiiai.client.Driver;
import de.minekonst.mariokartwiiai.client.emulator.ButtonState;
import de.minekonst.mariokartwiiai.client.emulator.ConnectorState;
import de.minekonst.mariokartwiiai.client.recorder.RecordFrame;
import de.minekonst.mariokartwiiai.main.Constants;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.server.ai.properties.AiProperties;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethod;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningObserver;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningObserverResponse;
import de.minekonst.mariokartwiiai.shared.utils.IngameTime;
import de.minekonst.mariokartwiiai.tracks.Hardness;
import java.util.ArrayList;
import lombok.Getter;

@Getter
public class LearningTask<L extends LearningMethod<N>, N> extends Task {

    private final String type;
    private final AiProperties<L, N> properties;
    private final IngameTime bestTime;
    private final boolean userControlled;

    private final LearningObserver<N> observer;
    private final byte[] networkData;
    private final int episodes;
    private final boolean evaluationEpisode;
    private boolean isEvaluationEpisode;

    // Transient
    private transient int frame;
    private transient int lastOnRoad;
    private transient int lastProgress;
    private transient int lastCheckpointProgress;
    private transient int lastCheckpointRaw;
    private transient boolean init;
    private transient ConnectorState state;
    private transient ArrayList<RecordFrame> replay;
    private transient int lastLap;
    private transient int lastLapStart;
    private transient int inputFrame;
    private transient int firstFrame;
    private transient volatile ButtonState virtualInput;
    private transient N network;
    private transient int episodesFinished;
    private transient Score maxScore;
    private transient Vector3D bestPosition;
    private transient String initialAdditionalTitle;

    /**
     * Create a Learning Task
     *
     * @param observer        The learning observer
     * @param track           The ID of the track
     * @param network         The network to run using the learning method
     * @param type            The origin of the network
     * @param properties      The properties of the AI
     * @param additionalTitle Additional Title displayed on emulator
     * @param bestTime        The best time of the AI or null
     * @param userControlled  If set to true, the input will be computed but not
     *                        actually set. The Task can not fail.
     */
    public LearningTask(LearningObserver<N> observer, N network, int track, String type, AiProperties<L, N> properties, IngameTime bestTime,
            boolean userControlled, String additionalTitle) {
        this(observer, network, track, type, properties, bestTime, userControlled, additionalTitle, 1, false);
    }

    public LearningTask(LearningObserver<N> observer, N network, int track, String type, AiProperties<L, N> properties, IngameTime bestTime,
            boolean userControlled, String additionalTitle, boolean replay) {
        this(observer, network, track, type, properties, bestTime, userControlled, additionalTitle, 1, false);
        isEvaluationEpisode = replay;
    }

    public LearningTask(LearningObserver<N> observer, N network, int track, String type, AiProperties<L, N> properties, IngameTime bestTime,
            boolean userControlled, String additionalTitle, int episodes, boolean evaluationEpisode) {
        super(track, observer.getInputMethod(), additionalTitle);
        this.type = type;
        this.properties = properties;
        this.bestTime = bestTime;
        this.userControlled = userControlled;
        this.observer = observer;
        this.network = network;
        this.networkData = observer.getLearningMethod().toByteArray(this.network);
        this.episodes = episodes;
        this.evaluationEpisode = evaluationEpisode;

        if (episodes == 0 && evaluationEpisode) {
            isEvaluationEpisode = true;
        }
    }

    @Override
    public ButtonState onNextFrame(Driver driver) {
        //<editor-fold defaultstate="collapsed" desc="Init">
        boolean wasInit = !init;
        if (!init) {
            Profiler.enter("Init");
            lastLap = 1;
            replay = new ArrayList<>(10 * 60);
            init = true;
            network = observer.getLearningMethod().fromByteArray(networkData);
            observer.init(network, driver);
            observer.onEpisodeStart(network);

            initialAdditionalTitle = super.additionalTitle;
            Profiler.exit("Init");
            inputFrame = firstFrame = driver.getConnector().getState().getFrame();
        }

        if (driver.getConnector().getState().getFrame() != inputFrame) {
            Main.log("Expected frame %,d got %,d", inputFrame,
                    driver.getConnector().getState().getFrame());
        }
        inputFrame++;
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Update progress and score">
        state = driver.getConnector().getState();
        if (state.getGround() >= Hardness.ROAD.getValue()) {
            lastOnRoad = frame;
        }
        int checkpointDiff = state.getCheckpoint() - lastCheckpointRaw;
        lastCheckpointRaw = state.getCheckpoint();
        if ((state.getCheckpoint() > lastCheckpointProgress && state.getLap() == lastLap) // Progress made
                || state.getLap() > lastLap) { // or new lap
            if (state.getLap() > lastLap) {// New lap
                lastLapStart = frame;
                checkpointDiff = 1; // Also positive
            }
            lastCheckpointProgress = state.getCheckpoint();
            lastLap = state.getLap();
            lastProgress = frame;
        }

        score = Score.calculateScore(driver, frame, properties);
        frame++;

        if (!wasInit) {
            observer.afterFrame(network, score, checkpointDiff);
        }
        //</editor-fold>

        Profiler.enter("Network Output");
        LearningObserverResponse r = observer.beforeFrame(network, this.observer.getInputMethod().calculate(driver), isEvaluationEpisode);
        ButtonState bs = r.getButtons();
        updateTitle(r.getTitle());
        virtualInput = bs;
        Profiler.exit("Network Output");
        replay.add(bs.toRecordFrame());

        boolean space = Main.getKeyListener() != null ? Main.getKeyListener().isSpaceDown() : false;
        return (userControlled && !space) ? null : bs;
    }

    @Override
    public String getAdditionalTitle() {
        if (userControlled) {
            boolean space = Main.getKeyListener() != null ? Main.getKeyListener().isSpaceDown() : false;
            return !space ? "- User Controlled -" : "- AI Controlled";
        }
        else {
            return super.getAdditionalTitle();
        }
    }

    @Override
    public TaskResponse checkFinished(Driver driver) {
        final int FRAMES_NO_PROGRESS = (int) (Constants.FRAMERATE * (isEvaluationEpisode ? properties.getMaxTimeNoProgressEvaluation().getValue() : properties.getMaxTimeNoProgress().getValue()) + 0.5);
        final int FRAMES_OFFROAD = (int) (Constants.FRAMERATE * (isEvaluationEpisode ? properties.getMaxTimeOffroadEvaluation().getValue() : properties.getMaxTimeOffroad().getValue()) + 0.5);
        final double MAX_DIFF = isEvaluationEpisode ? properties.getMaxDiffBestTimeEvaluation().getValue() : properties.getMaxDiffBestTime().getValue();

        String reason = null;
        if (!userControlled) {
            if (frame - lastProgress > FRAMES_NO_PROGRESS) {
                reason = String.format("No Pogress for more than %d frames", FRAMES_NO_PROGRESS);
            }
            else if (frame - lastOnRoad > FRAMES_OFFROAD) {
                reason = String.format("In Offroad for more than %d frames", FRAMES_OFFROAD);
            }
            else if (frame - lastLapStart > driver.getTrack().getLapTime() * IngameTime.FRAMES_PER_SECOND * 2) {
                reason = String.format("Task is taking way to long (longer than %d s)", driver.getTrack().getLapTime() * 2);
            }
            else if (state.getLap() > properties.getLaps().getValue()) {
                reason = "Finished all " + properties.getLaps().getValue() + " laps";
            }
            else if (bestTime != null) {
                int diff = frame - bestTime.getFrames();
                if (diff > MAX_DIFF * IngameTime.FRAMES_PER_SECOND) {
                    reason = String.format("Current Time %s is more than %.2f s higher than best time %s",
                            new IngameTime(frame), MAX_DIFF, bestTime);
                }
            }
        }

        if (reason != null) {
            observer.onEpisodeEnd(network, score);
            episodesFinished++;
            if (episodesFinished < episodes || (evaluationEpisode && episodesFinished == episodes)) {
                observer.onEpisodeStart(network);
                reason = null;
                lastLap = 0;
                lastCheckpointRaw = lastCheckpointProgress = 1;
                lastOnRoad = lastLapStart = lastProgress = inputFrame = firstFrame;
                frame = 0;

                if (episodesFinished == episodes) {
                    isEvaluationEpisode = true;
                }
                driver.loadTrack(super.getTrack());
            }
            else {
                Main.log("End of Tasks because of \"%s\"", reason);
            }
        }

        if (maxScore == null || score.getScorePoints() > maxScore.getScorePoints()) {
            maxScore = score;
            bestPosition = driver.getConnector().getState().getPosition();
        }

        if (reason != null && evaluationEpisode && !isEvaluationEpisode) {
            throw new IllegalStateException();
        }

        return reason == null ? null
                : new TaskResponse(this, evaluationEpisode ? score : maxScore, replay, type,
                        bestPosition,
                        driver.getConnector().getState().getRotation(), reason);
    }

    @Override
    public IngameTime getDrivingSince() {
        return new IngameTime(frame);
    }

    private void updateTitle(String fromObserver) {
        super.additionalTitle = "";

        if (episodes > 0 || isEvaluationEpisode) {
            if (isEvaluationEpisode) {
                super.additionalTitle = "- Evaluation -\n";
            }
            else {
                super.additionalTitle += String.format("Episode %,d / %,d\n", episodesFinished + 1, episodes);
            }
        }

        if (initialAdditionalTitle != null) {
            super.additionalTitle += initialAdditionalTitle + "\n";
        }

        if (fromObserver != null) {
            super.additionalTitle += fromObserver;
        }
    }

    public N getNetwork() {
        return this.observer.getLearningMethod().fromByteArray(networkData);
    }
}
