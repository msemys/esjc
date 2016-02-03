package lt.msemys.esjc.util.concurrent;

import lt.msemys.esjc.util.Throwables;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ResettableLatch {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private volatile boolean released;

    public ResettableLatch(boolean released) {
        this.released = released;
    }

    public boolean await(long time, TimeUnit unit) {
        lock.lock();
        try {
            return (!released) ? condition.await(time, unit) : true;
        } catch (InterruptedException e) {
            throw Throwables.propagate(e);
        } finally {
            lock.unlock();
        }
    }

    public void release() {
        lock.lock();
        try {
            condition.signal();
            released = true;
        } finally {
            lock.unlock();
        }
    }

    public void reset() {
        lock.lock();
        try {
            released = false;
        } finally {
            lock.unlock();
        }
    }

}
