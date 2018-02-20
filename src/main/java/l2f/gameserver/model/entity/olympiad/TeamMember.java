package l2f.gameserver.model.entity.olympiad;

import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import l2f.commons.configuration.Config;
import l2f.gameserver.instancemanager.ReflectionManager;
import l2f.gameserver.model.Effect;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.Summon;
import l2f.gameserver.model.base.TeamType;
import l2f.gameserver.model.entity.Hero;
import l2f.gameserver.model.entity.Reflection;
import l2f.gameserver.model.entity.events.impl.DuelEvent;
import l2f.gameserver.model.items.ItemInstance;
import l2f.gameserver.network.serverpackets.ExAutoSoulShot;
import l2f.gameserver.network.serverpackets.ExOlympiadMatchEnd;
import l2f.gameserver.network.serverpackets.ExOlympiadMode;
import l2f.gameserver.network.serverpackets.Revive;
import l2f.gameserver.network.serverpackets.SkillCoolTime;
import l2f.gameserver.network.serverpackets.SkillList;
import l2f.gameserver.network.serverpackets.SystemMessage;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.skills.EffectType;
import l2f.gameserver.skills.TimeStamp;
import l2f.gameserver.taskmanager.CancelTaskManager;
import l2f.gameserver.templates.InstantZone;
import l2f.gameserver.templates.StatsSet;
import l2f.gameserver.utils.FixEnchantOlympiad;
import l2f.gameserver.utils.Location;
import l2f.gameserver.utils.Log;

public class TeamMember
{
	private String _name = StringUtils.EMPTY;
	private String _clanName = StringUtils.EMPTY;
	private int _classId;
	private double _damage;
	private boolean _isDead;

	private final int _objId;
	private final OlympiadGame _game;
	private final CompType _type;
	private final int _side;

	private Player _player;
	private Location _returnLoc = null;

	public boolean isDead()
	{
		return _isDead;
	}

	public void doDie()
	{
		_isDead = true;
	}

	public TeamMember(int obj_id, String name, Player player, OlympiadGame game, int side)
	{
		_objId = obj_id;
		_name = name;
		_game = game;
		_type = game.getType();
		_side = side;

		_player = player;
		if (_player == null)
			return;

		_clanName = player.getClan() == null ? StringUtils.EMPTY : player.getClan().getName();
		_classId = player.getActiveClassId();

		player.setOlympiadSide(side);
		player.setOlympiadGame(game);
	}

	public StatsSet getStat()
	{
		return Olympiad._nobles.get(_objId);
	}

	public void incGameCount()
	{
		StatsSet set = getStat();
		switch (_type)
		{
			case TEAM:
				set.set(Olympiad.GAME_TEAM_COUNT, set.getInteger(Olympiad.GAME_TEAM_COUNT) + 1);
				break;
			case CLASSED:
				set.set(Olympiad.GAME_CLASSES_COUNT, set.getInteger(Olympiad.GAME_CLASSES_COUNT) + 1);
				break;
			case NON_CLASSED:
				set.set(Olympiad.GAME_NOCLASSES_COUNT, set.getInteger(Olympiad.GAME_NOCLASSES_COUNT) + 1);
				break;
		}
	}

	public void takePointsForCrash()
	{
		if(!checkPlayer())
		{
			StatsSet stat = getStat();
			int points = stat.getInteger(Olympiad.POINTS);
			int diff = Math.min(OlympiadGame.MAX_POINTS_LOOSE, points / _type.getLooseMult());
			stat.set(Olympiad.POINTS, points - diff);
			Log.add("Olympiad Result: " + _name + " lost " + diff + " points for crash", "olympiad");

			// TODO: Снести подробный лог после исправления беспричинного отъёма очков.
			Player player = _player;
			if (player == null)
				Log.add("Olympiad info: " + _name + " crashed coz player == null", "olympiad");
			else
			{
				if (player.isLogoutStarted())
					Log.add("Olympiad info: " + _name + " crashed coz player.isLogoutStarted()", "olympiad");
				if (!player.isOnline())
					Log.add("Olympiad info: " + _name + " crashed coz !player.isOnline()", "olympiad");
				if (!player.isConnected())
					Log.add("Olympiad info: " + _name + " crashed coz !player.isOnline()", "olympiad");
				if (player.getOlympiadGame() == null)
					Log.add("Olympiad info: " + _name + " crashed coz player.getOlympiadGame() == null", "olympiad");
				if (player.getOlympiadObserveGame() != null)
					Log.add("Olympiad info: " + _name + " crashed coz player.getOlympiadObserveGame() != null", "olympiad");
			}
		}
	}

