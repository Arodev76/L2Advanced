package l2f.gameserver.hwid;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2f.commons.threading.RunnableImpl;
import l2f.gameserver.Config;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.model.Player;
import l2f.gameserver.network.clientpackets.EnterWorld;
import l2f.gameserver.network.serverpackets.ExShowScreenMessage;
import l2f.gameserver.network.serverpackets.ExShowScreenMessage.ScreenMessageAlign;
import l2f.gameserver.utils.MaxElementsList;

public class ClickersDetector
{
	private static final Logger _log = LoggerFactory.getLogger(EnterWorld.class);
	
	public ClickersDetector(Player player)
	{
	}
	
	public static void botPunish(final Player player, int reason)
	{
		if (player.getHwidGamer() == null)
			return;// :/
		
		if (reason >= 0 && reason <= 10)
			return;
		
		/*int warning = player.getHwidGamer().getWarnings();
		
		if (player.isConnected())
		{
			if (warning == 0)
			{
				for (Player singlePlayer : HwidEngine.getInstance().getGamerByHwid(player.getHWID()).getOnlineChars())
				{
					singlePlayer.sendPacket(new ExShowScreenMessage("It's first and last warning! On next usage of 3rd party programs, you will be banned!", 60000, ScreenMessageAlign.TOP_CENTER, true));
				}
				_log.info("Player:"+player.getName()+" HWID:"+player.getHWID()+" AUTO WARNED FOR "+reason);
			}
			else
			{
				int hours = warning >= Config.BOT_BAN_PUNISHMENTS.length ? Config.BOT_BAN_PUNISHMENTS[Config.BOT_BAN_PUNISHMENTS.length-1] : Config.BOT_BAN_PUNISHMENTS[warning];
				for (Player singlePlayer : HwidEngine.getInstance().getGamerByHwid(player.getHWID()).getOnlineChars())
				{
					singlePlayer.sendPacket(new ExShowScreenMessage("You have been banned for "+hours+" hours for 3rd Party Programs!", 60000, ScreenMessageAlign.TOP_CENTER, true));
				}
				HwidEngine.getInstance().botBanHwid(player.getHWID(), hours);
				_log.info("Player:"+player.getName()+" HWID:"+player.getHWID()+" AUTO BANNED FOR "+reason);
			}
			//Kicking
			ThreadPoolManager.getInstance().schedule(new Kick(player.getHWID()), 3000);
		}
		
		player.getHwidGamer().setWarnings(warning+1);*/
	}
	
	private static class Kick extends RunnableImpl
	{
		private String HWID;
		private Kick(String hwid)
		{
			HWID = hwid;
		}
		@Override
		public void runImpl() throws Exception
		{
			for (Player player : HwidEngine.getInstance().getGamerByHwid(HWID).getOnlineChars())
			{
				if (player.getNetConnection() != null)
					player.getNetConnection().closeNow(false);
			}
		}
		
	}
}
