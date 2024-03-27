package de.minekonst.mariokartwiiai.server.ai.properties;

import de.minekonst.mariokartwiiai.shared.utils.editortable.EditorBase;
import de.minekonst.mariokartwiiai.shared.utils.editortable.EditorHeading;
import de.minekonst.mariokartwiiai.shared.utils.editortable.EditorValue;
import de.minekonst.mariokartwiiai.shared.utils.editortable.Validator;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.server.ai.types.Statistics;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethod;
import de.minekonst.mariokartwiiai.shared.utils.IngameTime;
import de.minekonst.mariokartwiiai.shared.utils.dynamictype.TypeFinder;
import de.minekonst.mariokartwiiai.tracks.Track;
import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.Getter;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

@Getter
@SuppressWarnings("serial")
public class AiProperties<L extends LearningMethod<N>, N> implements Serializable {

    static {
        TypeFinder.registerType(IngameTime.class, IngameTime::fromString);
    }

    // Generell
    private final EditorValue<Track> track = new EditorValue<>("Track", Track.MarioRaceway_N64, Track.class, "The track for this AI");
    private final EditorValue<Integer> era = new EditorValue<>("Era", 1, Integer.class, "The current Era");
    private final EditorValue<Integer> species = new EditorValue<>("Species", 1, Integer.class, "The current Sepcies");
    private final EditorValue<Integer> generation = new EditorValue<>("Generation", 1, Integer.class, "The current Generation");
    private final EditorValue<Integer> genomesPerGeneration = new EditorValue<>("Genomes per Generation",
            1, Integer.class, "The amount of genomes per generation and tribe", Validator.greaterOrEqual(1));

    // Gameplay
    private final EditorValue<Integer> laps = new EditorValue<>("Laps", 3, Integer.class, "The amount of Laps after which the AI will be reset",
            Validator.greaterOrEqual(1), Validator.lessOrEqual(3));
    private final EditorValue<Double> score = new EditorValue<>("Score", 0.0, Double.class, "The current score of the AI");
    private final EditorValue<Double> maxScore = new EditorValue<>("Max Score", 0.0, Double.class, "The max score the AI ever scored");
    private final EditorValue<Double> maxScoreDrop = new EditorValue<>("Max Score Drop", 0.0, Double.class, "The max value the \"Score\" can diff from the \"Max Score\"");
    private final EditorValue<Double> totalTime = new EditorValue<>("Time", 0.0, Double.class, "The time the AI is active in seconds");

    // Generations
    private final EditorValue<Integer> generationsPerSpecies = new EditorValue<>("Generations per Species", 20, Integer.class, "How many generations equal one species", Validator.NON_NEG);
    private final EditorValue<Integer> speciesPerEra = new EditorValue<>("Species per Era", 20, Integer.class, "How many species equal one era", Validator.NON_NEG);
    private final EditorValue<Integer> archiveEveryGen = new EditorValue<>("Archive every", 10, Integer.class, "After how many generations the network and replay will be archived", Validator.NON_NEG);

    // Scoring
    private final EditorValue<Double> maxProgressScore = new EditorValue<>("Max Progress Score", 300.0, Double.class, "The maximal score for 3 Laps", Validator.NON_NEG);
    private final EditorValue<Double> maxSpeedScore = new EditorValue<>("Max Speed Score", 50.0, Double.class, "The maximal score for speed (= driving with \"Max Speed\")", Validator.NON_NEG);
    private final EditorValue<Double> maxSpeed = new EditorValue<>("Max Speed", 85.0, Double.class, "The maximal speed (for speed score)", Validator.NON_NEG);
    private final EditorValue<Double> maxTimeScore = new EditorValue<>("Max Time Score", 150.0, Double.class, "The maximal score for time (= ending with \"Good Time\")", Validator.NON_NEG);
    private final EditorValue<IngameTime> goodTime = new EditorValue<>("Good Time", IngameTime.fromString("2:50"), IngameTime.class, "Upper bound for point limit");
    private final EditorValue<IngameTime> badTime = new EditorValue<>("Bad Time", IngameTime.fromString("4:20"), IngameTime.class, "Lower bound for point limit");

    // Time
    private final EditorValue<Double> maxTimeOffroad = new EditorValue<>("Max Time Offroad", 15.0, Double.class, "Max time offroad without task ending (in seconds)", Validator.NON_NEG);
    private final EditorValue<Double> maxTimeNoProgress = new EditorValue<>("Max Time Without Progress", 40.0, Double.class, "Max time with no progress without task ending (in seconds)", Validator.NON_NEG);
    private final EditorValue<Double> maxDiffBestTime = new EditorValue<>("Max Diff to Best Time", 10.0, Double.class, "Max time difference to the best time (if exists)", Validator.NON_NEG);

