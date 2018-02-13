package l2f.commons.util.concurrent.atomic;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Atomic, non-overlapping state flag.
 * @author Arodev
 */
public class AtomicState
{
	private static final AtomicIntegerFieldUpdater<AtomicState> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(AtomicState.class, "value");
	
	private volatile int value;
	
	public AtomicState(boolean initialValue)
	{
		value = initialValue ? 1 : 0;
	}
	
	public AtomicState()
	{
	}
	
	public final boolean get()
	{
		return value != 0;
	}
	
	private static boolean getBool(int value)
	{
		if (value < 0)
			throw new IllegalStateException();
		return value > 0;
	}
	
	public final boolean setAndGet(boolean newValue)
	{
		return getBool(newValue ? stateUpdater.incrementAndGet(this) : stateUpdater.decrementAndGet(this));
	}
	
	public final boolean getAndSet(boolean newValue)
	{
		return getBool(newValue ? stateUpdater.getAndIncrement(this) : stateUpdater.getAndDecrement(this));
	}
	
}
