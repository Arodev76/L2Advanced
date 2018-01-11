package l2f.gameserver.model.entity.events.impl;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import javolution.util.FastMap;
import l2f.commons.annotations.Nullable;
import l2f.commons.collections.MultiValueSet;
import l2f.commons.threading.RunnableImpl;
import l2f.commons.util.Rnd;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.data.xml.holder.InstantZoneHolder;
import l2f.gameserver.instancemanager.ReflectionManager;
import l2f.gameserver.listener.actor.player.OnPlayerExitListener;
import l2f.gameserver.listener.zone.OnZoneEnterLeaveListener;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Effect;
import l2f.gameserver.model.GameObject;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Party;
import l2f.gameserver.model.Playable;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.SimpleSpawner;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.World;
import l2f.gameserver.model.Zone;
import l2f.gameserver.model.base.InvisibleType;
import l2f.gameserver.model.base.RestartType;
import l2f.gameserver.model.entity.Reflection;
import l2f.gameserver.model.entity.events.GlobalEvent;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubEventManager;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubEventManager.CLASSES;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubGameRoom;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubLastStatsManager;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubLastStatsManager.FightClubStatType;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubMap;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubPlayer;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubTeam;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.model.instances.PetInstance;
import l2f.gameserver.model.instances.SchemeBufferInstance;
import l2f.gameserver.network.serverpackets.Earthquake;
import l2f.gameserver.network.serverpackets.ExPVPMatchCCRecord;
import l2f.gameserver.network.serverpackets.ExPVPMatchCCRetire;
import l2f.gameserver.network.serverpackets.ExShowScreenMessage;
import l2f.gameserver.network.serverpackets.RelationChanged;
import l2f.gameserver.network.serverpackets.Say2;
import l2f.gameserver.network.serverpackets.TutorialShowQuestionMark;
import l2f.gameserver.network.serverpackets.components.ChatType;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.skills.AbnormalEffect;
import l2f.gameserver.tables.SkillTable;
import l2f.gameserver.templates.DoorTemplate;
import l2f.gameserver.templates.InstantZone;
import l2f.gameserver.templates.ZoneTemplate;
import l2f.gameserver.utils.Location;
import l2f.gameserver.utils.Util;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.napile.primitive.maps.IntObjectMap;
import org.napile.primitive.maps.impl.HashIntObjectMap;

public abstract class AbstractFightClub extends GlobalEvent
{
	private class ExitListener implements OnPlayerExitListener
	{
		@Override
		public void onPlayerExit(Player player)
		{
			loggedOut(player);
		}
	}

	private class ZoneListener implements OnZoneEnterLeaveListener
	{
		@Override
		public void onZoneEnter(Zone zone, Creature actor)
		{
			if (actor.isPlayer())
			{
				FightClubPlayer fPlayer = getFightClubPlayer(actor);
				if (fPlayer != null)
				{
					actor.sendPacket(new Earthquake(actor.getLoc(), 0, 1));
					_leftZone.remove(getFightClubPlayer(actor));
				}
			}
		}

		@Override
		public void onZoneLeave(Zone zone, Creature actor)
		{
			if (actor.isPlayer() && _state != EVENT_STATE.NOT_ACTIVE)
			{
				FightClubPlayer fPlayer = getFightClubPlayer(actor);
				if (fPlayer != null)
					_leftZone.put(getFightClubPlayer(actor), zone);
			}
		}
	}


	public static enum EVENT_STATE {
		NOT_ACTIVE,
		COUNT_DOWN,
		PREPARATION,
		STARTED,
		OVER
	}

	public static final String REGISTERED_PLAYERS = "registered_players";
	public static final String LOGGED_OFF_PLAYERS = "logged_off_players";
	public static final String FIGHTING_PLAYERS = "fighting_players";


	public static final int INSTANT_ZONE_ID = 400;
	private static final int CLOSE_LOCATIONS_VALUE = 150;//Used for spawning players
	private static int LAST_OBJECT_ID = 1;//Used for event object id

	private static final int BADGES_FOR_MINUTE_OF_AFK = -1;

	private static final int TIME_FIRST_TELEPORT = 10;//in seconds
	private static final int TIME_PLAYER_TELEPORTING = 15;//in seconds
	private static final int TIME_PREPARATION_BEFORE_FIRST_ROUND = 30;//in seconds
	private static final int TIME_PREPARATION_BETWEEN_NEXT_ROUNDS = 30;//in seconds - usable if there are more than 1 round
	private static final int TIME_AFTER_ROUND_END_TO_RETURN_SPAWN = 15;//in seconds - usable if there are more than 1 round
	private static final int TIME_TELEPORT_BACK_TOWN = 30;//in seconds
	private static final int TIME_MAX_SECONDS_OUTSIDE_ZONE = 10;
	private static final int TIME_TO_BE_AFK = 120;

	private static final String[] ROUND_NUMBER_IN_STRING = {"", "1st", "2nd", "3rd", "4th", "5th","6th", "7th", "8th", "9th", "10th"};

	// Properties
	private final int _objId;
	private final String _desc;
	private final String _icon;
	private final int _roundRunTime;//in minutes
	private final boolean _isAutoTimed;
	private final int[][] _autoStartTimes;
	private final boolean _teamed;
	private final boolean _buffer;
	private final int[][] _fighterBuffs;
	private final int[][] _mageBuffs;
	private final boolean _rootBetweenRounds;
	private final CLASSES[] _excludedClasses;
	private final int[] _excludedSkills;
	private final boolean _roundEvent;
	private final int _rounds;
	private final int _respawnTime;//in seconds
	private final boolean _ressAllowed;
	private final boolean _instanced;
	private final boolean _showPersonality;
	//Badges
	private final double _badgesKillPlayer;
	private final double _badgesKillPet;
	private final double _badgesDie;
	protected final double _badgeWin;
	private final int topKillerReward;


	//Event variables
	protected EVENT_STATE _state = EVENT_STATE.NOT_ACTIVE;
	private ExitListener _exitListener = new ExitListener();
	private ZoneListener _zoneListener = new ZoneListener();
	private FightClubMap _map;
	private Reflection _reflection;
	private final List<FightClubTeam> _teams = new CopyOnWriteArrayList<>();
	private final Map<FightClubPlayer, Zone> _leftZone = new ConcurrentHashMap<>();
	private int _currentRound = 0;
	private boolean _dontLetAnyoneIn = false;
	private FightClubGameRoom _room;

	//For duplicating events:
	private MultiValueSet<String> _set;

	//Scores
	private final Map<String, Integer> _scores = new ConcurrentHashMap<>();
	private Map<String, Integer> _bestScores = new ConcurrentHashMap<>();
	private boolean _scoredUpdated = true;

	//Before event start
	private ScheduledFuture<?> _timer;

	public AbstractFightClub(MultiValueSet<String> set)
	{
		super(set);
		_objId = LAST_OBJECT_ID++;
		_desc = set.getString("desc");
		_icon = set.getString("icon");
		_roundRunTime = set.getInteger("roundRunTime", -1);
		_teamed = set.getBool("teamed");
		_buffer = set.getBool("buffer");
		_fighterBuffs = parseBuffs(set.getString("fighterBuffs", null));
		_mageBuffs = parseBuffs(set.getString("mageBuffs", null));
		_rootBetweenRounds = set.getBool("rootBetweenRounds");
		_excludedClasses = parseExcludedClasses(set.getString("excludedClasses", ""));
		_excludedSkills = parseExcludedSkills(set.getString("excludedSkills", null));
		_isAutoTimed = set.getBool("isAutoTimed", false);
		_autoStartTimes = parseAutoStartTimes(set.getString("autoTimes", ""));
		_roundEvent = set.getBool("roundEvent");
		_rounds = set.getInteger("rounds", -1);
		_respawnTime = set.getInteger("respawnTime");
		_ressAllowed = set.getBool("ressAllowed");
		_instanced = set.getBool("instanced", true);
		_showPersonality = set.getBool("showPersonality", true);
		//Badges
		_badgesKillPlayer = set.getDouble("badgesKillPlayer", 0);
		_badgesKillPet = set.getDouble("badgesKillPet", 0);
		_badgesDie = set.getDouble("badgesDie", 0);
		_badgeWin = set.getDouble("badgesWin", 0);
		topKillerReward = set.getInteger("topKillerReward", 0);

		_set = set;
	}

	/**
	 * - Getting map
	 * - Saving room
	 * - Saving all players from Room as Registered in Event
	 * - Starting Teleport Timer
	 * @param room of the event
	 */
	public void prepareEvent(FightClubGameRoom room)
	{
		_map = room.getMap();
		_room = room;

		for (Player player : room.getAllPlayers())
		{
			addObject(REGISTERED_PLAYERS, new FightClubPlayer(player));
			player.addEvent(this);
		}

		startTeleportTimer(room);
	}

