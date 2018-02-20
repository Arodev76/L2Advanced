package l2f.gameserver.model.entity.olympiad;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2f.commons.configuration.Config;
import l2f.commons.lang.ArrayUtils;
import l2f.commons.util.Rnd;
import l2f.gameserver.cache.Msg;
import l2f.gameserver.data.xml.holder.InstantZoneHolder;
import l2f.gameserver.instancemanager.OlympiadHistoryManager;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Party;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.base.TeamType;
import l2f.gameserver.model.entity.Reflection;
import l2f.gameserver.model.instances.DoorInstance;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.model.quest.QuestState;
import l2f.gameserver.network.serverpackets.ExOlympiadUserInfo;
import l2f.gameserver.network.serverpackets.ExReceiveOlympiad;
import l2f.gameserver.network.serverpackets.L2GameServerPacket;
import l2f.gameserver.network.serverpackets.Say2;
import l2f.gameserver.network.serverpackets.SystemMessage;
import l2f.gameserver.network.serverpackets.SystemMessage2;
import l2f.gameserver.network.serverpackets.components.ChatType;
import l2f.gameserver.network.serverpackets.components.IStaticPacket;
import l2f.gameserver.network.serverpackets.components.NpcString;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.scripts.Functions;
import l2f.gameserver.templates.InstantZone;
import l2f.gameserver.templates.StatsSet;
import l2f.gameserver.utils.Log;

public class OlympiadGame
{
	private static final Logger _log = LoggerFactory.getLogger(OlympiadGame.class);

	public static final int MAX_POINTS_LOOSE = 10;
	private static final int[] STADIUMS_INSTANCE_ID = {147, 148, 149, 150};

	public boolean validated = false;

	private int _winner = 0;
	private int _state = 0;

	private final int _id;
	private final Reflection _reflection;
	private final CompType _type;

	private final OlympiadTeam _team1;
	private final OlympiadTeam _team2;

	private final List<Player> _spectators = new CopyOnWriteArrayList<Player>();

	private long _startTime;

	private boolean _buffersSpawned = false;

	public OlympiadGame(int id, CompType type, List<Integer> opponents)
	{
		_type = type;
		_id = id;
		_reflection = new Reflection();
		InstantZone instantZone = InstantZoneHolder.getInstance().getInstantZone(Rnd.get(STADIUMS_INSTANCE_ID));
		_reflection.init(instantZone);

		_team1 = new OlympiadTeam(this, 1);
		_team2 = new OlympiadTeam(this, 2);

		for (int i = 0; i < opponents.size() / 2; i++)
		{
			_team1.addMember(opponents.get(i));
		}

		for (int i = opponents.size() / 2; i < opponents.size(); i++)
		{
			_team2.addMember(opponents.get(i));
		}

		Log.add("Olympiad System: Game - " + id + ": " + _team1.getName() + " Vs " + _team2.getName(), "olympiad");
	}

	public void addBuffers()
	{
		if (!_type.hasBuffer())
			return;

		_reflection.spawnByGroup("olympiad_" + _reflection.getInstancedZoneId() + "_buffers");
		_buffersSpawned = true;
	}

	public void deleteBuffers()
	{
		if (!_buffersSpawned)
			return;

		_reflection.despawnByGroup("olympiad_" + _reflection.getInstancedZoneId() + "_buffers");
		_buffersSpawned = false;
	}

	public void managerShout()
	{
		for(NpcInstance npc : Olympiad.getNpcs())
		{
			NpcString npcString;
			switch (_type)
			{
				case TEAM:
					npcString = NpcString.OLYMPIAD_CLASSFREE_TEAM_MATCH_IS_GOING_TO_BEGIN_IN_ARENA_S1_IN_A_MOMENT;
					break;
				case CLASSED:
					npcString = NpcString.OLYMPIAD_CLASS_INDIVIDUAL_MATCH_IS_GOING_TO_BEGIN_IN_ARENA_S1_IN_A_MOMENT;
					break;
				case NON_CLASSED:
					npcString = NpcString.OLYMPIAD_CLASSFREE_INDIVIDUAL_MATCH_IS_GOING_TO_BEGIN_IN_ARENA_S1_IN_A_MOMENT;
					break;
				default:
					continue;
			}
			Functions.npcShout(npc, npcString, String.valueOf(_id + 1));
		}
	}

	public void portPlayersToArena()
	{
		_team1.portPlayersToArena();
		_team2.portPlayersToArena();
	}

