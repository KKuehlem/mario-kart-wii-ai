package de.minekonst.mariokartwiiai.server.ai.types;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import de.minekonst.mariokartwiiai.shared.utils.IngameTime;
import de.minekonst.mariokartwiiai.shared.utils.MathUtils;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jdom2.Element;

@AllArgsConstructor
@Getter
public class Statistics implements Serializable {

    private final int era, species, generation;
    private final double score, totalTimePassed;
    private final Vector3D endPosition;
    private final double average;
    private final boolean newBest;
    private final IngameTime endTime;

    public Element toXML() {
        Element e = new Element("statistic")
                .setAttribute("era", "" + era)
                .setAttribute("species", "" + species)
                .setAttribute("generation", "" + generation)
                .setAttribute("score", "" + score)
                .setAttribute("time", "" + totalTimePassed)
                .setAttribute("average", "" + average)
                .setAttribute("newBest", (newBest ? "1" : "0"))
                .setAttribute("position", "" + endPosition.toString());

        if (endTime != null) {
            e.setAttribute("endTime", "" + endTime.getFrames());
        }

        return e;
    }

    public static Statistics fromXML(Element e) {
        double avg = 0;
        IngameTime et = null;
        boolean newBest = true;
        if (e.getAttributeValue("average") != null) {
            avg = d(e, "average");
        }
        if (e.getAttributeValue("endTime") != null) {
            et = new IngameTime(i(e, "endTime"));
        }
        if (e.getAttributeValue("newBest") != null) {
            newBest = i(e, "newBest") == 1;
        }
        
        return new Statistics(i(e, "era"), i(e, "species"), i(e, "generation"),
                d(e, "score"), d(e, "time"),
                MathUtils.parseVector3(e.getAttributeValue("position")), avg, newBest, et);
    }

    private static int i(Element e, String s) {
        return Integer.parseInt(e.getAttributeValue(s));
    }

    private static double d(Element e, String s) {
        return Double.parseDouble(e.getAttributeValue(s).replace(',', '.'));
    }

    public String getTimeString() {
        int minutes = (int) (totalTimePassed / 60) % 60;
        int hours = (int) ((totalTimePassed / 60) / 60);
        return String.format("%d h %d min", hours, minutes);
    }

}
