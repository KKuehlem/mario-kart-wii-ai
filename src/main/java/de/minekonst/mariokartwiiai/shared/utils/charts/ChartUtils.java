package de.minekonst.mariokartwiiai.shared.utils.charts;

import java.util.List;
import java.util.Objects;
import javax.swing.JFrame;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.XYDataset;

public class ChartUtils {

    public static JFreeChart createXYChart(String title, String xAxis, String yAxis, String dataName, List<Vector2D> data) {
        return createXYChart(title, xAxis, yAxis, new XYChartEntry(dataName, data));
    }

    public static JFreeChart createXYChart(String title, String xAxis, String yAxis, XYChartEntry... entries) {
        return ChartFactory.createXYLineChart(title, xAxis, yAxis, toXYDataset(entries));
    }

    public static XYDataset toXYDataset(String name, List<Vector2D> data) {
        return toXYDataset(new XYChartEntry(name, data));
    }

    public static XYDataset toXYDataset(XYChartEntry... entries) {
        Objects.requireNonNull(entries);

        DefaultXYDataset data = new DefaultXYDataset();
        for (XYChartEntry entry : entries) {
            double[] x = new double[entry.getData().size()];
            double[] y = new double[entry.getData().size()];
            int i = 0;
            for (Vector2D v : entry.getData()) {
                x[i] = v.getX();
                y[i++] = v.getY();
            }

            data.addSeries(entry.getName(), new double[][]{x, y});
        }
        return data;
    }

    public static void showChart(JFreeChart c) {
        showChart(c, false);
    }
    
    public static void showChart(JFreeChart c, boolean exitOnClose) {
        JFrame frame = new JFrame(c.getTitle().getText());
        ChartPanel panel = new ChartPanel(c, false);
        frame.setContentPane(panel);
        frame.setSize(800, 500);
        frame.setVisible(true);

        if (exitOnClose) {
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        }
    }

}
