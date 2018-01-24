package l2f.gameserver.cache;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import l2f.gameserver.model.Player;
import l2f.gameserver.model.World;
import l2f.gameserver.model.items.ItemInfo;
import l2f.gameserver.model.items.ItemInstance;

public class ItemInfoCache
{
	private final static ItemInfoCache _instance = new ItemInfoCache();

	public final static ItemInfoCache getInstance()
	{
		return _instance;
	}

	private static Map<Integer, ItemInfo> _cache;
	
	private ItemInfoCache()
	{
		_cache = new ConcurrentHashMap<>();
	}
	
	public void put(ItemInstance item)
	{
		_cache.put(item.getObjectId(), new ItemInfo(item));
	}

	/**
	 * Get information from the cache on objecId subject. If a player is online and still owns this subject information will be updated.
	 * @param objectId - identifier objects
	 * @return returns a description of things, or null if no description, or has already been removed from the cache
	 */
	public ItemInfo get(int objectId)
	{
		if(!_cache.containsKey(objectId))
			return null;
		
		ItemInfo info = _cache.get(objectId);

		Player player = null;

		if (info != null)
		{
			player = World.getPlayer(info.getOwnerId());

			ItemInstance item = null;

			if (player != null)
				item = player.getInventory().getItemByObjectId(objectId);

			if (item != null && item.getItemId() == info.getItemId())
				_cache.put(item.getObjectId(), info = new ItemInfo(item));
		}

		return info;
	}
}
