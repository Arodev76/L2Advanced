package services;

import l2f.gameserver.Config;
import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.data.xml.holder.ItemHolder;
import l2f.gameserver.model.Player;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.scripts.Functions;

public class BuyWashPk extends Functions
{
	private static final int[] PKS = {1, 2, 5, 10, 25, 50, 100, 250, 500, 1000};
	public void list()
	{
		Player player = getSelf();
		if (!Config.SERVICES_WASH_PK_ENABLED)
		{
			show(HtmCache.getInstance().getNotNull("npcdefault.htm", player), player);
			return;
		}
		String html = null;
		
		html = HtmCache.getInstance().getNotNull("scripts/services/BuyWashPk.htm", player);
		String add = "";
		
		for (int pk : PKS)
		{
			if (player.getPkKills() <= pk)
				break;
			add += getPkButton(pk);
		}
		
		add += getPkButton(player.getPkKills());
		
		html = html.replaceFirst("%toreplace%", add);
		
		
		show(html, player);
	}
	
	private static String getPkButton(int i)
	{
		return "<a action=\"bypass -h scripts_services.BuyWashPk:get " + i + "\"> for " + i + " PK - " + Config.SERVICES_WASH_PK_PRICE * i + " " + ItemHolder.getInstance().getTemplate(Config.SERVICES_WASH_PK_ITEM).getName() + "</a><br>";
	}

	public void get(String[] param)
	{
		Player player = getSelf();
		if (!Config.SERVICES_WASH_PK_ENABLED)
		{
			show(HtmCache.getInstance().getNotNull("npcdefault.htm", player), player);
			return;
		}
		int i = Integer.parseInt(param[0]);
		if (!player.getInventory().destroyItemByItemId(Config.SERVICES_WASH_PK_ITEM, Config.SERVICES_WASH_PK_PRICE * i, "BuyWashPk$get"))
		{
			player.sendPacket(SystemMsg.INCORRECT_ITEM_COUNT);
		}
		else
		{
			int kills = player.getPkKills();
			player.setPkKills(kills - i);
			player.broadcastCharInfo();
		}
	}
}