    private final EditorValue<Double> maxTimeOffroadEvaluation = new EditorValue<>("Max Time Offroad (Eval)", 1.0, Double.class, "Max time offroad without task ending (in seconds)", Validator.NON_NEG);
    private final EditorValue<Double> maxTimeNoProgressEvaluation = new EditorValue<>("Max Time Without Progress (Eval)", 6.0, Double.class, "Max time with no progress without task ending (in seconds)", Validator.NON_NEG);
    private final EditorValue<Double> maxDiffBestTimeEvaluation = new EditorValue<>("Max Diff to Best Time (Eval)", 10.0, Double.class, "Max time difference to the best time (if exists)", Validator.NON_NEG);

    private final List<Statistics> statistics = new ArrayList<>(20);

    private final List<EditorBase> all;
    private final File xmlFile;
    private final Map<String, EditorValue<?>> map = new HashMap<>();
    private final transient DataHolder<L, N> dataHolder;

    public AiProperties(String name) {
        this(name, null);
    }

    @SuppressWarnings("unchecked")
    public AiProperties(String name, DataHolder<L, N> holder) {
        this.xmlFile = new File(Main.getDataDir() + File.separator
                + "AIs" + File.separator + name);

        DataHolder<L, N> d = loadDataholder();
        if (d == null && holder == null) {
            throw new IllegalStateException("Dataholder caannot be loaded");
        }
        dataHolder = d != null ? d : holder;

        all = new ArrayList<>();
    }

    public void onLearningMethodReady() {
        all.addAll(List.of(
                new EditorHeading("Generell", Color.LIGHT_GRAY),
                track, era, species, generation, genomesPerGeneration,
                new EditorHeading("Gameplay", Color.LIGHT_GRAY),
                laps, score, maxScore, maxScoreDrop, totalTime,
                new EditorHeading("Training (" + dataHolder.getLearningMethod().getClass().getSimpleName() + ")", Color.LIGHT_GRAY)
        ));
        all.addAll(dataHolder.getLearningMethod().getEditorValues());
        all.add(new EditorHeading("Input Method (" + dataHolder.getLearningMethod().getInputMethod().getClass().getSimpleName() + ")", Color.LIGHT_GRAY));
        all.addAll(dataHolder.getLearningMethod().getInputMethod().getEditorValues());
        all.addAll(List.of(
                new EditorHeading("Generations", Color.LIGHT_GRAY),
                generationsPerSpecies, speciesPerEra, archiveEveryGen,
                new EditorHeading("Scoring", Color.LIGHT_GRAY),
                maxProgressScore, maxSpeedScore, maxSpeed,
                maxTimeScore, badTime, goodTime,
                new EditorHeading("Timing (Learning)", Color.LIGHT_GRAY),
                maxTimeOffroad, maxTimeNoProgress, maxDiffBestTime,
                new EditorHeading("Timing (Evaluation)", Color.LIGHT_GRAY),
                maxTimeOffroadEvaluation, maxTimeNoProgressEvaluation, maxDiffBestTimeEvaluation
        ));

        for (EditorBase base : all) {
            if (base instanceof EditorValue<?>) {
                map.put(base.getName(), (EditorValue) base);
            }
        }
        loadProperties();
    }

    //<editor-fold defaultstate="collapsed" desc="Load / Save">
    private DataHolder<L, N> loadDataholder() {
        if (xmlFile.exists()) {
            try {
                Document doc = new SAXBuilder().build(xmlFile);
                return DataHolder.fromXML(doc.getRootElement());
            }
            catch (JDOMException | IOException ex) {
                Main.log("Cannot load AI from %s", xmlFile.getAbsolutePath());
                ex.printStackTrace();
            }
        }

        return null;
    }

    private void loadProperties() {
        if (xmlFile.exists()) {
            try {
                Document doc = new SAXBuilder().build(xmlFile);
                readProperties(doc.getRootElement().getChild("properties"));
                readOther(doc.getRootElement());
            }
            catch (JDOMException | IOException ex) {
                Main.log("Cannot load AI from %s", xmlFile.getAbsolutePath());
                ex.printStackTrace();
            }
        }
    }

