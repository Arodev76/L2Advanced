package l2f.gameserver.model.entity.events.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import l2f.commons.collections.MultiValueSet;
import l2f.commons.dao.JdbcEntityState;
import l2f.commons.util.Rnd;
import l2f.gameserver.dao.DominionRewardDAO;
import l2f.gameserver.dao.SiegeClanDAO;
import l2f.gameserver.dao.SiegePlayerDAO;
import l2f.gameserver.data.xml.holder.EventHolder;
import l2f.gameserver.data.xml.holder.ResidenceHolder;
import l2f.gameserver.instancemanager.QuestManager;
import l2f.gameserver.listener.actor.OnDeathListener;
import l2f.gameserver.listener.actor.OnKillListener;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.Zone;
import l2f.gameserver.model.base.RestartType;
import l2f.gameserver.model.entity.events.EventType;
import l2f.gameserver.model.entity.events.objects.DoorObject;
import l2f.gameserver.model.entity.events.objects.SiegeClanObject;
import l2f.gameserver.model.entity.events.objects.ZoneObject;
import l2f.gameserver.model.entity.residence.Dominion;
import l2f.gameserver.model.entity.residence.Residence;
import l2f.gameserver.model.instances.DoorInstance;
import l2f.gameserver.model.pledge.Clan;
import l2f.gameserver.model.quest.Quest;
import l2f.gameserver.model.quest.QuestState;
import l2f.gameserver.network.serverpackets.ExDominionWarEnd;
import l2f.gameserver.network.serverpackets.L2GameServerPacket;
import l2f.gameserver.network.serverpackets.PledgeShowInfoUpdate;
import l2f.gameserver.network.serverpackets.RelationChanged;
import l2f.gameserver.network.serverpackets.components.IStaticPacket;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.templates.DoorTemplate;
import l2f.gameserver.utils.Location;

import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.CHashIntObjectMap;

/**
 * @author VISTALL
 * @date 15:14/14.02.2011
 */
public class DominionSiegeEvent extends SiegeEvent<Dominion, SiegeClanObject>
{
	public class DoorDeathListener implements OnDeathListener
	{
		@Override
		public void onDeath(Creature actor, Creature killer)
		{
			if (!isInProgress())
				return;

			DoorInstance door = (DoorInstance) actor;
			if (door.getDoorType() == DoorTemplate.DoorType.WALL)
				return;

			Player player = killer.getPlayer();
			if (player != null)
				player.sendPacket(SystemMsg.THE_CASTLE_GATE_HAS_BEEN_DESTROYED);

			Clan owner = getResidence().getOwner();
			if (owner != null && owner.getLeader().isOnline())
				owner.getLeader().getPlayer().sendPacket(SystemMsg.THE_CASTLE_GATE_HAS_BEEN_DESTROYED);
		}
	}

	public class KillListener implements OnKillListener
	{
		@Override
		public void onKill(Creature actor, Creature victim)
		{
			Player winner = actor.getPlayer();

			if (winner == null || !victim.isPlayer() || winner.getLevel() < 40 || winner == victim || victim.getEvent(DominionSiegeEvent.class) == DominionSiegeEvent.this || !actor.isInZone(Zone.ZoneType.SIEGE) || !victim.isInZone(Zone.ZoneType.SIEGE))
				return;

			winner.setFame(winner.getFame() + Rnd.get(10, 20), DominionSiegeEvent.this.toString());

			addReward(winner, KILL_REWARD, 1);

			if (victim.getLevel() >= 61)
			{
				Quest q = _runnerEvent.getClassQuest(((Player)victim).getClassId());
				if (q == null)
					return;

				QuestState questState = winner.getQuestState(q.getClass());
				if (questState == null)
				{
					questState = q.newQuestState(winner, Quest.CREATED);
					q.notifyKill(((Player)victim), questState);
				}
			}
		}

		@Override
		public boolean ignorePetOrSummon()
		{
			return true;
		}
	}

	public static final int KILL_REWARD = 0;
	public static final int ONLINE_REWARD = 1;
	public static final int STATIC_BADGES = 2;
	//
	public static final int REWARD_MAX = 3;
	// object name
	public static final String ATTACKER_PLAYERS = "attacker_players";
	public static final String DEFENDER_PLAYERS = "defender_players";
	public static final String DISGUISE_PLAYERS = "disguise_players";
	public static final String TERRITORY_NPC = "territory_npc";
	public static final String CATAPULT = "catapult";
	public static final String CATAPULT_DOORS = "catapult_doors";

