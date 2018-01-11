package l2f.gameserver.taskmanager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2f.commons.threading.RunnableImpl;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.model.Effect;
import l2f.gameserver.model.Playable;
import l2f.gameserver.network.serverpackets.ExShowScreenMessage;
import l2f.gameserver.network.serverpackets.ExShowScreenMessage.ScreenMessageAlign;

public class CancelTaskManager 
{
	private List<DispelClass> _taskTimes = new CopyOnWriteArrayList<>();
	private static CancelTaskManager _instance;
	
	public CancelTaskManager()
	{
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new ManageTasks(), 0, 500);
	}
	
	public void addNewCancelTask(Playable playable, List<Effect> buffs)
	{
		int buffCancelTime = 45;
		playable.sendMessage("You will get your buffs back in "+buffCancelTime+" secs.");
		
		_taskTimes.add(new DispelClass((System.currentTimeMillis()+(buffCancelTime*1000)), playable, buffs));
	}
	
	private class DispelClass
	{
		Playable _cancelled;
		List<Effect> _effects;
		long _time;
		private DispelClass(long time, Playable character, List<Effect> effects)
		{
			_time = time;
			_cancelled = character;
			_effects = effects;
		}
	}
	
	public void cancelPlayerTasks(Playable playable)
	{
		for (DispelClass task : _taskTimes)
			if (task != null && task._cancelled.equals(playable))
			{
				_taskTimes.add(task);
				return;
			}
	}
	
	private class ManageTasks extends RunnableImpl
	{
		@Override
		public void runImpl() 
		{
			long current = System.currentTimeMillis();
			List<DispelClass> toRemove = new ArrayList<>();
			
			for (DispelClass task : _taskTimes)
			{
				if (task._time > current)
					continue;
				
				for (Effect effect : task._effects)
				{
					task._cancelled.getEffectList().addEffect(effect);
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				task._cancelled.updateStats();
				task._cancelled.updateEffectIcons();
				
				toRemove.add(task);
				
				task._cancelled.sendPacket(new ExShowScreenMessage("Cancelled buffs returned!", 2000, ScreenMessageAlign.TOP_LEFT, false));
				task._cancelled.sendMessage("Cancelled buffs returned!");
			}
			
			for (DispelClass task : toRemove)
				_taskTimes.remove(task);
		}
	}
	
	public static CancelTaskManager getInstance()
	{
		if (_instance == null)
			_instance = new CancelTaskManager();
		return _instance;
	}
}
