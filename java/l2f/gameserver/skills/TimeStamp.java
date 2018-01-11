package l2f.gameserver.skills;

import l2f.gameserver.model.Skill;

public class TimeStamp
{
	private final int _id;
	private final int _level;
	private final long _reuse;
	private final long _endTime;

	public TimeStamp(int id, long endTime, long reuse)
	{
		_id = id;
		_level = 0;
		_reuse = reuse;
		_endTime = endTime;
	}

	public TimeStamp(Skill skill, long reuse)
	{
		this(skill, System.currentTimeMillis() + reuse, reuse);
	}

	public TimeStamp(Skill skill, long endTime, long reuse)
	{
		_id = skill.getId();
		_level = skill.getLevel();
		_reuse = reuse;
		_endTime = endTime;
	}

	public long getReuseBasic()
	{
		if (_reuse == 0)
			return getReuseCurrent();
		return _reuse;
	}

	public long getReuseCurrent()
	{
		return Math.max(_endTime - System.currentTimeMillis(), 0);
	}

	public long getEndTime()
	{
		return _endTime;
	}

	public boolean hasNotPassed()
	{
		return System.currentTimeMillis() < _endTime;
	}

	public int getId()
	{
		return _id;
	}

	public int getLevel()
	{
		return _level;
	}
}