	private DominionSiegeRunnerEvent _runnerEvent;
	private Quest _forSakeQuest;

	private final IntObjectMap<int[]> _playersRewards = new CHashIntObjectMap<int[]>();

	public DominionSiegeEvent(MultiValueSet<String> set)
	{
		super(set);
		_killListener = new KillListener();
		_doorDeathListener = new DoorDeathListener();
	}

	@Override
	public void initEvent()
	{
		_runnerEvent = EventHolder.getInstance().getEvent(EventType.MAIN_EVENT, 1);

		super.initEvent();

		SiegeEvent castleSiegeEvent = getResidence().getCastle().getSiegeEvent();

		addObjects("mass_gatekeeper", castleSiegeEvent.getObjects("mass_gatekeeper"));
		addObjects(CastleSiegeEvent.CONTROL_TOWERS, castleSiegeEvent.getObjects(CastleSiegeEvent.CONTROL_TOWERS));

		List<DoorObject> doorObjects = getObjects(DOORS);
		for (DoorObject doorObject : doorObjects)
			doorObject.getDoor().addListener(_doorDeathListener);
	}

	@Override
	public void reCalcNextTime(boolean onInit)
	{
		//
	}

	@Override
	public void startEvent()
	{
		List<Dominion> registeredDominions = _runnerEvent.getRegisteredDominions();
		List<DominionSiegeEvent> dominions = new ArrayList<DominionSiegeEvent>(9);
		for (Dominion d : registeredDominions)
			if (d.getSiegeDate().getTimeInMillis() != 0 && d != getResidence())
				dominions.add(d.<DominionSiegeEvent>getSiegeEvent());

		SiegeClanObject ownerClan = new SiegeClanObject(DEFENDERS, getResidence().getOwner(), 0);

		addObject(DEFENDERS, ownerClan);

		for (DominionSiegeEvent d : dominions)
		{
			// овнер текущей територии, аттакер , в всех других
			d.addObject(ATTACKERS, ownerClan);

			// все наёмники, являются аттакерами для других територий
			List<Integer> defenderPlayers = d.getObjects(DEFENDER_PLAYERS);
			for (int i : defenderPlayers)
				addObject(ATTACKER_PLAYERS, i);

			List<SiegeClanObject> otherDefenders = d.getObjects(DEFENDERS);
			for (SiegeClanObject siegeClan : otherDefenders)
				if (siegeClan.getClan() != d.getResidence().getOwner())
					addObject(ATTACKERS, siegeClan);
		}

		int[] flags = getResidence().getFlags();
		if (flags.length > 0)
		{
			getResidence().removeSkills();
			getResidence().getOwner().broadcastToOnlineMembers(SystemMsg.THE_EFFECT_OF_TERRITORY_WARD_IS_DISAPPEARING);
		}

		SiegeClanDAO.getInstance().delete(getResidence());
		SiegePlayerDAO.getInstance();
		SiegePlayerDAO.delete(getResidence());

		for (int i : flags)
			spawnAction("ward_" + i, true);

		updateParticles(true);

		super.startEvent();
	}

	@Override
	public void stopEvent(boolean t)
	{
		getObjects(DISGUISE_PLAYERS).clear();

		int[] flags = getResidence().getFlags();
		for (int i : flags)
			spawnAction("ward_" + i, false);

		getResidence().rewardSkills();
		getResidence().setJdbcState(JdbcEntityState.UPDATED);
		getResidence().update();

		updateParticles(false);

		List<SiegeClanObject> defenders = getObjects(DEFENDERS);
		for (SiegeClanObject clan : defenders)
			clan.deleteFlag();

		super.stopEvent(t);

		DominionRewardDAO.getInstance().insert(getResidence());
		
		for (SiegeClanObject clan : defenders)
			clan.getClan().getOnlineMembers(0).stream().filter(plr -> plr != null).forEach(plr -> plr.getCounters().dominionSiegesWon++);
	}

	@Override
	public void loadSiegeClans()
	{
		addObjects(DEFENDERS, SiegeClanDAO.getInstance().load(getResidence(), DEFENDERS));
		addObjects(DEFENDER_PLAYERS, SiegePlayerDAO.getInstance().select(getResidence(), 0));

		DominionRewardDAO.getInstance().select(getResidence());
	}

