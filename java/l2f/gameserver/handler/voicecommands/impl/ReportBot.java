package l2f.gameserver.handler.voicecommands.impl;

import l2f.gameserver.Config;
import l2f.gameserver.handler.voicecommands.IVoicedCommandHandler;
import l2f.gameserver.model.Player;

public class ReportBot implements IVoicedCommandHandler
{
	private static final String[] COMMANDS = {};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String args)
	{
		if (Config.ENABLE_AUTO_HUNTING_REPORT)
		{
			if ((activeChar.getTarget() instanceof Player))
			{
				activeChar.sendMessage("Player has been reported!");
			}
			else
			{
				activeChar.sendMessage("Your target is not a player!");
			}
		}
		else
		{
			activeChar.sendMessage("Action disabled.");
		}
		return false;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return COMMANDS;
	}
}