package l2f.gameserver.model.entity.CCPHelpers.itemLogs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import l2f.gameserver.Config;
import l2f.gameserver.database.DatabaseFactory;
import l2f.gameserver.model.Player;
import l2f.gameserver.utils.BatchStatement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ItemLogList
{
	private static final Logger LOG = LoggerFactory.getLogger(ItemLogList.class);
	public static final SingleItemLog[] EMPTY_ITEM_LOGS = new SingleItemLog[0];
	private final Map<Integer, List<ItemActionLog>> _logLists;

	public ItemLogList()
	{
		this._logLists = new ConcurrentHashMap<>();
	}

	public List<ItemActionLog> getLogs(Player player)
	{
		if (!Config.ENABLE_PLAYER_ITEM_LOGS)
		{
			return new ArrayList<>();
		}
		List<ItemActionLog> list = _logLists.get(Integer.valueOf(player.getObjectId()));
		if (list == null)
			return new ArrayList<>();
		return list;
	}

	public void addLogs(ItemActionLog logs)
	{
		if (!Config.ENABLE_PLAYER_ITEM_LOGS)
		{
			return;
		}
		Integer playerObjectId = Integer.valueOf(logs.getPlayerObjectId());
		List<ItemActionLog> list;
		if (this._logLists.containsKey(playerObjectId))
		{
			list = this._logLists.get(playerObjectId);
		}
		else
		{
			list = new CopyOnWriteArrayList<>();
			this._logLists.put(playerObjectId, list);
		}

		list.add(logs);
	}

	public void fillReceiver(int itemObjectId, String playerName)
	{
		if (!Config.ENABLE_PLAYER_ITEM_LOGS)
		{
			return;
		}
		for (List<ItemActionLog> logList : this._logLists.values())
		{
			for (ItemActionLog log : logList)
			{
				if (!log.getActionType().isReceiverKnown())
				{
					for (SingleItemLog item : log.getItemsLost())
					{
						if ((item.getItemObjectId() != itemObjectId) || ((item.getReceiverName() != null) && (!item.getReceiverName().isEmpty())))
							continue;
						item.setReceiverName(playerName);
						return;
					}
				}
			}
		}
	}

	public void loadAllLogs()
	{
		if (!Config.ENABLE_PLAYER_ITEM_LOGS)
		{
			return;
		}
		ItemLogHandler.getInstance().loadLastActionId();

		Map<Integer, ItemActionLog> logsById = new HashMap<>();
		long logsSince = System.currentTimeMillis() - Config.PLAYER_ITEM_LOGS_MAX_TIME;

		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("SELECT * FROM logs WHERE time > ? ORDER BY time ASC"))
			{
				statement.setLong(1, logsSince);
				try (ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
					{
						int logId = rset.getInt("log_id");
						int playerObjectId = rset.getInt("player_object_id");
						ItemActionType actionType = ItemActionType.valueOf(rset.getString("action_type"));
						long time = rset.getLong("time");

						List<ItemActionLog> logs = this._logLists.get(Integer.valueOf(playerObjectId));
						if (logs == null)
						{
							logs = new ArrayList<>();
							this._logLists.put(Integer.valueOf(playerObjectId), logs);
						}

						ItemActionLog log = new ItemActionLog(logId, playerObjectId, actionType, time, EMPTY_ITEM_LOGS, EMPTY_ITEM_LOGS, true);
						logs.add(log);
						logsById.put(Integer.valueOf(logId), log);
					}
				}
			}
			catch (SQLException e)
			{
				LOG.error("ItemLogList.loadAllLogs():", e);
			}

			int smallestLogId = getSmallestLogId(logsById.keySet());

			try (PreparedStatement statement = con.prepareStatement("SELECT * FROM logs_items WHERE log_id >= ?"))
			{
				statement.setInt(1, smallestLogId);
				try (ResultSet rset = statement.executeQuery())
				{
					while (rset.next())
					{
						ItemActionLog log = logsById.get(Integer.valueOf(rset.getInt("log_id")));
						if (log != null)
						{
							int itemObjectId = rset.getInt("item_object_id");
							int itemTemplateId = rset.getInt("item_template_id");
							long itemCount = rset.getLong("item_count");
							int itemEnchantLevel = rset.getInt("item_enchant_level");
							String receiverName = rset.getString("receiver_name");

							SingleItemLog itemLog = new SingleItemLog(itemTemplateId, itemCount, itemEnchantLevel, itemObjectId, receiverName);

							log.addItemLog(itemLog, rset.getInt("lost") == 1);
						}
					}
				}
			}
			catch (SQLException e)
			{
				LOG.error("ItemLogList.loadAllLogs():", e);
			}
		}
		catch (SQLException e)
		{
			e.printStackTrace();
		}
	}

	private static int getSmallestLogId(Set<Integer> set)
	{
		int smallest = Integer.MAX_VALUE;
		for (int i : set)
		{
			if (i < smallest)
			{
				smallest = i;
			}
		}

		return smallest;
	}

	public void saveAllLogs()
	{
		if (!Config.ENABLE_PLAYER_ITEM_LOGS)
		{
			return;
		}

		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			LOG.info("Saving Logs");

			try (PreparedStatement statement = BatchStatement.createPreparedStatement(con, "INSERT INTO `logs` VALUES (?, ?, ?, ?);"))
			{
				for (List<ItemActionLog> list : this._logLists.values())
				{
					for (ItemActionLog log : list)
					{
						if (!log.isSavedInDatabase())
						{
							statement.setInt(1, log.getActionId());
							statement.setInt(2, log.getPlayerObjectId());
							statement.setString(3, log.getActionType().toString());
							statement.setLong(4, log.getTime());
							statement.addBatch();
						}
					}
				}

				statement.executeBatch();
				con.commit();
			}
			catch (Exception e)
			{
				LOG.error("Failed to save all Item Logs: ", e);
			}

			LOG.info("Saving Logs_Items");

			try (PreparedStatement statement = BatchStatement.createPreparedStatement(con, "INSERT INTO `logs_items` VALUES (?, ?, ?, ?, ?, ?, ?);"))
			{
				for (List<ItemActionLog> list : this._logLists.values())
				{
					for (ItemActionLog log : list)
					{
						if (!log.isSavedInDatabase())
						{
							for (int i = 0; i < 2; i++)
							{
								boolean isLostItem = i == 1;
								SingleItemLog[] items = isLostItem ? log.getItemsLost() : log.getItemsReceived();

								for (SingleItemLog item : items)
								{
									statement.setInt(1, log.getActionId());
									statement.setInt(2, item.getItemObjectId());
									statement.setInt(3, item.getItemTemplateId());
									statement.setLong(4, item.getItemCount());
									statement.setInt(5, item.getItemEnchantLevel());
									statement.setInt(6, isLostItem ? 1 : 0);
									statement.setString(7, item.getReceiverName() == null ? "" : item.getReceiverName());
									statement.addBatch();
								}
							}
						}
					}
				}

				statement.executeBatch();
				con.commit();
			}
			catch (SQLException e)
			{
				LOG.error("Failed to save all Single Item Logs: ", e);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		LOG.info("Logs Saved!");
	}

	public static ItemLogList getInstance()
	{
		return ItemLogListHolder.instance;
	}

	private static class ItemLogListHolder
	{
		private static final ItemLogList instance = new ItemLogList();
	}
}