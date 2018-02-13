package l2f.commons.util.concurrent.locks;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A class providing locking with a re-entry and a division into read / write. Simplified analogue {@link java.util.concurrent.locks.ReentrantReadWriteLock} Consumes less memory, less productive.
 * <p>
 * The ability to re-enter also provides a lowering of the lock from writing to reading, for this you need to hold the write lock, get a read lock and release the write lock. It should be remembered that increasing the write lock, while holding the lock on reading,
 * is impossible.
 * </p>
 * @author Arodev
 */
public class ReentrantReadWriteLock
{
	private static final AtomicIntegerFieldUpdater<ReentrantReadWriteLock> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(ReentrantReadWriteLock.class, "state");
	
	static final int SHARED_SHIFT = 16;
	static final int SHARED_UNIT = (1 << SHARED_SHIFT);
	static final int MAX_COUNT = (1 << SHARED_SHIFT) - 1;
	static final int EXCLUSIVE_MASK = (1 << SHARED_SHIFT) - 1;
	
	/**
	 * Returns the number of shared holds represented in count
	 * @param c
	 * @return
	 */
	static int sharedCount(int c)
	{
		return c >>> SHARED_SHIFT;
	}
	
	/**
	 * Returns the number of exclusive holds represented in count
	 * @param c
	 * @return
	 */
	static int exclusiveCount(int c)
	{
		return c & EXCLUSIVE_MASK;
	}
	
	/**
	 * A counter for per-thread read hold counts. Maintained as a ThreadLocal; cached in cachedHoldCounter
	 */
	static final class HoldCounter
	{
		int count;
		// Use id, not reference, to avoid garbage retention
		final long tid = Thread.currentThread().getId();
		
		/**
		 * Decrement if positive; return previous value
		 * @return
		 */
		int tryDecrement()
		{
			final int c = count;
			if (c > 0)
				count = c - 1;
			return c;
		}
	}
	
	/**
	 * ThreadLocal subclass. Easiest to explicitly define for sake of deserialization mechanics.
	 */
	static final class ThreadLocalHoldCounter extends ThreadLocal<HoldCounter>
	{
		@Override
		public HoldCounter initialValue()
		{
			return new HoldCounter();
		}
	}
	
	/**
	 * The number of read locks held by current thread. Initialized only in constructor and readObject.
	 */
	transient ThreadLocalHoldCounter readHolds;
	
	/**
	 * The hold count of the last thread to successfully acquire readLock. This saves ThreadLocal lookup in the common case where the next thread to release is the last one to acquire. This is non-volatile since it is just used as a heuristic, and would be great for threads to cache.
	 */
	transient HoldCounter cachedHoldCounter;
	
	private Thread owner;
	private volatile int state;
	
	public ReentrantReadWriteLock()
	{
		readHolds = new ThreadLocalHoldCounter();
		setState(0);
	}
	
	private final int getState()
	{
		return state;
	}
	
	private void setState(int newState)
	{
		state = newState;
	}
	
	private boolean compareAndSetState(int expect, int update)
	{
		return stateUpdater.compareAndSet(this, expect, update);
	}
	
	private Thread getExclusiveOwnerThread()
	{
		return owner;
	}
	
	private void setExclusiveOwnerThread(Thread thread)
	{
		owner = thread;
	}
	
	public void writeLock()
	{
		final Thread current = Thread.currentThread();
		for (;;)
		{
			final int c = getState();
			final int w = exclusiveCount(c);
			if (c != 0)
			{
				// (Note: if c != 0 and w == 0 then shared count != 0)
				if ((w == 0) || (current != getExclusiveOwnerThread()))
					continue;
				if ((w + exclusiveCount(1)) > MAX_COUNT)
					throw new Error("Maximum lock count exceeded");
			}
			if (compareAndSetState(c, c + 1))
			{
				setExclusiveOwnerThread(current);
				return;
			}
		}
	}
	
	public boolean tryWriteLock()
	{
		final Thread current = Thread.currentThread();
		final int c = getState();
		if (c != 0)
		{
			final int w = exclusiveCount(c);
			if ((w == 0) || (current != getExclusiveOwnerThread()))
				return false;
			if (w == MAX_COUNT)
				throw new Error("Maximum lock count exceeded");
		}
		if (!compareAndSetState(c, c + 1))
			return false;
		setExclusiveOwnerThread(current);
		return true;
	}
	
	final boolean tryReadLock()
	{
		final Thread current = Thread.currentThread();
		final int c = getState();
		final int w = exclusiveCount(c);
		if ((w != 0) && (getExclusiveOwnerThread() != current))
			return false;
		if (sharedCount(c) == MAX_COUNT)
			throw new Error("Maximum lock count exceeded");
		if (compareAndSetState(c, c + SHARED_UNIT))
		{
			HoldCounter rh = cachedHoldCounter;
			if ((rh == null) || (rh.tid != current.getId()))
				cachedHoldCounter = rh = readHolds.get();
			rh.count++;
			return true;
		}
		return false;
	}
	
	public void readLock()
	{
		final Thread current = Thread.currentThread();
		HoldCounter rh = cachedHoldCounter;
		if ((rh == null) || (rh.tid != current.getId()))
			rh = readHolds.get();
		for (;;)
		{
			final int c = getState();
			final int w = exclusiveCount(c);
			if ((w != 0) && (getExclusiveOwnerThread() != current))
				continue;
			if (sharedCount(c) == MAX_COUNT)
				throw new Error("Maximum lock count exceeded");
			if (compareAndSetState(c, c + SHARED_UNIT))
			{
				cachedHoldCounter = rh; // cache for release
				rh.count++;
				return;
			}
		}
	}
	
	public void writeUnlock()
	{
		final int nextc = getState() - 1;
		if (Thread.currentThread() != getExclusiveOwnerThread())
			throw new IllegalMonitorStateException();
		if (exclusiveCount(nextc) == 0)
		{
			setExclusiveOwnerThread(null);
			setState(nextc);
			return;
		}
		setState(nextc);
		return;
	}
	
	public void readUnlock()
	{
		HoldCounter rh = cachedHoldCounter;
		final Thread current = Thread.currentThread();
		if ((rh == null) || (rh.tid != current.getId()))
			rh = readHolds.get();
		if (rh.tryDecrement() <= 0)
			throw new IllegalMonitorStateException();
		for (;;)
		{
			final int c = getState();
			final int nextc = c - SHARED_UNIT;
			if (compareAndSetState(c, nextc))
				return;
		}
	}
}
