package npc.model;

import l2f.gameserver.model.Player;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.templates.npc.NpcTemplate;
import instances.CrystalCaverns;

/**
 * @author pchayka
 */
public class SteamCorridorControllerInstance extends NpcInstance
{
	private static final long serialVersionUID = -1L;
	public SteamCorridorControllerInstance(int objectId, NpcTemplate template)
	{
		super(objectId, template);
	}

	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (!canBypassCheck(player, this))
			return;

		if (command.equalsIgnoreCase("move_next"))
		{
			if (getReflection().getInstancedZoneId() == 10)
				((CrystalCaverns) getReflection()).notifyNextLevel(this);
		}
		else
			super.onBypassFeedback(player, command);
	}
}
