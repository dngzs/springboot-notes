import java.util.concurrent.CountDownLatch;

/**
 * @author dngzs
 * @date 2019-10-10 15:40
 */
public class Lock {
    volatile static int sum = 0;

    static CountDownLatch countDownLatch = new CountDownLatch(10);


    public static void sum(){
        sum++;
    }

    public static void main(String[] args) throws InterruptedException {
        for (int i = 0; i < 10 ; i++) {

            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int j = 0; j < 50; j++) {
                        sum();
                    }

                }
            });
            countDownLatch.countDown();
        }
        countDownLatch.await();
        System.out.println(sum);
    }
}
