package thread;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;

/**
 * @author dngzs
 * @date 2019-10-08 13:52
 */
public class SerialExecutor implements Executor {

    private final Deque<Runnable> deque = new ArrayDeque();
    private final Executor executor;
    private Runnable active;

    public SerialExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable run) {
        deque.offer(new Runnable() {
            @Override
            public void run() {
                try {
                    run.run();
                } finally {
                    scheduleNext();
                }
            }
        });
        if (active == null) {
            scheduleNext();
        }
    }

    public synchronized void scheduleNext() {
        if ((active = deque.poll()) != null) {
            executor.execute(active);
        }
    }
}
