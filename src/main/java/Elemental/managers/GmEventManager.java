/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package Elemental.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2f.gameserver.Announcements;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.data.xml.holder.InstantZoneHolder;
import l2f.gameserver.instancemanager.ReflectionManager;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.base.RestartType;
import l2f.gameserver.model.entity.Reflection;
import l2f.gameserver.model.entity.events.impl.DuelEvent;
import l2f.gameserver.model.entity.olympiad.Olympiad;
import l2f.gameserver.tables.SkillTable;
import l2f.gameserver.templates.InstantZone;
import l2f.gameserver.utils.Location;
import l2f.gameserver.utils.TeleportUtils;

/**
 * A simple engine to facilitate the task of the GameMasters when doing a manual event
 *
 * @author Prims
 */
public class GmEventManager
{
	private static final int INSTANCE_ID = 909;

	private static final int RESURRECTION_DELAY = 10000;

	private String _eventName;
	private StateEnum _state = StateEnum.INACTIVE;
	private int _instanceId = 0;
	private Location _location;
	private int _minLvl = 0;
	private int _maxLvl = 85;
	private int _minOnlineTime = 0;
	private int _maxOnlineTime = Integer.MAX_VALUE;
	private boolean _isPvPEvent = false;
	private boolean _isPeaceEvent = false;
	private boolean _isAutoRes = false;

	private final Map<Integer, GmEventParticipant> _participants = new ConcurrentHashMap<>();

	public enum EventParameter
	{
		MIN_LVL,
		MAX_LVL,
		MIN_TIME,
		MAX_TIME,
		IS_PVP_EVENT,
		IS_PEACE_EVENT,
		IS_AUTO_RES
	}

	public enum StateEnum
	{
		ACTIVE, INACTIVE, REGISTERING, STARTING
	}

	/**
	 * Create a new event with particular name at position pj
	 *
	 * @param gameMaster
	 * @param eventName
	 */
	public void createEvent(Player gameMaster, String eventName)
	{
		// Creamos la instancia y metemos al pj en la misma
		final InstantZone iz = InstantZoneHolder.getInstance().getInstantZone(INSTANCE_ID);
		Reflection r = new Reflection();
		r.init(iz);

		_instanceId = r.getId();
		if (_instanceId < 1)
		{
			gameMaster.sendMessage("An error ocurred while creating the initial instance for the event");
			return;
		}

		gameMaster.teleToLocation(gameMaster.getLoc(), r);

		_eventName = eventName;
		_location = gameMaster.getLoc();

		_state = StateEnum.STARTING;

		gameMaster.sendMessage("Event created succesfuly");
	}

	/**
	 * Cambia parametros del evento
	 *
	 * @param param
	 * @param value
	 */
	public void changeEventParameter(EventParameter param, int value)
	{
		switch (param)
		{
			case MIN_LVL:
				_minLvl = value;
				break;
			case MAX_LVL:
				_maxLvl = value;
				break;
			case MIN_TIME:
				_minOnlineTime = value;
				break;
			case MAX_TIME:
				_maxOnlineTime = value;
				break;
			case IS_PVP_EVENT:
				_isPvPEvent = value == 1;
				break;
			case IS_PEACE_EVENT:
				_isPeaceEvent = value == 1;
				break;
			case IS_AUTO_RES:
				_isAutoRes = value == 1;
				break;
		}
	}

	/**
	 * Comienza el periodo de registro del evento
	 */
	public void startRegistration()
	{
		if (_state != StateEnum.STARTING)
			return;

		// Avisamos que el evento comenzo
		Announcements.getInstance().announceToAll("The registration period for the event " + _eventName + " is now opened");

		// Ponemos el evento en registro
		_state = StateEnum.REGISTERING;
	}

	/**
	 * Comienza el evento
	 */
	public void startEvent()
	{
		if (_state != StateEnum.REGISTERING)
			return;

		// Avisamos que el evento comenzo
		Announcements.getInstance().announceToAll("The event " + _eventName + " has started");

		// Ponemos el evento en activo
		_state = StateEnum.ACTIVE;

		// Si es un evento PvP le cancelamos todos los buffs a los pjs al comenzar
		if (isPvPEvent())
		{
			for (GmEventParticipant participant : _participants.values())
			{
				if (participant == null)
					continue;

				if (participant.getPlayer() != null)
					participant.getPlayer().getEffectList().stopAllEffects();
			}
		}
	}

