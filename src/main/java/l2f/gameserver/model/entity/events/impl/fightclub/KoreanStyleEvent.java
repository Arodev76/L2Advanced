package l2f.gameserver.model.entity.events.impl.fightclub;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import l2f.commons.collections.MultiValueSet;
import l2f.commons.util.Rnd;
import l2f.gameserver.Config;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Effect;
import l2f.gameserver.model.Playable;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubPlayer;
import l2f.gameserver.model.entity.events.fightclubmanager.FightClubTeam;
import l2f.gameserver.model.entity.events.impl.AbstractFightClub;
import l2f.gameserver.model.instances.PetInstance;
import l2f.gameserver.network.serverpackets.SkillCoolTime;
import l2f.gameserver.skills.AbnormalEffect;
import l2f.gameserver.utils.Location;

public class KoreanStyleEvent extends AbstractFightClub
{
	private static final long MAX_FIGHT_TIME = 90000L;
	protected final FightClubPlayer[] _fightingPlayers;
	private final int[] lastTeamChosenSpawn;
	protected long _lastKill;

	public KoreanStyleEvent(MultiValueSet<String> set)
	{
		super(set);
		_lastKill = 0L;
		_fightingPlayers = new FightClubPlayer[2];
		lastTeamChosenSpawn = new int[]
		{
			0,
			0
		};
	}

	@Override
	public void onKilled(Creature actor, Creature victim)
	{
		if (actor != null && actor.isPlayable())
		{
			FightClubPlayer realActor = getFightClubPlayer(actor.getPlayer());
			if (victim.isPlayer() && realActor != null)
			{
				realActor.increaseKills(true);
				updatePlayerScore(realActor);
				updateScreenScores();
				sendMessageToPlayer(realActor, MESSAGE_TYPES.GM, "You have killed " + victim.getName());
			}
			actor.getPlayer().sendUserInfo();
		}

		if (victim.isPlayer())
		{
      		if ((victim.getPet() != null) && (!victim.getPet().isDead()))
        		victim.getPet().doDie(actor);

			FightClubPlayer realVictim = getFightClubPlayer(victim);
			realVictim.increaseDeaths();
			if (actor != null)
				sendMessageToPlayer(realVictim, MESSAGE_TYPES.GM, "You have been killed by " + actor.getName());
			victim.broadcastCharInfo();

			_lastKill = System.currentTimeMillis();
		}
		checkFightingPlayers();
		super.onKilled(actor, victim);
	}

	// Synerge
	@Override
	public void onDamage(Creature actor, Creature victim, double damage)
	{
		if (actor != null && actor.isPlayable())
		{
			FightClubPlayer realActor = getFightClubPlayer(actor.getPlayer());
			if (victim.isPlayer() && realActor != null)
			{
				realActor.increaseDamage(damage);
			}
		}

		super.onDamage(actor, victim, damage);
	}

