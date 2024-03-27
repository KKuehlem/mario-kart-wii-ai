package de.minekonst.mariokartwiiai.server.ai.properties;

import de.minekonst.mariokartwiiai.server.ai.types.ArchivedNetwork;
import de.minekonst.mariokartwiiai.server.ai.types.Replay;
import de.minekonst.mariokartwiiai.server.ai.types.Statistics;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethod;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jdom2.Element;

@Getter
@AllArgsConstructor
@SuppressWarnings("serial")
public class DataHolder<L extends LearningMethod<N>, N> implements Serializable {

    private final LearningMethod<N> learningMethod;
    private final int[] layers;
    private N baseNetwork;
    private final List<ArchivedNetwork<L, N>> archivedNetworks;
    private final List<Replay> replays;

    public void resetStatistics() {
        archivedNetworks.clear();
        replays.clear();
    }

    public void toXML(Element root) throws IOException {
        if (baseNetwork != null) {
            Element baseNet = new Element("baseNetwork").setText(XmlUtils.encodeNetwork(learningMethod, baseNetwork));
            root.addContent(baseNet);
        }

        Element aNets = new Element("archivedNetworks");
        for (ArchivedNetwork<L, N> a : archivedNetworks) {
            Element net = new Element("network").setText(XmlUtils.encodeNetwork(learningMethod, a.getNetwork(learningMethod)));
            aNets.addContent(new Element("archivedNetwork")
                    .addContent(a.getStatistics().toXML())
                    .addContent(net)
            );
        }
        root.addContent(aNets);

        Element reps = new Element("replays");
        for (Replay r : replays) {
            reps.addContent(new Element("replay")
                    .addContent(r.getStatistics().toXML())
                    .addContent(new Element("data").setText(XmlUtils.encodeObject(r.getReplay())))
            );
        }
        root.addContent(reps);

        root.addContent(new Element("layers").setText(XmlUtils.encodeObject(layers)));
        root.addContent(new Element("learningMethod").setText(XmlUtils.encodeObject(learningMethod)));
    }

    @SuppressWarnings("unchecked")
    public static <L extends LearningMethod<N>, N> DataHolder<L, N> fromXML(Element root) {
        LearningMethod<N> learningMethod = XmlUtils.decodeObject(root.getChildText("learningMethod"), LearningMethod.class);

        N baseNetwork = root.getChild("baseNetwork") != null ? XmlUtils.decodeNetwork(learningMethod, root.getChildText("baseNetwork")) : null;

        List<ArchivedNetwork<L, N>> archivedNetworks = new ArrayList<>();
        for (Element e : root.getChild("archivedNetworks").getChildren()) {
            archivedNetworks.add(new ArchivedNetwork<>(Statistics.fromXML(e.getChild("statistic")),
                    learningMethod,
                    XmlUtils.decodeNetwork(learningMethod, e.getChildText("network"))));
        }

        List<Replay> replays = new ArrayList<>();
        for (Element e : root.getChild("replays").getChildren()) {
            replays.add(new Replay(Statistics.fromXML(e.getChild("statistic")),
                    XmlUtils.decodeObject(e.getChildText("data"), List.class)));
        }

        int[] layers = XmlUtils.decodeObject(root.getChildText("layers"), int[].class);

        return new DataHolder<>(learningMethod, layers, baseNetwork, archivedNetworks, replays);
    }

    @SuppressWarnings("unchecked")
    public void setBaseNetwork(Object n) {
        this.baseNetwork = (N) n;
    }

}
