package l2f.gameserver.handler.voicecommands.impl;

import l2f.gameserver.GameServer;
import l2f.gameserver.handler.voicecommands.IVoicedCommandHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.scripts.Functions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerInfo extends Functions implements IVoicedCommandHandler
{
	private static final String[] _commandList = {};

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");

	@Override
	public String[] getVoicedCommandList()
	{
		return _commandList;
	}

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (command.equals("rev") || command.equals("ver"))
		{
			activeChar.sendMessage("Revision: Final Revision");
			activeChar.sendMessage("Build date: " + GameServer.getInstance().getVersion().getBuildDate());
		}
		else if (command.equals("date") || command.equals("time"))
		{
			activeChar.sendMessage(DATE_FORMAT.format(new Date(System.currentTimeMillis())));
			return true;
		}

		return false;
	}
}
