package de.minekonst.mariokartwiiai.server;

import de.minekonst.mariokartwiiai.shared.tasks.Task;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TaskSupplier {
    
    private static final Map<Integer, Consumer<TaskResponse>> map = new HashMap<>(100);
    private static int nextID = 0;
    
    public synchronized static void sendTask(RemoteDriver driver, Task task, Consumer<TaskResponse> onTaskFinished) {
        task.onActivate(nextID++, driver.getServerClient().getID());
        map.put(task.getTaskID(), onTaskFinished);
        driver.getServerClient().send(task);
    }
    
    public static void sendTasks(int n, RemoteDriver driver, Supplier<? extends Task> tasks, 
            Consumer<Integer> onNextTask,
            Consumer<List<TaskResponse>> onFinish) {
        
        new MultiTaskExecutor(n, driver, tasks, onNextTask, onFinish);
    }
    
    static void onReponse(TaskResponse response) {
        Consumer<TaskResponse> c = map.get(response.getTask().getTaskID());
        if (c != null) {
            c.accept(response);
        }
    }
}
