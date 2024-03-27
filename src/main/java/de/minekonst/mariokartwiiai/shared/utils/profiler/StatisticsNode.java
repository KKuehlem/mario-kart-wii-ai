package de.minekonst.mariokartwiiai.shared.utils.profiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

public abstract class StatisticsNode {

    protected final String name;
    protected final ArrayList<StatisticsNode> children = new ArrayList<>();
    protected long lastStartTime;
    protected double time;
    protected boolean running;

    /**
     * Create a new Statistics Node
     *
     * @param name Name for this node
     */
    public StatisticsNode(String name) {
        Objects.requireNonNull(name);

        this.name = name;
    }

    /**
     * Get the name of this node
     *
     * @return The name of this node
     */
    public String getName() {
        return name;
    }

    /**
     * Register start of measurement
     */
    public void registerStart() {
        if (running) {
            throw new IllegalStateException("Node already started. Register end first");
        }

        lastStartTime = System.currentTimeMillis();
        running = true;
    }

    /**
     * Register end of measurement
     */
    public void registerEnd() {
        if (!running) {
            throw new IllegalStateException("Node not started. Register start first");
        }

        time += (System.currentTimeMillis() - lastStartTime);
        running = false;
    }

    /**
     * Get the total time
     *
     * @return The time
     */
    public double getTime() {
        return time;
    }

    public double getTimeOfChildren() {
        double sum = 0;

        for (StatisticsNode n : getChildren()) {
            sum += n.getTime();
        }

        return sum;
    }

    /**
     * Get children
     *
     * @return Immutable Collection
     */
    public Collection<StatisticsNode> getChildren() {
        return Collections.unmodifiableCollection(children);
    }

    public void sortChildren() {
        children.sort((StatisticsNode a, StatisticsNode b) -> {
            return Double.compare(b.getTime(), a.getTime());
        });
    }

    public void addTime(double time) {
        this.time += time;
    }

    public boolean isRunning() {
        return running;
    }

    void addChild(StatisticsNode n) {
        children.add(n);
    }

}