	/**
	 * Detiene el evento, devuelve todos los pjs a su posicion original y resetea variables
	 */
	public void stopEvent()
	{
		if (_state == StateEnum.INACTIVE)
			return;

		// Avisamos que el evento fue detenido
		Announcements.getInstance().announceToAll("The event " + _eventName + " has finished");

		// Enviamos a todos los pjs de nuevo a la ubicacion de donde se anotaron
		for (GmEventParticipant participant : _participants.values())
		{
			if (participant == null || participant.getPlayer() == null)
				continue;

			if (participant.getPlayer().isDead())
				participant.getPlayer().doRevive(100);

			participant.getPlayer().teleToLocation(participant.getInitialLoc(), 0);
		}

		// Reseteamos todas las variables
		_participants.clear();
		_eventName = "";
		_state = StateEnum.INACTIVE;
		_minLvl = 0;
		_maxLvl = 85;
		_minOnlineTime = 0;
		_maxOnlineTime = Integer.MAX_VALUE;
		_isPvPEvent = false;
		_isPeaceEvent = false;
		_isAutoRes = false;

		// Destruimos la instancia
		Reflection r = ReflectionManager.getInstance().get(_instanceId);
		if (r != null)
			r.startCollapseTimer(5);
	}

	/**
	 * Registra un pj al evento. Realiza todos los chequeos de si puede ingresar o no
	 *
	 * @param player
	 */
	public void registerToEvent(Player player)
	{
		// Si no es momento de registro, no hacemos nada
		if (_state != StateEnum.REGISTERING)
			return;

		// Si ya esta registrado, no hacemos nada
		if (_participants.containsKey(player.getObjectId()))
			return;

		// Chequeamos que el pj cumpla con los requisitos impuestos para registrarse en el evento
		if (player.getLevel() < _minLvl)
		{
			player.sendMessage("You have not enough level to register to this event");
			return;
		}

		if (player.getLevel() > _maxLvl)
		{
			player.sendMessage("Your level is too high to be able to register to this event");
			return;
		}

		if (player.getOnlineTime() / 3600 < _minOnlineTime)
		{
			player.sendMessage("Your total online time is too low to be able to register to this event");
			return;
		}

		if (player.getOnlineTime() / 3600 > _maxOnlineTime)
		{
			player.sendMessage("Your total online time is too high to be able to register to this event");
			return;
		}

		// Chequeamos todas las demas condiciones de estado para unirse al evento
		if (player.isBlocked())
		{
			player.sendMessage("Blocked players cannot join the event");
			return;
		}

		if (player.getCursedWeaponEquippedId() > 0)
		{
			player.sendMessage("Cursed Weapon owners may not participate in the event!");
			return;
		}

		if (Olympiad.isRegistered(player) || player.isInOlympiadMode() || player.getOlympiadGame() != null)
		{
			player.sendMessage("Players registered to Olympiad Match may not participate in the event!");
			return;
		}

		if (player.isInObserverMode())
			return;

		if (player.isDead() || player.isAlikeDead())
			return;

		if (!player.isInPeaceZone() && player.getPvpFlag() > 0)
		{
			player.sendMessage("Players in PvP Battle may not participate in the event!");
			return;
		}

		if (player.isInCombat())
		{
			player.sendMessage("Players in Combat may not participate in the event!");
			return;
		}

		if (player.getEvent(DuelEvent.class) != null)
		{
			player.sendMessage("Players engaged in Duel may not participate in the event!");
			return;
		}

		if (player.getKarma() > 0)
		{
			player.sendMessage("Chaotic players may not participate in the event!");
			return;
		}

		if (player.isInOfflineMode())
		{
			player.sendMessage("Players in Offline mode may not participate in the event!");
			return;
		}

		if (player.isInStoreMode())
		{
			player.sendMessage("Players in Store mode may not participate in the event!");
			return;
		}

		if (player.getReflectionId() > 0)
		{
			player.sendMessage("Players in instances may not participate in the event!");
			return;
		}

		// Salvamos la ubicacion del pj actual y lo registramos al evento
		_participants.put(player.getObjectId(), new GmEventParticipant(player, player.getLoc()));

		// Lo transportamos hacia la ubicacion del evento
		player.teleToLocation(_location, _instanceId);

		// Le enviamos el mensaje
		player.sendMessage("You have succesfully registered to the event");
	}

	/**
	 * Remueve al pj del evento si se habia registrado antes
	 *
	 * @param player
	 */
	public void unregisterOfEvent(Player player)
	{
		// Si el pj apreta desregistrarse, pero no esta anotado aunque esta en la zona del evento, lo enviamos a la ciudad. Esto puede pasar si se desloguea y cuando vuelve ya termino
		if (!_participants.containsKey(player.getObjectId()) && player.getReflectionId() == _instanceId)
		{
			player.teleToLocation(TeleportUtils.getRestartLocation(player, RestartType.TO_VILLAGE), 0);
			return;
		}

		// Si no es momento de registro, no hacemos nada
		if (_state != StateEnum.REGISTERING)
			return;

		// Si no esta registrado, no hacemos nada
		if (!_participants.containsKey(player.getObjectId()))
			return;

		// Lo transportamos nuevamente a donde el pj se registro al evento
		player.teleToLocation(_participants.get(player.getObjectId()).getInitialLoc(), 0);

		// Quitamos al pj del evento
		_participants.remove(player.getObjectId());

		// Le enviamos el mensaje
		player.sendMessage("You have succesfully unregistered from the event");
	}

