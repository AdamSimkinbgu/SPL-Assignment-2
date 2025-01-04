package bgu.spl.mics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A Future object represents a promised result - an object that will
 * eventually be resolved to hold a result of some operation. The class allows
 * Retrieving the result once it is available.
 * 
 * Only private methods may be added to this class.
 * No public constructor is allowed except for the empty constructor.
 */
public class Future<T> {

	private AtomicReference<T> result;
	private AtomicBoolean isDone;

	/**
	 * This should be the the only public constructor in this class.
	 */
	public Future() {
		this.result = new AtomicReference<T>(null);
		this.isDone = new AtomicBoolean(false);
	}
	/**
	 * retrieves the result the Future object holds if it has been resolved.
	 * This is a blocking method! It waits for the computation in case it has
	 * not been completed.
	 * <p>
	 * 
	 * @return return the result of type T if it is available, if not wait until it
	 *         is available.
	 * 
	 */
	public T get() {
		return result.get();
	}

	/**
	 * Resolves the result of this Future object.
	 */
	public void resolve(T result) {
		Future<T> oldVal;
		Future<T> newVal;
		do {
			oldVal = this;
			newVal = new Future<T>();
			newVal.result.set(result);
			newVal.isDone.set(true);
		} while (!this.compareAndSet(oldVal, newVal));
	}

	/**
	 * @return true if this object has been resolved, false otherwise
	 */
	public synchronized boolean isDone() {
		return isDone;
	}

	/**
	 * retrieves the result the Future object holds if it has been resolved,
	 * This method is non-blocking, it has a limited amount of time determined
	 * by {@code timeout}
	 * <p>
	 * 
	 * @param timout the maximal amount of time units to wait for the result.
	 * @param unit   the {@link TimeUnit} time units to wait.
	 * @return return the result of type T if it is available, if not,
	 *         wait for {@code timeout} TimeUnits {@code unit}. If time has
	 *         elapsed, return null.
	 */
	public synchronized T get(long timeout, TimeUnit unit) {
		if (!isDone) {
			long millisToWait = unit.toMillis(timeout);
			long startTime = System.currentTimeMillis();
			long elapsed = 0;

			while (!isDone && elapsed < millisToWait) {
				long remaining = millisToWait - elapsed;
				try {
					wait(remaining);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
				elapsed = System.currentTimeMillis() - startTime;
			}
		}
		return isDone ? result : null;
	}

}
