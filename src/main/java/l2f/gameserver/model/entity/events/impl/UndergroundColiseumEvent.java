package l2f.gameserver.model.entity.events.impl;

import java.util.Calendar;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import l2f.commons.collections.MultiValueSet;
import l2f.commons.threading.RunnableImpl;
import l2f.commons.time.cron.SchedulingPattern;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.events.GlobalEvent;

public class UndergroundColiseumEvent extends GlobalEvent
{
	private class Timer extends RunnableImpl
	{
		@Override
		public void runImpl() throws Exception
		{
			if (_registeredPlayers.size() < 2 || _battleEvent != null)
				return;

			Player player1 = _registeredPlayers.get(0);
			Player player2 = _registeredPlayers.get(1);

			_battleEvent = new UndergroundColiseumBattleEvent(player1, player2);
			_battleEvent.reCalcNextTime(false);
		}
	}

	private static final SchedulingPattern DATE_PATTERN = new SchedulingPattern("0 21 * * mon,sat,sun");

	private Calendar _startCalendar = Calendar.getInstance();
	private List<Player> _registeredPlayers = new CopyOnWriteArrayList<Player>();

	private final int _minLevel;
	private final int _maxLevel;

	private UndergroundColiseumBattleEvent _battleEvent;

	public UndergroundColiseumEvent(MultiValueSet<String> set)
	{
		super(set);
		_minLevel = set.getInteger("min_level");
		_maxLevel = set.getInteger("max_level");
	}

	@Override
	public void startEvent()
	{
		super.startEvent();
	}

	@Override
	public void stopEvent()
	{
		super.stopEvent();
	}

	@Override
	public void reCalcNextTime(boolean onInit)
	{
		clearActions();

		_startCalendar.setTimeInMillis(DATE_PATTERN.next(System.currentTimeMillis()));

		registerActions();
	}

	@Override
	protected long startTimeMillis()
	{
		return _startCalendar.getTimeInMillis();
	}

	public List<Player> getRegisteredPlayers()
	{
		return _registeredPlayers;
	}

	public int getMinLevel()
	{
		return _minLevel;
	}

	public int getMaxLevel()
	{
		return _maxLevel;
	}
}