	public boolean checkPlayer()
	{
		Player player = _player;
		if(player == null || player.isLogoutStarted() || player.getOlympiadGame() == null || player.isInObserverMode())
			return false;
		return true;
	}

	public void portPlayerToArena()
	{
		Player player = _player;
		if(!checkPlayer() || player.isTeleporting())
		{
			_player = null;
			return;
		}

		//Fix for Cancel exploit
		CancelTaskManager.getInstance().cancelPlayerTasks(player);

		DuelEvent duel = player.getEvent(DuelEvent.class);
		if (duel != null)
			duel.abortDuel(player);

		_returnLoc = player._stablePoint == null ? player.getReflection().getReturnLoc() == null ? player.getLoc() : player.getReflection().getReturnLoc() : player._stablePoint;

		if (player.isDead())
			player.setPendingRevive(true);
		if (player.isSitting())
			player.standUp();
		if (player.isRiding() || player.isFlying())
			player.dismount();

		player.setTarget(null);
		player.setIsInOlympiadMode(true);

		player.leaveParty();

		Reflection ref = _game.getReflection();
		InstantZone instantZone = ref.getInstancedZone();

		Location tele = Location.findPointToStay(instantZone.getTeleportCoords().get(_side - 1), 50, 50, ref.getGeoIndex());

		player._stablePoint = _returnLoc;
		player.teleToLocation(tele, ref);

		if (_type == CompType.TEAM)
			player.setTeam(_side == 1 ? TeamType.BLUE : TeamType.RED);

		player.sendPacket(new ExOlympiadMode(_side));
	}

	public void portPlayerBack()
	{
		Player player = _player;
		if (player == null)
			return;

		player.setOlympiadSide(-1); // эти параметры ставятся в конструкторе и должны очищаться всегда
		player.setOlympiadGame(null);

		if (_returnLoc == null) // игрока не портнуло на стадион
			return;

		player.setIsInOlympiadMode(false);
		player.setOlympiadCompStarted(false);
		if (_type == CompType.TEAM)
			player.setTeam(TeamType.NONE);

		removeBuffs(true);

		// Возвращаем клановые скиллы если репутация положительная.
		if (player.getClan() != null && player.getClan().getReputationScore() >= 0)
			player.getClan().enableSkills(player);

		// Add Hero Skills
		if(player.isHero())
			Hero.addSkills(player);

		if (player.isDead())
		{
			player.setCurrentHp(player.getMaxHp(), true);
			player.broadcastPacket(new Revive(player));
		}
		else
			player.setCurrentHp(player.getMaxHp(), false);

		player.setCurrentCp(player.getMaxCp());
		player.setCurrentMp(player.getMaxMp());

		// Обновляем скилл лист, после добавления скилов
		player.sendPacket(new SkillList(player));
		player.sendPacket(new ExOlympiadMode(0));
		player.sendPacket(new ExOlympiadMatchEnd());

		player._stablePoint = null;
		player.teleToLocation(_returnLoc, ReflectionManager.DEFAULT);

		// Восстанавливаем точку итемов
		if (Config.OLY_ENCH_LIMIT_ENABLE && player.getVar("EnItemOlyRec") != null)
		{
			FixEnchantOlympiad.restoreEnchantItemsOly(player);
		}
	}

