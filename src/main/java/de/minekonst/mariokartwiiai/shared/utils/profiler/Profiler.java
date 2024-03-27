package de.minekonst.mariokartwiiai.shared.utils.profiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public final class Profiler {

    private static final Map<String, StatisticsNode> map = new HashMap<>(20);
    private static final List<StatisticsRootNode> rootNodes = new ArrayList<>(4);
    private static final Map<Thread, String> enterMap = new HashMap<>(4);

    private Profiler() {
    }

    /**
     * Register the start of an node
     *
     * @param name The name of the node to start (need to be full name, for
     *             example "A.B", will start the node "B" which is an childnode
     *             of "A")
     *
     * @throws IllegalStateException         if the node is already running
     * @throws UnsupportedOperationException if an childnode cannot be created
     *                                       because its parent does not exist.
     *                                       For example an start of "A.B" will
     *                                       fail, if there is no node "A"
     */
    public synchronized static void registerStart(String name) {
        getOrCreate(name).registerStart();
    }

    /**
     * Register the end of an node
     *
     * @param name The name of the node to stop (need to be full name, for
     *             example "A.B", will start the node "B" which is an childnode
     *             of "A")
     *
     * @throws IllegalStateException         if the node is not running
     * @throws UnsupportedOperationException if an childnode cannot be created
     *                                       because its parent does not exist.
     *                                       For example an start of "A.B" will
     *                                       fail, if there is no node "A"
     */
    public synchronized static void registerEnd(String name) {
        getOrCreate(name).registerEnd();
    }

    /**
     * Add time to an node (can be negative)
     *
     * @param name The name of the node to add time to (need to be full name,
     *             for example "A.B", will start the node "B" which is an
     *             childnode of "A")
     * @param time The time to add to this node
     *
     * @throws UnsupportedOperationException if an childnode cannot be created
     *                                       because its parent does not exist.
     *                                       For example an start of "A.B" will
     *                                       fail, if there is no node "A"
     */
    public synchronized static void addTime(String name, double time) {
        getOrCreate(name).addTime(time);
    }

    /**
     * Enter a node in this thread. Example: <br>
     * enter("A") - Start time for A <br>
     * enter("B") - Start time for A.B <br>
     * enter("B") - Stop time for A.B <br>
     * enter("A") - Stop time for A <br>
     *
     * @param name The name of the node to enter
     *
     * @throws IllegalArgumentException if the name contains "." - Since the dot
     *                                  is used to seperate nodes in the full
     *                                  name, it is not allowed for a single
     *                                  node name
     */
    public synchronized static void enter(String name) {
        checkNameConventions(name);

        Thread thread = Thread.currentThread();
        String node = enterMap.get(thread);

        String e = node != null ? node + "." + name : name;
        enterMap.put(thread, e);

        registerStart(e);
    }

    /**
     * Exit a node in this thread. Example: <br>
     * enter("A") - Start time for A <br>
     * enter("B") - Start time for A.B <br>
     * enter("B") - Stop time for A.B <br>
     * enter("A") - Stop time for A <br>
     *
     * @param name The name of the node to enter
     *
     * @throws IllegalStateException    if not in a node
     * @throws IllegalArgumentException if the node you want to leave is not on
     *                                  top (if you are in e.g. "A.B" you cannot
     *                                  exit("A"), because you need to exit("B")
     *                                  first
     * @throws IllegalArgumentException if the name contains "." - Since the dot
     *                                  is used to seperate nodes in the full
     *                                  name, it is not allowed for a single
     *                                  node name
     */
    public synchronized static void exit(String name) {
        checkNameConventions(name);

        Thread thread = Thread.currentThread();
        String node = enterMap.get(thread);

        if (node == null) {
            throw new IllegalStateException("Not entered a node in this thread yet");
        }
        if (!node.equals(name) && !node.endsWith("." + name)) {
            throw new IllegalArgumentException("The node you want to leave is not on top. "
                    + "(Tried to leave \"" + name + "\" while in \"" + node + "\")");
        }

        registerEnd(node);

        int i = node.lastIndexOf(".");
        if (i == -1) {
            enterMap.put(thread, null);
        }
        else {
            enterMap.put(thread, node.substring(0, i));
        }
    }

    /**
     * Get the name of the node, which has been entered last by enter
     *
     * @return The full name of the node (Null, if not entered a node, of
     *         already left all)
     */
    public synchronized static String getNameOfCurrentNode() {
        return enterMap.get(Thread.currentThread());
    }

    /**
     * Close all entries opened using "enter" (for every Thread).Stop time for
     * all open nodes is now
     */
    public synchronized static void exitAll() {
        for (Thread t : enterMap.keySet()) {
            exitAll(t);
        }
    }

    /**
     * Close all entries opened using "enter" (for this Thread).Stop time for
     * all open nodes is now
     *
     * @param thread The Thread to exit the profiler for
     */
    public synchronized static void exitAll(Thread thread) {
        String node = enterMap.get(thread);

        if (node != null) {
            while (node != null) {
                registerEnd(node);
                int i = node.lastIndexOf(".");

                if (i != -1) {
                    node = node.substring(0, i);
                }
                else {
                    node = null;
                }
            }
        }

        enterMap.put(thread, null);
    }

    /**
     * Get a string representing the node
     *
     * @param n The node
     *
     * @return The node as string (depends on type)
     *
     * @throws UnsupportedOperationException if the nodetype is neigher
     *                                       StatisticsChildNode or
     *                                       StatisticsRootNode
     */
    public static String textFor(StatisticsNode n) {
        if (n instanceof StatisticsChildNode) {
            int it = ((StatisticsChildNode) n).getRootNode().getIterations();

            return nodeText(n.getName(), n.getTime(), ((StatisticsChildNode) n).getParent().getTime(), it);
        }
        else if (n instanceof StatisticsRootNode) {
            int it = ((StatisticsRootNode) n).getIterations();

            return nodeText(n.getName(), n.getTime(), -1, it);
        }
        else {
            throw new UnsupportedOperationException("Can only process nodes of type StatisticsRootNode and StatisticsChildNode");
        }
    }

    /**
     * Get a string representation of the nodes self time
     *
     * @param n The node
     *
     * @return The string for the self time of a node
     */
    public static String selfTimeStringFor(StatisticsNode n) {
        int it = 1;
        if (n instanceof StatisticsChildNode) {
            it = ((StatisticsChildNode) n).getRootNode().getIterations();
        }
        else if (n instanceof StatisticsRootNode) {
            it = ((StatisticsRootNode) n).getIterations();
        }
        else {
            throw new UnsupportedOperationException("Unknown type of StatisticsNode");
        }

        return nodeText("Self", n.getTime() - n.getTimeOfChildren(), n.getTime(), it);
    }

    /**
     * Get a root node by its name
     *
     * @param name The name of the node
     *
     * @return null, if there is no root node by this name
     */
    public synchronized static StatisticsRootNode getRootNode(String name) {
        for (StatisticsRootNode node : rootNodes) {
            if (node.getName().equals(name)) {
                return node;
            }
        }

        return null;
    }

    /**
     * Reset all statistics
     */
    public synchronized static void removeAll() {
        rootNodes.clear();
        map.clear();
    }

    /**
     * Reset a specific root node
     *
     * @param rootNode Name of the root node
     *
     * @return True, if this node was found and removed
     */
    public synchronized static boolean remove(String rootNode) {
        if (!rootNodes.removeIf((StatisticsRootNode n) -> {
            return n.getName().equals(rootNode);
        })) {
            return false;
        }

        map.remove(rootNode);

        map.keySet().removeIf((String name) -> {
            return name.startsWith(rootNode + ".");
        });

        return true;
    }

    public static void updateTree(JTree tree) {
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("Profiler", true);

        DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.setRoot(rootNode);
        rootNode.removeAllChildren();

        recursiveCreateNodes(rootNode, rootNodes);
        model.reload();

        tree.expandRow(0);
        tree.repaint();
    }

    /**
     * Create nodes for profiler tree recursively
     *
     * @param node  The node, which the statistics node will be added to
     * @param nodes
     */
    private static void recursiveCreateNodes(DefaultMutableTreeNode node, Collection<? extends StatisticsNode> nodes) {
        for (StatisticsNode n : nodes) {
            boolean hasChildren = !n.getChildren().isEmpty();
            DefaultMutableTreeNode treeNode = new DefaultMutableTreeNode(Profiler.textFor(n), hasChildren);
            node.add(treeNode);

            if (hasChildren) {
                n.sortChildren();
                recursiveCreateNodes(treeNode, n.getChildren());

                DefaultMutableTreeNode selfNode = new DefaultMutableTreeNode(Profiler.selfTimeStringFor(n), false);
                treeNode.add(selfNode);
            }
        }
    }

    private static StatisticsNode getOrCreate(String name) {
        StatisticsNode node = map.get(name);

        if (node != null) { // The node already exists
            return node;
        }
        else { // The node needs to be created
            int dotIndex = name.lastIndexOf(".");

            if (dotIndex != -1) { // Parent Node is passed
                String parentName = name.substring(0, dotIndex);
                StatisticsNode parent = map.get(parentName);

                if (parent != null) { // The parent exists
                    StatisticsRootNode root = null;
                    if (parent instanceof StatisticsChildNode) {
                        root = ((StatisticsChildNode) parent).getRootNode();
                    }
                    else if (parent instanceof StatisticsRootNode) {
                        root = (StatisticsRootNode) parent;
                    }

                    StatisticsNode newNode = new StatisticsChildNode(name.substring(dotIndex + 1), root, parent);
                    map.put(name, newNode);

                    parent.addChild(newNode);

                    return newNode;
                }
                else { // The parent does not exist
                    throw new UnsupportedOperationException("No parent node is named \"" + parentName + "\"");
                }
            }

            // The new Node is a root Node
            StatisticsRootNode newNode = new StatisticsRootNode(name);
            map.put(name, newNode);
            rootNodes.add(newNode);

            return newNode;
        }
    }

    private static String nodeText(String name, double time, double ofTime, int iterations) {
        String percentage = "";
        if (ofTime != -1) {
            percentage = String.format("| %d %c |  ", Math.round(time / ofTime * 100), '%');
        }

        return String.format("%s%s:    %s/i   (Total: %s)", percentage, name, convertTime(time / (iterations > 0 ? iterations : 1)), convertTime(time));
    }

    private static String convertTime(double ms) {
        if (ms < 100) {
            return String.format("%.2f ms", ms);
        }
        else {
            return String.format("%.2f s", ms / 1_000);
        }
    }

    private static void checkNameConventions(String nodeName) {
        if (nodeName.contains(".")) {
            throw new IllegalArgumentException("Names for nodes are not allowed to contain dots (\".\")");
        }
    }

}
