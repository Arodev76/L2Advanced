package l2f.gameserver.model.actor.recorder;

import l2f.commons.collections.CollectionUtils;
import l2f.gameserver.Config;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.base.Element;
import l2f.gameserver.model.base.TeamType;
import l2f.gameserver.model.matching.MatchingRoom;
import l2f.gameserver.network.serverpackets.ExStorageMaxCount;

public final class PlayerStatsChangeRecorder extends CharStatsChangeRecorder<Player>
{
  public static final int BROADCAST_KARMA = 8;
  public static final int SEND_STORAGE_INFO = 16;
  public static final int SEND_MAX_LOAD = 32;
  public static final int SEND_CUR_LOAD = 64;
  public static final int BROADCAST_CHAR_INFO2 = 128;
  private int _maxCp;
  private int _maxLoad;
  private int _curLoad;
   private int[] _attackElement = new int[6];
   private int[] _defenceElement = new int[6];
  private long _exp;
  private int _sp;
  private int _karma;
  private int _pk;
  private int _pvp;
  private int _fame;
  private int _inventory;
  private int _warehouse;
  private int _clan;
  private int _trade;
  private int _recipeDwarven;
  private int _recipeCommon;
  private int _partyRoom;
   private String _title = "";
  private int _cubicsHash;

  public PlayerStatsChangeRecorder(Player activeChar)
  {
	super(activeChar);
  }

  protected void refreshStats()
  {
	this._maxCp = set(4, this._maxCp, ((Player)this._activeChar).getMaxCp());

	super.refreshStats();

	this._maxLoad = set(34, this._maxLoad, ((Player)this._activeChar).getMaxLoad());
	this._curLoad = set(64, this._curLoad, ((Player)this._activeChar).getCurrentLoad());

	for (Element e : Element.VALUES)
	{
	this._attackElement[e.getId()] = set(2, this._attackElement[e.getId()], ((Player)this._activeChar).getAttack(e));
	this._defenceElement[e.getId()] = set(2, this._defenceElement[e.getId()], ((Player)this._activeChar).getDefence(e));
	}

	this._exp = set(2, this._exp, ((Player)this._activeChar).getExp());
	this._sp = set(2, this._sp, ((Player)this._activeChar).getIntSp());
	this._pk = set(2, this._pk, ((Player)this._activeChar).getPkKills());
	this._pvp = set(2, this._pvp, ((Player)this._activeChar).getPvpKills());
	this._fame = set(2, this._fame, ((Player)this._activeChar).getFame());

	this._karma = set(8, this._karma, ((Player)this._activeChar).getKarma());

	this._inventory = set(16, this._inventory, ((Player)this._activeChar).getInventoryLimit());
	this._warehouse = set(16, this._warehouse, ((Player)this._activeChar).getWarehouseLimit());
	this._clan = set(16, this._clan, Config.WAREHOUSE_SLOTS_CLAN);
	this._trade = set(16, this._trade, ((Player)this._activeChar).getTradeLimit());
	this._recipeDwarven = set(16, this._recipeDwarven, ((Player)this._activeChar).getDwarvenRecipeLimit());
	this._recipeCommon = set(16, this._recipeCommon, ((Player)this._activeChar).getCommonRecipeLimit());
	this._cubicsHash = set(1, this._cubicsHash, CollectionUtils.hashCode(((Player)this._activeChar).getCubics()));
	this._partyRoom = set(1, this._partyRoom, ((((Player)this._activeChar).getMatchingRoom() != null) && (((Player)this._activeChar).getMatchingRoom().getType() == MatchingRoom.PARTY_MATCHING) && (((Player)this._activeChar).getMatchingRoom().getLeader() == this._activeChar)) ? ((Player)this._activeChar).getMatchingRoom().getId() : 0);
	this._team = ((TeamType)set(128, this._team, ((Player)this._activeChar).getTeam()));
	this._title = set(1, this._title, ((Player)this._activeChar).getTitle());
  }

  protected void onSendChanges()
  {
	super.onSendChanges();

	if ((this._changes & 0x80) == 128)
	{
	((Player)this._activeChar).broadcastCharInfo();
	if (((Player)this._activeChar).getPet() != null)
		((Player)this._activeChar).getPet().broadcastCharInfo();
	}
/* 100 */	if ((this._changes & 0x1) == 1)
	((Player)this._activeChar).broadcastCharInfo();
	else if ((this._changes & 0x2) == 2) {
	((Player)this._activeChar).sendUserInfo();
	}
	if ((this._changes & 0x40) == 64) {
	((Player)this._activeChar).sendStatusUpdate(false, false, new int[] { 14 });
	}
	if ((this._changes & 0x20) == 32) {
	((Player)this._activeChar).sendStatusUpdate(false, false, new int[] { 15 });
	}
	if ((this._changes & 0x8) == 8) {
	((Player)this._activeChar).sendStatusUpdate(true, false, new int[] { 27 });
	}
	if ((this._changes & 0x10) == 16)
	((Player)this._activeChar).sendPacket(new ExStorageMaxCount((Player)this._activeChar));
  }
}