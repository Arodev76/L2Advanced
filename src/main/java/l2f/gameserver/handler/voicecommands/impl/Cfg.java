package l2f.gameserver.handler.voicecommands.impl;

import l2f.gameserver.cache.HtmCache;
import l2f.gameserver.handler.voicecommands.IVoicedCommandHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.CharacterControlPanel;
import l2f.gameserver.scripts.Functions;

public class Cfg extends Functions implements IVoicedCommandHandler
{
	private static final String[] COMMANDS = new String[]
	{
		"control", "cfg", "menu"
	};

	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String args)
	{
		String nextPage = CharacterControlPanel.getInstance().useCommand(activeChar, args, "-h user_control ");

		if (nextPage == null || nextPage.isEmpty())
			return true;

		String html = "command/" + nextPage;

		String dialog = HtmCache.getInstance().getNotNull(html, activeChar);

		String additionalText = args.split(" ").length > 1 ? args.split(" ")[1] : "";
		dialog = CharacterControlPanel.getInstance().replacePage(dialog, activeChar, additionalText, "-h user_control ");

		show(dialog, activeChar);

		return true;
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return COMMANDS;
	}
}