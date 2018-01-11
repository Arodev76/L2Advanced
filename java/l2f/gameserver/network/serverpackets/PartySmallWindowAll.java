package l2f.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import l2f.gameserver.model.Party;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Summon;


/**
 * format   ddd+[dSddddddddddddd{ddSddddd}]
 */
public class PartySmallWindowAll extends L2GameServerPacket
{
	private int leaderId, loot;
	private List<PartySmallWindowMemberInfo> members = new ArrayList<PartySmallWindowMemberInfo>();

	public PartySmallWindowAll(Party party, Player exclude)
	{
		leaderId = party.getLeader().getObjectId();
		loot = party.getLootDistribution();

		for (Player member : party.getMembers())
			if (member != exclude)
				members.add(new PartySmallWindowMemberInfo(member));
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x4E);
		writeD(leaderId); // c3 party leader id
		writeD(loot); //c3 party loot type (0,1,2,....)
		writeD(members.size());
		for (PartySmallWindowMemberInfo member : members)
		{
			writeD(member._id);
			writeS(member._name);
			writeD(member.curCp);
			writeD(member.maxCp);
			writeD(member.curHp);
			writeD(member.maxHp);
			writeD(member.curMp);
			writeD(member.maxMp);
			writeD(member.level);
			writeD(member.class_id);
			writeD(0);//writeD(0x01); ??
			writeD(member.race_id);
			writeD(0);
			writeD(0);

			if (member.pet_id != 0)
			{
				writeD(member.pet_id);
				writeD(member.pet_NpcId);
				writeS(member.pet_Name);
				writeD(member.pet_curHp);
				writeD(member.pet_maxHp);
				writeD(member.pet_curMp);
				writeD(member.pet_maxMp);
				writeD(member.pet_level);
			}
			else
				writeD(0);
		}
	}

	public static class PartySmallWindowMemberInfo
	{
		public String _name, pet_Name;
		public int _id, curCp, maxCp, curHp, maxHp, curMp, maxMp, level, class_id, race_id;
		public int pet_id, pet_NpcId, pet_curHp, pet_maxHp, pet_curMp, pet_maxMp, pet_level;

		public PartySmallWindowMemberInfo(Player member)
		{
			_name = member.getName();
			_id = member.getObjectId();
			curCp = (int) member.getCurrentCp();
			maxCp = member.getMaxCp();
			curHp = (int) member.getCurrentHp();
			maxHp = member.getMaxHp();
			curMp = (int) member.getCurrentMp();
			maxMp = member.getMaxMp();
			level = member.getLevel();
			class_id = member.getClassId().getId();
			race_id = member.getRace().ordinal();

			Summon pet = member.getPet();
			if (pet != null)
			{
				pet_id = pet.getObjectId();
				pet_NpcId = pet.getNpcId() + 1000000;
				pet_Name = pet.getName();
				pet_curHp = (int) pet.getCurrentHp();
				pet_maxHp = pet.getMaxHp();
				pet_curMp = (int) pet.getCurrentMp();
				pet_maxMp = pet.getMaxMp();
				pet_level = pet.getLevel();
			}
			else
				pet_id = 0;
		}
	}
}