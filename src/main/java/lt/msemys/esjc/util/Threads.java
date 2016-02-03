package lt.msemys.esjc.util;

public class Threads {

    public static void sleepUninterruptibly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

}
