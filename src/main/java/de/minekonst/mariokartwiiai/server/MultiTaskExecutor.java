package de.minekonst.mariokartwiiai.server;

import de.minekonst.mariokartwiiai.shared.tasks.Task;
import de.minekonst.mariokartwiiai.shared.tasks.TaskResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MultiTaskExecutor {
    private final int n;
    private final Supplier<? extends Task> supplier;
    private final RemoteDriver driver;
    private final Consumer<Integer> onNextTask;
    private final Consumer<List<TaskResponse>> onFinish;
    private final List<TaskResponse> responses = new ArrayList<>();
    private volatile int done;

    MultiTaskExecutor(int n, RemoteDriver driver, Supplier<? extends Task> tasks, 
            Consumer<Integer> onNextTask, Consumer<List<TaskResponse>> onFinish) {
        this.n = n;
        this.supplier = tasks;
        this.driver = driver;
        this.onNextTask = onNextTask;
        this.onFinish = onFinish;
        
        sendNext();
    }

    private void sendNext() {
        TaskSupplier.sendTask(driver, supplier.get(), (TaskResponse r) -> {
            synchronized (responses) {
                responses.add(r);
            }
            done++;
            if (done >= n) {
                onFinish.accept(responses);
            }
            else {
                onNextTask.accept(done);
                sendNext();
            }
        });
    }
    
    
}