	public void preparePlayer()
	{
		Player player = _player;

		if (player == null)
			return;

		if (player.isInObserverMode())
			if(player.isInOlympiadObserverMode())
				player.leaveOlympiadObserverMode(true);
			else
				player.leaveObserverMode();

		// Un activate clan skills
		if (player.getClan() != null)
			player.getClan().disableSkills(player);

		// Remove Hero Skills
		if (player.isHero() || player.FakeHeroSkill())
			Hero.removeSkills(player);

		// Abort casting if player casting
		if (player.isCastingNow())
			player.abortCast(true, true);

		// Удаляем баффы и чужие кубики
		removeBuffs(true);

		// unsummon agathion
		if (player.getAgathionId() > 0)
			player.setAgathion(0);

		// Сброс кулдауна всех скилов, время отката которых меньше 15 минут
		for (TimeStamp sts : player.getSkillReuses())
		{
			if (sts == null)
				continue;

			Skill skill = player.getKnownSkill(sts.getId());
			if(skill == null || skill.getLevel() != sts.getLevel())
				continue;

			if(skill.getReuseDelay(player) <= 15 * 60000L)
				player.enableSkill(skill);
		}

		// Обновляем скилл лист, после удаления скилов
		player.sendPacket(new SkillList(player));
		// Обновляем куллдаун, после сброса
		player.sendPacket(new SkillCoolTime(player));

		// Remove Hero weapons
		player.getInventory().refreshEquip();

		// remove bsps/sps/ss automation
		Set<Integer> activeSoulShots = player.getAutoSoulShot();
		for (int itemId : activeSoulShots)
		{
			player.removeAutoSoulShot(itemId);
			player.sendPacket(new ExAutoSoulShot(itemId, false));
		}

		// Разряжаем заряженные соул и спирит шоты
		ItemInstance weapon = player.getActiveWeaponInstance();
		if (weapon != null)
		{
			weapon.setChargedSpiritshot(ItemInstance.CHARGED_NONE);
			weapon.setChargedSoulshot(ItemInstance.CHARGED_NONE);
		}

		// Проверяем точку итемов
		if (Config.OLY_ENCH_LIMIT_ENABLE)
		{
			FixEnchantOlympiad.storeEnchantItemsOly(player);
		}

		heal();
	}

	public void startComp()
	{
		Player player = _player;
		if(player == null)
			return;
		_player.setOlympiadCompStarted(true);
	}

	public void stopComp()
	{
		Player player = _player;
		if(player == null)
			return;
		_player.setOlympiadCompStarted(false);
	}

	public void heal()
	{
		Player player = _player;
		if(player == null)
			return;

		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
		player.broadcastUserInfo(true);

	}

	public void removeBuffs(boolean fromSummon)
	{
		Player player = _player;
		if(player == null)
			return;

		player.abortAttack(true, false);
		if(player.isCastingNow())
			player.abortCast(true, true);

		for(Effect e : player.getEffectList().getAllEffects())
		{
			if (e == null)
				continue;
			if (e.getEffectType() == EffectType.Cubic && player.getSkillLevel(e.getSkill().getId()) > 0)
				continue;
			if (e.getSkill().isToggle())
				continue;
			player.sendPacket(new SystemMessage(SystemMsg.THE_EFFECT_OF_S1_HAS_BEEN_REMOVED).addSkillName(e.getSkill().getId(), e.getSkill().getLevel()));
			e.exit();
		}

		if (player.isFrozen())
			player.stopFrozen();

		Summon servitor = player.getPet();
		if(servitor != null)
		{
			servitor.abortAttack(true, false);
			if(servitor.isCastingNow())
				servitor.abortCast(true, true);

			if (fromSummon)
			{
				if(servitor.isPet())
					servitor.unSummon();
				else
					servitor.getEffectList().stopAllEffects();
			}

			if (servitor.isFrozen())
				servitor.stopFrozen();
		}
	}

	public void saveNobleData()
	{
		OlympiadDatabase.saveNobleData(_objId);
	}

	public void logout()
	{
		_player = null;
	}

	public Player getPlayer()
	{
		return _player;
	}

	public String getName()
	{
		return _name;
	}

	public void addDamage(double d)
	{
		_damage += d;
	}

	public double getDamage()
	{
		return _damage;
	}

	public String getClanName()
	{
		return _clanName;
	}

	public int getClassId()
	{
		return _classId;
	}

	public int getObjectId()
	{
		return _objId;
	}
}