package l2f.gameserver.handler.admincommands.impl;

import l2f.gameserver.handler.admincommands.IAdminCommandHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.network.FakeGameClient;

public class AdminFakePlayers implements IAdminCommandHandler
{
	private static enum Commands
	{
		admin_create_fake_players;
	}

	@Override
	public boolean useAdminCommand(Enum comm, String[] wordList, String fullString, Player activeChar)
	{
		Commands command = (Commands) comm;

		if (!activeChar.getPlayerAccess().CanReload)
			return false;

		switch (command)
		{
			case admin_create_fake_players:
				int count = Integer.parseInt(wordList[1]);
				for (int i = 0 ; i < count ; i ++)
					new FakeGameClient(null);
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
