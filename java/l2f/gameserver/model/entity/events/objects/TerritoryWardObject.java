package l2f.gameserver.model.entity.events.objects;

import l2f.commons.dao.JdbcEntityState;
import l2f.commons.threading.RunnableImpl;
import l2f.gameserver.Config;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.data.xml.holder.EventHolder;
import l2f.gameserver.data.xml.holder.NpcHolder;
import l2f.gameserver.idfactory.IdFactory;
import l2f.gameserver.listener.zone.OnZoneEnterLeaveListener;
import l2f.gameserver.model.*;
import l2f.gameserver.model.Zone.ZoneType;
import l2f.gameserver.model.entity.events.EventType;
import l2f.gameserver.model.entity.events.GlobalEvent;
import l2f.gameserver.model.entity.events.impl.DominionSiegeEvent;
import l2f.gameserver.model.entity.events.impl.DominionSiegeRunnerEvent;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.model.instances.TerritoryWardInstance;
import l2f.gameserver.model.items.Inventory;
import l2f.gameserver.model.items.ItemInstance;
import l2f.gameserver.model.items.attachment.FlagItemAttachment;
import l2f.gameserver.network.serverpackets.ExShowScreenMessage;
import l2f.gameserver.network.serverpackets.ExShowScreenMessage.ScreenMessageAlign;
import l2f.gameserver.network.serverpackets.SystemMessage2;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.templates.npc.NpcTemplate;
import l2f.gameserver.utils.ItemFunctions;
import l2f.gameserver.utils.Location;
import org.apache.commons.lang3.ArrayUtils;

import java.util.concurrent.ScheduledFuture;

public class TerritoryWardObject implements SpawnableObject, FlagItemAttachment
{
	private static final long RETURN_FLAG_DELAY = 120000L;

	private final int _itemId;
	private final NpcTemplate _template;
	protected final Location _location;
	private boolean _isOutOfZone;
	private ScheduledFuture<?> _startTimerTask;
	private ScheduledFuture<?> teleportBackTask;

	protected NpcInstance _wardNpcInstance;
	protected ItemInstance _wardItemInstance;

	public TerritoryWardObject(int itemId, int npcId, Location location)
	{
		_itemId = itemId;
		_template = NpcHolder.getInstance().getTemplate(npcId);
		_location = location;
	}

	@Override
	public void spawnObject(GlobalEvent event)
	{
		_wardItemInstance = ItemFunctions.createItem(_itemId);
		_wardItemInstance.setAttachment(this);

		_wardNpcInstance = new TerritoryWardInstance(IdFactory.getInstance().getNextId(), _template, this);
		_wardNpcInstance.addEvent(event);
		_wardNpcInstance.setCurrentHpMp(_wardNpcInstance.getMaxHp(), _wardNpcInstance.getMaxMp());
		_wardNpcInstance.spawnMe(_location);
		_startTimerTask = null;
		_isOutOfZone = false;

		if (_wardNpcInstance.getZone(ZoneType.SIEGE) != null)
		{
			_wardNpcInstance.getZone(ZoneType.SIEGE).addListener(new OnZoneEnterLeaveListenerImpl());
		}

		ThreadPoolManager.getInstance().schedule(new Runnable() {

			@Override
			public void run() {
				if (_wardNpcInstance.getZone(ZoneType.SIEGE) != null)
				{
					_wardNpcInstance.getZone(ZoneType.SIEGE).addListener(new OnZoneEnterLeaveListenerImpl());
				}
			}
		}, 1000L);
	}

	private void stopTerrFlagCountDown()
	{
		if (_startTimerTask == null)
			return;
		_startTimerTask.cancel(false);
		_startTimerTask = null;
		_isOutOfZone = false;
	  }

	@Override
	public void despawnObject(GlobalEvent event)
	{
		if (_wardItemInstance == null || _wardNpcInstance == null)
			return;

		Player owner = GameObjectsStorage.getPlayer(_wardItemInstance.getOwnerId());
		if (owner != null)
		{
			owner.getInventory().destroyItem(_wardItemInstance, "Territory Ward");
			owner.sendDisarmMessage(_wardItemInstance);
		}

		if (teleportBackTask != null)
			teleportBackTask.cancel(true);

		_wardItemInstance.setAttachment(null);
		_wardItemInstance.setJdbcState(JdbcEntityState.UPDATED);
		_wardItemInstance.delete();
		_wardItemInstance.deleteMe();
		_wardItemInstance = null;

		_wardNpcInstance.deleteMe();
		_wardNpcInstance = null;

		stopTerrFlagCountDown();
	}

