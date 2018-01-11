package zones;

import l2f.gameserver.listener.zone.OnZoneEnterLeaveListener;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Zone;
import l2f.gameserver.network.serverpackets.components.CustomMessage;
import l2f.gameserver.scripts.ScriptFile;
import l2f.gameserver.utils.Location;
import l2f.gameserver.utils.ReflectionUtils;

public class EpicZone implements ScriptFile
{
	private static ZoneListener _zoneListener;

	@Override
	public void onLoad()
	{
		_zoneListener = new ZoneListener();
		Zone zone = ReflectionUtils.getZone("[queen_ant_epic]");
		zone.addListener(_zoneListener);
		
		Zone zone1 = ReflectionUtils.getZone("[fix_exploit_beleth]");
		zone1.addListener(_zoneListener);
		
		Zone zone2 = ReflectionUtils.getZone("[fix_exploit_beleth_2]");
		zone2.addListener(_zoneListener);
		
	}

	@Override
	public void onReload()
	{

	}

	@Override
	public void onShutdown()
	{

	}

	public class ZoneListener implements OnZoneEnterLeaveListener
	{
		@Override
		public void onZoneEnter(Zone zone, Creature cha)
		{
			if (zone.getParams() == null || !cha.isPlayable() || cha.getPlayer().isGM())
				return;
			// Synerge - Added protection to only allow x max class level to certain zones if set. It also checks if player has subclasses, that should be the same as having 3rd class
			final int maxClassLvl = zone.getParams().getInteger("maxClassLevelAllowed", -1);
			if (cha.getLevel() > zone.getParams().getInteger("levelLimit") 
				|| (maxClassLvl >= 0 && cha.getPlayer().getClassId().getLevel() > maxClassLvl)
				|| (maxClassLvl >= 0 && cha.getPlayer().isSubClassActive()))
			{
					cha.getPlayer().sendMessage(new CustomMessage("scripts.zones.epic.banishMsg", cha.getPlayer()));
				cha.teleToLocation(Location.parseLoc(zone.getParams().getString("tele")));
			}
		}

		@Override
		public void onZoneLeave(Zone zone, Creature cha)
		{

		}
	}
}

