package l2f.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import l2f.gameserver.model.pledge.Alliance;
import l2f.gameserver.model.pledge.Clan;
import l2f.gameserver.model.pledge.SubUnit;
import l2f.gameserver.model.pledge.UnitMember;


public class PledgeShowMemberListAll extends L2GameServerPacket
{
	private int _clanObjectId, _clanCrestId, _level, _rank, _reputation, _allianceObjectId, _allianceCrestId;
	private int _hasCastle, _hasClanHall, _hasFortress, _atClanWar;
	private String _unitName, _leaderName, _allianceName;
	private int _pledgeType, _territorySide;
	private List<PledgePacketMember> _members;

	public PledgeShowMemberListAll(Clan clan, final SubUnit sub)
	{
		_pledgeType = sub.getType();
		_clanObjectId = clan.getClanId();
		_unitName = sub.getName();
		_leaderName = sub.getLeaderName();
		_clanCrestId = clan.getCrestId();
		_level = clan.getLevel();
		_hasCastle = clan.getCastle();
		_hasClanHall = clan.getHasHideout();
		_hasFortress = clan.getHasFortress();
		_rank = clan.getRank();
		_reputation = clan.getReputationScore();
		_atClanWar = clan.isAtWarOrUnderAttack();
		_territorySide = clan.getWarDominion();

		Alliance ally = clan.getAlliance();

		if (ally != null)
		{
			_allianceObjectId = ally.getAllyId();
			_allianceName = ally.getAllyName();
			_allianceCrestId = ally.getAllyCrestId();
		}

		_members = new ArrayList<PledgePacketMember>(sub.size());

		for (UnitMember m : sub.getUnitMembers())
			_members.add(new PledgePacketMember(m));
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0x5a);

		writeD(_pledgeType == Clan.SUBUNIT_MAIN_CLAN ? 0 : 1);
		writeD(_clanObjectId);
		writeD(_pledgeType);
		writeS(_unitName);
		writeS(_leaderName);
		writeD(_clanCrestId); // crest id .. is used again
		writeD(_level);
		writeD(_hasCastle);
		writeD(_hasClanHall);
		writeD(_hasFortress);
		writeD(_rank);
		writeD(_reputation);
		writeD(0x00);
		writeD(0x00);
		writeD(_allianceObjectId);
		writeS(_allianceName);
		writeD(_allianceCrestId);
		writeD(_atClanWar);
		writeD(_territorySide);//territory Id

		writeD(_members.size());
		for (PledgePacketMember m : _members)
		{
			writeS(m._name);
			writeD(m._level);
			writeD(m._classId);
			writeD(m._sex);
			writeD(m._race);
			writeD(m._online);
			writeD(m._hasSponsor ? 1 : 0);
		}
	}

	private class PledgePacketMember
	{
		private String _name;
		private int _level;
		private int _classId;
		private int _sex;
		private int _race;
		private int _online;
		private boolean _hasSponsor;

		public PledgePacketMember(UnitMember m)
		{
			_name = m.getName();
			_level = m.getLevel();
			_classId = m.getClassId();
			_sex = m.getSex();
			_race = 0; //TODO m.getRace()
			_online = m.isOnline() ? m.getObjectId() : 0;
			_hasSponsor = m.getSponsor() != 0;
		}
	}
}