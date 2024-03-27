package de.minekonst.mariokartwiiai.shared.utils.profiler;

public final class StatisticsRootNode extends StatisticsNode {

    private int iterations;

    public StatisticsRootNode(String name) {
        super(name);
    }

    public int getIterations() {
        return iterations;
    }

    public void nextIteration() {
        this.iterations++;
    }

}
