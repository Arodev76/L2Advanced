package services.community;

import java.util.StringTokenizer;

import l2f.gameserver.Config;
import l2f.gameserver.cache.ImagesCache;
import l2f.gameserver.cache.Msg;
import l2f.gameserver.dao.CharacterDAO;
import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.handler.bbs.CommunityBoardManager;
import l2f.gameserver.handler.bbs.ICommunityBoardHandler;
import l2f.gameserver.listener.actor.player.OnAnswerListener;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.events.impl.SiegeEvent;
import l2f.gameserver.model.instances.SchemeBufferInstance;
import l2f.gameserver.model.pledge.Clan;
import l2f.gameserver.model.pledge.SubUnit;
import l2f.gameserver.network.clientpackets.CharacterCreate;
import l2f.gameserver.network.serverpackets.ConfirmDlg;
import l2f.gameserver.network.serverpackets.HideBoard;
import l2f.gameserver.network.serverpackets.Say2;
import l2f.gameserver.network.serverpackets.ShowBoard;
import l2f.gameserver.network.serverpackets.SystemMessage;
import l2f.gameserver.network.serverpackets.components.ChatType;
import l2f.gameserver.network.serverpackets.components.CustomMessage;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.scripts.ScriptFile;
import l2f.gameserver.tables.ClanTable;
import l2f.gameserver.taskmanager.AutoImageSenderManager;
import l2f.gameserver.templates.item.ItemTemplate;
import l2f.gameserver.utils.Log;
import l2f.gameserver.utils.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunityNpcs implements ScriptFile, ICommunityBoardHandler
{
	private static final Logger _log = LoggerFactory.getLogger(CommunityNpcs.class);
	@Override
	public void onLoad()
	{
		if (Config.COMMUNITYBOARD_ENABLED)
		{
			_log.info("CommunityBoard: Npcs loaded.");
			CommunityBoardManager.getInstance().registerHandler(this);
		}
	}

	@Override
	public void onReload()
	{
		if (Config.COMMUNITYBOARD_ENABLED)
			CommunityBoardManager.getInstance().removeHandler(this);
	}

	@Override
	public void onShutdown()
	{
	}

	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
			"_bbsgetfav",
			"_bbsnpcs",
			"_bbsgatekeeper",
			"_bbsbuffer",
			"_bbsbufferbypass",
			"_decreasePKPage",
			"_decreasePK",
			"_actionToAsk",
			"_changeNick",
			"_changeClanName"
		};
	}

	@Override
	public void onBypassCommand(Player player, String bypass)
	{
		//Checking if all required images were sent to the player, if not - not allowing to pass
		if (!AutoImageSenderManager.wereAllImagesSent(player))
		{
			player.sendPacket(new Say2(player.getObjectId(), ChatType.CRITICAL_ANNOUNCE, "CB", "Community wasn't loaded yet, try again in few seconds."));
			return;
		}

		StringTokenizer st = new StringTokenizer(bypass, "_");
		String cmd = st.nextToken();
		String folder = "";
		String file = "";

		if ("bbsgetfav".equals(cmd) || "bbsnpcs".equals(cmd))
		{
			sendFileToPlayer(player, "bbs_npcs.htm", true);
			return;
		}

		if ("bbsgatekeeper".equals(cmd))
			folder = "gatekeeper";

		if ("bbsbuffer".equals(cmd))
		{
			SchemeBufferInstance.showWindow(player);
			return;
		}

		if ("bbsbufferbypass".equals(cmd))
		{
			if (bypass.contains("_bbsbufferbypass_"))
				SchemeBufferInstance.onBypass(player, bypass.substring("_bbsbufferbypass_".length()));
			return;
		}

		if ("decreasePKPage".equals(cmd))
		{
			decreasePKPage(player);
			return;
		}

		if ("decreasePK".equals(cmd))
		{
			if (!player.isInPeaceZone())
			{
				player.sendMessage("You cannot do it right now!");
				return;
			}
			int pksToDecrease = Integer.parseInt(st.nextToken());
			if (player.getInventory().getCountOf(Config.SERVICES_WASH_PK_ITEM) >= Config.SERVICES_WASH_PK_PRICE)
			{
				player.getInventory().destroyItemByItemId(Config.SERVICES_WASH_PK_ITEM, Config.SERVICES_WASH_PK_PRICE, "Decreasing Pk");
				player.setPkKills(player.getPkKills() - pksToDecrease);
				player.broadcastCharInfo();
				player.sendMessage(pksToDecrease + " PKs were decreased!");
			}
			else
			{
				player.sendMessage("You don't have required items!");
			}
			onBypassCommand(player, "decreasePKPage");
			return;
		}

		if ("actionToAsk".equals(cmd))
		{
			int id = Integer.parseInt(st.nextToken());
			String message = null;
			if (id == 0)
				message = "Would you like to increase Warehouse Slots by 1 for " + Config.SERVICES_EXPAND_INVENTORY_PRICE + " Coin?";
			else if (id == 1)
				message = "Would you like to increase Clan Warehouse Slots by 1 for " + Config.SERVICES_EXPAND_CWH_PRICE + " Coin?";

			if (message != null)
			{
				player.ask(new ConfirmDlg(SystemMsg.S1, 60000).addString(message), new ActionAnswerListener(player, id));
			}
			sendFileToPlayer(player, "smallNpcs/exclusiveShop.htm", true);
			return;
		}

		if ("changeNick".equals(cmd))
		{
			if (st.hasMoreTokens())
			{
				String newName = st.nextToken().trim();
				changeNick(player, newName);
			}
			return;
		}

		if ("changeClanName".equals(cmd))
		{
			if (st.hasMoreTokens())
			{
				String newName = st.nextToken().trim();
				changeClanName(player, newName);
			}
			return;
		}

		if (!folder.isEmpty())
		{
			file = st.hasMoreTokens() ? st.nextToken() : "main.htm";
		}

		sendFileToPlayer(player, folder + '/' + file, false);
	}

	private static void changeNick(Player player, String newName)
	{
		if (player == null)
			return;
		if (!Config.SERVICES_CHANGE_NICK_ENABLED2)
		{
			player.sendMessage("Service is disabled.");
			return;
		}
		if (player.isHero())
		{
			player.sendMessage("Not available for heroes.");
			return;
		}

		if (player.getEvent(SiegeEvent.class) != null)
		{
			player.sendMessage(new CustomMessage("scripts.services.Rename.SiegeNow", player));
			return;
		}

		if (!CharacterCreate.checkName(newName) && !Config.SERVICES_CHANGE_NICK_ALLOW_SYMBOL2)
		{
			player.sendMessage(new CustomMessage("scripts.services.Rename.incorrectinput", player));
			return;
		}

		if (player.getInventory().getCountOf(Config.SERVICES_CHANGE_NICK_ITEM2) < Config.SERVICES_CHANGE_NICK_PRICE2)
		{
			if (Config.SERVICES_CHANGE_NICK_ITEM == ItemTemplate.ITEM_ID_ADENA)
				player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
			else
				player.sendPacket(SystemMsg.INCORRECT_ITEM_COUNT);
			return;
		}

		if (CharacterDAO.getInstance().getObjectIdByName(newName) > 0)
		{
			player.sendMessage(new CustomMessage("scripts.services.Rename.Thisnamealreadyexists", player));
			return;
		}

		player.getInventory().destroyItemByItemId(Config.SERVICES_CHANGE_NICK_ITEM2, Config.SERVICES_CHANGE_NICK_PRICE2, "Nick Change");

		String oldName = player.getName();
		player.reName(newName, true);
		Log.add("Character " + oldName + " renamed to " + newName, "renames");
		player.sendMessage(new CustomMessage("scripts.services.Rename.changedname", player).addString(oldName).addString(newName));
		player.sendPacket(new HideBoard());
	}

	private static void changeClanName(Player player, String newName)
	{
		if (player == null)
			return;

		if (!Config.SERVICES_CHANGE_CLAN_NAME_ENABLED2)
		{
			player.sendMessage("Service is disabled.");
			return;
		}

		if (player.getClan() == null || !player.isClanLeader())
		{
			player.sendPacket(new SystemMessage(SystemMessage.S1_IS_NOT_A_CLAN_LEADER).addName(player));
			return;
		}

		if (player.getEvent(SiegeEvent.class) != null)
		{
			player.sendMessage(new CustomMessage("scripts.services.Rename.SiegeNow", player));
			return;
		}

		if (!Util.isMatchingRegexp(newName, Config.CLAN_NAME_TEMPLATE))
		{
			player.sendPacket(Msg.CLAN_NAME_IS_INCORRECT);
			return;
		}

		if (ClanTable.getInstance().getClanByName(newName) != null)
		{
			player.sendPacket(Msg.THIS_NAME_ALREADY_EXISTS);
			return;
		}

		if (player.getInventory().getCountOf(Config.SERVICES_CHANGE_CLAN_NAME_ITEM2) < Config.SERVICES_CHANGE_CLAN_NAME_PRICE2)
		{
			player.sendMessage("Incorrect Coin Count. You need " + Config.SERVICES_CHANGE_CLAN_NAME_PRICE2 + ". You have " + player.getInventory().getCountOf(Config.SERVICES_CHANGE_CLAN_NAME_ITEM2));
			return;
		}

		player.getInventory().destroyItemByItemId(Config.SERVICES_CHANGE_CLAN_NAME_ITEM2, Config.SERVICES_CHANGE_CLAN_NAME_PRICE2, "Clan Name Change");

		final String oldName = player.getClan().getName();
		Log.add("Clan " + oldName + " renamed to " + newName, "renames");
		player.sendMessage("You have changed your clan's name from " + oldName + " to " + newName);
		player.sendPacket(new HideBoard());

		SubUnit sub = player.getClan().getSubUnit(Clan.SUBUNIT_MAIN_CLAN);
		sub.setName(newName, true);
		player.getClan().broadcastClanStatus(true, true, false);
	}

	private static void sendFileToPlayer(Player player, String path, boolean sendImages, String... replacements)
	{
		String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + path, player);

		if (sendImages)
			ImagesCache.getInstance().sendUsedImages(html, player);

		for (int i = 0; i < replacements.length; i += 2)
		{
			String toReplace = replacements[i + 1];
			if (toReplace == null)
				toReplace = "<br>";
			html = html.replace(replacements[i], toReplace);
		}

		ShowBoard.separateAndSend(html, player);
	}

	private static final int FIELDS_IN_SUB_SELECT_PAGE = 11;

	private String[] fillReplacements(String[] currentReplacements, String stringInside, int count)
	{
		for (int i = 0; i <= count; i++)
			currentReplacements[i * 2] = '%' + stringInside + i + '%';
		return currentReplacements;
	}

	private static final int[] PKS =
	{
		1,
		5,
		10,
		50,
		100 //, 250, 500, 1000
	};

	private void decreasePKPage(Player player)
	{
		String[] replacements = new String[FIELDS_IN_SUB_SELECT_PAGE * 2];
		replacements = fillReplacements(replacements, "action", 10);

		int replaceIndex = 1;

		for (int pk : PKS)
		{
			if (player.getPkKills() <= pk)
				break;
			replacements[replaceIndex] = getDecreasePkButton(pk);
			replaceIndex += 2;
		}

		if (player.getPkKills() > 0)
		{
			replacements[replaceIndex] = getDecreasePkButton(player.getPkKills());
			replaceIndex += 2;
		}
		replacements[replaceIndex] = "<button value=\"Back\" action=\"bypass _bbsfile:smallNpcs/exclusiveShop\" width=100 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">";

		sendFileToPlayer(player, "smallNpcs/exclusiveShop_select.htm", true, replacements);
	}

	private static String getDecreasePkButton(int pks)
	{
		return "<button value=\"" + pks + " PKs for " + (Config.SERVICES_WASH_PK_PRICE * pks) + " Coins\" action=\"bypass _decreasePK_" + pks + "\" width=200 height=30 back=\"L2UI_CT1.OlympiadWnd_DF_HeroConfirm_Down\" fore=\"L2UI_ct1.OlympiadWnd_DF_HeroConfirm\">";
	}

	private static class ActionAnswerListener implements OnAnswerListener
	{
		private final Player player;
		private final int action;

		private ActionAnswerListener(Player player, int action)
		{
			this.player = player;
			this.action = action;
		}

		@Override
		public void sayYes()
		{
			if (action == 0)// Inventory
			{
				if (player.getInventory().destroyItemByItemId(Config.SERVICES_EXPAND_WAREHOUSE_ITEM, Config.SERVICES_EXPAND_WAREHOUSE_PRICE, "Inventory Expand"))
				{
					player.setExpandWarehouse(player.getExpandWarehouse() + 1);
					player.setVar("ExpandWarehouse", String.valueOf(player.getExpandWarehouse()), -1);
					player.sendMessage("Warehouse capacity is now " + player.getWarehouseLimit());
				}
				else if (Config.SERVICES_EXPAND_WAREHOUSE_ITEM == 57)
					player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				else
					player.sendPacket(SystemMsg.INCORRECT_ITEM_COUNT);
			}
			else if (action == 1)// CWH
			{
				if (player.getClan() == null)
				{
					player.sendMessage("You need to have Clan, to use that option!");
					return;
				}
				if (player.getClan().getWhBonus() >= 500)
				{
					player.sendMessage("500 Slots is Max for Clan Warehouse");
					return;
				}
				if (player.getClan() == null)
				{
					player.sendMessage("You must be in clan.");
					return;
				}

				if (player.getInventory().destroyItemByItemId(Config.SERVICES_EXPAND_CWH_ITEM, Config.SERVICES_EXPAND_CWH_PRICE, "CWH Expand"))
				{
					player.getClan().setWhBonus(player.getClan().getWhBonus() + 1);
					player.sendMessage("Warehouse capacity is now " + (Config.WAREHOUSE_SLOTS_CLAN + player.getClan().getWhBonus()));
				}
				else if (Config.SERVICES_EXPAND_CWH_ITEM == ItemTemplate.ITEM_ID_ADENA)
					player.sendPacket(Msg.YOU_DO_NOT_HAVE_ENOUGH_ADENA);
				else
					player.sendPacket(SystemMsg.INCORRECT_ITEM_COUNT);
			}
		}

		@Override
		public void sayNo()
		{
		}
	}

	@Override
	public void onWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
	}
}
