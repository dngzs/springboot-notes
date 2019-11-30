package thread;

import java.util.concurrent.Executor;

/**
 * @author dngzs
 * @date 2019-10-08 13:59
 */
public class SerialDemo {

    public static void main(String[] args) {
        SerialExecutor serialExecutor = new SerialExecutor(new DirectExecutor());
        serialExecutor.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("hello2");
            }
        });
        serialExecutor.execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("hello1");
            }
        });
    }


     static class DirectExecutor implements Executor {
        public void execute(Runnable r) {
            r.run();
        }
    }
}
