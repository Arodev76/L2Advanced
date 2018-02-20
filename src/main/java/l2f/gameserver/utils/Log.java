package l2f.gameserver.utils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2f.commons.configuration.Config;
import l2f.commons.text.PrintfFormat;
import l2f.gameserver.hwid.HwidGamer;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.GameObject;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.base.Element;
import l2f.gameserver.model.items.ItemInstance;
import l2f.gameserver.model.pledge.Clan;

public class Log
{
	public static final PrintfFormat LOG_BOSS_KILLED = new PrintfFormat("%s: %s[%d] killed by %s at Loc(%d %d %d) in %s");
	public static final PrintfFormat LOG_BOSS_RESPAWN = new PrintfFormat("%s: %s[%d] scheduled for respawn in %s at %s");

	private static final Logger _logChat = LoggerFactory.getLogger("chat");
	private static final Logger _logEvents = LoggerFactory.getLogger("events");
	private static final Logger _logGm = LoggerFactory.getLogger("gmactions");
	private static java.util.logging.Logger _logItems = null;
	private static final Logger _logGame = LoggerFactory.getLogger("game");
	private static final Logger _logService = LoggerFactory.getLogger("service");
	private static final Logger _logDebug = LoggerFactory.getLogger("debug");

	public static final String Create = "Create";
	public static final String Delete = "Delete";
	public static final String Drop = "Drop";
	public static final String PvPDrop = "PvPDrop";
	public static final String Crystalize = "Crystalize";
	public static final String EnchantFail = "EnchantFail";
	public static final String Pickup = "Pickup";
	public static final String PartyPickup = "PartyPickup";
	public static final String PrivateStoreBuy = "PrivateStoreBuy";
	public static final String PrivateStoreSell = "PrivateStoreSell";
	public static final String TradeBuy = "TradeBuy";
	public static final String TradeSell = "TradeSell";
	public static final String PostRecieve = "PostRecieve";
	public static final String PostSend = "PostSend";
	public static final String PostCancel = "PostCancel";
	public static final String PostExpire = "PostExpire";
	public static final String RefundSell = "RefundSell";
	public static final String RefundReturn = "RefundReturn";
	public static final String WarehouseDeposit = "WarehouseDeposit";
	public static final String WarehouseWithdraw = "WarehouseWithdraw";
	public static final String FreightWithdraw = "FreightWithdraw";
	public static final String FreightDeposit = "FreightDeposit";
	public static final String ClanWarehouseDeposit = "ClanWarehouseDeposit";
	public static final String ClanWarehouseWithdraw = "ClanWarehouseWithdraw";

	public static void add(PrintfFormat fmt, Object[] o, String cat)
	{
		add(fmt.sprintf(o), cat);
	}

	public static void add(String fmt, Object[] o, String cat)
	{
		add(new PrintfFormat(fmt).sprintf(o), cat);
	}

	public static void add(String text, String cat, Player player)
	{
		StringBuilder output = new StringBuilder();

		output.append(cat);
		if (player != null)
		{
			output.append(' ');
			output.append(player);
		}
		output.append(' ');
		output.append(text);

		_logGame.info(output.toString());
	}

	public static void add(String text, String cat)
	{
		add(text, cat, null);
	}

	public static void debug(String text)
	{
		_logDebug.debug(text);
	}

	public static void debug(String text, Throwable t)
	{
		_logDebug.debug(text, t);
	}

	public static void LogChat(String type, String player, String target, String text)
	{
		if (!Config.LOG_CHAT)
			return;

		StringBuilder output = new StringBuilder();
		output.append(type);
		output.append(' ');
		output.append('[');
		output.append(player);
		if (target != null)
		{
			output.append(" -> ");
			output.append(target);
		}
		output.append(']');
		output.append(' ');
		output.append(text);

		_logChat.info(output.toString());
	}

	public static void service(String text, String cat)
	{
		if (!Config.LOG_SERVICES)
		{
			return;
		}
		StringBuilder output = new StringBuilder();

		output.append(cat);
		output.append(": ");
		output.append(text);

		_logService.info(output.toString());
	}
	public static void LogEvents(String name, String action, String player, String target, String text)
	{
		StringBuilder output = new StringBuilder();
		output.append(name);
		output.append(": ");
		output.append(action);
		output.append(' ');
		output.append('[');
		output.append(player);
		if (target != null)
		{
			output.append(" -> ");
			output.append(target);
		}
		output.append(']');
		output.append(' ');
		output.append(text);

		_logEvents.info(output.toString());
	}

	public static void LogCommand(Player player, GameObject target, String command, boolean success)
	{
		//if (!Config.LOG_GM)
		//	return;

		StringBuilder output = new StringBuilder();

		if (success)
			output.append("SUCCESS");
		else
			output.append("FAIL   ");

		output.append(' ');
		output.append(player);
		if (target != null)
		{
			output.append(" -> ");
			output.append(target);
		}
		output.append(' ');
		output.append(command);

		_logGm.info(output.toString());
	}

	private static int DAY = -1;
	private static int HOUR_RANGE = -1;

	public static void LogDestroyItem(String owner, String process, ItemInstance item, long count)
	{
		if (item == null)
			return;
		LogActionItem("DESTROY", owner, process, itemToString(item, item.getCount()-count), count);
	}


