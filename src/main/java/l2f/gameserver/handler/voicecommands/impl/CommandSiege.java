package l2f.gameserver.handler.voicecommands.impl;

import l2f.gameserver.data.xml.holder.ResidenceHolder;
import l2f.gameserver.handler.voicecommands.IVoicedCommandHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.residence.Castle;
import l2f.gameserver.network.serverpackets.CastleSiegeInfo;
import l2f.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Command .siege which allows players to Participate to Castle Sieges or check their starting dates.
 */
public class CommandSiege implements IVoicedCommandHandler
{
	/**
	 * Shows Main Siege Page(@link #showMainPage) Also if target contains Castle Id, showing Siege Info of that Castle
	 * @param command - "siege"
	 * @param activeChar - player using command
	 * @param target - Empty(if just clicked .siege) or just number of castle siege
	 * @return always true
	 */
	@Override
	public boolean useVoicedCommand(String command, Player activeChar, String target)
	{
		if (!target.isEmpty())
		{
			int castleId = Integer.parseInt(target);
			Castle castle = ResidenceHolder.getInstance().getResidence(castleId);
			activeChar.sendPacket(new CastleSiegeInfo(castle, activeChar));
		}
		showMainPage(activeChar);
		return true;
	}

	/**
	 * Showing file command/siege.htm to the player
	 * @param activeChar Player that will receive the main Siege Page
	 */
	private static void showMainPage(Player activeChar)
	{
		activeChar.sendPacket(new NpcHtmlMessage(0).setFile("command/siege.htm"));
	}

	@Override
	public String[] getVoicedCommandList()
	{
		return new String[]
		{
			"siege"
		};
	}

}