	@Override
	public void updateParticles(boolean start, String... arg)
	{
		boolean battlefieldChat = _runnerEvent.isBattlefieldChatActive();
		List<SiegeClanObject> siegeClans = getObjects(DEFENDERS);
		for (SiegeClanObject s : siegeClans)
		{
			if (battlefieldChat)
			{
				s.getClan().setWarDominion(start ? getId() : 0);

				PledgeShowInfoUpdate packet = new PledgeShowInfoUpdate(s.getClan());
				for (Player player : s.getClan().getOnlineMembers(0))
				{
					player.sendPacket(packet);

					updatePlayer(player, start);
				}
			}
			else
			{
				for (Player player : s.getClan().getOnlineMembers(0))
					updatePlayer(player, start);
			}
		}

		List<Integer> players = getObjects(DEFENDER_PLAYERS);
		for (int i : players)
		{
			Player player = GameObjectsStorage.getPlayer(i);
			if (player != null)
				updatePlayer(player, start);
		}
	}

	public void updatePlayer(Player player, boolean start)
	{
		player.setBattlefieldChatId(_runnerEvent.isBattlefieldChatActive() ? getId() : 0);

		if (_runnerEvent.isBattlefieldChatActive())
		{
			if (start)
			{
				player.addEvent(this);
				// for starting the TW 6
				addReward(player, STATIC_BADGES, 5);
			}
			else
			{
				player.removeEvent(this);
				// beyond the end of the TW 6
				addReward(player, STATIC_BADGES, 5);

				player.getEffectList().stopEffect(Skill.SKILL_BATTLEFIELD_DEATH_SYNDROME);
				player.addExpAndSp(270000, 27000);
			}

			player.broadcastCharInfo();

			if (!start)
				player.sendPacket(ExDominionWarEnd.STATIC);

			questUpdate(player, start);
		}
	}

	public void questUpdate(Player player, boolean start)
	{
		if (start)
		{
			QuestState sakeQuestState = _forSakeQuest.newQuestState(player, Quest.CREATED);
			sakeQuestState.setState(Quest.STARTED);
			sakeQuestState.setCond(1);

			Quest protectCatapultQuest = QuestManager.getQuest("_729_ProtectTheTerritoryCatapult");
			if (protectCatapultQuest == null)
				return;

			QuestState questState = protectCatapultQuest.newQuestStateAndNotSave(player, Quest.CREATED);
			questState.setCond(1, false);
			questState.setStateAndNotSave(Quest.STARTED);
		}
		else
		{
			for (Quest q : _runnerEvent.getBreakQuests())
			{
				QuestState questState = player.getQuestState(q.getClass());
				if (questState != null)
					questState.abortQuest();
			}
		}
	}

	@Override
	public boolean isParticle(Player player)
	{
		if (_runnerEvent == null)
			return false;
		if (isInProgress() || _runnerEvent.isBattlefieldChatActive())
		{
			boolean registered = getObjects(DEFENDER_PLAYERS).contains(player.getObjectId()) || getSiegeClan(DEFENDERS, player.getClan()) != null;
			if (!registered)
				return false;
			else
			{
				if (isInProgress())
					return true;
				else
				{
					player.setBattlefieldChatId(getId());
					return false;
				}
			}
		}
		else
			return false;
	}

	//========================================================================================================================================================================
	//                                                                   Overrides GlobalEvent
	//========================================================================================================================================================================
	@Override
	public int getRelation(Player thisPlayer, Player targetPlayer, int result)
	{
		DominionSiegeEvent event2 = targetPlayer.getEvent(DominionSiegeEvent.class);
		if (event2 == null)
			return result;

		result |= RelationChanged.RELATION_ISINTERRITORYWARS;
		return result;
	}

	@Override
	public int getUserRelation(Player thisPlayer, int oldRelation)
	{
		oldRelation |= 0x1000;
		return oldRelation;
	}

	@Override
	public SystemMsg checkForAttack(Creature target, Creature attacker, Skill skill, boolean force)
	  {
	    DominionSiegeEvent siegeEvent = target.getEvent(DominionSiegeEvent.class);
	    DominionSiegeEvent siegeEvent2 = attacker.getEvent(DominionSiegeEvent.class);
	    if (siegeEvent == siegeEvent2) {
	      return SystemMsg.YOU_CANNOT_FORCE_ATTACK_A_MEMBER_OF_THE_SAME_TERRITORY;
	    }

	    return null;
	  }

	@Override
	public void broadcastTo(IStaticPacket packet, String... types)
	{
		List<SiegeClanObject> siegeClans = getObjects(DEFENDERS);
		for (SiegeClanObject siegeClan : siegeClans)
			siegeClan.broadcast(packet);

		List<Integer> players = getObjects(DEFENDER_PLAYERS);
		for (int i : players)
		{
			Player player = GameObjectsStorage.getPlayer(i);
			if (player != null)
				player.sendPacket(packet);
		}
	}

