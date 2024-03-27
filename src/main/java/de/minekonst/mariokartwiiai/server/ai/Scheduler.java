package de.minekonst.mariokartwiiai.server.ai;

import de.minekonst.mariokartwiiai.client.DriverState;
import de.minekonst.mariokartwiiai.main.Main;
import de.minekonst.mariokartwiiai.server.AIServer;
import de.minekonst.mariokartwiiai.server.RemoteDriver;
import de.minekonst.mariokartwiiai.server.TaskSupplier;
import de.minekonst.mariokartwiiai.shared.methods.learning.LearningMethod;
import de.minekonst.mariokartwiiai.shared.tasks.LearningTask;
import de.minekonst.mariokartwiiai.shared.tasks.Task;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;
import lombok.Getter;

@Getter
public class Scheduler<L extends LearningMethod<N>, N> {

    private final List<LearningTask<LearningMethod<N>, N>> openTasks;
    private final List<LearningTask<LearningMethod<N>, N>> runningTasks;
    private final List<TaskResponse> responses;
    private final AIServer server;

    /**
     * Create a new scheduler
     */
    public Scheduler() {
        this.server = AIServer.getInstance();
        openTasks = new ArrayList<>(10);
        runningTasks = new ArrayList<>(10);
        responses = new CopyOnWriteArrayList<>();
    }

    /**
     * Takes all currently running tasks matching the filter back, aka move them
     * back to open tasks
     *
     * @param filter All tasks for which the filter is true, will be taken
     *               back
     */
    synchronized void takeBack(Predicate<LearningTask> filter) {
        runningTasks.removeIf((LearningTask t) -> {
            if (filter.test(t)) {
                if (t.getTaskID() != -1) {
                    openTasks.add(t);
                }
                return true;
            }
            else {
                return false;
            }
        });
    }

    /**
     * Call when a task has been finished
     *
     * @param response The response
     */
    synchronized void onResponse(TaskResponse response) {
        if (response.getTask().getTaskID() == -1) {
            return;
        }

        for (Task t : runningTasks) { // Find Task for this response
            if (response.getTask().getTaskID() == t.getTaskID()) {
                responses.add(response);
                runningTasks.remove(t);
                return;
            }
        }

        Main.log("There is no open task with ID %d", response.getTask().getTaskID());
    }

    /**
     * Add an open task to the scheduler
     *
     * @param creator The creator will be given the task id and is responsible
     *                for creating the open task from it
     */
    synchronized void addOpenTask(LearningTask<LearningMethod<N>, N> task) {
        openTasks.add(task);
    }

    synchronized void addOpenTasks(List<LearningTask<LearningMethod<N>, N>> task) {
        openTasks.addAll(task);
    }

    synchronized void update() {
        assignTasks();
    }

    synchronized void reset() {
        openTasks.clear();
        runningTasks.clear();
        responses.clear();
    }

    synchronized String listAnswers() {
        responses.sort((TaskResponse a, TaskResponse b) -> {
            return Double.compare(b.getScore().getScorePoints(), a.getScore().getScorePoints());
        });

        StringBuilder sb = new StringBuilder(20);
        double sum = 0;
        for (TaskResponse r : responses) {
            sb.append(r.toString()).append("\n");
            sum += r.getScore().getScorePoints();
        }

        if (!responses.isEmpty()) {
            sb.append(String.format("Average: %.2f\n", sum / responses.size()));
        }
        sb.append("There are ").append(runningTasks.size() + openTasks.size()).append(" Tasks left for this generation");

        return sb.toString();
    }

    private void assignTasks() {
        openTasks.removeIf((LearningTask<LearningMethod<N>, N> task) -> {
            RemoteDriver s = nextFreeClient();

            if (s != null) {
                TaskSupplier.sendTask(s, task, (TaskResponse r) -> onResponse(r));
                runningTasks.add(task);
                return true;
            }
            else {
                return false;
            }
        });
    }

    /**
     * Search for the first driver which is free (aka it is used for AI, is
     * waiting and has no assigned task)
     *
     * @return The first free driver
     */
    private RemoteDriver nextFreeClient() {
        for (RemoteDriver s : server.getRemoteDrivers()) {
            if (s.isUsedForAI() && s.getState() == DriverState.Waiting) {
                boolean hasTask = runningTasks.stream()
                        .anyMatch(t -> t.getClientID() == s.getServerClient().getID());
                if (!hasTask) {
                    return s;
                }
            }
        }
        return null;
    }

}
