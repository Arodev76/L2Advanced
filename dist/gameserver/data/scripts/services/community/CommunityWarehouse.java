package services.community;

import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2f.commons.configuration.Config;
import l2f.gameserver.cache.HtmCache;
import l2f.gameserver.handler.bbs.CommunityBoardManager;
import l2f.gameserver.handler.bbs.ICommunityBoardHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.network.serverpackets.ShowBoard;
import l2f.gameserver.scripts.ScriptFile;
import l2f.gameserver.utils.WarehouseFunctions;

/**
 * @author RuleZzz
 */
public class CommunityWarehouse implements ScriptFile, ICommunityBoardHandler
{
	private static final Logger _log = LoggerFactory.getLogger(CommunityWarehouse.class);

	@Override
	public String[] getBypassCommands()
	{
		return new String[] { "_bbswarehouse", };
	}

	@Override
	public void onBypassCommand(Player player, String bypass)
	{
		if (!Config.ALLOW_BBS_WAREHOUSE) return;

		if (player == null) return;

		if (!Config.BBS_WAREHOUSE_ALLOW_PK && player.getKarma() > 0)
		{
			player.sendMessage("PK can not use a warehouse");
			return;
		}

		StringTokenizer st = new StringTokenizer(bypass, ":");
		st.nextToken();
		String action = st.hasMoreTokens() ? st.nextToken() : "";
		if (action.equalsIgnoreCase("private_deposit")) WarehouseFunctions.showDepositWindow(player);
		else if (action.equalsIgnoreCase("private_retrieve")) WarehouseFunctions.showRetrieveWindow(player, getVal(st.nextToken()));
		else if (action.equalsIgnoreCase("clan_deposit")) WarehouseFunctions.showDepositWindowClan(player);
		else if (action.equalsIgnoreCase("clan_retrieve")) WarehouseFunctions.showWithdrawWindowClan(player, getVal(st.nextToken()));
		showMain(player);
	}

	private int getVal(String name)
	{
		name = name.trim();
		if (name.equalsIgnoreCase("weapon") || name.equalsIgnoreCase("weapon") || name.equalsIgnoreCase("1")) 
			return 1;
		else if (name.equalsIgnoreCase("armor") || name.equalsIgnoreCase("armor") || name.equalsIgnoreCase("2")) 
			return 2;
		else if (name.equalsIgnoreCase("jewelry") || name.equalsIgnoreCase("jewelry") || name.equalsIgnoreCase("3")) 
			return 3;
		else if (name.equalsIgnoreCase("ornamentation") || name.equalsIgnoreCase("ornamentation") || name.equalsIgnoreCase("4")) 
			return 4;
		else if (name.equalsIgnoreCase("supplies") || name.equalsIgnoreCase("supplies") || name.equalsIgnoreCase("5")) 
			return 5;
		else if (name.equalsIgnoreCase("materials") || name.equalsIgnoreCase("materials") || name.equalsIgnoreCase("6")) 
			return 6;
		else if (name.equalsIgnoreCase("key materials") || name.equalsIgnoreCase("key materials") || name.equalsIgnoreCase("7")) 
			return 7;
		else if (name.equalsIgnoreCase("recipes") || name.equalsIgnoreCase("recipes") || name.equalsIgnoreCase("8")) 
			return 8;
		else if (name.equalsIgnoreCase("books") || name.equalsIgnoreCase("books") || name.equalsIgnoreCase("9")) 
			return 9;
		else if (name.equalsIgnoreCase("Miscellaneous") || name.equalsIgnoreCase("Miscellaneous") || name.equalsIgnoreCase("10")) 
			return 10;
		else if (name.equalsIgnoreCase("Other") || name.equalsIgnoreCase("Other") || name.equalsIgnoreCase("11")) 
			return 11;

		return 0;
	}

	private void showMain(Player player)
	{
		if (player == null) return;
		String htm = HtmCache.getInstance().getNotNull("scripts/services/community/pages/warehouse.htm", player);
		StringBuilder sb = new StringBuilder();
		htm = htm.replace("<?content?>", sb.toString());
		ShowBoard.separateAndSend(htm, player);
	}

	@Override
	public void onWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5)
	{}

	@Override
	public void onLoad()
	{
		if (Config.COMMUNITYBOARD_ENABLED)
		{
			_log.info("CommunityBoard: Warehouse loaded.");
			CommunityBoardManager.getInstance().registerHandler(this);
		}
	}

	@Override
	public void onReload()
	{
		if (Config.COMMUNITYBOARD_ENABLED) CommunityBoardManager.getInstance().removeHandler(this);
	}

	@Override
	public void onShutdown()
	{}

}
