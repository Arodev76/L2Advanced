package l2f.gameserver.model;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;

import l2f.commons.collections.MultiValueSet;
import l2f.commons.listener.Listener;
import l2f.commons.listener.ListenerList;
import l2f.commons.util.Rnd;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.listener.zone.OnZoneEnterLeaveListener;
import l2f.gameserver.model.base.Race;
import l2f.gameserver.model.entity.Reflection;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.network.serverpackets.EventTrigger;
import l2f.gameserver.network.serverpackets.L2GameServerPacket;
import l2f.gameserver.network.serverpackets.SystemMessage2;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.stats.Stats;
import l2f.gameserver.stats.funcs.FuncAdd;
import l2f.gameserver.templates.ZoneTemplate;
import l2f.gameserver.utils.Location;
import l2f.gameserver.utils.PositionUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Zone
{
	@SuppressWarnings("unused")
	private static final Logger _log = LoggerFactory.getLogger(Zone.class);

	public static final Zone[] EMPTY_L2ZONE_ARRAY = new Zone[0];

	public static enum ZoneType
	{
		AirshipController,

		SIEGE,
		RESIDENCE,
		HEADQUARTER,
		FISHING,
		UnderGroundColiseum,
		water,
		battle_zone,
		damage,
		instant_skill,
		mother_tree,
		peace_zone,
		poison,
		ssq_zone,
		swamp,
		no_escape,
		no_landing,
		no_restart,
		no_summon,
		dummy,
		offshore,
		epic,
		buff_store_only,
		fix_beleth
	}

	public enum ZoneTarget
	{
		pc,
		npc,
		only_pc
	}

	public static final String BLOCKED_ACTION_PRIVATE_STORE = "open_private_store";
	public static final String BLOCKED_ACTION_PRIVATE_WORKSHOP = "open_private_workshop";
	public static final String BLOCKED_ACTION_DROP_MERCHANT_GUARD = "drop_merchant_guard";
	public static final String BLOCKED_ACTION_SAVE_BOOKMARK = "save_bookmark";
	public static final String BLOCKED_ACTION_USE_BOOKMARK = "use_bookmark";
	public static final String BLOCKED_ACTION_MINIMAP = "open_minimap";

	// Prims - Better implementation for zone effects and damage threads
	private Future<?> _effectThread = null;
	private Future<?> _damageThread = null;

	private final class SkillTimer implements Runnable
	{
		private final Skill _skill;
		private final int _zoneTime;
		private final int _randomTime;
		private long _activateTime = 0;

		protected SkillTimer()
		{
			_skill = getZoneSkill();
			_zoneTime = getTemplate().getUnitTick() * 1000;
			_randomTime = getTemplate().getRandomTick() * 1000;
		}

		@Override
		public void run()
		{
			if (!isActive())
				return;

			if (_skill == null)
				return;

			for (Creature target : getObjects())
			{
				if (target == null || target.isDead())
					continue;

				if (!checkTarget(target))
					continue;

				if (Rnd.chance(getTemplate().getSkillProb()) && !target.isDead())
				{
					_skill.getEffects(target, target, false, false);
				}
			}

			// Prims - Soporte para efectos de daño que se activan y desactivan segun tiempo online, como los de Den of Evil, que duran algo asi como 10 minutos activos y 1 minuto inactivos
			// TODO: This is not the same as in l2j, as we dont have on, off times, only unit ticks, so we use the same for both
			// Si el efecto posee un tiempo de online, entonces debemos calcular cuando termina para desactivar la zona
			if (_activateTime == 0)
			{
				_activateTime = System.currentTimeMillis() + (_zoneTime + Rnd.get(-_randomTime, _randomTime));
			}
			// Si la zona esta activada y el tiempo paso, desactivamos la zona y seteamos la proxima activacion
			else if (isActive())
			{
				if (_activateTime < System.currentTimeMillis())
				{
					setActive(false);
					_activateTime = System.currentTimeMillis() + (_zoneTime + Rnd.get(-_randomTime, _randomTime));
				}
			}
			// Si la zona esta desactivada y el tiempo ya paso, activamos la zona y seteamos la proxima desactivacion
			else
			{
				if (_activateTime < System.currentTimeMillis())
				{
					setActive(true);
					_activateTime = System.currentTimeMillis() + (_zoneTime + Rnd.get(-_randomTime, _randomTime));
				}
			}
		}
	}

	private final class DamageTimer implements Runnable
	{
		private final int _hp;
		private final int _mp;
		private final int _message;
		private final int _zoneTime;
		private final int _randomTime;
		private long _activateTime = 0;

		protected DamageTimer()
		{
			_hp = getDamageOnHP();
			_mp = getDamageOnMP();
			_message = getDamageMessageId();
			_zoneTime = getTemplate().getUnitTick() * 1000;
			_randomTime = getTemplate().getRandomTick() * 1000;
		}

		@Override
		public void run()
		{
			if (!isActive())
				return;

			if (_hp == 0 || _mp == 0)
				return;

			for (Creature target : getObjects())
			{
				if (target == null || target.isDead())
					continue;

				if (!checkTarget(target))
					continue;

				if (_hp > 0)
				{
					target.reduceCurrentHp(_hp, target, null, false, false, true, false, false, false, true);
					if (_message > 0)
						target.sendPacket(new SystemMessage2(SystemMsg.valueOf(_message)).addInteger(_hp));
				}

				if (_mp > 0)
				{
					target.reduceCurrentMp(_mp, null);
					if (_message > 0)
						target.sendPacket(new SystemMessage2(SystemMsg.valueOf(_message)).addInteger(_mp));
				}
			}

			// Prims - Soporte para efectos de daño que se activan y desactivan segun tiempo online, como los de Den of Evil, que duran algo asi como 10 minutos activos y 1 minuto inactivos
			// TODO: This is not the same as in l2j, as we dont have on, off times, only unit ticks, so we use the same for both
			// Si el efecto posee un tiempo de online, entonces debemos calcular cuando termina para desactivar la zona
			if (_activateTime == 0)
			{
				_activateTime = System.currentTimeMillis() + (_zoneTime + Rnd.get(-_randomTime, _randomTime));
			}
			// Si la zona esta activada y el tiempo paso, desactivamos la zona y seteamos la proxima activacion
			else if (isActive())
			{
				if (_activateTime < System.currentTimeMillis())
				{
					setActive(false);
					_activateTime = System.currentTimeMillis() + (_zoneTime + Rnd.get(-_randomTime, _randomTime));
				}
			}
			// Si la zona esta desactivada y el tiempo ya paso, activamos la zona y seteamos la proxima desactivacion
			else
			{
				if (_activateTime < System.currentTimeMillis())
				{
					setActive(true);
					_activateTime = System.currentTimeMillis() + (_zoneTime + Rnd.get(-_randomTime, _randomTime));
				}
			}
		}
	}

	public class ZoneListenerList extends ListenerList<Zone>
	{
		public void onEnter(Creature actor)
		{
			if (!getListeners().isEmpty())
				for (Listener<Zone> listener : getListeners())
					((OnZoneEnterLeaveListener) listener).onZoneEnter(Zone.this, actor);
		}

		public void onLeave(Creature actor)
		{
			if (!getListeners().isEmpty())
				for (Listener<Zone> listener : getListeners())
					((OnZoneEnterLeaveListener) listener).onZoneLeave(Zone.this, actor);
		}
	}

	private ZoneType _type;
	private boolean _active;
	private final MultiValueSet<String> _params;

	private final ZoneTemplate _template;

	private Reflection _reflection;

	private final ZoneListenerList listeners = new ZoneListenerList();

	private final List<Creature> _objects = new CopyOnWriteArrayList<Creature>();

	/**
	 * Ордер в зонах, с ним мы и добавляем/убираем статы.
	 * TODO: сравнить ордер с оффом, пока от фонаря
	 */
	public final static int ZONE_STATS_ORDER = 0x40;

	public Zone(ZoneTemplate template)
	{
		this(template.getType(), template);
	}

	public Zone(ZoneType type, ZoneTemplate template)
	{
		_type = type;
		_template = template;
		_params = template.getParams();
	}

	public ZoneTemplate getTemplate()
	{
		return _template;
	}

	public final String getName()
	{
		return getTemplate().getName();
	}

	public ZoneType getType()
	{
		return _type;
	}

	public void setType(ZoneType type)
	{
		_type = type;
	}

	public Territory getTerritory()
	{
		return getTemplate().getTerritory();
	}

	public final int getEnteringMessageId()
	{
		return getTemplate().getEnteringMessageId();
	}

	public final int getLeavingMessageId()
	{
		return getTemplate().getLeavingMessageId();
	}

	public Skill getZoneSkill()
	{
		return getTemplate().getZoneSkill();
	}

	public ZoneTarget getZoneTarget()
	{
		return getTemplate().getZoneTarget();
	}

	public Race getAffectRace()
	{
		return getTemplate().getAffectRace();
	}

	/**
	 * Номер системного вообщения которое будет отослано игроку при нанесении урона зоной
	 * @return SystemMessage ID
	 */
	public int getDamageMessageId()
	{
		return getTemplate().getDamageMessageId();
	}

	/**
	 * Сколько урона зона нанесет по хп
	 * @return количество урона
	 */
	public int getDamageOnHP()
	{
		return getTemplate().getDamageOnHP();
	}

	/**
	 * Сколько урона зона нанесет по мп
	 * @return количество урона
	 */
	public int getDamageOnMP()
	{
		return  getTemplate().getDamageOnMP();
	}

	/**
	 * @return Бонус к скорости движения в зоне
	 */
	public double getMoveBonus()
	{
		return getTemplate().getMoveBonus();
	}

	/**
	 * Возвращает бонус регенерации хп в этой зоне
	 * @return Бонус регенарации хп в этой зоне
	 */
	public double getRegenBonusHP()
	{
		return getTemplate().getRegenBonusHP();
	}

	/**
	 * Возвращает бонус регенерации мп в этой зоне
	 * @return Бонус регенарации мп в этой зоне
	 */
	public double getRegenBonusMP()
	{
		return getTemplate().getRegenBonusMP();
	}

	public long getRestartTime()
	{
		return getTemplate().getRestartTime();
	}

	public List<Location> getRestartPoints()
	{
		return getTemplate().getRestartPoints();
	}

	public List<Location> getPKRestartPoints()
	{
		return getTemplate().getPKRestartPoints();
	}

	public Location getSpawn()
	{
		if (getRestartPoints() == null)
			return null;
		Location loc = getRestartPoints().get(Rnd.get(getRestartPoints().size()));
		return loc.clone();
	}

	public Location getPKSpawn()
	{
		if (getPKRestartPoints() == null)
			return getSpawn();
		Location loc = getPKRestartPoints().get(Rnd.get(getPKRestartPoints().size()));
		return loc.clone();
	}

	/**
	 * Проверяет находятся ли даные координаты в зоне.
	 * _loc - стандартная территория для зоны
	 * @param x координата
	 * @param y координата
	 * @return находятся ли координаты в локации
	 */
	public boolean checkIfInZone(int x, int y)
	{
		return getTerritory().isInside(x, y);
	}

	public boolean checkIfInZone(int x, int y, int z)
	{
		return checkIfInZone(x, y, z, getReflection());
	}

	public boolean checkIfInZone(int x, int y, int z, Reflection reflection)
	{
		return isActive() && _reflection == reflection && getTerritory().isInside(x, y, z);
	}

	public boolean checkIfInZone(Creature cha)
	{
		return _objects.contains(cha);
	}

	public final double findDistanceToZone(GameObject obj, boolean includeZAxis)
	{
		return findDistanceToZone(obj.getX(), obj.getY(), obj.getZ(), includeZAxis);
	}

	public final double findDistanceToZone(int x, int y, int z, boolean includeZAxis)
	{
		return PositionUtils.calculateDistance(x, y, z, (getTerritory().getXmax() + getTerritory().getXmin()) / 2, (getTerritory().getYmax() + getTerritory().getYmin()) / 2, (getTerritory().getZmax() + getTerritory().getZmin()) / 2, includeZAxis);
	}

	/**
	 * Обработка входа в территорию
	 * Персонаж всегда добавляется в список вне зависимости от активности территории.
	 * Если зона акивная, то обработается вход в зону
	 * @param cha кто входит
	 */
	public void doEnter(Creature cha)
	{
		boolean added = false;

		if (!_objects.contains(cha))
			added = _objects.add(cha);

		if (added)
			onZoneEnter(cha);
		
		if ((cha != null) && (cha.isPlayer()) && (cha.getPlayer().isGM()))
		{
			cha.sendMessage("Entered the zone " + getName());
		}
	}

	/**
	 * Обработка входа в зону
	 * @param actor кто входит
	 */
	protected void onZoneEnter(Creature actor)
	{
		checkEffects(actor, true);
		addZoneStats(actor);

		if (actor.isPlayer())
		{
			if (getType() == ZoneType.buff_store_only)
				actor.sendMessage("You have entered a buff store zone!");
			if (getEnteringMessageId() != 0)
				actor.sendPacket(new SystemMessage2(SystemMsg.valueOf(getEnteringMessageId())));
			if (getTemplate().getEventId() != 0)
				actor.sendPacket(new EventTrigger(getTemplate().getEventId(), true));
			if (getTemplate().getBlockedActions() != null)
				((Player)actor).blockActions(getTemplate().getBlockedActions());
			if (getType() == ZoneType.fix_beleth)
			{
				((Player)actor).sendMessage("Anti-beleth exploit");
				((Player)actor).teleToClosestTown();
			}
		}

		listeners.onEnter(actor);
	}

	/**
	 * Обработка выхода из зоны
	 * Object всегда убирается со списка вне зависимости от зоны
	 * Если зона активная, то обработается выход из зоны
	 * @param cha кто выходит
	 */
	public void doLeave(Creature cha)
	{
		boolean removed = false;

		removed = _objects.remove(cha);

		if (removed)
			onZoneLeave(cha);
		
		if ((cha != null) && (cha.isPlayer()) && (cha.getPlayer().isGM()))
		{
			cha.sendMessage("Left the area " + getName());
		}
	}

	/**
	 * Обработка выхода из зоны
	 * @param actor кто выходит
	 */
	protected void onZoneLeave(Creature actor)
	{
		checkEffects(actor, false);
		removeZoneStats(actor);

		if (actor.isPlayer())
		{
			if (getType() == ZoneType.buff_store_only)
				actor.sendMessage("You have left the Buff Store Only Zone");
			if (getLeavingMessageId() != 0 && actor.isPlayer())
				actor.sendPacket(new SystemMessage2(SystemMsg.valueOf(getLeavingMessageId())));
			if (getTemplate().getEventId() != 0 && actor.isPlayer())
				actor.sendPacket(new EventTrigger(getTemplate().getEventId(), false));
			if (getTemplate().getBlockedActions() != null)
				((Player)actor).unblockActions(getTemplate().getBlockedActions());
		}

		listeners.onLeave(actor);
	}

	/**
	 * Добавляет статы зоне
	 * @param cha персонаж которому добавляется
	 */
	private void addZoneStats(Creature cha)
	{
		// Проверка цели
		if (!checkTarget(cha))
			return;

		// Скорость движения накладывается только на L2Playable
		// affectRace в базе не указан, если надо будет влияние, то поправим
		if (getMoveBonus() != 0)
			if (cha.isPlayable())
			{
				cha.addStatFunc(new FuncAdd(Stats.RUN_SPEED, ZONE_STATS_ORDER, this, getMoveBonus()));
				cha.sendChanges();
			}

		// Если у нас есть что регенить
		if (getRegenBonusHP() != 0)
			cha.addStatFunc(new FuncAdd(Stats.REGENERATE_HP_RATE, ZONE_STATS_ORDER, this, getRegenBonusHP()));

		// Если у нас есть что регенить
		if (getRegenBonusMP() != 0)
			cha.addStatFunc(new FuncAdd(Stats.REGENERATE_MP_RATE, ZONE_STATS_ORDER, this, getRegenBonusMP()));
	}

	/**
	 * Убирает добавленые зоной статы
	 * @param cha персонаж у которого убирается
	 */
	private void removeZoneStats(Creature cha)
	{
		if (getRegenBonusHP() == 0 && getRegenBonusMP() == 0 && getMoveBonus() == 0)
			return;

		cha.removeStatsOwner(this);

		cha.sendChanges();
	}

	/**
	 * Применяет эффекты при входе/выходе из(в) зону
	 * @param cha обьект
	 * @param enter вошел или вышел
	 */
	private void checkEffects(Creature cha, boolean enter)
	{
		if (checkTarget(cha))
		{
			// Prims - New implementation of zone effect and damage threads
			if (enter)
			{
				if (getZoneSkill() != null)
				{
					if (_effectThread == null)
					{
						synchronized(this)
						{
							if (_effectThread == null)
							{
								// TODO: Reuse 30 hardcoded
								_effectThread = ThreadPoolManager.getInstance().scheduleAtFixedRate(new SkillTimer(), getTemplate().getInitialDelay(), 30000);
							}
						}
					}
				}
				else if (getDamageOnHP() > 0 || getDamageOnHP() > 0)
				{
					if (_damageThread == null)
					{
						synchronized(this)
						{
							if (_damageThread == null)
							{
								// TODO: Reuse 30 hardcoded
								_damageThread = ThreadPoolManager.getInstance().scheduleAtFixedRate(new DamageTimer(), getTemplate().getInitialDelay(), 30000);
							}
						}
					}
				}
			}
			else
			{
				if (getZoneSkill() != null)
					cha.getEffectList().stopEffect(getZoneSkill());
			}
		}
	}

	/**
	 * Проверяет подходит ли персонаж для вызвавшего действия
	 * @param cha персонаж
	 * @return подошел ли
	 */
	protected boolean checkTarget(Creature cha)
	{
		switch (getZoneTarget())
		{
			case pc:
				if (!cha.isPlayable())
					return false;
				break;
			case only_pc:
				if (!cha.isPlayer())
					return false;
				break;
			case npc:
				if (!cha.isNpc())
					return false;
				break;
		}

		// Если у нас раса не "all"
		if (getAffectRace() != null)
		{
			Player player = cha.getPlayer();
			//если не игровой персонаж
			if (player == null)
				return false;
			// если раса не подходит
			if (player.getRace() != getAffectRace())
				return false;
		}

		return true;
	}

	public Creature[] getObjects()
	{
		return _objects.toArray(new Creature[_objects.size()]);
	}

	public List<Player> getInsidePlayers()
	{
		final List<Player> result = new ArrayList<Player>();

		Creature cha;
		for (int i = 0; i < _objects.size(); i++)
		{
			if ((cha = _objects.get(i)) != null && cha.isPlayer())
				result.add((Player) cha);
		}

		return result;
	}

	public List<Playable> getInsidePlayables()
	{
		final List<Playable> result = new ArrayList<Playable>();

		Creature cha;
		for (int i = 0; i < _objects.size(); i++)
		{
			if ((cha = _objects.get(i)) != null && cha.isPlayable())
				result.add((Playable) cha);
		}

		return result;
	}

	public List<NpcInstance> getInsideNpcs()
	{
		List<NpcInstance> result = new ArrayList<NpcInstance>();

		Creature cha;
		for (int i = 0; i < _objects.size(); i++)
		{
			if ((cha = _objects.get(i)) != null && cha.isNpc())
				result.add((NpcInstance)cha);
		}

		return result;
	}

	/**
	 * Установка активности зоны. При установки флага активности, зона добавляется в соотвествующие регионы. В случае сброса
	 * - удаляется.
	 * @param value активна ли зона
	 */
	public void setActive(boolean value)
	{
		if (_active == value)
			return;

		_active = value;

		if (isActive())
			World.addZone(Zone.this);
		else
			World.removeZone(Zone.this);
	}

	public boolean isActive()
	{
		return _active;
	}

	public void setReflection(Reflection reflection)
	{
		_reflection = reflection;
	}

	public Reflection getReflection()
	{
		return _reflection;
	}

	public void setParam(String name, String value)
	{
		_params.put(name, value);
	}

	public void setParam(String name, Object value)
	{
		_params.put(name, value);
	}

	public MultiValueSet<String> getParams()
	{
		return _params;
	}

	public <T extends Listener<Zone>> boolean addListener(T listener)
	{
		return listeners.add(listener);
	}

	public <T extends Listener<Zone>> boolean removeListener(T listener)
	{
		return listeners.remove(listener);
	}

	@Override
	public final String toString()
	{
		return "[Zone " + getType() + " name: " + getName() + "]";
	}

	public void broadcastPacket(L2GameServerPacket packet, boolean toAliveOnly)
	{
		List<Player> insideZoners = getInsidePlayers();

		if (insideZoners != null && !insideZoners.isEmpty())
			for (Player player : insideZoners)
				if (toAliveOnly)
				{
					if (!player.isDead())
						player.broadcastPacket(packet);
				}
				else
					player.broadcastPacket(packet);
	}
}