    public void save() {
        try {
            Element root = new Element("ai");
            Document d = new Document(root);
            Element props = new Element("properties");
            writeProperties(props);
            root.addContent(props);
            writeOther(root);
            dataHolder.toXML(root);

            XMLOutputter out = new XMLOutputter();
            out.setFormat(Format.getPrettyFormat());
            FileWriter fw = new FileWriter(xmlFile);
            out.output(d, fw);
            fw.close();
        }
        catch (IOException ex) {
            Main.log("Cannot save AI to %s", xmlFile.getAbsolutePath());
            ex.printStackTrace();
        }
    }

    private void writeOther(Element root) {
        Element es = new Element("statistics");
        for (Statistics s : statistics) {
            es.addContent(s.toXML());
        }
        root.addContent(es);
    }

    private void readOther(Element root) {
        if (root.getChild("statistics") != null) {
            for (Element e : root.getChild("statistics").getChildren()) {
                statistics.add(Statistics.fromXML(e));
            }
        }
    }

    private void writeProperties(Element props) {
        for (EditorValue<?> ev : map.values()) {
            props.addContent(new Element("property")
                    .setAttribute("name", ev.getName())
                    .setText(ev.getValue().toString()));
        }
    }

    private void readProperties(Element props) {
        for (Element e : props.getChildren()) {
            String name = e.getAttribute("name").getValue();
            EditorValue<?> ev = map.get(name);
            if (ev != null) {
                ev.fromString(e.getContent(0).getValue());
            }
            else {
                Main.log("No Property exist with name " + name);
            }
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Getter">
    public Map<String, EditorValue<?>> getMap() {
        return map;
    }

    public List<EditorBase> getAllProperties() {
        return all;
    }
    //</editor-fold>

    public String onCommand(String cmd) {
        String[] split = cmd.split("\\s+");
        switch (split[0]) {
            case "/props" -> {
                //<editor-fold defaultstate="collapsed" desc="List Properties">
                StringBuilder sb = new StringBuilder();
                int x = 0;
                for (EditorBase base : all) {
                    if (base instanceof EditorHeading) {
                        sb.append("== ").append(base.getName()).append(" ==\n");
                    }
                    else if (base instanceof EditorValue) {
                        Object val = ((EditorValue) base).getValue();
                        String str = (val instanceof Double) ? String.format("%.2f", (double) val) : val.toString();
                        sb.append(String.format("%2d | %s = %s\n", x, base.getName(), str));
                        x++;
                    }
                }
                return sb.toString();
                //</editor-fold>
            }
            case "/prop" -> {
                if (split.length == 2 || split.length == 3) {
                    EditorValue<?> val = null;
                    //<editor-fold defaultstate="collapsed" desc="Get Property">
                    try {
                        int n = Integer.parseInt(split[1]);
                        int x = 0;
                        for (EditorBase base : all) {
                            if (base instanceof EditorValue) {
                                if (x == n) {
                                    val = (EditorValue<?>) base;
                                    break;
                                }
                                x++;
                            }
                        }
                        if (val == null) {
                            return "No Property with id " + n;
                        }
                    }
                    catch (NumberFormatException ex) {
                        return "\"" + split[1] + "\" cannot be converted to a number";
                    }
                    //</editor-fold>

                    if (split.length == 2) {
                        String desc = val.getDescription().isEmpty() ? "" : "Description: " + val.getDescription();
                        String en = "";
                        if (val.getType().isEnum()) {
                            en = "Enum Values: " + Arrays.toString(val.getType().getEnumConstants()) + "\n";
                        }

                        return String.format("%s = %s\n"
                                + "Type = %s\n"
                                + "%s%s", val.getName(), val.getType().getSimpleName(),
                                toString(val.getValue()), en, desc);
                    }
                    else {
                        Object old = val.getValue();
                        String a = val.fromStringNoGui(split[2]);
                        if (a == null) {
                            return String.format("Property %s has been set from %s to %s",
                                    val.getName(), toString(old), toString(val.getValue()));
                        }
                        else {
                            return String.format("Cannot set Property %s to %s:\n%s", val.getName(), split[2], a);
                        }
                    }
                }
                return "Usage: /prop <number> (Value)";
            }
            default -> {
                return null;
            }
        }
    }

    private static String toString(Object o) {
        if (o instanceof Integer i) {
            return String.format("%,d", i);
        }
        else if (o instanceof Number n) {
            return String.format(Locale.GERMANY, "%,.2f", n.doubleValue());
        }
        else {
            return o != null ? o.toString() : "<null>";
        }
    }

}
