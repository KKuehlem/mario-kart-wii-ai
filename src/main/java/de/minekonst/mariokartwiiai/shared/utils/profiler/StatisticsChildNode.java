package de.minekonst.mariokartwiiai.shared.utils.profiler;

import java.util.Objects;

public final class StatisticsChildNode extends StatisticsNode {

    private final StatisticsNode parent;
    private final StatisticsRootNode rootNode;

    public StatisticsChildNode(String name, StatisticsRootNode rootNode, StatisticsNode parent) {
        super(name);

        Objects.requireNonNull(rootNode);
        Objects.requireNonNull(parent);

        this.parent = parent;
        this.rootNode = rootNode;
    }

    public StatisticsNode getParent() {
        return parent;
    }

    public StatisticsRootNode getRootNode() {
        return rootNode;
    }

}