	@Override
	public void loggedOut(Player player)
	{
		super.loggedOut(player);
		for (FightClubPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer() != null && fPlayer.getPlayer().equals(player))
				checkFightingPlayers();
		}
	}

	@Override
	public boolean leaveEvent(Player player, boolean teleportTown)
	{
		super.leaveEvent(player, teleportTown);
		try
		{
			if (player.isRooted())
				player.stopRooted();
		}
		catch (IllegalStateException e)
		{
		}
		player.stopAbnormalEffect(AbnormalEffect.ROOT);
		if (player.getPet() != null)
			player.getPet().stopAbnormalEffect(AbnormalEffect.ROOT);
		if (getState() != EVENT_STATE.STARTED)
			return true;
		for (FightClubPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer() != null && fPlayer.getPlayer().equals(player))
				checkFightingPlayers();
		}
		return true;
	}

	@Override
	public void startEvent()
	{
		super.startEvent();
		for (FightClubPlayer fPlayer : getPlayers(FIGHTING_PLAYERS, REGISTERED_PLAYERS))
		{
			Player player = fPlayer.getPlayer();
			if (player.isDead())
				player.doRevive();
			if (player.isFakeDeath())
				player.setFakeDeath(false);
			player.sitDown(null, true);
			player.resetReuse();
      		player.sendPacket(new SkillCoolTime(player));
			if (player.getPet() != null)
				player.getPet().startAbnormalEffect(AbnormalEffect.ROOT);
		}
	}

	@Override
	public void startRound()
	{
		super.startRound();
		checkFightingPlayers();
    	_lastKill = System.currentTimeMillis();
		ThreadPoolManager.getInstance().schedule(new CheckFightersInactive(this), 5000L);
	}

	@Override
	public void endRound()
	{
		super.endRound();
		super.unrootPlayers();
	}

	private void checkFightingPlayers()
	{
		if (getState() == EVENT_STATE.OVER || getState() == EVENT_STATE.NOT_ACTIVE)
			return;
		boolean changed = false;
		for (int i = 0; i < _fightingPlayers.length; i++)
		{
			FightClubPlayer oldPlayer = _fightingPlayers[i];
			if (oldPlayer == null || !isPlayerActive(oldPlayer.getPlayer()) || getFightClubPlayer(oldPlayer.getPlayer()) == null)
			{
				if (oldPlayer != null && !oldPlayer.getPlayer().isDead())
				{
					oldPlayer.getPlayer().doDie(null);
					oldPlayer.setDamage(0);
					return;
				}
				FightClubPlayer newPlayer = chooseNewPlayer(i + 1);
				if (newPlayer == null)
				{
					for (FightClubTeam team : getTeams())
					{
						if (team.getIndex() != (i + 1))
							team.incScore(1);
					}
					endRound();
					return;
				}
				newPlayer.getPlayer().isntAfk();
				_fightingPlayers[i] = newPlayer;
				changed = true;
			}
		}

		if (changed)
		{
			StringBuilder msg = new StringBuilder();
			for (int i = 0; i < _fightingPlayers.length; i++)
			{
				if (i > 0)
					msg.append(" VS ");
				msg.append(_fightingPlayers[i].getPlayer().getName());
			}
			sendMessageToFighting(MESSAGE_TYPES.SCREEN_BIG, msg.toString(), false);
			preparePlayers();
		}
	}

	private FightClubPlayer chooseNewPlayer(int teamIndex)
	{
		List<FightClubPlayer> alivePlayersFromTeam = new ArrayList<>();
		for (FightClubPlayer fPlayer : getPlayers(FIGHTING_PLAYERS))
		{
			if (fPlayer.getPlayer().isSitting() && fPlayer.getTeam().getIndex() == teamIndex)
			{
				alivePlayersFromTeam.add(fPlayer);
			}
		}

		if (alivePlayersFromTeam.isEmpty())
			return null;
		if (alivePlayersFromTeam.size() == 1)
			return alivePlayersFromTeam.get(0);
		return Rnd.get(alivePlayersFromTeam);
	}

	private void preparePlayers()
	{
		for (int i = 0; i < _fightingPlayers.length; i++)
		{
			FightClubPlayer fPlayer = _fightingPlayers[i];
			Player player = fPlayer.getPlayer();
			try
			{
				if (player.isBlocked())
					player.unblock();
			}
			catch (IllegalStateException e)
			{

			}
			player.standUp();
			player.isntAfk();
			if (Config.EVENT_KOREAN_RESET_REUSE)
				player.resetReuse();
			player.sendPacket(new SkillCoolTime(player));
			healFull(player);
			if (player.getPet() instanceof PetInstance)
				player.getPet().unSummon();
			if (player.getPet() != null && !player.getPet().isDead())
				healFull(player.getPet());

			fPlayer.setLastDamageTime();

			// Teleport to the zone directly so they can start preparing for the battle
			Location loc = getMap().getKeyLocations()[i];
			player.teleToLocation(loc, getReflection());

			player.sendMessage("You have 10 seconds to prepare yourself for the battle");
		}

		// Synerge - Unroot the players 10 seconds after the teleport so they can start fighting
		ThreadPoolManager.getInstance().schedule(new Runnable()
		{
			@Override
			public void run()
			{
				for (int i = 0; i < _fightingPlayers.length; i++)
				{
					FightClubPlayer fPlayer = _fightingPlayers[i];
					Player player = fPlayer.getPlayer();

					try
					{
						if (player.isRooted())
							player.stopRooted();
					}
					catch (IllegalStateException e)
					{

					}
					player.stopAbnormalEffect(AbnormalEffect.ROOT);

					healFull(player);
					if (player.getPet() instanceof PetInstance)
						player.getPet().unSummon();
					if (player.getPet() != null && !player.getPet().isDead())
					{
						healFull(player.getPet());
						player.getPet().stopAbnormalEffect(AbnormalEffect.ROOT);
					}
				}
			}
		}, 10000);
	}

	private static void healFull(Playable playable)
	{
		cleanse(playable);
		playable.setCurrentHp(playable.getMaxHp(), false);
		playable.setCurrentMp(playable.getMaxMp());
		playable.setCurrentCp(playable.getMaxCp());
	}

	private static void cleanse(Playable playable)
	{
		try
		{
			for (Effect e : playable.getEffectList().getAllEffects())
			{
				if (e.isOffensive() && e.isCancelable())
					e.exit();
			}
		}
		catch (IllegalStateException e)
		{
		}
	}

	@Override
	public boolean canAttack(Creature target, Creature attacker, Skill skill, boolean force)
	{
		if (getState() != EVENT_STATE.STARTED)
			return false;
		if (target == null || !target.isPlayable() || attacker == null || !attacker.isPlayable())
			return false;
		if (isFighting(target) && isFighting(attacker))
			return true;
		return false;
	}

	private boolean isFighting(Creature actor)
	{
		for (FightClubPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer() != null && fPlayer.getPlayer().equals(actor.getPlayer()))
				return true;
		}
		return false;
	}

	protected static class CheckFightersInactive implements Runnable
	{
		private final KoreanStyleEvent _fightClub;

		public CheckFightersInactive(KoreanStyleEvent fightClub)
		{
			_fightClub = fightClub;
		}

		@Override
		public void run()
		{
			if (_fightClub.getState() != EVENT_STATE.STARTED)
				return;

			final long currentTime = System.currentTimeMillis();

			// Synerge - If the player was not damaged in at least 120 seconds, then he is exploiting, so we should kill him
			for (FightClubPlayer fPlayer : _fightClub._fightingPlayers)
			{
				if (fPlayer != null && fPlayer.getPlayer() != null)
				{
					if (fPlayer.getLastDamageTime() < currentTime - 120000)
					{
						fPlayer.getPlayer().doDie(null);
					}
				}
			}

			if (_fightClub._lastKill + MAX_FIGHT_TIME < currentTime)
			{
				double playerMinDamage = Double.MAX_VALUE;
				Player playerToKill = null;
				for (FightClubPlayer fPlayer : _fightClub._fightingPlayers)
				{
					if (fPlayer != null && fPlayer.getPlayer() != null)
					{
						if (!fPlayer.getPlayer().getNetConnection().isConnected())
						{
							playerToKill = fPlayer.getPlayer();
							playerMinDamage = -100.0;
						}
						else if (currentTime - fPlayer.getPlayer().getLastNotAfkTime() > 8000L)
						{
							playerToKill = fPlayer.getPlayer();
							playerMinDamage = -1.0;
						}
						else if (fPlayer.getDamage() < playerMinDamage)
						{
							playerToKill = fPlayer.getPlayer();
							playerMinDamage = fPlayer.getDamage();
						}
					}
				}

				if (playerToKill != null)
					playerToKill.doDie(null);
			}

			ThreadPoolManager.getInstance().schedule(this, 5000L);
		}
	}

	@Override
	protected Location getSinglePlayerSpawnLocation(FightClubPlayer fPlayer)
	{
		Location[] spawnLocations = getMap().getTeamSpawns().get(fPlayer.getTeam().getIndex());
		int ordinalTeamIndex = fPlayer.getTeam().getIndex() - 1;
		int lastSpawnIndex = lastTeamChosenSpawn[ordinalTeamIndex];
		lastSpawnIndex++;
		if (lastSpawnIndex >= spawnLocations.length)
			lastSpawnIndex = 0;
		lastTeamChosenSpawn[ordinalTeamIndex] = lastSpawnIndex;
		return spawnLocations[lastSpawnIndex];
	}

	@Override
	protected int getRewardForWinningTeam(FightClubPlayer fPlayer, boolean atLeast1Kill)
	{
		return super.getRewardForWinningTeam(fPlayer, false);
	}

	@Override
	protected void handleAfk(FightClubPlayer fPlayer, boolean setAsAfk)
	{
	}

	@Override
	protected void unrootPlayers()
	{
	}

	@Override
	protected boolean inScreenShowBeScoreNotKills()
	{
		return false;
	}

	@Override
	protected boolean inScreenShowBeTeamNotInvidual()
	{
		return false;
	}

	@Override
	protected boolean isAfkTimerStopped(Player player)
	{
		return player.isSitting() || super.isAfkTimerStopped(player);
	}

	@Override
	public boolean canStandUp(Player player)
	{
		for (FightClubPlayer fPlayer : _fightingPlayers)
		{
			if (fPlayer != null && fPlayer.getPlayer().equals(player))
				return true;
		}
		return false;
	}

	@Override
	protected List<List<Player>> spreadTeamInPartys(FightClubTeam team)
	{
		return Collections.emptyList();
	}

	@Override
	protected void createParty(List<Player> listOfPlayers)
	{
	}
}
