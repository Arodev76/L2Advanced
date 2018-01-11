package l2f.gameserver.taskmanager;

import java.util.concurrent.Future;

import l2f.commons.threading.RunnableImpl;
import l2f.commons.threading.SteppingRunnableQueueManager;
import l2f.commons.util.Rnd;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.model.Player;

public class AutoSaveManager extends SteppingRunnableQueueManager
{
	private static final AutoSaveManager _instance = new AutoSaveManager();

	public static final AutoSaveManager getInstance()
	{
		return _instance;
	}

	private AutoSaveManager()
	{
		super(10000L);
		ThreadPoolManager.getInstance().scheduleAtFixedRate(this, 10000L, 10000L);
		//Очистка каждые 60 секунд
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new RunnableImpl()
		{
			@Override
			public void runImpl()
			{
				AutoSaveManager.this.purge();
			}

		}, 60000L, 60000L);
	}

	public Future<?> addAutoSaveTask(final Player player)
	{
		long delay = Rnd.get(180, 360) * 1000L;

		return scheduleAtFixedRate(new RunnableImpl()
		{
			@Override
			public void runImpl()
			{
				if (player == null || !player.isOnline())
					return;

				player.store(true);
			}

		}, delay, delay);
	}
}