	public void preparePlayers()
	{
		setState(1);
		_team1.preparePlayers();
		_team2.preparePlayers();
	}

	public void startComp()
	{
		setState(2);
		_team1.startComp();
		_team2.startComp();
	}

	public void portPlayersBack()
	{
		_team1.portPlayersBack();
		_team2.portPlayersBack();
	}

	public void heal()
	{
		_team1.heal();
		_team2.heal();
	}

	public void collapse()
	{
		portPlayersBack();
		clearSpectators();
		deleteBuffers();
		_reflection.collapse();
	}

	public void validateWinner(boolean aborted)
	{
		int state = _state;
		_state = 0;

		if (validated)
		{
			Log.add("Olympiad Result: " + _team1.getName() + " vs " + _team2.getName() + " ... double validate check!!!", "olympiad");
			return;
		}
		validated = true;

		// Если игра закончилась до телепортации на стадион, то забираем очки у вышедших из игры, не засчитывая никому победу
		if (state < 1 && aborted)
		{
			_team1.takePointsForCrash();
			_team2.takePointsForCrash();
			broadcastPacket(Msg.THE_GAME_HAS_BEEN_CANCELLED_BECAUSE_THE_OTHER_PARTY_ENDS_THE_GAME, true, false);
			return;
		}

		boolean teamOneCheck = _team1.checkPlayers();
		boolean teamTwoCheck = _team2.checkPlayers();

		if (_winner <= 0)
		{
			if (!teamOneCheck && !teamTwoCheck)
				_winner = 0;
			else if (!teamTwoCheck)
				_winner = 1; // Выиграла первая команда
			else if (!teamOneCheck)
				_winner = 2; // Выиграла вторая команда
			else if (_team1.getDamage() < _team2.getDamage()) // Вторая команда нанесла вреда меньше, чем первая
				_winner = 1; // Выиграла первая команда
			else if (_team1.getDamage() > _team2.getDamage()) // Вторая команда нанесла вреда больше, чем первая
				_winner = 2; // Выиграла вторая команда
		}

		if (_winner == 1) // Выиграла первая команда
			winGame(_team1, _team2);
		else if (_winner == 2) // Выиграла вторая команда
			winGame(_team2, _team1);
		else
			tie();

		_team1.saveNobleData();
		_team2.saveNobleData();

		broadcastRelation();
		broadcastPacket(new SystemMessage2(SystemMsg.YOU_WILL_BE_MOVED_BACK_TO_TOWN_IN_S1_SECONDS).addInteger(20), true, true);
	}

