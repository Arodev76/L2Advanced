package l2f.gameserver.model.entity.CCPHelpers.itemLogs;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import l2f.gameserver.Config;
import l2f.gameserver.data.HtmPropHolder;
import l2f.gameserver.data.HtmPropList;
import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.data.xml.holder.ItemHolder;
import l2f.gameserver.model.Player;
import l2f.gameserver.network.serverpackets.ShowBoard;
import l2f.gameserver.templates.item.ItemTemplate;

public class CCPItemLogs
{
	public static final int FIRST_PAGE_INDEX = 0;

	private static int tableHeight = -100;
	private static int headerHeight = -100;
	private static int itemHeight = -100;
	private static int maxHeight = -100;

	public static void showPage(Player player)
	{
		showPage(player, 0);
	}

	public static void showPage(Player player, int pageIndex)
	{
		if (!Config.ENABLE_PLAYER_ITEM_LOGS)
		{
			return;
		}
		if (tableHeight == -100)
		{
			HtmPropList props = HtmPropHolder.getList(new StringBuilder().append(Config.BBS_HOME_DIR).append("pages/itemLogs.prop.htm").toString());
			tableHeight = Integer.parseInt(props.getText("table_height"));
			headerHeight = Integer.parseInt(props.getText("header_height"));
			itemHeight = Integer.parseInt(props.getText("item_height"));
			maxHeight = Integer.parseInt(props.getText("page_max_height"));
		}

		String html = preparePage(player, pageIndex);
		ShowBoard.separateAndSend(html, player);
	}

	private static String preparePage(Player player, int pageIndex)
	{
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");
		HtmPropList props = HtmPropHolder.getList(new StringBuilder().append(Config.BBS_HOME_DIR).append("pages/itemLogs.prop.htm").toString());
		String html = HtmCache.getInstance().getNotNull(new StringBuilder().append(Config.BBS_HOME_DIR).append("pages/itemLogs.htm").toString(), player);

		List<ItemActionLog> wrongOrderAllLogs = ItemLogList.getInstance().getLogs(player);
		List<ItemActionLog> allLogs = changeOrder(wrongOrderAllLogs);
		int[] pageItemToStartFrom = getLogIndexToStartFrom(allLogs, pageIndex);

		StringBuilder tablesLeft = new StringBuilder();
		StringBuilder tablesRight = new StringBuilder();
		int side = 0;
		int heightReached = 0;
		int startingItemIndex = pageItemToStartFrom[1];
		int itemIndex = 0;
		int logCount = startingItemIndex;

		for (int logIndex = pageItemToStartFrom[0]; (logIndex < allLogs.size()) && (side < 2); logIndex++)
		{
			boolean changeSide = false;
			ItemActionLog log = allLogs.get(logIndex);

			String table = getLogsTable(player, log, heightReached, startingItemIndex, itemIndex, dateFormat);
			if ((table == null) || (player.containsQuickVar("CCPItemLogsStartingItemIndex")))
			{
				changeSide = true;
				heightReached = 0;
				logCount--;
				startingItemIndex = player.getQuickVarI("CCPItemLogsStartingItemIndex", new int[]
					{
					0
					});
				player.deleteQuickVar("CCPItemLogsStartingItemIndex");
			}
			else
			{
				itemIndex += log.getItemsReceived().length + log.getItemsLost().length;
				heightReached += player.getQuickVarI("CCPItemLogsHeightReached", new int[]
					{
					0
					});
				startingItemIndex = 0;
				logCount++;
			}

			if (table != null)
			{
				if (side == 0)
					tablesLeft.append(table);
				else
				{
					tablesRight.append(table);
				}
			}
			if (changeSide)
			{
				side++;
			}
		}
		html = html.replace("%tablesLeft%", tablesLeft.length() > 0 ? tablesLeft : "<br>");
		html = html.replace("%tablesRight%", tablesRight.length() > 0 ? tablesRight : "<br>");
		html = html.replace("%previousBtn%", pageIndex > 0 ? props.getText("PreviousBtn").replace("%page%", String.valueOf(pageIndex - 1)) : "<br>");
		html = html.replace("%nextBtn%", (logCount < allLogs.size()) || (startingItemIndex > 0) ? props.getText("NextBtn").replace("%page%", String.valueOf(pageIndex + 1)) : "<br>");

		return html;
	}