	/**
	 * Invocado cuando un pj muere
	 *
	 * @param killed
	 * @param killer
	 */
	public void onPlayerKill(Player killed, Creature killer)
	{
		if (killed == null || killer == null)
			return;

		// Chequeamos si el pj que murio esta participando de este evento
		if (!isParticipating(killed))
			return;

		// Si el evento debe revivir a los muertos, ponemos un thread a 10 segundos para revivirlo
		if (isAutoRes())
			ThreadPoolManager.getInstance().schedule(new ResurrectionTask(killed), RESURRECTION_DELAY);
	}

	/**
	 * @param player
	 * @return Devuelve true si el pj puede resucitar
	 */
	public boolean canResurrect(Player player)
	{
		// No puede revivir si esta participando del evento
		if (!isParticipating(player))
			return true;

		return false;
	}

	/**
	 * @param player
	 * @return Devuelve true si el pj esta participando en el evento
	 */
	public boolean isParticipating(Player player)
	{
		if (getEventStatus() != StateEnum.ACTIVE || player == null)
			return false;

		if (_participants.containsKey(player.getObjectId()))
		{
			// Si el pj esta anotado pero no esta en la instancia esta, lo desanotamos, porque seguro se salio
			if (player.getReflectionId() != _instanceId)
			{
				_participants.remove(player.getObjectId());
				return false;
			}

			return true;
		}

		return false;
	}

	/**
	 * @return Devuelve el nombre del evento
	 */
	public String getEventName()
	{
		return _eventName;
	}

	/**
	 * @return Devuelve el lvl minimo de los pjs para registrarse
	 */
	public int getMinLvl()
	{
		return _minLvl;
	}

	/**
	 * @return Devuelve el lvl maximo de los pjs para registrarse
	 */
	public int getMaxLvl()
	{
		return _maxLvl;
	}

	/**
	 * @return Devuelve el tiempo minimo online de los pjs para registrarse
	 */
	public int getMinOnlineTime()
	{
		return _minOnlineTime;
	}

	/**
	 * @return Devuelve el tiempo maximo online de los pjs para registrarse
	 */
	public int getMaxOnlineTime()
	{
		return _maxOnlineTime;
	}

	/**
	 * @return Devuelve si este es un evento pvp
	 */
	public boolean isPvPEvent()
	{
		return _isPvPEvent;
	}

	/**
	 * @return Devuelve si este es un evento pasivo, los pjs no se pueden pegar
	 */
	public boolean isPeaceEvent()
	{
		return _isPeaceEvent;
	}

	/**
	 * @return Devuelve true si los pjs que mueren son revividos automaticamente
	 */
	public boolean isAutoRes()
	{
		return _isAutoRes;
	}

	/**
	 * @return Devuelve el estado actual del evento, o sea en que etapa se encuentra
	 */
	public StateEnum getEventStatus()
	{
		return _state;
	}

	/**
	 * @return Devuelve el id de la instancia que se esta usando para el evento
	 */
	public int getEventInstanceId()
	{
		return _instanceId;
	}

	// Un thread que maneja los res de los pjs cuando mueren
	private class ResurrectionTask implements Runnable
	{
		private final Player _player;

		public ResurrectionTask(Player player)
		{
			_player = player;
		}

		@Override
		public void run()
		{
			if (_player == null || !_player.isDead() || !isParticipating(_player))
				return;

			// Revivimos al pj
			_player.doRevive(100);

			// Le damos celestial shield asi esta invulnerable al principio
			SkillTable.getInstance().getInfo(5576, 1).getEffects(_player, _player, false, true);
		}
	}

	// Clase para guardar cada participante y alguna que otra info extra necesaria de cada uno
	public static class GmEventParticipant
	{
		private final Player _player;
		private final Location _initialLoc;

		public GmEventParticipant(Player player, Location initialLoc)
		{
			_player = player;
			_initialLoc = initialLoc;
		}

		public Player getPlayer()
		{
			return _player;
		}

		public Location getInitialLoc()
		{
			return _initialLoc;
		}
	}

	public static GmEventManager getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final GmEventManager _instance = new GmEventManager();
	}
}
