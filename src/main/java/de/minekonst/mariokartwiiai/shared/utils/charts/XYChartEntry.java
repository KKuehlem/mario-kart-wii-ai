package de.minekonst.mariokartwiiai.shared.utils.charts;

import java.util.List;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

public class XYChartEntry {

    private final String name;
    private final List<Vector2D> data;

    public XYChartEntry(String name, List<Vector2D> data) {
        this.name = name;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public List<Vector2D> getData() {
        return data;
    }

}