	public static void LogRemoveItem(String owner, String process, ItemInstance item, long count)
	{
		if (item == null)
			return;
		LogActionItem("REMOVE", owner, process, itemToString(item, item.getCount()-count), count);
	}


	public static void LogAddItem(Clan clan, String process, ItemInstance item, long count)
	{
		if (clan == null || item == null)
			return;
		LogAddItem("Clan "+clan.toString(), process, itemToString(item, item.getCount()), count);
	}

	public static void LogAddItem(Creature activeChar, String process, ItemInstance item, long count)
	{
		if (activeChar == null || item == null)
			return;
		LogAddItem(activeChar.toString(), process, itemToString(item, item.getCount()), count);
	}

	public static void LogAddItem(String activeChar, String process, ItemInstance item, long count)
	{
		if (item == null)
			return;
		LogAddItem(activeChar, process, itemToString(item, item.getCount()), count);
	}

	public static void LogAddItem(String activeChar, String process, String item, long count)
	{
		LogActionItem("ADD", activeChar, process, item, count);
	}

	private static void LogActionItem(String action, String activeChar, String process, String item, long count)
	{
		if (activeChar == null || activeChar.isEmpty())
			return;
		if (process == null || process.isEmpty())
			return;
		if (item == null || item.isEmpty())
			return;
		if (!Config.ALLOW_ITEMS_LOGGING)
			return;

		Calendar c = Calendar.getInstance();
		if (_logItems == null || (c.get(Calendar.DAY_OF_MONTH) != DAY && c.get(Calendar.HOUR_OF_DAY) / 6 != HOUR_RANGE))
		{
			if (_logItems != null)
				closeLogger();
			DAY = c.get(Calendar.DAY_OF_MONTH);
			HOUR_RANGE = c.get(Calendar.HOUR_OF_DAY) / 6;
			_logItems = java.util.logging.Logger.getLogger(getFileName("Items"));
			installItemsHandler();
		}

		_logItems.info(action + ' ' + process + ' ' + item + ' ' + activeChar + ' ' + Util.getNumberWithCommas(count));
	}

	/**
	 * Logging Short Message to Items Log.
	 * Time will be included
	 * @param message to appear in Logs
	 */
	public static void LogMsgToItems(String message)
	{
		if (!Config.ALLOW_ITEMS_LOGGING)
			return;

		Calendar c = Calendar.getInstance();
		if (_logItems == null || (c.get(Calendar.DAY_OF_MONTH) != DAY && c.get(Calendar.HOUR_OF_DAY) / 6 != HOUR_RANGE))
		{
			if (_logItems != null)
				closeLogger();
			DAY = c.get(Calendar.DAY_OF_MONTH);
			HOUR_RANGE = c.get(Calendar.HOUR_OF_DAY) / 6;
			_logItems = java.util.logging.Logger.getLogger(getFileName("Items"));
			installItemsHandler();
		}

		_logItems.info(message);
	}

	private static String getFileName(String fileType)
	{
		Calendar c = Calendar.getInstance();
		return fileType+ ' ' + c.get(Calendar.DAY_OF_MONTH)+ '-' + (c.get(Calendar.MONTH)+1) + '-' + c.get(Calendar.YEAR) + " Part" + HOUR_RANGE;
	}

	private static void installItemsHandler()
	{
		try
		{
			new File("log/itemLogs").mkdirs();
			FileHandler handler = new FileHandler("log/itemLogs/" + getFileName("Items") + ".log", true);
			handler.setFormatter(new LogFormatter());
			_logItems.addHandler(handler);
			_logItems.setUseParentHandlers(false);
		}
		catch (SecurityException | IOException e)
		{
			e.printStackTrace();
		}
	}

	public static void closeLogger()
	{
		if (_logItems == null)
			return;
		for (Handler h : _logItems.getHandlers())
		{
			h.close();
		}

	}

	public static void LogPetition(Player fromChar, Integer Petition_type, String Petition_text)
	{
		//TODO: implement
	}

	public static void LogAudit(Player player, String type, String msg)
	{
		//TODO: implement
	}

	public static void LogToPlayerCommunity(HwidGamer gamer, Player player, String action)
	{
		if (gamer == null)
			return;

		gamer.logToPlayer(player.getObjectId(), action);
	}

	private static String itemToString(ItemInstance item, long count)
	{
		if (item == null)
			return "";

		StringBuilder sb = new StringBuilder();

		sb.append(item.getTemplate().getItemId());
		sb.append(' ');
		if (item.getEnchantLevel() > 0)
		{
			sb.append('+');
			sb.append(item.getEnchantLevel());
			sb.append(' ');
		}
		sb.append(item.getTemplate().getName());
		if (!item.getTemplate().getAdditionalName().isEmpty())
		{
			sb.append(' ');
			sb.append('\\').append(item.getTemplate().getAdditionalName()).append('\\');
		}
		for (Map.Entry<Element, Integer> attribute : item.getAttributes().getElements().entrySet())
		{
			sb.append(' ');
			sb.append(attribute.getKey().name());
			sb.append('=');
			sb.append(attribute.getValue());
			sb.append(' ');
		}
		if (item.getAugmentationId() > 0)
		{
			sb.append("Augment=");
			sb.append(item.getAugmentationId());
			sb.append(' ');
		}
		sb.append('(');
		sb.append(Util.getNumberWithCommas(count));
		sb.append(')');
		sb.append('[');
		sb.append(item.getObjectId());
		sb.append(']');

		return sb.toString();
	}
}