	@Override
	public void broadcastTo(L2GameServerPacket packet, String... types)
	{
		List<SiegeClanObject> siegeClans = getObjects(DEFENDERS);
		for (SiegeClanObject siegeClan : siegeClans)
			siegeClan.broadcast(packet);

		List<Integer> players = getObjects(DEFENDER_PLAYERS);
		for (int i : players)
		{
			Player player = GameObjectsStorage.getPlayer(i);
			if (player != null)
				player.sendPacket(packet);
		}
	}

	@Override
	public void giveItem(Player player, int itemId, long count)
	{
		Zone zone = player.getZone(Zone.ZoneType.SIEGE);
		if (zone == null)
			count = 0;
		else
		{
			int id = zone.getParams().getInteger("residence");
			if (id < 100)
				count = 125;
			else
				count = 31;
		}

		addReward(player, ONLINE_REWARD, 1);
		super.giveItem(player, itemId, count);
	}

	@Override
	public List<Player> itemObtainPlayers()
	{
		List<Player> playersInZone = getPlayersInZone();

		List<Player> list = new ArrayList<>(playersInZone.size());
		for (Player player : getPlayersInZone())
		{
			if (player.getEvent(DominionSiegeEvent.class) != null)
				list.add(player);
		}
		return list;
	}

	@Override
	public void checkRestartLocs(Player player, Map<RestartType, Boolean> r)
	{
		if (getObjects(FLAG_ZONES).isEmpty())
			return;

		SiegeClanObject clan = getSiegeClan(DEFENDERS, player.getClan());
		if (clan != null && clan.getFlag() != null)
			r.put(RestartType.TO_FLAG, Boolean.TRUE);
	}

	@Override
	public Location getRestartLoc(Player player, RestartType type)
	{
		if (type == RestartType.TO_FLAG)
		{
			SiegeClanObject defenderClan = getSiegeClan(DEFENDERS, player.getClan());

			if (defenderClan != null && defenderClan.getFlag() != null)
				return Location.findPointToStay(defenderClan.getFlag(), 50, 75);
			else
				player.sendPacket(SystemMsg.IF_A_BASE_CAMP_DOES_NOT_EXIST_RESURRECTION_IS_NOT_POSSIBLE);

			return null;
		}

		return super.getRestartLoc(player, type);
	}

	@Override
	public Location getEnterLoc(Player player)
	{
		Zone zone = player.getZone(Zone.ZoneType.SIEGE);
		if (zone == null)
			return player.getLoc();

		SiegeClanObject siegeClan = getSiegeClan(DEFENDERS, player.getClan());
		if (siegeClan != null)
		{
			if (siegeClan.getFlag() != null)
				return Location.findAroundPosition(siegeClan.getFlag(), 50, 75);
		}

		Residence r = ResidenceHolder.getInstance().getResidence(zone.getParams().getInteger("residence"));
		if (r == null)
		{
			error(toString(), new Exception("Not find residence: " + zone.getParams().getInteger("residence")));
			return player.getLoc();
		}
		return r.getNotOwnerRestartPoint(player);
	}

	@Override
	public void teleportPlayers(String t)
	{
		List<ZoneObject> zones = getObjects(SIEGE_ZONES);
		for (ZoneObject zone : zones)
		{
			Residence r = ResidenceHolder.getInstance().getResidence(zone.getZone().getParams().getInteger("residence"));

			r.banishForeigner();
		}
	}