	@Override
	public void refreshObject(GlobalEvent event)
	{
		//
	}

	@Override
	public void onLogout(Player player)
	{
		if (player.getActiveWeaponInstance() != null)
		{
			player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, null);
			player.getInventory().setPaperdollItem(Inventory.PAPERDOLL_RHAND, null);
		}
		player.getInventory().removeItem(_wardItemInstance, "Territory Ward");

		_wardItemInstance.setOwnerId(0);
		_wardItemInstance.setJdbcState(JdbcEntityState.UPDATED);
		_wardItemInstance.update();

		_wardNpcInstance.setCurrentHpMp(_wardNpcInstance.getMaxHp(), _wardNpcInstance.getMaxMp(), true);
		_wardNpcInstance.spawnMe(_location);


		DominionSiegeRunnerEvent runnerEvent = EventHolder.getInstance().getEvent(EventType.MAIN_EVENT, 1);
		runnerEvent.broadcastTo(new ExShowScreenMessage("Territory Ward returned to the castle!", 3000, ScreenMessageAlign.TOP_CENTER, false));

		stopTerrFlagCountDown();
		_isOutOfZone = false;
	}

	@Override
	public void onDeath(Player owner, Creature killer)
	{
		Location loc = owner.getLoc();

		if (owner.getActiveWeaponInstance() != null)
		{
			owner.getInventory().setPaperdollItem(Inventory.PAPERDOLL_LHAND, null);
			owner.getInventory().setPaperdollItem(Inventory.PAPERDOLL_RHAND, null);
		}
		owner.getInventory().removeItem(_wardItemInstance, "Territory Ward");
		owner.sendPacket(new SystemMessage2(SystemMsg.YOU_HAVE_DROPPED_S1).addName(_wardItemInstance));

		_wardItemInstance.setOwnerId(0);
		_wardItemInstance.setJdbcState(JdbcEntityState.UPDATED);
		_wardItemInstance.update();

		DominionSiegeRunnerEvent runnerEvent = EventHolder.getInstance().getEvent(EventType.MAIN_EVENT, 1);

		_wardNpcInstance.setCurrentHpMp(_wardNpcInstance.getMaxHp(), _wardNpcInstance.getMaxMp(), true);
		if (owner.isInZone(ZoneType.SIEGE))
		{
			_wardNpcInstance.spawnMe(loc);
			teleportBackTask = ThreadPoolManager.getInstance().schedule(new ReturnFlagThread(), RETURN_FLAG_DELAY);
		}
		else
		{
			_wardNpcInstance.spawnMe(_location);
			runnerEvent.broadcastTo(new ExShowScreenMessage("Territory Ward returned to the castle!", 3000, ScreenMessageAlign.TOP_CENTER, false));
		}

		runnerEvent.broadcastTo(new SystemMessage2(SystemMsg.THE_CHARACTER_THAT_ACQUIRED_S1S_WARD_HAS_BEEN_KILLED).addResidenceName(getDominionId()));
		stopTerrFlagCountDown();
		_isOutOfZone = false;
	}

	@Override
	public boolean canPickUp(Player player)
	{
		if (player.getActiveWeaponFlagAttachment() != null)
			return false;
		return true;
	}

	@Override
	public void pickUp(Player player)
	{
		player.getInventory().addItem(_wardItemInstance, "Territory Ward");
		player.getInventory().equipItem(_wardItemInstance);

		player.sendPacket(SystemMsg.YOUVE_ACQUIRED_THE_WARD);

		DominionSiegeRunnerEvent runnerEvent = EventHolder.getInstance().getEvent(EventType.MAIN_EVENT, 1);
		runnerEvent.broadcastTo(new SystemMessage2(SystemMsg.THE_S1_WARD_HAS_BEEN_DESTROYED_C2_NOW_HAS_THE_TERRITORY_WARD).addResidenceName(getDominionId()).addName(player));
		checkZoneForTerr(player);

		if (teleportBackTask != null)
			teleportBackTask.cancel(true);
	}

	public boolean isFlagOut()
	{
		return this._isOutOfZone;
	}

	protected void checkZoneForTerr(Player player)
	{
		if (!player.isInZone(ZoneType.SIEGE))
		{
			startTerrFlagCountDown(player);
		}
	}

	public void startTerrFlagCountDown(Player player)
	{
		if (_startTimerTask != null)
		{
			_startTimerTask.cancel(false);
			_startTimerTask = null;
		}
		_startTimerTask = ThreadPoolManager.getInstance().schedule(new DropFlagInstance(player), Config.INTERVAL_FLAG_DROP * 1000);

		player.sendMessage("You've leaved the battle zone! The flag will dissapear in " + Config.INTERVAL_FLAG_DROP + " seconds!");

		_isOutOfZone = true;
	}


	@Override
	public boolean canAttack(Player player)
	{
		player.sendPacket(SystemMsg.THAT_WEAPON_CANNOT_PERFORM_ANY_ATTACKS);
		return false;
	}

	@Override
	public boolean canCast(Player player, Skill skill)
	{
		Skill[] skills = player.getActiveWeaponItem().getAttachedSkills();
		if (player.getActiveWeaponItem().getAttachedSkills() == null)
		{
			player.sendPacket(SystemMsg.THAT_WEAPON_CANNOT_USE_ANY_OTHER_SKILL_EXCEPT_THE_WEAPONS_SKILL);
			return false;
		}

		if (!ArrayUtils.contains(skills, skill))
		{
			player.sendPacket(SystemMsg.THAT_WEAPON_CANNOT_USE_ANY_OTHER_SKILL_EXCEPT_THE_WEAPONS_SKILL);
			return false;
		}

		return true;
	}

	@Override
	public boolean canBeLost()
	{
		return true;
	}

	@Override
	public boolean canBeUnEquiped()
	{
		return false;
	}

	@Override
	public void setItem(ItemInstance item)
	{

	}

	public Location getWardLocation()
	{
		if (_wardItemInstance == null || _wardNpcInstance == null)
			return null;

		if (_wardItemInstance.getOwnerId() > 0)
		{
			Player player = GameObjectsStorage.getPlayer(_wardItemInstance.getOwnerId());
			if (player != null)
				return player.getLoc();
		}

		return _wardNpcInstance.getLoc();
	}

	public NpcInstance getWardNpcInstance()
	{
		return _wardNpcInstance;
	}

	public ItemInstance getWardItemInstance()
	{
		return _wardItemInstance;
	}

	public int getDominionId()
	{
		return _itemId - 13479;
	}

	public DominionSiegeEvent getEvent()
	{
		return _wardNpcInstance.getEvent(DominionSiegeEvent.class);
	}

	private class OnZoneEnterLeaveListenerImpl implements OnZoneEnterLeaveListener
	{
		@Override
		public void onZoneEnter(Zone zone, Creature actor)
		{
		}

		@Override
		public void onZoneLeave(Zone zone, Creature actor)
		{
			if (_wardItemInstance != null && actor.isPlayer() && _wardItemInstance.getOwnerId() == actor.getObjectId())
			{
				checkZoneForTerr(actor.getPlayer());
			}
		}
	}

	private class DropFlagInstance extends RunnableImpl
	{
		private final Player _player;

		public DropFlagInstance(Player paramPlayer)
		{
			_player = paramPlayer;
		}

		@Override
		public void runImpl()
		{
			if (!_player.isInZone(ZoneType.SIEGE))
				onLogout(_player);
		}
	}

	private class ReturnFlagThread implements Runnable
	{
		@Override
		public void run()
		{
			if (_wardNpcInstance != null)
			{
				_wardNpcInstance.teleToLocation(_location);
				DominionSiegeRunnerEvent runnerEvent = EventHolder.getInstance().getEvent(EventType.MAIN_EVENT, 1);
				runnerEvent.broadcastTo(new ExShowScreenMessage("Territory Ward returned to the castle!", 3000, ScreenMessageAlign.TOP_CENTER, false));
			}
		}
	}
}