	public void winGame(OlympiadTeam winnerTeam, OlympiadTeam looseTeam)
	{
		ExReceiveOlympiad.MatchResult packet = new ExReceiveOlympiad.MatchResult(false, winnerTeam.getName());

		int pointDiff = 0;

		TeamMember[] looserMembers = looseTeam.getMembers().toArray(new TeamMember[looseTeam.getMembers().size()]);
		TeamMember[] winnerMembers = winnerTeam.getMembers().toArray(new TeamMember[winnerTeam.getMembers().size()]);

		for (int i = 0; i < Party.MAX_SIZE; i++)
		{
			TeamMember looserMember = ArrayUtils.valid(looserMembers, i);
			TeamMember winnerMember = ArrayUtils.valid(winnerMembers, i);
			if (looserMember != null && winnerMember != null)
			{
				winnerMember.incGameCount();
				looserMember.incGameCount();

				int gamePoints = transferPoints(looserMember.getStat(), winnerMember.getStat());

				packet.addPlayer(winnerTeam == _team1 ? TeamType.BLUE : TeamType.RED, winnerMember, gamePoints);
				packet.addPlayer(looseTeam == _team1 ? TeamType.BLUE : TeamType.RED, looserMember, -gamePoints);

				pointDiff += gamePoints;
				if (winnerMember.getPlayer() != null)
				{
					if (winnerMember.getPlayer().getCounters().olyHiScore < winnerMember.getStat().getInteger(Olympiad.POINTS))
						winnerMember.getPlayer().getCounters().olyHiScore = winnerMember.getStat().getInteger(Olympiad.POINTS);

					winnerMember.getPlayer().getCounters().olyGamesWon++;
				}

				if (looserMember.getPlayer() != null)
					looserMember.getPlayer().getCounters().olyGamesLost++;
			}
		}

		if (_type != CompType.TEAM)
		{
			int team = _team1 == winnerTeam ? 1 : 2;

			TeamMember member1 = ArrayUtils.valid(_team1 == winnerTeam ? winnerMembers : looserMembers, 0);
			TeamMember member2 = ArrayUtils.valid(_team2 == winnerTeam ? winnerMembers : looserMembers, 0);
			if (member1 != null && member2 != null)
			{
				int diff = (int) ((System.currentTimeMillis() - _startTime) / 1000L);
				OlympiadHistory h = new OlympiadHistory(member1.getObjectId(), member2.getObjectId(), member1.getClassId(), member2.getClassId(), member1.getName(), member2.getName(), _startTime, diff, team, _type.ordinal());

				OlympiadHistoryManager.getInstance().saveHistory(h);
			}
		}

		_team1.removeBuffs(false);
		_team2.removeBuffs(false);

		broadcastPacket(new SystemMessage(SystemMsg.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH).addString(winnerTeam.getName()), true, true);
		winnerTeam.broadcast(new SystemMessage(SystemMsg.C1_HAS_EARNED_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES).addString(winnerTeam.getName()).addNumber(pointDiff));
		looseTeam.broadcast(new SystemMessage(SystemMsg.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES).addString(looseTeam.getName()).addNumber(pointDiff));

		for (Player player : winnerTeam.getPlayers())
		{
			player.getInventory().addItem(Config.ALT_OLY_BATTLE_REWARD_ITEM, getType().getReward(), "Olympiad Match Reward");
			player.sendPacket(SystemMessage2.obtainItems(Config.ALT_OLY_BATTLE_REWARD_ITEM, getType().getReward(), 0));
			player.sendChanges();
		}

		List<Player> teamsPlayers = new ArrayList<Player>();
		teamsPlayers.addAll(winnerTeam.getPlayers());
		teamsPlayers.addAll(looseTeam.getPlayers());
		for (Player player : teamsPlayers)
			if (player != null)
			{
				for (QuestState qs : player.getAllQuestsStates())
					if (qs.isStarted())
						qs.getQuest().onOlympiadEnd(this, qs);
			}

		broadcastPacket(packet, true, false);

		broadcastPacket(new SystemMessage2(SystemMsg.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH).addString(winnerTeam.getName()), false, true);

		// Synerge - Announce on critical to all players in the world, who won this match
		IStaticPacket criticalAnn = new Say2(0, ChatType.CRITICAL_ANNOUNCE, "", "Olympiad: " + _team1.getName() + " VS " + _team2.getName() + ". Winner is: " + winnerTeam.getName() + "!");
		for (Player player : GameObjectsStorage.getAllPlayersForIterate())
		{
			if (player == null)
				continue;

			player.sendPacket(criticalAnn);
		}

		Log.add("Olympiad Result: " + winnerTeam.getName() + " vs " + looseTeam.getName() + " ... (" + (int) winnerTeam.getDamage() + " vs " + (int) looseTeam.getDamage() + ") " + winnerTeam.getName() + " win " + pointDiff + " points", "olympiad");
	}

	public void tie()
	{
		TeamMember[] teamMembers1 = _team1.getMembers().toArray(new TeamMember[_team1.getMembers().size()]);
		TeamMember[] teamMembers2 = _team2.getMembers().toArray(new TeamMember[_team2.getMembers().size()]);

		ExReceiveOlympiad.MatchResult packet = new ExReceiveOlympiad.MatchResult(true, StringUtils.EMPTY);
		for (int i = 0; i < teamMembers1.length; i++)
		{
			try
			{
				TeamMember member1 = ArrayUtils.valid(teamMembers1, i);
				TeamMember member2 = ArrayUtils.valid(teamMembers2, i);

				if (member1 != null)
				{
					member1.incGameCount();
					StatsSet stat1 = member1.getStat();
					packet.addPlayer(TeamType.BLUE, member1, -2);

					stat1.set(Olympiad.POINTS, stat1.getInteger(Olympiad.POINTS) - 2);
				}

				if (member2 != null)
				{
					member2.incGameCount();
					StatsSet stat2 = member2.getStat();
					packet.addPlayer(TeamType.RED, member2, -2);

					stat2.set(Olympiad.POINTS, stat2.getInteger(Olympiad.POINTS) - 2);
				}
			}
			catch(Exception e)
			{
				_log.error("OlympiadGame.tie(): " + e, e);
				}
		}

		if (_type != CompType.TEAM)
		{
			TeamMember member1 = ArrayUtils.valid(teamMembers1, 0);
			TeamMember member2 = ArrayUtils.valid(teamMembers2, 0);
			if (member1 != null && member2 != null)
			{
				int diff = (int) ((System.currentTimeMillis() - _startTime) / 1000L);
				OlympiadHistory h = new OlympiadHistory(member1.getObjectId(), member1.getObjectId(), member1.getClassId(), member2.getClassId(), member1.getName(), member2.getName(), _startTime, diff, 0, _type.ordinal());

				OlympiadHistoryManager.getInstance().saveHistory(h);
			}
		}

		broadcastPacket(SystemMsg.THERE_IS_NO_VICTOR_THE_MATCH_ENDS_IN_A_TIE, true, true);
		_team1.broadcast(new SystemMessage(SystemMsg.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES).addString(_team1.getName()).addNumber(2));
		_team2.broadcast(new SystemMessage(SystemMsg.C1_HAS_LOST_S2_POINTS_IN_THE_GRAND_OLYMPIAD_GAMES).addString(_team2.getName()).addNumber(2));
		broadcastPacket(packet, true, false);

		_team1.removeBuffs(false);
		_team2.removeBuffs(false);

		Log.add("Olympiad Result: " + _team1.getName() + " vs " + _team2.getName() + " ... tie", "olympiad");
	}

