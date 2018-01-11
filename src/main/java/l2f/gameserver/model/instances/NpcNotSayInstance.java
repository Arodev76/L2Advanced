package l2f.gameserver.model.instances;

import l2f.gameserver.templates.npc.NpcTemplate;

public class NpcNotSayInstance extends NpcInstance
{
	public NpcNotSayInstance(final int objectID, final NpcTemplate template)
	{
		super(objectID, template);
		setHasChatWindow(false);
	}
}
