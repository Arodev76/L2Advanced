package l2f.gameserver.network.clientpackets;

import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.base.EnchantSkillLearn;
import l2f.gameserver.network.serverpackets.ExEnchantSkillInfo;
import l2f.gameserver.network.serverpackets.ExEnchantSkillResult;
import l2f.gameserver.network.serverpackets.SkillList;
import l2f.gameserver.network.serverpackets.SystemMessage;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.scripts.Functions;
import l2f.gameserver.tables.SkillTable;
import l2f.gameserver.tables.SkillTreeTable;
import l2f.gameserver.utils.Log;

public final class RequestExEnchantSkillUntrain extends L2GameClientPacket
{
	private int _skillId;
	private int _skillLvl;

	@Override
	protected void readImpl()
	{
		_skillId = readD();
		_skillLvl = readD();
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		if (activeChar.getTransformation() != 0)
		{
			activeChar.sendMessage("You must leave transformation mode first.");
			return;
		}

		if (activeChar.getLevel() < 76 || activeChar.getClassId().getLevel() < 4)
		{
			activeChar.sendMessage("You must have 3rd class change quest completed.");
			return;
		}

		int oldSkillLevel = activeChar.getSkillDisplayLevel(_skillId);
		if (oldSkillLevel == -1)
			return;

		if (_skillLvl != (oldSkillLevel - 1) || (_skillLvl / 100) != (oldSkillLevel / 100))
			return;

		EnchantSkillLearn sl = SkillTreeTable.getSkillEnchant(_skillId, oldSkillLevel);
		if (sl == null)
			return;

		Skill newSkill;

		if (_skillLvl % 100 == 0)
		{
			_skillLvl = sl.getBaseLevel();
			newSkill = SkillTable.getInstance().getInfo(_skillId, _skillLvl);
		}
		else
			newSkill = SkillTable.getInstance().getInfo(_skillId, SkillTreeTable.convertEnchantLevel(sl.getBaseLevel(), _skillLvl, sl.getMaxLevel()));

		if (newSkill == null)
			return;

		if (Functions.getItemCount(activeChar, SkillTreeTable.UNTRAIN_ENCHANT_BOOK) == 0)
		{
			activeChar.sendPacket(SystemMsg.YOU_DO_NOT_HAVE_ALL_OF_THE_ITEMS_NEEDED_TO_ENCHANT_THAT_SKILL);
			return;
		}

		Functions.removeItem(activeChar, SkillTreeTable.UNTRAIN_ENCHANT_BOOK, 1, "SkillEnchantUntrain");

		activeChar.addExpAndSp(0, sl.getCost()[1] * sl.getCostMult());
		activeChar.addSkill(newSkill, true);

		if (_skillLvl > 100)
		{
			SystemMessage sm = new SystemMessage(SystemMessage.Untrain_of_enchant_skill_was_successful_Current_level_of_enchant_skill_S1_has_been_decreased_by_1);
			sm.addSkillName(_skillId, _skillLvl);
			activeChar.sendPacket(sm);
		}
		else
		{
			SystemMessage sm = new SystemMessage(SystemMessage.Untrain_of_enchant_skill_was_successful_Current_level_of_enchant_skill_S1_became_0_and_enchant_skill_will_be_initialized);
			sm.addSkillName(_skillId, _skillLvl);
			activeChar.sendPacket(sm);
		}

		Log.add(activeChar.getName() + "|Successfully untranes|" + _skillId + "|to+" + _skillLvl + "|---", "enchant_skills");

		activeChar.sendPacket(new ExEnchantSkillInfo(_skillId, newSkill.getDisplayLevel()), ExEnchantSkillResult.SUCCESS, new SkillList(activeChar));
		RequestExEnchantSkill.updateSkillShortcuts(activeChar, _skillId, _skillLvl);
	}
}