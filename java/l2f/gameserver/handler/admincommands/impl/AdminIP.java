package l2f.gameserver.handler.admincommands.impl;

import l2f.gameserver.handler.admincommands.IAdminCommandHandler;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.World;

public class AdminIP implements IAdminCommandHandler
{
	private enum Commands
	{
		admin_charip,
		admin_ip
	}

	@Override
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar)
	{
		Commands command = (Commands) comm;

		if (!activeChar.getPlayerAccess().CanBan)
			return false;

		switch (command)
		{
			case admin_charip:
				if (wordList.length != 2)
				{
					activeChar.sendMessage("Command syntax: //charip <char_name>");
					activeChar.sendMessage(" Gets character's IP.");
					break;
				}

				Player pl = World.getPlayer(wordList[1]);

				if (pl == null)
				{
					activeChar.sendMessage("Character " + wordList[1] + " not found.");
					break;
				}

				String ip_adr = pl.getIP();
				if (ip_adr.equalsIgnoreCase("<not connected>"))
				{
					activeChar.sendMessage("Character " + wordList[1] + " not found.");
					break;
				}

				activeChar.sendMessage("Character's IP: " + ip_adr);
				break;
			case admin_ip:
				Player target;
				if (activeChar.getTarget() != null && activeChar.getTarget().isPlayer())
					target = activeChar.getTarget().getPlayer();
				else
					target = activeChar;
				
				if (target.getIP().equalsIgnoreCase("<not connected>"))
				{
					activeChar.sendMessage("Target not found.");
					return false;
				}
				
				activeChar.sendMessage("IP:"+target.getIP());
				
				for (Player player : GameObjectsStorage.getAllPlayersForIterate())
					if (player.getIP().equals(target.getIP()))
						activeChar.sendMessage("Player with same IP:"+player.getName());
				break;
		}
		return true;
	}

	@Override
	public Enum[] getAdminCommandEnum()
	{
		return Commands.values();
	}
}