	private int transferPoints(StatsSet from, StatsSet to)
	{
		int fromPoints = from.getInteger(Olympiad.POINTS);
		int fromLoose = from.getInteger(Olympiad.COMP_LOOSE);
		int fromPlayed = from.getInteger(Olympiad.COMP_DONE);

		int toPoints = to.getInteger(Olympiad.POINTS);
		int toWin = to.getInteger(Olympiad.COMP_WIN);
		int toPlayed = to.getInteger(Olympiad.COMP_DONE);

		int pointDiff = Math.max(1, (int)Math.ceil((double)Math.min(fromPoints, toPoints) / getType().getLooseMult()));
		pointDiff = pointDiff > OlympiadGame.MAX_POINTS_LOOSE ? OlympiadGame.MAX_POINTS_LOOSE : pointDiff;

		from.set(Olympiad.POINTS, fromPoints - pointDiff);
		from.set(Olympiad.COMP_LOOSE, fromLoose + 1);
		from.set(Olympiad.COMP_DONE, fromPlayed + 1);

		to.set(Olympiad.POINTS, toPoints + pointDiff);
		to.set(Olympiad.COMP_WIN, toWin + 1);
		to.set(Olympiad.COMP_DONE, toPlayed + 1);

		return pointDiff;
	}

	public void openDoors()
	{
		for (DoorInstance door : _reflection.getDoors())
			door.openMe();
	}

	public int getId()
	{
		return _id;
	}

	public Reflection getReflection()
	{
		return _reflection;
	}

	public boolean isRegistered(int objId)
	{
		return _team1.contains(objId) || _team2.contains(objId);
	}

	public List<Player> getSpectators()
	{
		return _spectators;
	}

	public void addSpectator(Player spec)
	{
		_spectators.add(spec);
	}

	public void removeSpectator(Player spec)
	{
		_spectators.remove(spec);
	}

	public void clearSpectators()
	{
		for (Player pc : _spectators)
			if (pc != null && pc.isInObserverMode())
				pc.leaveOlympiadObserverMode(false);
		_spectators.clear();
	}

	public void broadcastInfo(Player sender, Player receiver, boolean onlyToSpectators)
	{
		// TODO заюзать пакеты:
		// ExEventMatchCreate
		// ExEventMatchFirecracker
		// ExEventMatchManage
		// ExEventMatchMessage
		// ExEventMatchObserver
		// ExEventMatchScore
		// ExEventMatchTeamInfo
		// ExEventMatchTeamUnlocked
		// ExEventMatchUserInfo

		if (sender != null)
		{
			if (receiver != null)
				receiver.sendPacket(new ExOlympiadUserInfo(sender, sender.getOlympiadSide()));
			else
				broadcastPacket(new ExOlympiadUserInfo(sender, sender.getOlympiadSide()), !onlyToSpectators, true);
		}
		else
		{
			// Рассылаем информацию о первой команде
			for (Player player : _team1.getPlayers())
			{
				if (receiver != null)
					receiver.sendPacket(new ExOlympiadUserInfo(player, player.getOlympiadSide()));
				else
				{
					broadcastPacket(new ExOlympiadUserInfo(player, player.getOlympiadSide()), !onlyToSpectators, true);
					player.broadcastRelationChanged();
				}
			}

			// Рассылаем информацию о второй команде
			for (Player player : _team2.getPlayers())
			{
				if (receiver != null)
					receiver.sendPacket(new ExOlympiadUserInfo(player, player.getOlympiadSide()));
				else
				{
					broadcastPacket(new ExOlympiadUserInfo(player, player.getOlympiadSide()), !onlyToSpectators, true);
					player.broadcastRelationChanged();
				}
			}
		}
	}

