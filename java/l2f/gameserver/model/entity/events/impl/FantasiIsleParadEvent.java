package l2f.gameserver.model.entity.events.impl;

import l2f.commons.collections.MultiValueSet;
import l2f.gameserver.model.entity.events.GlobalEvent;

public class FantasiIsleParadEvent extends GlobalEvent
{
	public FantasiIsleParadEvent(MultiValueSet<String> set)
	{
		super(set);
	}

	@Override
	public void reCalcNextTime(boolean onStart)
	{
		clearActions();
	}

	@Override
	protected long startTimeMillis()
	{
		return System.currentTimeMillis() + 30000L;
	}
}