package l2f.gameserver.model.entity.events.actions;

import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.events.EventAction;
import l2f.gameserver.model.entity.events.GlobalEvent;

public class GiveItemAction implements EventAction
{
	private int _itemId;
	private long _count;

	public GiveItemAction(int itemId, long count)
	{
		_itemId = itemId;
		_count = count;
	}

	@Override
	public void call(GlobalEvent event)
	{
		for (Player player : event.itemObtainPlayers())
			event.giveItem(player, _itemId, _count);
	}
}
