package l2f.commons.threading;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Delayed;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.mutable.MutableLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2f.commons.collections.LazyArrayList;

/**
 * Task queue manager with a multiple scheduled execution time.
 * @author Arodev
 */
public abstract class SteppingRunnableQueueManager implements Runnable
{
	protected static final Logger _log = LoggerFactory.getLogger(SteppingRunnableQueueManager.class);
	
	protected final long tickPerStepInMillis;
	private final List<SteppingScheduledFuture<?>> queue = new CopyOnWriteArrayList<>();
	private final AtomicBoolean isRunning = new AtomicBoolean();
	
	public SteppingRunnableQueueManager(long tickPerStepInMillis)
	{
		this.tickPerStepInMillis = tickPerStepInMillis;
	}
	
	public class SteppingScheduledFuture<V> implements RunnableScheduledFuture<V>
	{
		protected final Runnable r;
		private final long stepping;
		private final boolean isPeriodic;
		
		private long step;
		private boolean isCancelled;
		
		public SteppingScheduledFuture(Runnable r, long initial, long stepping, boolean isPeriodic)
		{
			this.r = r;
			this.step = initial;
			this.stepping = stepping;
			this.isPeriodic = isPeriodic;
		}
		
		@Override
		public void run()
		{
			if (--step == 0)
				try
				{
					r.run();
				}
				catch (final Exception e)
				{
					_log.error("Exception in a Runnable execution:", e);
				}
				finally
				{
					if (isPeriodic)
						step = stepping;
				}
		}
		
		@Override
		public boolean isDone()
		{
			return isCancelled || (!isPeriodic && (step == 0));
		}
		
		@Override
		public boolean isCancelled()
		{
			return isCancelled;
		}
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning)
		{
			return isCancelled = true;
		}
		
		@Override
		public V get()
		{
			return null;
		}
		
		@Override
		public V get(long timeout, TimeUnit unit)
		{
			return null;
		}
		
		@Override
		public long getDelay(TimeUnit unit)
		{
			return unit.convert(step * tickPerStepInMillis, TimeUnit.MILLISECONDS);
		}
		
		@Override
		public int compareTo(Delayed o)
		{
			return 0;
		}
		
		@Override
		public boolean isPeriodic()
		{
			return isPeriodic;
		}
	}
	
	/**
	 * Schedule the task after a period of time
	 * @param r task to perform
	 * @param delay in milliseconds
	 * @return Stepping ScheduledFuture control object responsible for task execution
	 */
	public SteppingScheduledFuture<?> schedule(Runnable r, long delay)
	{
		return schedule(r, delay, delay, false);
	}
	
	/**
	 * Schedule the task at regular intervals, with an initial delay
	 * @param r task to perform
	 * @param initial 
	 * @param delay execution time in milliseconds
	 * @return Stepping ScheduledFuture control object responsible for task execution
	 */
	public SteppingScheduledFuture<?> scheduleAtFixedRate(Runnable r, long initial, long delay)
	{
		return schedule(r, initial, delay, true);
	}
	
	private SteppingScheduledFuture<?> schedule(Runnable r, long initial, long delay, boolean isPeriodic)
	{
		SteppingScheduledFuture<?> sr;
		
		final long initialStepping = getStepping(initial);
		final long stepping = getStepping(delay);
		
		queue.add(sr = new SteppingScheduledFuture<Boolean>(r, initialStepping, stepping, isPeriodic));
		
		return sr;
	}
	
	/**
	 * Select "stepping" for the task: if the delay is less than the execution step, the result will be 1 if the delay is more than the execution step, the result will be the result of rounding off the delay / step division
	 * @param delay
	 * @return
	 */
	private long getStepping(long delay)
	{
		delay = Math.max(0, delay);
		return (delay % tickPerStepInMillis) > (tickPerStepInMillis / 2) ? (delay / tickPerStepInMillis) + 1 : delay < tickPerStepInMillis ? 1 : delay / tickPerStepInMillis;
	}
	
	@Override
	public void run()
	{
		if (!isRunning.compareAndSet(false, true))
		{
			_log.warn("Slow running queue, managed by " + this + ", queue size : " + queue.size() + "!");
			return;
		}
		
		try
		{
			if (queue.isEmpty())
				return;
			
			for (final SteppingScheduledFuture<?> sr : queue)
				if (!sr.isDone())
					sr.run();
		}
		finally
		{
			isRunning.set(false);
		}
	}
	
	/**
	 * Clear the queue of completed and canceled tasks.
	 */
	public void purge()
	{
		final LazyArrayList<SteppingScheduledFuture<?>> purge = LazyArrayList.newInstance();
		
		for (final SteppingScheduledFuture<?> sr : queue)
			if (sr.isDone())
				purge.add(sr);
			
		queue.removeAll(purge);
		
		LazyArrayList.recycle(purge);
	}
	
	public CharSequence getStats()
	{
		final StringBuilder list = new StringBuilder();
		
		final Map<String, MutableLong> stats = new TreeMap<>();
		int total = 0;
		int done = 0;
		
		for (final SteppingScheduledFuture<?> sr : queue)
		{
			if (sr.isDone())
			{
				done++;
				continue;
			}
			total++;
			MutableLong count = stats.get(sr.r.getClass().getName());
			if (count == null)
				stats.put(sr.r.getClass().getName(), count = new MutableLong(1L));
			else
				count.increment();
		}
		
		for (final Map.Entry<String, MutableLong> e : stats.entrySet())
			list.append("\t").append(e.getKey()).append(" : ").append(e.getValue().longValue()).append("\n");
		
		list.append("Scheduled: ....... ").append(total).append("\n");
		list.append("Done/Cancelled: .. ").append(done).append("\n");
		
		return list;
	}
}