	public void broadcastRelation()
	{
		for (Player player : _team1.getPlayers())
		{
			player.broadcastRelationChanged();
		}

		for (Player player : _team2.getPlayers())
		{
			player.broadcastRelationChanged();
		}
	}

	public void broadcastPacket(L2GameServerPacket packet, boolean toTeams, boolean toSpectators)
	{
		if (toTeams)
		{
			_team1.broadcast(packet);
			_team2.broadcast(packet);
		}

		if (toSpectators && !_spectators.isEmpty())
		{
			for (Player spec : _spectators)
			{
				if (spec != null)
					spec.sendPacket(packet);
			}
		}
	}

	public void broadcastPacket(IStaticPacket packet, boolean toTeams, boolean toSpectators)
	{
		if (toTeams)
		{
			_team1.broadcast(packet);
			_team2.broadcast(packet);
		}

		if (toSpectators && !_spectators.isEmpty())
		{
			for (Player spec : _spectators)
			{
				if (spec != null)
					spec.sendPacket(packet);
			}
		}
	}

	public List<Player> getAllPlayers()
	{
		List<Player> result = new ArrayList<Player>();
		for (Player player : _team1.getPlayers())
		{
			result.add(player);
		}
		for (Player player : _team2.getPlayers())
		{
			result.add(player);
		}
		if (!_spectators.isEmpty())
		{
			for (Player spec : _spectators)
			{
				if (spec != null)
					result.add(spec);
			}
		}
		return result;
	}

	public void setWinner(int val)
	{
		_winner = val;
	}

	public OlympiadTeam getWinnerTeam()
	{
		if (_winner == 1) // Выиграла первая команда
			return _team1;
		else if (_winner == 2) // Выиграла вторая команда
			return _team2;
		return null;
	}

	public void setState(int val)
	{
		_state = val;
		if (_state == 1)
			_startTime = System.currentTimeMillis();
	}

	public int getState()
	{
		return _state;
	}

	public List<Player> getTeamMembers(Player player)
	{
		return player.getOlympiadSide() == 1 ? _team1.getPlayers() : _team2.getPlayers();
	}

	public void addDamage(Player player, double damage)
	{
		if (player.getOlympiadSide() == 1)
			_team1.addDamage(player, damage);
		else
			_team2.addDamage(player, damage);
	}

	public boolean doDie(Player player)
	{
		return player.getOlympiadSide() == 1 ? _team1.doDie(player) : _team2.doDie(player);
	}

	public boolean checkPlayersOnline()
	{
		return _team1.checkPlayers() && _team2.checkPlayers();
	}

	public boolean logoutPlayer(Player player)
	{
		return player != null && (player.getOlympiadSide() == 1 ? _team1.logout(player) : _team2.logout(player));
	}

	OlympiadGameTask _task;
	ScheduledFuture<?> _shedule;

	public synchronized void sheduleTask(OlympiadGameTask task)
	{
		if (_shedule != null)
			_shedule.cancel(false);
		_task = task;
		_shedule = task.shedule();
	}

	public OlympiadGameTask getTask()
	{
		return _task;
	}

	public BattleStatus getStatus()
	{
		if (_task != null)
			return _task.getStatus();
		return BattleStatus.Begining;
	}

	public void endGame(int time, boolean aborted)
	{
		try
		{
			validateWinner(aborted);
			_team1.stopComp();
			_team2.stopComp();
		}
		catch(Exception e)
		{
			_log.error("Error on Olympiad End Game!", e);
		}

		sheduleTask(new OlympiadGameTask(this, time > 1 ? BattleStatus.PortBack : BattleStatus.Ending, time, 100));
	}

	public CompType getType()
	{
		return _type;
	}

	public String getTeamName1()
	{
		return _team1.getName();
	}

	public String getTeamName2()
	{
		return _team2.getName();
	}
}