	@Override
	public void startEvent()
	{
		super.startEvent();

		_state = EVENT_STATE.PREPARATION;

		//Getting all zones
		IntObjectMap<DoorTemplate> doors = new HashIntObjectMap<>(0);
		Map<String, ZoneTemplate> zones = new HashMap<>();
		for (Entry<Integer, Map<String, ZoneTemplate>> entry : getMap().getTerritories().entrySet())
		{
			for (Entry<String, ZoneTemplate> team : entry.getValue().entrySet())
			{
				zones.put(team.getKey(), team.getValue());
			}
		}

		//Creating reflection if needed
		if (isInstanced())
			createReflection(doors, zones);

		List<FightClubPlayer> playersToRemove = new ArrayList<>();
		for (FightClubPlayer iFPlayer : getPlayers(REGISTERED_PLAYERS))
		{
			stopInvisibility(iFPlayer.getPlayer());
			if (!checkIfRegisteredPlayerMeetCriteria(iFPlayer))
			{
				playersToRemove.add(iFPlayer);
				continue;
			}

			if (isHidePersonality())
				iFPlayer.getPlayer().setPolyId(FightClubGameRoom.getPlayerClassGroup(iFPlayer.getPlayer()).getTransformId());
		}

		for (FightClubPlayer playerToRemove : playersToRemove)
			unregister(playerToRemove.getPlayer());

		if (isTeamed())
		{
			spreadIntoTeamsAndPartys();
		}

		teleportRegisteredPlayers();

		updateEveryScore();

		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS, REGISTERED_PLAYERS))
		{
			iFPlayer.getPlayer().isntAfk();
			iFPlayer.getPlayer().setFightClubGameRoom(null);
			SchemeBufferInstance.showWindow(iFPlayer.getPlayer());
		}

		startNewTimer(true, TIME_PLAYER_TELEPORTING*1000, "startRoundTimer", TIME_PREPARATION_BEFORE_FIRST_ROUND);

		ThreadPoolManager.getInstance().schedule(new LeftZoneThread(), 5000L);
	}

	public void startRound()
	{
		_state = EVENT_STATE.STARTED;

		_currentRound++;

		if (isRoundEvent())
			if (_currentRound == _rounds)
				sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "Last Round STARTED!", true);
			else
				sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "Round "+_currentRound+" STARTED!", true);
		else
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "Fight!", true);

		unrootPlayers();

		if (getRoundRuntime() > 0)
		{
			startNewTimer(true, (int)((double)getRoundRuntime()/2*60000), "endRoundTimer", (int)((double)getRoundRuntime()/2*60));
		}

		if (_currentRound == 1)
		{
			ThreadPoolManager.getInstance().schedule(new TimeSpentOnEventThread(), 10*1000);
			ThreadPoolManager.getInstance().schedule(new CheckAfkThread(), 1000);
		}

		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			iFPlayer.getPlayer().updateZones();
			hideScores(iFPlayer.getPlayer());
			iFPlayer.getPlayer().broadcastUserInfo(true);
		}
	}

	public void endRound()
	{
		_state = EVENT_STATE.OVER;

		if (!isLastRound())
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "Round "+_currentRound+" is over!", false);
		else
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, "Event is now Over!", false);

		ressAndHealPlayers();

		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			showScores(iFPlayer.getPlayer());

		if (!isLastRound())
		{
			//Changing team spawn location
			if (isTeamed())
			{
				for (FightClubTeam team : getTeams())
					team.setSpawnLoc(null);
			}

			ThreadPoolManager.getInstance().schedule(() ->
			{
				for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
				{
					teleportSinglePlayer(iFPlayer, false, true);
				}

				startNewTimer(true, 0, "startRoundTimer", TIME_PREPARATION_BETWEEN_NEXT_ROUNDS);

			}, TIME_AFTER_ROUND_END_TO_RETURN_SPAWN*1000);
		}
		else
		{
			ThreadPoolManager.getInstance().schedule(() -> stopEvent(), 10*1000);

			if (isTeamed())
				announceWinnerTeam(true, null);
			else
				announceWinnerPlayer(true, null);
		}

		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			iFPlayer.getPlayer().broadcastUserInfo(true);
	}


	@Override
	public void stopEvent()
	{
		_state = EVENT_STATE.NOT_ACTIVE;
		super.stopEvent();
		reCalcNextTime(false);
		_room = null;

		showLastAFkMessage();
		FightClubPlayer[] topKillers = getTopKillers();
		announceTopKillers(topKillers);
		giveRewards(topKillers);

		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			iFPlayer.getPlayer().broadcastCharInfo();
			if (iFPlayer.getPlayer().getPet() != null)
				iFPlayer.getPlayer().getPet().broadcastCharInfo();
		}
		for (Player player : getAllFightingPlayers())
		{
			showScores(player);
		}

		ThreadPoolManager.getInstance().schedule(() ->
		{
			for (Player player : getAllFightingPlayers())
			{
				leaveEvent(player, true);
				player.sendPacket(new ExShowScreenMessage("", 10, ExShowScreenMessage.ScreenMessageAlign.TOP_LEFT, false));
			}
			destroyMe();
		}, 10 * 1000);
	}

	public void destroyMe()
	{
		if (getReflection() != null)
		{
			for (Zone zone : getReflection().getZones())
				zone.removeListener(_zoneListener);
			getReflection().collapse();
		}
		if (_timer != null)
			_timer.cancel(false);
		_timer = null;
		_bestScores.clear();
		_scores.clear();
		_leftZone.clear();
		getObjects().clear();
		_set = null;
		_room = null;
		_zoneListener = null;
		for (Player player : GameObjectsStorage.getAllPlayersForIterate())
			player.removeListener(_exitListener);
		_exitListener = null;
	}


	//===============================================================================================================
	//										Unique event related actions
	//===============================================================================================================

	/**
	 * Remember that actor may be NULL/player/pet/npc. Victim: player/pet/npc
	 * @param actor
	 * @param victim
	 */
	public void onKilled(Creature actor, Creature victim)
	{
		if (victim.isPlayer() && getRespawnTime() > 0)
			showScores(victim);

		if (actor != null && actor.isPlayer() && getFightClubPlayer(actor) != null)
			FightClubLastStatsManager.getInstance().updateStat(actor.getPlayer(), FightClubStatType.KILL_PLAYER, getFightClubPlayer(actor).getKills(true));

		//Respawn type for non teamed events
		if (victim.isPlayer() && getRespawnTime() > 0 && !_ressAllowed && getFightClubPlayer(victim.getPlayer()) != null)
			startNewTimer(false, 0, "ressurectionTimer", getRespawnTime(), getFightClubPlayer(victim));
	}

	/**
	 * Synerge
	 * Support for keeping track of damage done
	 *
	 * @param actor
	 * @param victim
	 * @param damage
	 */
	public void onDamage(Creature actor, Creature victim, double damage)
	{

	}

	public void requestRespawn(Player activeChar, RestartType restartType)
	{
		if (getRespawnTime() > 0)
			startNewTimer(false, 0, "ressurectionTimer", getRespawnTime(), getFightClubPlayer(activeChar));
	}

	@Override
	public boolean canAttack(Creature target, Creature attacker, Skill skill, boolean force)
	{
		if (_state != EVENT_STATE.STARTED)
			return false;
		Player player = attacker.getPlayer();
		if (player == null)
			return true;

		if (isTeamed())
		{
			FightClubPlayer targetFPlayer = getFightClubPlayer(target);
			FightClubPlayer attackerFPlayer = getFightClubPlayer(attacker);

			if (targetFPlayer == null || attackerFPlayer == null || targetFPlayer.getTeam().equals(attackerFPlayer.getTeam()))
				return false;
		}

		//if (isInvisible(player, player))
		{
		//	return false;
		}

		return true;
	}

	@Override
	public boolean canUseSkill(Creature caster, Creature target, Skill skill)
	{
		if (_excludedSkills != null)
		{
			for (int id : _excludedSkills)
				if (skill.getId() == id)
				{
					return false;
				}
		}

		return true;
	}

	@Override
	public SystemMsg checkForAttack(Creature target, Creature attacker, Skill skill, boolean force)
	{
		if (!canAttack(target, attacker, skill, force))
			return SystemMsg.INVALID_TARGET;
		return null;
	}

	@Override
	public boolean canRessurect(Player player, Creature creature, boolean force)
	{
		return _ressAllowed;
	}

	/**
	 * @param player
	 * @return -1 if it have to be unchanged
	 */
	public int getMySpeed(Player player)
	{
		return -1;
	}

	/**
	 * @param player
	 * @return -1 if it have to be unchanged
	 */
	public int getPAtkSpd(Player player)
	{
		return -1;
	}

	/**
	 * Removing window that appears after death
	 */
	@Override
	public void checkRestartLocs(Player player, Map<RestartType, Boolean> r)
	{
		r.clear();
		if (isTeamed() && getRespawnTime() > 0 && getFightClubPlayer(player) != null && _ressAllowed)
		{
			r.put(RestartType.TO_FLAG, true);
		}
	}

	public boolean canUseBuffer(Player player, boolean heal)
	{
		FightClubPlayer fPlayer = getFightClubPlayer(player);
		if (!getBuffer())
			return false;
		if (player.isInCombat())
			return false;
		if (heal)
		{
			if (player.isDead())
				return false;
			if (_state != EVENT_STATE.STARTED)
				return true;
			if (fPlayer.isInvisible())
				return true;
			return false;
		}
		return true;
	}

	public boolean canUsePositiveMagic(Creature user, Creature target)
	{
		Player player = user.getPlayer();
		if (player == null)
			return true;

		return isFriend(user, target);
	}

	@Override
	public int getRelation(Player thisPlayer, Player target, int oldRelation)
	{
		if (_state == EVENT_STATE.STARTED)
			return isFriend(thisPlayer, target) ? getFriendRelation() : getWarRelation();
		else
			return oldRelation;
	}

	public boolean canJoinParty(Player sender, Player receiver)
	{
		return isFriend(sender, receiver);
	}

	/**
	 * Join Clan, Join Ally, Command Channel
	 * @param sender
	 * @param receiver
	 * @return
	 */
	public boolean canReceiveInvitations(Player sender, Player receiver)
	{
		return true;
	}

	public boolean canOpenStore(Player player)
	{
		return false;
	}

	public boolean canStandUp(Player player)
	{
		return true;
	}

	public boolean loseBuffsOnDeath(Player player)
	{
		return false;
	}

	protected boolean inScreenShowBeScoreNotKills()
	{
		return true;
	}

	protected boolean inScreenShowBeTeamNotInvidual()
	{
		return isTeamed();
	}

	public boolean isFriend(Creature c1, Creature c2)
	{
		if (c1.equals(c2))
			return true;

		if (!c1.isPlayable() || !c2.isPlayable())
		{
			return true;
		}

		if (c1.isSummon() && c2.isPlayer() && c2.getPlayer().getPet() != null && c2.getPlayer().getPet().equals(c1))
		{
			return true;
		}

		if (c2.isSummon() && c1.isPlayer() && c1.getPlayer().getPet() != null && c1.getPlayer().getPet().equals(c2))
		{
			return true;
		}

		FightClubPlayer fPlayer1 = getFightClubPlayer(c1.getPlayer());
		FightClubPlayer fPlayer2 = getFightClubPlayer(c2.getPlayer());

		if (isTeamed())
		{
			//Different teams
			if (fPlayer1 == null || fPlayer2 == null || !fPlayer1.getTeam().equals(fPlayer2.getTeam()))
			{
				return false;
			}
			//Same team
			else
			{
				return true;
			}
		}
		else
		{
			return false;
		}
	}

	public boolean isInvisible(Player actor, Player watcher)
	{
		return actor.getInvisibleType() == InvisibleType.NORMAL;
	}

	public String getVisibleName(Player player, String currentName, boolean toMe)
	{
		if (isHidePersonality() && !toMe)
			return "Player";
		return currentName;
	}

	public String getVisibleTitle(Player player, String currentTitle, boolean toMe)
	{
		return currentTitle;
	}

	public int getVisibleTitleColor(Player player, int currentTitleColor, boolean toMe)
	{
		return currentTitleColor;
	}

	public int getVisibleNameColor(Player player, int currentNameColor, boolean toMe)
	{
		if (isTeamed())
		{
			FightClubPlayer fPlayer = getFightClubPlayer(player);
			return fPlayer.getTeam().getNickColor();
		}
		return currentNameColor;
	}

	/*
	 *
	 * Badges
	 *
	 */
	protected int getBadgesEarned(FightClubPlayer fPlayer, int currentValue, boolean topKiller)
	{
		if (fPlayer == null)
			return 0;
		currentValue += addMultipleBadgeToPlayer(fPlayer.getKills(true), _badgesKillPlayer);

		currentValue += getRewardForWinningTeam(fPlayer, true);

		int minutesAFK = (int) Math.round((double)fPlayer.getTotalAfkSeconds()/60);
		currentValue += minutesAFK * BADGES_FOR_MINUTE_OF_AFK;

		if (topKiller)
		{
			currentValue += topKillerReward;
		}

		return currentValue;
	}

	/**
	 * @param score
	 * @param badgePerScore
	 * @return score*badges
	 */
	protected int addMultipleBadgeToPlayer(int score, double badgePerScore)
	{
		return (int) Math.floor(score*badgePerScore);
	}

	private int getEndEventBadges(FightClubPlayer fPlayer)
	{
		return 0;
	}


	//===============================================================================================================
	//											Actions same for all events
	//===============================================================================================================

	public void startTeleportTimer(FightClubGameRoom room)
	{
		setState(EVENT_STATE.COUNT_DOWN);

		startNewTimer(true, 0, "teleportWholeRoomTimer", TIME_FIRST_TELEPORT);
	}

	/**
	 * teleportSinglePlayer for first time, healing and ressing - all REGISTERED_PLAYERS
	 */
	protected void teleportRegisteredPlayers()
	{
		for (FightClubPlayer player : getPlayers(REGISTERED_PLAYERS))
		{
			teleportSinglePlayer(player, true, true);
		}
	}

	/**
	 * Teleporting player to event random location, rooting if _state is PREPARATION or OVER, cancelling negative effects
	 * IF @firstTime - removing from REGISTERED_PLAYERS and adding to FIGHTING_PLAYERS, showing question mark, buffing, sending event menu,
	 * sending messages about chat, setting invisibility if needed
	 * @param fPlayer
	 * @param firstTime
	 * @param healAndRess
	 */
	protected void teleportSinglePlayer(FightClubPlayer fPlayer, boolean firstTime, boolean healAndRess)
	{
		Player player = fPlayer.getPlayer();

		if (healAndRess)
		{
			if (player.isDead())
				player.doRevive(100.);
		}

		Location loc = getSinglePlayerSpawnLocation(fPlayer);

		if (isInstanced())
			player.teleToLocation(loc, getReflection());
		else
			player.teleToLocation(loc);

		if (_state == EVENT_STATE.PREPARATION || _state == EVENT_STATE.OVER)
			rootPlayer(player);

		//Cancelling negative effects
		cancelNegativeEffects(player);
		// Unsummon pets
		if (player.getPet() instanceof PetInstance)
			player.getPet().unSummon();
		if (player.getPet() != null)
			cancelNegativeEffects(player.getPet());

		if (firstTime)
		{
			removeObject(REGISTERED_PLAYERS, fPlayer);
			addObject(FIGHTING_PLAYERS, fPlayer);

			player.getEffectList().stopAllEffects();
			if (player.getPet() != null)
				player.getPet().getEffectList().stopAllEffects();

			player.store(true);
			player.sendPacket(new TutorialShowQuestionMark(100));

			player.sendPacket(new Say2(player.getObjectId(), ChatType.ALL, getName(), "Normal Chat is visible for every player in event."));
			if (isTeamed())
			{
				player.sendPacket(new Say2(player.getObjectId(), ChatType.ALL, getName(), "Battlefield(^) Chat is visible only to your team!"));
				player.sendPacket(new Say2(player.getObjectId(), ChatType.BATTLEFIELD, getName(), "Battlefield(^) Chat is visible only to your team!"));
			}
		}

		if (healAndRess)
		{
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp(), true);
			player.setCurrentCp(player.getMaxCp());
			if (player.getPet() != null && !player.getPet().isDead())
			{
				Playable pet = player.getPet();
				pet.setCurrentHpMp(pet.getMaxHp(), player.getMaxMp(), false);
				pet.broadcastCharInfo();
			}
			player.broadcastUserInfo(true);
			player.broadcastStatusUpdate();
			player.broadcastCharInfo();
		}

		if (player.isMounted())
		{
			player.setMount(0, 0, 0);
		}

		if (player.getTransformation() > 0)
		{
			player.setTransformation(0);
			player.setTransformationTemplate(0);
			player.setTransformationName(null);
		}
	}

	protected Location getSinglePlayerSpawnLocation(FightClubPlayer fPlayer)
	{
		Location[] spawns = null;
		Location loc = null;

		if (!isTeamed())
			spawns = getMap().getPlayerSpawns();
		else
			loc = getTeamSpawn(fPlayer, true);

		if (!isTeamed())
			loc = getSafeLocation(spawns);

		//Make it a bit random
		return Location.findPointToStay(loc, 0, CLOSE_LOCATIONS_VALUE / 2, fPlayer.getPlayer().getGeoIndex());
	}

	public void unregister(Player player)
	{
		FightClubPlayer fPlayer = getFightClubPlayer(player, REGISTERED_PLAYERS);
		player.removeEvent(this);
		removeObject(REGISTERED_PLAYERS, fPlayer);
		player.sendMessage("You are no longer registered!");
	}

	public boolean leaveEvent(Player player, boolean teleportTown)
	{
		FightClubPlayer fPlayer = getFightClubPlayer(player);

		if (fPlayer == null)
			return true;

		if (_state == EVENT_STATE.NOT_ACTIVE)
		{
			if (fPlayer.isInvisible())
				stopInvisibility(player);
			removeObject(FIGHTING_PLAYERS, fPlayer);
			if (isTeamed())
				fPlayer.getTeam().removePlayer(fPlayer);
			player.removeEvent(this);

			if (teleportTown)
				teleportBackToTown(player);
			else
				player.doRevive();
		}
		else //Leaving during event
		{
			rewardPlayer(fPlayer, false);
			if (teleportTown)
				setInvisible(player, TIME_TELEPORT_BACK_TOWN, false);
			else
				setInvisible(player, -1, false);
			removeObject(FIGHTING_PLAYERS, fPlayer);
			//Killing player - protection for Last Man standing
			player.doDie(null);//Dont change order. Removing from 1. FIGHTING_PLAYERS 2. Do Die 3. Remove Event
			player.removeEvent(this);

			if (teleportTown)
				startNewTimer(false, 0, "teleportBackSinglePlayerTimer", TIME_TELEPORT_BACK_TOWN, player);
			else
				player.doRevive();
		}

		hideScores(player);
		updateScreenScores();

		if (getPlayers(FIGHTING_PLAYERS, REGISTERED_PLAYERS).isEmpty())
			destroyMe();

		if (player.getParty() != null)
			player.getParty().removePartyMember(player, true);

		return true;
	}

	public void loggedOut(Player player)
	{
		leaveEvent(player, true);
	}

	protected void teleportBackToTown(Player player)
	{
		if (player.isDead())
			player.doRevive();
		player.setPolyId(0);
		Location loc = Location.findPointToStay(FightClubEventManager.RETURN_LOC, 0, 100, player.getGeoIndex());
		player.teleToLocation(loc, ReflectionManager.DEFAULT);
	}

	protected void rewardPlayer(FightClubPlayer fPlayer, boolean isTopKiller)
	{
		int badgesToGive = getBadgesEarned(fPlayer, 0, isTopKiller);

		if (getState() == EVENT_STATE.NOT_ACTIVE)
			badgesToGive += getEndEventBadges(fPlayer);

		badgesToGive = Math.max(0, badgesToGive);

		fPlayer.getPlayer().getInventory().addItem(FightClubEventManager.FIGHT_CLUB_BADGE_ID, badgesToGive, getName()+" Event");
		sendMessageToPlayer(fPlayer, MESSAGE_TYPES.SCREEN_BIG, "You have earned " + badgesToGive + " Festival Coins!");
		sendMessageToPlayer(fPlayer, MESSAGE_TYPES.NORMAL_MESSAGE, "You have earned "+badgesToGive+" Festival Coins!");
	}

	@Nullable
	private FightClubPlayer[] getTopKillers()
	{
		if (!_teamed || topKillerReward == 0)
			return null;

		FightClubPlayer[] topKillers = new FightClubPlayer[_teams.size()];
		int[] topKillersKills = new int[_teams.size()];

		int teamIndex = 0;
		for (FightClubTeam team : _teams)
		{
			for (FightClubPlayer fPlayer : team.getPlayers())
			{
				if (fPlayer != null)
				{
					if (fPlayer.getKills(true) == topKillersKills[teamIndex])
					{
						topKillers[teamIndex] = null;
					}
					else if (fPlayer.getKills(true) > topKillersKills[teamIndex])
					{
						topKillers[teamIndex] = fPlayer;
						topKillersKills[teamIndex] = fPlayer.getKills(true);
					}
				}
			}
			teamIndex ++;
		}
		return topKillers;
	}

	/**
	 * Make isTeamed() check before
	 * @param wholeEvent
	 * @param winnerOfTheRound
	 * @wholeEvent - true - We won Last Man Standing! false - We Won Round!
	 * @winnerOfTheRound if wholeEvent == false, set it to null
	 */
	protected void announceWinnerTeam(boolean wholeEvent, FightClubTeam winnerOfTheRound)
	{
		int bestScore = -1;
		FightClubTeam bestTeam = null;
		boolean draw = false;
		if (wholeEvent)
		{
			for (FightClubTeam team : getTeams())
				if (team.getScore() > bestScore)
				{
					draw = false;
					bestScore = team.getScore();
					bestTeam = team;
				}
				else if (team.getScore() == bestScore)
				{
					draw = true;
				}
		}
		else
			bestTeam = winnerOfTheRound;

		if (!draw)
		{
			Say2 packet = new Say2(0, ChatType.COMMANDCHANNEL_ALL, bestTeam.getName()+" Team", "We won "+(wholeEvent ? getName() : " Round")+"!");
			for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
				iFPlayer.getPlayer().sendPacket(packet);
		}

		updateScreenScores();
	}

	/**
	 * It is checking all players by their scores. Not for teamed events!
	 * @param wholeEvent
	 * @param winnerOfTheRound
	 * @wholeEvent - true - I won Last Man Standing! false - I Won Round!
	 * @winnerOfTheRound if wholeEvent == false, set it to null
	 */
	protected void announceWinnerPlayer(boolean wholeEvent, FightClubPlayer winnerOfTheRound)
	{
		int bestScore = -1;
		FightClubPlayer bestPlayer = null;
		boolean draw = false;
		if (wholeEvent)
		{
			for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
				if (iFPlayer.getPlayer() != null && iFPlayer.getPlayer().isOnline())
					if (iFPlayer.getScore() > bestScore)
					{
						bestScore = iFPlayer.getScore();
						bestPlayer = iFPlayer;
					}
					else if (iFPlayer.getScore() == bestScore)
					{
						draw = true;
					}
		}
		else
			bestPlayer = winnerOfTheRound;

		if (!draw)
		{
			Say2 packet = new Say2(0, ChatType.COMMANDCHANNEL_ALL, bestPlayer.getPlayer().getName(), "I Won "+(wholeEvent ? getName() : "Round")+"!");
			for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
				iFPlayer.getPlayer().sendPacket(packet);
		}

		updateScreenScores();
	}

	protected void updateScreenScores()
	{
		String msg = getScreenScores(inScreenShowBeScoreNotKills(), inScreenShowBeTeamNotInvidual());

		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			sendMessageToPlayer(iFPlayer, MESSAGE_TYPES.SCREEN_SMALL, msg);
	}

	protected void updateScreenScores(Player player)
	{
		if (getFightClubPlayer(player) != null)
				sendMessageToPlayer(getFightClubPlayer(player), MESSAGE_TYPES.SCREEN_SMALL, getScreenScores(inScreenShowBeScoreNotKills(), inScreenShowBeTeamNotInvidual()));
	}

	protected String getScorePlayerName(FightClubPlayer fPlayer)
	{
		return fPlayer.getPlayer().getName() + (isTeamed() ? " ("+fPlayer.getTeam().getName()+" Team)" : "");
	}

	/**
	 * Player Ranking is being updated
	 * @param fPlayer
	 */
	protected void updatePlayerScore(FightClubPlayer fPlayer)
	{
		_scores.put(getScorePlayerName(fPlayer), fPlayer.getKills(true));
		_scoredUpdated = true;

		/*for(FightClubPlayer iterFPlayer : getPlayers(FIGHTING_PLAYERS))
			if (iterFPlayer.isShowRank())
				showScores(iterFPlayer.getPlayer());*/

		if (!isTeamed())
			updateScreenScores();
	}

	protected void showScores(Creature c)
	{
		Map<String, Integer> scores = getBestScores();

		FightClubPlayer fPlayer = getFightClubPlayer(c);
		fPlayer.setShowRank(true);

		c.sendPacket(new ExPVPMatchCCRecord(scores));
	}

	protected void hideScores(Creature c)
	{
		c.sendPacket(ExPVPMatchCCRetire.STATIC);
	}

	/**
	 * If player is AFK: getting him back from it
	 * If player isn't AFK: checking if he is in combat and asking if he really wants it
	 * @param fPlayer
	 * @param setAsAfk
	 */
	protected void handleAfk(FightClubPlayer fPlayer, boolean setAsAfk)
	{
		Player player = fPlayer.getPlayer();

		if (setAsAfk)
		{
			fPlayer.setAfk(true);
			fPlayer.setAfkStartTime(player.getLastNotAfkTime());

			sendMessageToPlayer(player, MESSAGE_TYPES.CRITICAL, "You are considered as AFK Player!");
		}
		else if (fPlayer.isAfk())
		{
			int totalAfkTime = (int)((System.currentTimeMillis() - fPlayer.getAfkStartTime()) / 1000);
			totalAfkTime -= TIME_TO_BE_AFK;
			if (totalAfkTime > 5)
			{
				fPlayer.setAfk(false);

				fPlayer.addTotalAfkSeconds(totalAfkTime);
				sendMessageToPlayer(player, MESSAGE_TYPES.CRITICAL, "You were afk for " + totalAfkTime + " seconds!");
			}
		}
	}

	/**
	 * @param player
	 * @param seconds
	 * @param sendMessages
	 * @seconds - If seconds > 0, starts new timer that stopsInvisiblity when its over.
	 * @SendMessages - Should timer send messages like VISIBLE IN X SECONDS?
	 */
	protected void setInvisible(Player player, int seconds, boolean sendMessages)
	{
		FightClubPlayer fPlayer = getFightClubPlayer(player);
		fPlayer.setInvisible(true);

		player.setInvisibleType(InvisibleType.NORMAL);
		player.startAbnormalEffect(AbnormalEffect.STEALTH);
		player.sendUserInfo(true);
		World.removeObjectFromPlayers(player);

		if (seconds > 0)
			startNewTimer(false, 0, "setInvisible", seconds, fPlayer, sendMessages);
	}

	protected void stopInvisibility(Player player)
	{
		FightClubPlayer fPlayer = getFightClubPlayer(player);

		if (fPlayer != null)
			fPlayer.setInvisible(false);

		player.setInvisibleType(InvisibleType.NONE);
		player.stopAbnormalEffect(AbnormalEffect.STEALTH);
		player.broadcastCharInfo();
		if (player.getPet() != null)
			player.getPet().broadcastCharInfo();
	}

	protected void rootPlayer(Player player)
	{
		if (!isRootBetweenRounds())
			return;
		List<Playable> toRoot = new ArrayList<>();
		toRoot.add(player);
		if (player.getPet() != null)
			toRoot.add(player.getPet());

		if (!player.isRooted())
		{
			player.startRooted();
		}
		player.stopMove();
		player.startAbnormalEffect(AbnormalEffect.ROOT);
	}

	protected void unrootPlayers()
	{
		if (!isRootBetweenRounds())
			return;

		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			Player player = iFPlayer.getPlayer();
			if (player.isRooted())
			{
				try
				{
					player.stopRooted();
				}
				catch (IllegalStateException e)
				{

				}
				player.stopAbnormalEffect(AbnormalEffect.ROOT);
			}
		}
	}

	protected void ressAndHealPlayers()
	{
		for (FightClubPlayer fPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			Player player = fPlayer.getPlayer();

			if (player.isDead())
				player.doRevive(100.);

			cancelNegativeEffects(player);
			if (player.getPet() != null)
				cancelNegativeEffects(player.getPet());

			buffPlayer(player);
		}
	}

	protected int getWarRelation()
	{
		int result = 0;

		result |= RelationChanged.RELATION_CLAN_MEMBER;
		result |= RelationChanged.RELATION_1SIDED_WAR;
		result |= RelationChanged.RELATION_MUTUAL_WAR;

		return result;
	}

	protected int getFriendRelation()
	{
		int result = 0;

		result |= RelationChanged.RELATION_CLAN_MEMBER;
		result |= RelationChanged.RELATION_CLAN_MATE;

		return result;
	}

	/**
	 * Getting one location out of locs and spawning npc in there.
	 * Set @respawnInSeconds to 0 if you don't want them to respawn
	 * @param id
	 * @param locs
	 * @param respawnInSeconds
	 * @return
	 */
	protected NpcInstance chooseLocAndSpawnNpc(int id, Location[] locs, int respawnInSeconds)
	{
		return spawnNpc(id, getSafeLocation(locs), respawnInSeconds);
	}

	/**
	 * Set @respawnInSeconds to 0 if you don't want them to respawn
	 * @param id
	 * @param loc
	 * @param respawnInSeconds
	 * @return
	 */
	protected NpcInstance spawnNpc(int id, Location loc, int respawnInSeconds)
	{
		SimpleSpawner spawn = new SimpleSpawner(id);
		spawn.setLoc(loc);
		spawn.setAmount(1);
		spawn.setHeading(loc.h);
		spawn.setRespawnDelay(Math.max(0, respawnInSeconds));
		spawn.setReflection(getReflection());
		List<NpcInstance> npcs = spawn.initAndReturn();

		if (respawnInSeconds <= 0)
			spawn.stopRespawn();

		return npcs.get(0);
	}

	/**
	 * @param seconds
	 * @return "5 minutes" after pasting 300 seconds
	 */
	protected static String getFixedTime(int seconds)
	{
		int minutes = seconds/60;
		String result = "";
		if (seconds >= 60)
		{
			result = minutes + " minute" + (minutes > 1 ? "s" : "");
		}
		else
		{
			result = seconds + " second" + (seconds > 1 ? "s" : "");
		}
		return result;
	}

	private void buffPlayer(Player player)
	{
		if (getBuffer())
		{
			int[][] buffs;
			if (player.isMageClass())
				buffs = _mageBuffs;
			else
				buffs = _fighterBuffs;

			giveBuffs(player, buffs);

			if (player.getPet() != null)
				giveBuffs(player.getPet(), _fighterBuffs);
		}
	}

	private static void giveBuffs(final Playable playable, int[][] buffs)
	{
		Skill buff;
		for (int[] buff1 : buffs)
		{
			buff = SkillTable.getInstance().getInfo(buff1[0], buff1[1]);
			if (buff == null)
			{
				continue;
			}
			buff.getEffects(playable, playable, false, false);
		}

		ThreadPoolManager.getInstance().schedule(() ->
		{
			playable.setCurrentHp(playable.getMaxHp(), true);
			playable.setCurrentMp(playable.getMaxMp());
			playable.setCurrentCp(playable.getMaxCp());
		}, 1000);
	}

	private void announceTopKillers(FightClubPlayer[] topKillers)
	{
		if (topKillers == null)
			return;

		for (FightClubPlayer fPlayer : topKillers)
			if (fPlayer != null)
			{
				String message = fPlayer.getPlayer().getName() + " had most kills" /*+ (_teamed ? " from " + fPlayer.getTeam().getName() + " Team" : "")*/ + " on " + getName() + " Event!";
				FightClubEventManager.getInstance().sendToAllMsg(this, message);
			}
	}

	public enum MESSAGE_TYPES {
		GM,
		NORMAL_MESSAGE,
		SCREEN_BIG,
		SCREEN_SMALL,
		CRITICAL
	}

	protected void sendMessageToFightingAndRegistered(MESSAGE_TYPES type, String msg)
	{
		sendMessageToFighting(type, msg, false);
		sendMessageToRegistered(type, msg);
	}

	protected void sendMessageToTeam(FightClubTeam team, MESSAGE_TYPES type, String msg)
	{
		//Team Members
		for (FightClubPlayer iFPlayer : team.getPlayers())
			sendMessageToPlayer(iFPlayer, type, msg);
	}
	protected void sendMessageToFighting(MESSAGE_TYPES type, String msg, boolean skipJustTeleported)//TODO ogarnac skipJustTeleported, to juz nie jest potrzebne
	{
		//Fighting
		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			if (!skipJustTeleported || !iFPlayer.isInvisible())
				sendMessageToPlayer(iFPlayer, type, msg);
	}

	protected void sendMessageToRegistered(MESSAGE_TYPES type, String msg)
	{
		//Registered
		for (FightClubPlayer iFPlayer : getPlayers(REGISTERED_PLAYERS))
			sendMessageToPlayer(iFPlayer, type, msg);
	}

	public void sendMessageToPlayer(FightClubPlayer fPlayer, MESSAGE_TYPES type, String msg)
	{
		sendMessageToPlayer(fPlayer.getPlayer(), type, msg);
	}

	protected void sendMessageToPlayer(Player player, MESSAGE_TYPES type, String msg)
	{
		switch (type)
		{
		case GM:
			player.sendPacket(new Say2(player.getObjectId(), ChatType.CRITICAL_ANNOUNCE, player.getName(), msg));
			updateScreenScores(player);
			break;
		case NORMAL_MESSAGE:
			player.sendMessage(msg);
			break;
		case SCREEN_BIG:
			player.sendPacket(new ExShowScreenMessage(msg, 3000, ExShowScreenMessage.ScreenMessageAlign.TOP_CENTER, true));
			updateScreenScores(player);
			break;
		case SCREEN_SMALL:
			player.sendPacket(new ExShowScreenMessage(msg, 600000, ExShowScreenMessage.ScreenMessageAlign.TOP_LEFT, false));
			break;
		case CRITICAL:
			player.sendPacket(new Say2(player.getObjectId(), ChatType.COMMANDCHANNEL_ALL, player.getName(), msg));
			updateScreenScores(player);
			break;
		}
	}

	public void setState(EVENT_STATE state)
	{
		_state = state;
	}

	//===============================================================================================================
	//												Event Getters
	//===============================================================================================================


	public EVENT_STATE getState()
	{
		return _state;
	}

	public int getObjectId()
	{
		return _objId;
	}

	public int getEventId()
	{
		return getId();
	}

	public String getDescription()
	{
		return _desc;
	}

	public String getIcon()
	{
		return _icon;
	}

	public boolean isAutoTimed()
	{
		return _isAutoTimed;
	}

	public int[][] getAutoStartTimes()
	{
		return _autoStartTimes;
	}

	public FightClubMap getMap()
	{
		return _map;
	}

	public boolean isTeamed()
	{
		return _teamed;
	}

	protected boolean isInstanced()
	{
		return _instanced;
	}

	@Override
	public Reflection getReflection()
	{
		return _reflection;
	}

	/**
	 * @return -1 in case
	 */
	public int getRoundRuntime()
	{
		return _roundRunTime;
	}

	public int getRespawnTime()
	{
		return _respawnTime;
	}

	public boolean isRoundEvent()
	{
		return _roundEvent;
	}

	public int getTotalRounds()
	{
		return _rounds;
	}

	public int getCurrentRound()
	{
		return _currentRound;
	}

	public boolean getBuffer()
	{
		return _buffer;
	}

	protected boolean isRootBetweenRounds()
	{
		return _rootBetweenRounds;
	}

	public boolean isLastRound()
	{
		return !isRoundEvent() || getCurrentRound() == getTotalRounds();
	}

	protected List<FightClubTeam> getTeams()
	{
		return _teams;
	}

	public MultiValueSet<String> getSet()
	{
		return _set;
	}

	public void clearSet()
	{
		_set = null;
	}

	public CLASSES[] getExcludedClasses()
	{
		return _excludedClasses;
	}

	public boolean isHidePersonality()
	{
		return !_showPersonality;
	}

	protected int getTeamTotalKills(FightClubTeam team)
	{
		if (!isTeamed())
			return 0;
		int totalKills = 0;
		for (FightClubPlayer iFPlayer : team.getPlayers())
			totalKills += iFPlayer.getKills(true);

		return totalKills;
	}

	/**
	 * @param groups
	 * @return players count from the groups
	 */
	public int getPlayersCount(String... groups)
	{
		return getPlayers(groups).size();
	}

	public List<FightClubPlayer> getPlayers(String... groups)
	{
		if (groups.length == 1)
		{
			List<FightClubPlayer> fPlayers = getObjects(groups[0]);
			return fPlayers;
		}
		else
		{
			List<FightClubPlayer> newList = new ArrayList<>();
			for (String group : groups)
			{
				List<FightClubPlayer> fPlayers = getObjects(group);
				newList.addAll(fPlayers);
			}
			return newList;
		}
	}

	public List<Player> getAllFightingPlayers()
	{
		List<FightClubPlayer> fPlayers = getPlayers(FIGHTING_PLAYERS);
		List<Player> players = new ArrayList<>(fPlayers.size());
		for (FightClubPlayer fPlayer : fPlayers)
			players.add(fPlayer.getPlayer());
		return players;
	}

	public List<Player> getMyTeamFightingPlayers(Player player)
	{
		FightClubTeam fTeam = getFightClubPlayer(player).getTeam();
		List<FightClubPlayer> fPlayers = getPlayers(FIGHTING_PLAYERS);
		List<Player> players = new ArrayList<>(fPlayers.size());

		if (!isTeamed())
		{
			player.sendPacket(new Say2(player.getObjectId(), ChatType.BATTLEFIELD, getName(), "(There are no teams, only you can see the message)"));
			players.add(player);
		}
		else
		{
			for (FightClubPlayer iFPlayer : fPlayers)
				if (iFPlayer.getTeam().equals(fTeam))
					players.add(iFPlayer.getPlayer());
		}
		return players;
	}

	/**
	 * Looking for FightClubPlayer ONLY in FIGHTING_PLAYERS
	 * @param creature
	 * @return
	 */
	public FightClubPlayer getFightClubPlayer(Creature creature)
	{
		return getFightClubPlayer(creature, FIGHTING_PLAYERS);
	}

	/**
	 * Looking for FightClubPlayer in specific groups
	 * @param creature
	 * @param groups
	 * @return
	 */
	public FightClubPlayer getFightClubPlayer(Creature creature, String... groups)
	{
		if (!creature.isPlayable())
			return null;

		int lookedPlayerId = creature.getPlayer().getObjectId();

		for (FightClubPlayer iFPlayer : getPlayers(groups))
			if (iFPlayer.getPlayer().getObjectId() == lookedPlayerId)
				return iFPlayer;
		return null;
	}


	//===============================================================================================================
	//												Private actions
	//===============================================================================================================

	/**
	 * Getting teams and partys from FightClubGameRoom and making them actually exist
	 */
	protected void spreadIntoTeamsAndPartys()
	{
		//Creating teams
		for (int i = 0 ; i < _room.getTeamsCount() ; i++)
		{
			_teams.add(new FightClubTeam(i+1));
		}

		// Sort the player list by level
		final List<Player> players = new ArrayList<>(_room.getAllPlayers());

		// Synerge - Sort participants list by level and then by class, trying to make the teams as equal as possible
		Collections.sort(players, PlayerComparator.getComparator(PlayerComparator.LEVEL_SORT, PlayerComparator.CLASS_SORT));

		int index = 0;
		for (Player player : players)
		{
			FightClubPlayer fPlayer = getFightClubPlayer(player, REGISTERED_PLAYERS);
			if (fPlayer == null)
				continue;

			final FightClubTeam team = _teams.get(index % _room.getTeamsCount());
			fPlayer.setTeam(team);
			team.addPlayer(fPlayer);

			index++;
		}

		for (FightClubTeam team : _teams)
		{
			List<List<Player>> partys = spreadTeamInPartys(team);
			for (List<Player> party : partys)
			{
				createParty(party);
			}
		}
	}

	/**
	 * Spreading Players in team into List of Partys(Party = List<Player> with 9 as MAX Count)
	 * @param team team to create Partys
	 * @return List<Party(List<Player>)>
	 */
	protected List<List<Player>> spreadTeamInPartys(FightClubTeam team)
	{
		//Creating Map<Class, List of Players>
		Map<CLASSES, List<Player>> classesMap = new FastMap<>();
		for (CLASSES clazz : CLASSES.values())
		{
			classesMap.put(clazz, new ArrayList<Player>());
		}

		//Adding players to map
		for (FightClubPlayer iFPlayer : team.getPlayers())
		{
			Player player = iFPlayer.getPlayer();
			CLASSES clazz = FightClubGameRoom.getPlayerClassGroup(player);
			classesMap.get(clazz).add(player);
		}

		//Getting Party Count
		int partyCount = (int) Math.ceil(team.getPlayers().size()/(double) Party.MAX_SIZE);

		//Creating "Partys"
		List<List<Player>> partys = new ArrayList<>();
		for (int i = 0;i<partyCount;i++)
		{
			partys.add(new ArrayList<Player>());
		}

		if (partyCount == 0)
			return partys;

		//Adding players from map to Partys
		int finishedOnIndex = 0;
		for (Entry<CLASSES, List<Player>> clazzEntry : classesMap.entrySet())
		{
			for (Player player : clazzEntry.getValue())
			{
				partys.get(finishedOnIndex).add(player);
				finishedOnIndex++;
				if (finishedOnIndex == partyCount)
					finishedOnIndex = 0;
			}
		}

		return partys;
	}

	/**
	 * Creating Real Party from List<Player>
	 * Checking listOfPlayers count, removing from current party
	 * @param listOfPlayers players to create party
	 */
	protected void createParty(List<Player> listOfPlayers)
	{
		if (listOfPlayers.size() <= 1)
			return;

		Party newParty = null;
		for (Player player : listOfPlayers)
		{
			if (player.getParty() != null)
				player.getParty().removePartyMember(player, true);

			if (newParty == null)
			{
				player.setParty(newParty = new Party(player, Party.ITEM_ORDER_SPOIL));
			}
			else
			{
				player.joinParty(newParty);
			}
		}
	}

	private synchronized void createReflection(IntObjectMap<DoorTemplate> doors, Map<String, ZoneTemplate> zones)
	{
		InstantZone iz = InstantZoneHolder.getInstance().getInstantZone(INSTANT_ZONE_ID);

		_reflection = new Reflection();
		_reflection.init(iz);
		_reflection.init(doors, zones);

		for (Zone zone : _reflection.getZones())
			zone.addListener(_zoneListener);
	}
	/**
	 * @param locations
	 * @return Finding location where nobodyIsClose
	 */
	private Location getSafeLocation(Location[] locations)
	{
		Location safeLoc = null;
		int checkedCount = 0;
		boolean isOk = false;

		while (!isOk)
		{
			safeLoc = Rnd.get(locations);

			//Checking if nobody is close to spawn, only in single player events
			isOk = nobodyIsClose(safeLoc);
			checkedCount++;

			//If players are close to every spawn, choose something anyway.
			if (checkedCount > locations.length*2)
				isOk = true;
		}

		return safeLoc;
	}

	/**
	 * @param fPlayer
	 * @param randomNotClosestToPt
	 * @return
	 * @randomNotClosestToPt If true - @return random location from team locs
	 * @randomNotClosestToPt If false - @return location closest to team members
	 */
	protected Location getTeamSpawn(FightClubPlayer fPlayer, boolean randomNotClosestToPt)
	{
		FightClubTeam team = fPlayer.getTeam();
		Location[] spawnLocs = getMap().getTeamSpawns().get(team.getIndex());

		if (randomNotClosestToPt || _state != EVENT_STATE.STARTED)
		{
			return Rnd.get(spawnLocs);
			//if (team.getSpawnLoc() == null)
			//{
				//team.setSpawnLoc(Rnd.get(spawnLocs));
			//}
			//return team.getSpawnLoc();
		}
		else
		{
			List<Player> playersToCheck = new ArrayList<>();
			if (fPlayer.getParty() != null)
				playersToCheck = fPlayer.getParty().getMembers();
			else
				for (FightClubPlayer iFPlayer : team.getPlayers())
					playersToCheck.add(iFPlayer.getPlayer());

			Map<Location, Integer> spawnLocations = new FastMap<>(spawnLocs.length);
			for (Location loc : spawnLocs)
				spawnLocations.put(loc, 0);

			for (Player player : playersToCheck)
				if (player != null && player.isOnline() && !player.isDead())
				{
					Location winner = null;
					double winnerDist = -1;
					for (Location loc : spawnLocs)
						if (winnerDist <= 0 || winnerDist < player.getDistance(loc))
						{
							winner = loc;
							winnerDist = player.getDistance(loc);
						}

					if (winner != null)
						spawnLocations.put(winner, spawnLocations.get(winner) + 1);
				}

			Location winner = null;
			double points = -1;
			for (Entry<Location, Integer> spawn : spawnLocations.entrySet())
				if (points < spawn.getValue())
				{
					winner = spawn.getKey();
					points = spawn.getValue();
				}

			if (points <= 0)
				return Rnd.get(spawnLocs);
			return winner;
		}
	}

	protected boolean isPlayerActive(Player player)
	{
		if (player == null)
			return false;
		if (player.isDead())
			return false;
		if (!player.getReflection().equals(getReflection()))
			return false;
		if (System.currentTimeMillis() - player.getLastNotAfkTime() > 120000)//If inactive for at least 2 minutes
			return false;

		boolean insideZone = false;
		for (Zone zone : getReflection().getZones())
			if (zone.checkIfInZone(player.getX(), player.getY(), player.getZ(), player.getReflection()))
				insideZone = true;
		if (!insideZone)
			return false;

		return true;
	}

	/**
	 * Sending rewardPlayer method to every FIGHTING_PLAYERS
	 * @param topKillers
	 */
	private void giveRewards(FightClubPlayer[] topKillers)
	{
		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			if (iFPlayer != null)
				rewardPlayer(iFPlayer, Util.arrayContains(topKillers, iFPlayer));
	}

	private void showLastAFkMessage()
	{
		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			int minutesAFK = (int) Math.round((double)iFPlayer.getTotalAfkSeconds()/60);
			int badgesDecreased = -minutesAFK * BADGES_FOR_MINUTE_OF_AFK;
			sendMessageToPlayer(iFPlayer, MESSAGE_TYPES.NORMAL_MESSAGE, "Reward decreased by "+badgesDecreased+" Coins for AFK time!");
		}
	}

	/**
	 * @return 25 <Name, Points> - players with most points
	 */
	private Map<String, Integer> getBestScores()
	{
		if (!_scoredUpdated)
			return _bestScores;

		List<Integer> points = new ArrayList<Integer>(_scores.values());
		Collections.sort(points);
		Collections.reverse(points);

		int cap;
		if (points.size() <= 26)
			cap = points.get(points.size() - 1).intValue();
		else
			cap = points.get(25).intValue();
		Map<String, Integer> finalResult = new LinkedHashMap<>();

		List<Entry<String, Integer>> toAdd = new ArrayList<>();
		for (Entry<String, Integer> i : _scores.entrySet())
			if (i.getValue().intValue() > cap && finalResult.size() < 25)
				toAdd.add(i);

		if (finalResult.size() < 25)
		{
			for (Entry<String, Integer> i : _scores.entrySet())
				if (i.getValue().intValue() == cap)
				{
					toAdd.add(i);
					if (finalResult.size() == 25)
						break;
				}
		}

		for (int i = 0;i<toAdd.size();i++)
		{
			Entry<String, Integer> biggestEntry = null;
			for (Entry<String, Integer> entry : toAdd)
			{
				if (!finalResult.containsKey(entry.getKey()) && (biggestEntry == null || entry.getValue().intValue() > biggestEntry.getValue().intValue()))
					biggestEntry = entry;
			}
			if (biggestEntry != null)
				finalResult.put(biggestEntry.getKey(), biggestEntry.getValue());
		}

		_bestScores = finalResult;
		_scoredUpdated = false;

		return finalResult;
	}

	/**
	 * Updating score of every FIGHTING_PLAYERS
	 */
	private void updateEveryScore()
	{
		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			_scores.put(getScorePlayerName(iFPlayer), iFPlayer.getKills(true));
			_scoredUpdated = true;
		}
	}

	/**
	 * @param showScoreNotKills - true - Score: - false - "Kills:"
	 * @param teamPointsNotInvidual - true: Team Score/Kills, false: Player Score/Kills
	 * @return
	 */
	private String getScreenScores(boolean showScoreNotKills, boolean teamPointsNotInvidual)
	{
		StringBuilder builder = new StringBuilder();
		if (_teamed && teamPointsNotInvidual)
		{
			List<FightClubTeam> teams = getTeams();
			Collections.sort(teams, new BestTeamComparator(showScoreNotKills));
			for (FightClubTeam team : teams)
				builder.append(team.getName()).append(" Team: ").append(showScoreNotKills ? team.getScore() : getTeamTotalKills(team)).append(' ').append(showScoreNotKills ? "Points" : "Kills").append('\n');
		}
		else
		{
			List<FightClubPlayer> fPlayers = getPlayers(FIGHTING_PLAYERS);
			List<FightClubPlayer> changedFPlayers = new ArrayList<>(fPlayers.size());
			changedFPlayers.addAll(fPlayers);

			Collections.sort(changedFPlayers, new BestPlayerComparator(showScoreNotKills));
			int max = Math.min(10, changedFPlayers.size());
			for (int i = 0;i<max;i++)
				builder.append(changedFPlayers.get(i).getPlayer().getName()).append(' ').append(showScoreNotKills ? "Score" : "Kills").append(": ").append(showScoreNotKills ? changedFPlayers.get(i).getScore() : changedFPlayers.get(i).getKills(true)).append('\n');
		}

		return builder.toString();
	}

	protected int getRewardForWinningTeam(FightClubPlayer fPlayer, boolean atLeast1Kill)
	{
		if (!_teamed || _state != EVENT_STATE.OVER && _state != EVENT_STATE.NOT_ACTIVE)
			return 0;

		if (atLeast1Kill && fPlayer.getKills(true) <= 0 && FightClubGameRoom.getPlayerClassGroup(fPlayer.getPlayer()) != CLASSES.HEALERS)
			return 0;

		FightClubTeam winner = null;
		int winnerPoints = -1;
		boolean sameAmount = false;
		for (FightClubTeam team : getTeams())
		{
			if (team.getScore() > winnerPoints)
			{
				winner = team;
				winnerPoints = team.getScore();
				sameAmount = false;
			}
			else if (team.getScore() == winnerPoints)
			{
				sameAmount = true;
			}
		}

		if (!sameAmount && fPlayer.getTeam().equals(winner))
		{
			return (int)_badgeWin;
		}
		else
		{
			return 0;
		}
	}

	/**
	 * @param loc
	 * @return checking if nobody is near(CLOSE_LOCATIONS_VALUE) loc
	 */
	private boolean nobodyIsClose(Location loc)
	{
		for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			Location playerLoc = iFPlayer.getPlayer().getLoc();
			if (Math.abs(playerLoc.getX()-loc.getX()) <= CLOSE_LOCATIONS_VALUE)
				return false;
			if (Math.abs(playerLoc.getY()-loc.getY()) <= CLOSE_LOCATIONS_VALUE)
				return false;
		}
		return true;
	}

	/**
	 * Checking every REGISTERED_PLAYERS if he meets criteria
	 */
	private void checkIfRegisteredMeetCriteria()
	{
		for (FightClubPlayer iFPlayer : getPlayers(REGISTERED_PLAYERS))
		{
			checkIfRegisteredPlayerMeetCriteria(iFPlayer);
		}
	}

	/**
	 * If he doesn't, unregistering player
	 * @param fPlayer
	 * @return player meets criteria
	 */
	private boolean checkIfRegisteredPlayerMeetCriteria(FightClubPlayer fPlayer)
	{
		if (!FightClubEventManager.getInstance().canPlayerParticipate(fPlayer.getPlayer(), true, true))
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	/**
	 * Removing all debuffs
	 * @param playable
	 */
	private void cancelNegativeEffects(Playable playable)
	{
		List<Effect> _buffList = new ArrayList<Effect>();

		for (Effect e : playable.getEffectList().getAllEffects())
		{
			if (e.isOffensive() && e.isCancelable())
				_buffList.add(e);
		}

		for (Effect e : _buffList)
		{
			e.exit();
		}
	}

	/**
	 * @param classes like TANKS;DAMAGE_DEALERS
	 * @return array of CLASS_TYPES
	 */
	private CLASSES[] parseExcludedClasses(String classes)
	{
		if (classes.equals(""))
			return new CLASSES[0];

		String[] classType = classes.split(";");
		CLASSES[] realTypes = new CLASSES[classType.length];

		for (int i = 0;i<classType.length;i++)
			realTypes[i] = CLASSES.valueOf(classType[i]);

		return realTypes;
	}

	protected int[] parseExcludedSkills(String ids)
	{
		if (ids == null || ids.isEmpty())
			return null;
		StringTokenizer st = new StringTokenizer(ids, ";");
		int[] realIds = new int[st.countTokens()];
		int index = 0;
		while (st.hasMoreTokens())
		{
			realIds[index] = Integer.parseInt(st.nextToken());
			index++;
		}
		return realIds;
	}

	private int[][] parseAutoStartTimes(String times)
	{
		if (times == null || times.isEmpty())
			return null;

		StringTokenizer st = new StringTokenizer(times, ",");
		int[][] realTimes = new int[st.countTokens()][2];
		int index = 0;
		while (st.hasMoreTokens())
		{
			String[] hourMin = st.nextToken().split(":");
			int[] realHourMin = {Integer.parseInt(hourMin[0]), Integer.parseInt(hourMin[1])};
			realTimes[index] = realHourMin;
			index++;
		}
		return realTimes;
	}

	private int[][] parseBuffs(String buffs)
	{
		if (buffs == null || buffs.isEmpty())
			return null;

		StringTokenizer st = new StringTokenizer(buffs, ";");
		int[][] realBuffs = new int[st.countTokens()][2];
		int index = 0;
		while (st.hasMoreTokens())
		{
			String[] skillLevel = st.nextToken().split(",");
			int[] realHourMin = {Integer.parseInt(skillLevel[0]), Integer.parseInt(skillLevel[1])};
			realBuffs[index] = realHourMin;
			index++;
		}
		return realBuffs;
	}

	/**
	 * Stops: {5, 15, 30, 60, 300, 600, 900}
	 * @param totalLeftTimeInSeconds
	 * @return
	 */
	private int getTimeToWait(int totalLeftTimeInSeconds)
	{
		int toWait = 1;

		int[] stops = {5, 15, 30, 60, 300, 600, 900};

		for (int stop : stops)
			if (totalLeftTimeInSeconds > stop)
				toWait = stop;

		return toWait;
	}

	/**
	 * Thread that adds Seconds spent on event to every fPlayer
	 */
	private class TimeSpentOnEventThread extends RunnableImpl
	{
		@Override
		public void runImpl()
		{
			if (_state == EVENT_STATE.STARTED)
			{
				for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
				{
					if (iFPlayer.getPlayer() == null || !iFPlayer.getPlayer().isOnline())
						continue;

					if (iFPlayer.isAfk())
						continue;

					iFPlayer.incSecondsSpentOnEvent(10);
				}
			}

			if (_state != EVENT_STATE.NOT_ACTIVE)
				ThreadPoolManager.getInstance().schedule(new TimeSpentOnEventThread(), 10*1000);
		}
	}

	private class LeftZoneThread extends RunnableImpl
	{
		@Override
		public void runImpl()
		{
			List<FightClubPlayer> toDelete = new ArrayList<>();
			Say2 packet = new Say2(0, ChatType.COMMANDCHANNEL_ALL, "Error", "Go Back To Event Zone!");

			for (Entry<FightClubPlayer, Zone> entry : _leftZone.entrySet())
			{
				Player player = entry.getKey().getPlayer();
				if (player == null || !player.isOnline() || _state == EVENT_STATE.NOT_ACTIVE || entry.getValue().checkIfInZone(player) || player.isDead()
						|| player.isTeleporting())
				{
					toDelete.add(entry.getKey());
					continue;
				}

				int power = (int)Math.max(400, entry.getValue().findDistanceToZone(player, true)-4000);

				player.sendPacket(new Earthquake(player.getLoc(), power, 5));
				player.sendPacket(packet);
				entry.getKey().increaseSecondsOutsideZone();

				if (entry.getKey().getSecondsOutsideZone() >= TIME_MAX_SECONDS_OUTSIDE_ZONE)
				{
					player.doDie(null);
					toDelete.add(entry.getKey());
					entry.getKey().clearSecondsOutsideZone();
				}
			}

			for (FightClubPlayer playerToDelete : toDelete)
			{
				if (playerToDelete != null)
				{
					_leftZone.remove(playerToDelete);
					playerToDelete.clearSecondsOutsideZone();
				}
			}

			if (_state != EVENT_STATE.NOT_ACTIVE)
				ThreadPoolManager.getInstance().schedule(this, 1000L);
		}
	}

	protected boolean isAfkTimerStopped(Player player)
	{
		return player.isDead() && !_ressAllowed && _respawnTime <= 0;
	}

	private class CheckAfkThread extends RunnableImpl
	{
		@Override
		public void runImpl()
		{
			long currentTime = System.currentTimeMillis();
			for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
			{
				Player player = iFPlayer.getPlayer();
				boolean isAfk = (player.getLastNotAfkTime() + TIME_TO_BE_AFK*1000) < currentTime;

				if (isAfkTimerStopped(player))//Cannot do any actions, doesn't mean he is afk
					continue;

				if (iFPlayer.isAfk())
				{
					if (!isAfk)
					{
						handleAfk(iFPlayer, false);//Just came back from afk
					}
					else if (_state != EVENT_STATE.OVER)
					{
						sendMessageToPlayer(player, MESSAGE_TYPES.CRITICAL, "You are in AFK mode!");
					}
				}
				else if (_state == EVENT_STATE.NOT_ACTIVE)
				{
					handleAfk(iFPlayer, false);
				}
				else if (isAfk)
				{
					handleAfk(iFPlayer, true);//Just started to be afk
				}
			}

			if (getState() != EVENT_STATE.NOT_ACTIVE)
			{
				ThreadPoolManager.getInstance().schedule(this, 1000);
			}
			else
			{
				for (FightClubPlayer iFPlayer : getPlayers(FIGHTING_PLAYERS))
				{
					if (iFPlayer.isAfk())
						handleAfk(iFPlayer, false);
				}
			}
		}
	}

	private class BestTeamComparator implements Comparator<FightClubTeam>, Serializable
	{
		private static final long serialVersionUID = -7744947898101934099L;
		private final boolean _scoreNotKills;
		private BestTeamComparator(boolean scoreNotKills)
		{
			_scoreNotKills = scoreNotKills;
		}

		@Override
		public int compare(FightClubTeam o1, FightClubTeam o2)
		{
			if (_scoreNotKills)
				return Integer.compare(o2.getScore(), o1.getScore());
			else
			{
				return Integer.compare(getTeamTotalKills(o2), getTeamTotalKills(o1));
			}
		}
	}

	private static class BestPlayerComparator implements Comparator<FightClubPlayer>, Serializable
	{
		private static final long serialVersionUID = -7889180585474342293L;
		private final boolean _scoreNotKills;
		private BestPlayerComparator(boolean scoreNotKills)
		{
			_scoreNotKills = scoreNotKills;
		}

		@Override
		public int compare(FightClubPlayer arg0, FightClubPlayer arg1)
		{
			if (_scoreNotKills)
				return Integer.compare(arg1.getScore(), arg0.getScore());
			else
				return Integer.compare(arg1.getKills(true), arg0.getKills(true));
		}
	}

	//===============================================================================================================
	//												Event Timers
	//===============================================================================================================

	/**
	 * Big Timer - Waiting for first players, later registered teleporting to event
	 * @param eventObjId
	 * @param secondsLeft
	 * @return
	 */
	@Deprecated
	public static boolean teleportWholeRoomTimer(int eventObjId, int secondsLeft)
	{
		AbstractFightClub event = FightClubEventManager.getInstance().getEventByObjId(eventObjId);
		if (secondsLeft == 0)
		{
			event._dontLetAnyoneIn = true;
			event.startEvent();
		}
		else
		{
			event.checkIfRegisteredMeetCriteria();
			event.sendMessageToRegistered(MESSAGE_TYPES.SCREEN_BIG, "You are going to be teleported in "+getFixedTime(secondsLeft)+"!");
		}
		return true;
	}

	/**
	 * Big Timer - Starting round timer
	 * @param eventObjId
	 * @param secondsLeft
	 * @return
	 */
	@Deprecated
	public static boolean startRoundTimer(int eventObjId, int secondsLeft)
	{
		AbstractFightClub event = FightClubEventManager.getInstance().getEventByObjId(eventObjId);

		if (secondsLeft > 0)
		{
			String firstWord;
			if (event.isRoundEvent())
				firstWord = ((event.getCurrentRound() + 1) == event.getTotalRounds() ? "Last" : ROUND_NUMBER_IN_STRING[event.getCurrentRound()+1]) + " Round";
			else
				firstWord = "Match";
			String message = firstWord+" is going to start in "+getFixedTime(secondsLeft)+"!";
			event.sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, message, true);
		}
		else
			event.startRound();

		return true;
	}

	/**
	 * Big timer - Watching when event ends
	 * @param eventObjId
	 * @param secondsLeft
	 * @return
	 */
	@Deprecated
	public static boolean endRoundTimer(int eventObjId, int secondsLeft)
	{
		AbstractFightClub event = FightClubEventManager.getInstance().getEventByObjId(eventObjId);
		if (secondsLeft > 0)
			event.sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, (!event.isLastRound() ? "Round" : "Match")+" is going to be Over in "+getFixedTime(secondsLeft)+"!", false);
		else
			event.endRound();

		return true;
	}

	/**
	 * Small timer - shutdown
	 * @param eventObjId
	 * @param secondsLeft
	 * @return
	 */
	@Deprecated
	public static boolean shutDownTimer(int eventObjId, int secondsLeft)
	{
		AbstractFightClub event = FightClubEventManager.getInstance().getEventByObjId(eventObjId);

		if (!FightClubEventManager.getInstance().serverShuttingDown())
		{
			event._dontLetAnyoneIn = false;
			return false;
		}

		if (secondsLeft < 180)
		{
			//Check to make it just once
			if (!event._dontLetAnyoneIn)
			{
				event.sendMessageToRegistered(MESSAGE_TYPES.CRITICAL, "You are no longer registered because of Shutdown!");
				for (FightClubPlayer player : event.getPlayers(REGISTERED_PLAYERS))
				{
					event.unregister(player.getPlayer());
				}
				event.getObjects(REGISTERED_PLAYERS).clear();
				event._dontLetAnyoneIn = true;
			}
		}

		if (secondsLeft < 60)
		{
			event._timer.cancel(false);
			event.sendMessageToFighting(MESSAGE_TYPES.CRITICAL, "Event ended because of Shutdown!", false);
			event.setState(EVENT_STATE.OVER);
			event.stopEvent();

			event._dontLetAnyoneIn = false;
			return false;
		}
		return true;
	}

	/**
	 * Small Timer - Teleporting player back from the event. Remove him from players list before
	 * @param eventObjId
	 * @param secondsLeft
	 * @param player
	 * @return
	 */
	@Deprecated
	public static boolean teleportBackSinglePlayerTimer(int eventObjId, int secondsLeft, Player player)
	{
		AbstractFightClub event = FightClubEventManager.getInstance().getEventByObjId(eventObjId);

		if (player == null || !player.isOnline())
			return false;

		if (secondsLeft > 0)
			event.sendMessageToPlayer(player, MESSAGE_TYPES.SCREEN_BIG, "You are going to be teleported back in "+getFixedTime(secondsLeft)+"!");
		else
		{
			event.teleportBackToTown(player);
		}

		return true;
	}

	/**
	 * Small Timer - Ressurecting player
	 * @param eventObjId
	 * @param secondsLeft
	 * @param fPlayer
	 * @return
	 */
	@Deprecated
	public static boolean ressurectionTimer(int eventObjId, int secondsLeft, FightClubPlayer fPlayer)
	{
		AbstractFightClub event = FightClubEventManager.getInstance().getEventByObjId(eventObjId);
		Player player = fPlayer.getPlayer();

		if (player == null || !player.isOnline() || !player.isDead())
			return false;

		if (secondsLeft > 0)
			player.sendMessage("Respawn in "+getFixedTime(secondsLeft)+"!");
		else
		{
			event.hideScores(player);
			event.teleportSinglePlayer(fPlayer, false, true);
		}
		return true;
	}

	/**
	 * Small Timer - Making player visible again
	 * @param eventObjId
	 * @param secondsLeft
	 * @param fPlayer
	 * @param sendMessages
	 * @return
	 */
	@Deprecated
	public static boolean setInvisible(int eventObjId, int secondsLeft, FightClubPlayer fPlayer, boolean sendMessages)
	{
		AbstractFightClub event = FightClubEventManager.getInstance().getEventByObjId(eventObjId);
		if (fPlayer.getPlayer() == null || !fPlayer.getPlayer().isOnline())
			return false;

		if (secondsLeft > 0)
		{
			if (sendMessages)
				event.sendMessageToPlayer(fPlayer, MESSAGE_TYPES.SCREEN_BIG, "Visible in "+getFixedTime(secondsLeft)+"!");
		}
		else
		{
			if (sendMessages && event.getState() == EVENT_STATE.STARTED)
				event.sendMessageToPlayer(fPlayer, MESSAGE_TYPES.SCREEN_BIG, "Fight!");
			event.stopInvisibility(fPlayer.getPlayer());
		}
		return true;

	}

	/**
	 * Starting new Small or Big timer
	 * @param saveAsMainTimer
	 * @param firstWaitingTimeInMilis
	 * @param methodName
	 * @param args
	 * @saveAsMainTimer - should it be save to _timer?
	 * @firstWaitingTimeInMilis - time before sending first msg
	 * @methodName - name of the method that will be run, between every sleep and at the end
	 * @args - arguments that method takes(except int eventObjId)
	 */
	public void startNewTimer(boolean saveAsMainTimer, int firstWaitingTimeInMilis, String methodName, Object... args)
	{
		ScheduledFuture<?> timer = ThreadPoolManager.getInstance().schedule(new SmartTimer(methodName, saveAsMainTimer, args), firstWaitingTimeInMilis);

		if (saveAsMainTimer)
			_timer = timer;
	}

	private class SmartTimer extends RunnableImpl
	{
		private final String _methodName;
		private final Object[] _args;
		private final boolean _saveAsMain;

		private SmartTimer(String methodName, boolean saveAsMainTimer, Object... args)
		{
			_methodName = methodName;

			Object[] changedArgs = new Object[args.length+1];
			changedArgs[0] = getObjectId();
			for (int i = 0;i<args.length;i++)
				changedArgs[i+1] = args[i];
			_args = changedArgs;
			_saveAsMain = saveAsMainTimer;
		}

		@Override
		public void runImpl()
		{
			//Preparing parameters
			Class<?>[] parameterTypes = new Class<?>[_args.length];
			for (int i = 0; i < _args.length; i++)
				parameterTypes[i] = _args[i] != null ? _args[i].getClass() : null;

			int waitingTime = (int) _args[1];

			try
			{
				Object ret = MethodUtils.invokeMethod(AbstractFightClub.this, _methodName, _args, parameterTypes);

				if ((boolean)ret == false)
					return;
			}
			catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
			{
				e.printStackTrace();
			}

			if (waitingTime > 0)
			{
				int toWait = getTimeToWait(waitingTime);

				waitingTime -= toWait;

				_args[1] = waitingTime;

				ScheduledFuture<?> timer = ThreadPoolManager.getInstance().schedule(this, toWait*1000);

				if (_saveAsMain)
					_timer = timer;
			}
			else
				return;
		}
	}

	@Override
	public void reCalcNextTime(boolean onInit)
	{
		clearActions();

		registerActions();
	}

	@Override
	protected long startTimeMillis()
	{
		return 0;
	}

	@Override
	public void onAddEvent(GameObject o)
	{
		if (o.isPlayer())
		{
			o.getPlayer().addListener(_exitListener);
		}
	}

	@Override
	public void onRemoveEvent(GameObject o)
	{
		if (o.isPlayer())
		{
			o.getPlayer().removeListener(_exitListener);
		}
	}

	@Override
	public boolean isInProgress()
	{
		return _state != EVENT_STATE.NOT_ACTIVE;
	}

	// Enum especial que contiene muchos sorts para los pjs y una funcion para hacer sorting encadenado
	public static enum PlayerComparator implements Comparator<Player>
	{
		LEVEL_SORT
		{
			@Override
			public int compare(Player left, Player right)
			{
				if (left == null || right == null)
					return 0;

		        return Integer.valueOf(left.getLevel()).compareTo(right.getLevel());
			}
		},
		CLASS_SORT
		{
			@Override
			public int compare(Player left, Player right)
			{
				if (left == null || right == null)
					return 0;

		        return Integer.valueOf(left.getClassId().getType2().ordinal()).compareTo(right.getClassId().getType2().ordinal());
			}
		};

		// Este comparador funciona como un comparador encadenado, donde si es igual se sigue al proximo y asi hasta lograr un resultado
		public static Comparator<Player> getComparator(final PlayerComparator... multipleOptions)
		{
			return new Comparator<Player>()
			{
				@Override
				public int compare(Player o1, Player o2)
				{
					for (PlayerComparator option : multipleOptions)
					{
						int result = option.compare(o1, o2);
						if (result != 0)
						{
							return result;
						}
					}
					return 0;
				}
			};
		}
	}
}