	@Override
	public boolean canRessurect(Player resurrectPlayer, Creature target, boolean force)
	{
		boolean playerInZone = resurrectPlayer.isInZone(Zone.ZoneType.SIEGE);
		boolean targetInZone = target.isInZone(Zone.ZoneType.SIEGE);
		// если оба вне зоны - рес разрешен
		if (!playerInZone && !targetInZone)
			return true;
		// если таргет вне осадный зоны - рес разрешен
		if (!targetInZone)
			return false;

		Player targetPlayer = target.getPlayer();
		// если таргет не с нашей осады(или вообще нету осады) - рес запрещен
		DominionSiegeEvent siegeEvent = target.getEvent(DominionSiegeEvent.class);
		if (siegeEvent == null)
		{
			if (force)
				targetPlayer.sendPacket(SystemMsg.IT_IS_NOT_POSSIBLE_TO_RESURRECT_IN_BATTLEFIELDS_WHERE_A_SIEGE_WAR_IS_TAKING_PLACE);
			resurrectPlayer.sendPacket(force ? SystemMsg.IT_IS_NOT_POSSIBLE_TO_RESURRECT_IN_BATTLEFIELDS_WHERE_A_SIEGE_WAR_IS_TAKING_PLACE : SystemMsg.INVALID_TARGET);
			return false;
		}

		SiegeClanObject targetSiegeClan = siegeEvent.getSiegeClan(DEFENDERS, targetPlayer.getClan());
		// если нету флага - рес запрещен
		if (targetSiegeClan == null || targetSiegeClan.getFlag() == null)
		{
			if (force)
				targetPlayer.sendPacket(SystemMsg.IF_A_BASE_CAMP_DOES_NOT_EXIST_RESURRECTION_IS_NOT_POSSIBLE);
			resurrectPlayer.sendPacket(force ? SystemMsg.IF_A_BASE_CAMP_DOES_NOT_EXIST_RESURRECTION_IS_NOT_POSSIBLE : SystemMsg.INVALID_TARGET);
			return false;
		}

		if (resurrectPlayer.getClan() == null)
		{
			resurrectPlayer.sendPacket((force) ? SystemMsg.IT_IS_NOT_POSSIBLE_TO_RESURRECT_IN_BATTLEFIELDS_WHERE_A_SIEGE_WAR_IS_TAKING_PLACE : SystemMsg.INVALID_TARGET);
			return false;
		}

		if ((resurrectPlayer.getClan() != null) && (resurrectPlayer.getClan() != targetPlayer.getClan()))
		{
			resurrectPlayer.sendPacket((force) ? SystemMsg.IT_IS_NOT_POSSIBLE_TO_RESURRECT_IN_BATTLEFIELDS_WHERE_A_SIEGE_WAR_IS_TAKING_PLACE : SystemMsg.INVALID_TARGET);
			return false;
		}

		if (force)
			return true;
		else
		{
			resurrectPlayer.sendPacket(SystemMsg.INVALID_TARGET);
			return false;
		}
	}
		//========================================================================================================================================================================
	//                                                                   Rewards
	//========================================================================================================================================================================
	public void setReward(int objectId, int type, int v)
	{
		int val[] = _playersRewards.get(objectId);
		if (val == null)
			_playersRewards.put(objectId, val = new int[REWARD_MAX]);

		val[type] = v;
	}

	public void addReward(Player player, int type, int v)
	{
		int val[] = _playersRewards.get(player.getObjectId());
		if (val == null)
			_playersRewards.put(player.getObjectId(), val = new int[REWARD_MAX]);

		val[type] += v;
	}

	public int getReward(Player player, int type)
	{
		int val[] = _playersRewards.get(player.getObjectId());
		if (val == null)
			return 0;
		else
			return val[type];
	}

	public void clearReward(int objectId)
	{
		if (_playersRewards.containsKey(objectId))
		{
			_playersRewards.remove(objectId);
			DominionRewardDAO.getInstance().delete(getResidence(), objectId);
		}
	}

	public Collection<IntObjectMap.Entry<int[]>> getRewards()
	{
		return _playersRewards.entrySet();
	}

	public int[] calculateReward(Player player)
	{
		int rewards[] = _playersRewards.get(player.getObjectId());
		if (rewards == null)
			return null;

		int[] out = new int[3];
		// статичные (старт, стоп, квесты, прочее)
		out[0] += rewards[STATIC_BADGES];
		// если онлайн ревард больше 14(70 мин в зоне) это 7 макс
		out[0] += rewards[ONLINE_REWARD] >= 14 ? 7 : rewards[ONLINE_REWARD] / 2;

		// насчитаем за убийство
		if (rewards[KILL_REWARD] < 50)
			out[0] += rewards[KILL_REWARD] * 0.1;
		else if (rewards[KILL_REWARD] < 120)
			out[0] += (5 + (rewards[KILL_REWARD] - 50) / 14);
		else
			out[0] += 10;

		//TODO [VISTALL] неверно, фейм дается и ниже, нету выдачи адены
		if (out[0] > 90)
		{
			out[0] = 90; // badges
			out[1] = 0; //TODO [VISTALL] adena count
			out[2] = 450; // fame
		}

		return out;
	}
	//========================================================================================================================================================================
	//                                                                   Getters/Setters
	//========================================================================================================================================================================

	public void setForSakeQuest(Quest forSakeQuest)
	{
		_forSakeQuest = forSakeQuest;
	}
}
