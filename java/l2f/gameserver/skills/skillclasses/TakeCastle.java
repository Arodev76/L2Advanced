package l2f.gameserver.skills.skillclasses;

import l2f.gameserver.model.*;
import l2f.gameserver.model.Zone.ZoneType;
import l2f.gameserver.model.entity.events.impl.CastleSiegeEvent;
import l2f.gameserver.network.serverpackets.Say2;
import l2f.gameserver.network.serverpackets.SystemMessage2;
import l2f.gameserver.network.serverpackets.components.ChatType;
import l2f.gameserver.network.serverpackets.components.IStaticPacket;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.templates.StatsSet;

import java.util.List;

public class TakeCastle extends Skill
{
	public TakeCastle(StatsSet set)
	{
		super(set);
	}

	@Override
	public boolean checkCondition(Creature activeChar, Creature target, boolean forceUse, boolean dontMove, boolean first)
	{
		Zone siegeZone = target.getZone(ZoneType.SIEGE);

		if (!super.checkCondition(activeChar, target, forceUse, dontMove, first))
			return false;

		if (activeChar == null || !activeChar.isPlayer())
			return false;

		Player player = (Player) activeChar;
		if (player.getClan() == null || !player.isClanLeader())
		{
			activeChar.sendPacket(new SystemMessage2(SystemMsg.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(this));
			return false;
		}

		CastleSiegeEvent siegeEvent = player.getEvent(CastleSiegeEvent.class);
		if (siegeEvent == null)
		{
			activeChar.sendPacket(new SystemMessage2(SystemMsg.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(this));
			return false;
		}

		if (siegeEvent.getSiegeClan(CastleSiegeEvent.ATTACKERS, player.getClan()) == null || siegeEvent.getResidence().getId() != siegeZone.getParams().getInteger("residence", 0))
		{
			activeChar.sendPacket(new SystemMessage2(SystemMsg.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(this));
			return false;
		}

		if (player.isMounted())
		{
			activeChar.sendPacket(new SystemMessage2(SystemMsg.S1_CANNOT_BE_USED_DUE_TO_UNSUITABLE_TERMS).addSkillName(this));
			return false;
		}

		if (!player.isInRangeZ(target, 185))
		{
			player.sendPacket(SystemMsg.YOUR_TARGET_IS_OUT_OF_RANGE);
			return false;
		}

		if (first)
			siegeEvent.broadcastTo(SystemMsg.THE_OPPOSING_CLAN_HAS_STARTED_TO_ENGRAVE_THE_HOLY_ARTIFACT, CastleSiegeEvent.DEFENDERS);

		return true;
	}

	@Override
	public void useSkill(Creature activeChar, List<Creature> targets)
	{
		for (Creature target : targets)
			if (target != null)
			{
				if (!target.isArtefact())
					continue;
				Player player = (Player) activeChar;

				CastleSiegeEvent siegeEvent = player.getEvent(CastleSiegeEvent.class);
				if (siegeEvent != null)
				{
					IStaticPacket lostPacket = siegeEvent.getResidence().getOwner() != null ? new Say2(activeChar.getObjectId(), ChatType.CRITICAL_ANNOUNCE, siegeEvent.getResidence().getName() + " Castle", "Clan "+siegeEvent.getResidence().getOwner().getName()+" has lost " + siegeEvent.getResidence().getName() + " Castle") : null;
					IStaticPacket winPacket = new Say2(activeChar.getObjectId(), ChatType.CRITICAL_ANNOUNCE, siegeEvent.getResidence().getName()+" Castle", "Clan "+player.getClan().getName() + " has taken " + siegeEvent.getResidence().getName()+" Castle");
					for (Player playerToSeeMsg : GameObjectsStorage.getAllPlayersForIterate())
					{
						if (lostPacket != null)
							playerToSeeMsg.sendPacket(lostPacket);
						playerToSeeMsg.sendPacket(winPacket);
					}
					siegeEvent.processStep(player.getClan());
				}
			}
	}
}