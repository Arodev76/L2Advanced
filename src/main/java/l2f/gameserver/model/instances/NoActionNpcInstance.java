package l2f.gameserver.model.instances;

import l2f.gameserver.model.Player;
import l2f.gameserver.templates.npc.NpcTemplate;

@Deprecated
public class NoActionNpcInstance extends NpcInstance
{
	public NoActionNpcInstance(final int objectID, final NpcTemplate template)
	{
		super(objectID, template);
	}

	@Override
	public void onAction(final Player player, final boolean dontMove)
	{
		player.sendActionFailed();
	}
}