	private static String getLogsTable(Player player, ItemActionLog log, int heightReached, int startingItemIndex, int itemIndex, SimpleDateFormat dateFormat)
	{
		HtmPropList props = HtmPropHolder.getList(new StringBuilder().append(Config.BBS_HOME_DIR).append("pages/itemLogs.prop.htm").toString());
		String date = dateFormat.format(new Date(log.getTime()));

		int newHeight = heightReached;
		if (newHeight + tableHeight + headerHeight + itemHeight > maxHeight)
		{
			return null;
		}

		newHeight += tableHeight;
		String table = props.getText("table");

		if (startingItemIndex == 0)
		{
			String header = props.getText("header");
			header = header.replace("%actionType%", log.getActionType().getNiceName());
			table = table.replace("%header%", header);
			newHeight += headerHeight;
		}
		else
		{
			table = table.replace("%header%", "");
		}

		StringBuilder itemsBuilder = new StringBuilder();
		for (int i = 0; i < 2; i++)
		{
			SingleItemLog[] items = i == 0 ? log.getItemsReceived() : log.getItemsLost();
			if (startingItemIndex > items.length)
			{
				startingItemIndex -= items.length;
			}
			else
			{
				for (int currentItemIndex = startingItemIndex; currentItemIndex < items.length; currentItemIndex++)
				{
					SingleItemLog item = items[currentItemIndex];

					if (newHeight + itemHeight > maxHeight)
					{
						int totalItemIndex = currentItemIndex + (i > 0 ? log.getItemsReceived().length : 0);
						player.addQuickVar("CCPItemLogsStartingItemIndex", Integer.valueOf(totalItemIndex));
						return table.replace("%items%", itemsBuilder.toString());
					}

					ItemTemplate template = ItemHolder.getInstance().getTemplate(item.getItemTemplateId());
					String itemText = props.getText("item");
					itemText = itemText.replace("%itemTableColor%", itemIndex % 2 == 0 ? props.getText("item_table_color_0") : props.getText("item_table_color_1"));
					itemText = itemText.replace("%icon%", template.getIcon());
					String itemName = new StringBuilder().append(template.getName()).append(item.getItemEnchantLevel() > 0 ? new StringBuilder().append(" + ").append(item.getItemEnchantLevel()).toString() : "").append(item.getItemCount() > 1L ? new StringBuilder().append(" x ").append(item.getItemCount()).toString() : "").toString();
					itemText = itemText.replace("%itemName%", itemName);
					itemText = itemText.replace("%time%", date);
					String receiverName = (item.getReceiverName() != null) && (!item.getReceiverName().isEmpty()) ? item.getReceiverName() : "Nobody";
					itemText = itemText.replace("%receiverColor%", receiverName.equals(player.getName()) ? props.getText("receiver_color_owner") : props.getText("receiver_color_alien"));
					itemText = itemText.replace("%receiverName%", receiverName);

					itemsBuilder.append(itemText);
					itemIndex++;
					newHeight += itemHeight;
				}

				startingItemIndex = 0;
			}
		}

		player.deleteQuickVar("CCPItemLogsStartingItemIndex");
		player.addQuickVar("CCPItemLogsHeightReached", Integer.valueOf(newHeight - heightReached));

		return table.replace("%items%", itemsBuilder.toString());
	}

	public static int[] getLogIndexToStartFrom(List<ItemActionLog> allLogs, int pageIndexToReach)
	{
		if (pageIndexToReach <= 0)
		{
			return new int[]
				{
				0,
				0
				};
		}
		int pageReached = 0;
		boolean useRightSide = false;
		int heightReached = 0;
		int startingItem = 0;

		for (int logIndex = 0; logIndex < allLogs.size(); logIndex++)
		{
			ItemActionLog log = allLogs.get(logIndex);

			int[] itemHeightReached = getItemAndHeightReached(log, startingItem, heightReached);

			startingItem = itemHeightReached[0];
			heightReached = itemHeightReached[1];
			if ((startingItem == -1) || (startingItem < 2147483647))
			{
				heightReached = 0;

				if (startingItem < 0)
				{
					startingItem = 0;
				}
				if (useRightSide)
				{
					pageReached++;
					if (pageReached >= pageIndexToReach)
					{
						return new int[]
							{
							logIndex,
							startingItem
							};
					}
				}
				else
				{
					useRightSide = true;
				}
				logIndex--;
			}
			else if (startingItem == 2147483647)
			{
				startingItem = 0;
			}
		}
		return new int[]
			{
			0,
			0
			};
	}

	private static int[] getItemAndHeightReached(ItemActionLog log, int startFromItem, int heightReached)
	{
		int newHeight = heightReached;

		if (newHeight + tableHeight + headerHeight + itemHeight > maxHeight)
		{
			return new int[]
				{
				-1,
				newHeight
				};
		}

		newHeight += tableHeight;
		if (startFromItem == 0)
		{
			newHeight += headerHeight;
		}
		for (int item = startFromItem; item < log.getItemsReceived().length + log.getItemsLost().length; item++)
		{
			if (newHeight + itemHeight > maxHeight)
			{
				return new int[]
					{
					item,
					newHeight
					};
			}
			newHeight += itemHeight;
		}
		return new int[]
			{
			2147483647,
			newHeight
			};
	}

	private static List<ItemActionLog> changeOrder(List<ItemActionLog> wrongOrderAllLogs)
	{
		if (wrongOrderAllLogs.isEmpty())
		{
			return wrongOrderAllLogs;
		}
		List<ItemActionLog> logs = new ArrayList<>();
		for (int i = wrongOrderAllLogs.size() - 1; i >= 0; i--)
			logs.add(wrongOrderAllLogs.get(i));
		return logs;
	}
}