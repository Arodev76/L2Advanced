/*
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package services.community;

import java.util.StringTokenizer;

import l2f.gameserver.Config;
import l2f.gameserver.handler.bbs.CommunityBoardManager;
import l2f.gameserver.handler.bbs.ICommunityBoardHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.network.serverpackets.ShowBoard;
import l2f.gameserver.scripts.ScriptFile;
import Elemental.datatables.ServerRanking;

public class ElementalBBSManager implements ScriptFile, ICommunityBoardHandler
{
	@Override
	public String[] getBypassCommands()
	{
		return new String[] {
				"_bbsloc",
				"_bbsrank",
				"_bbsrank;" };
	}
	
	@Override
	public void onLoad()
	{
		if(Config.COMMUNITYBOARD_ENABLED)
		{
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
	{}
	

	
	@Override
	public void onBypassCommand(Player activeChar, String command)
	{
		// Char in jail cannot use bbs
		if (activeChar.isInJail())
			return;

		command = command.replace("_bbsloc", "_bbsrank");

		// Synerge - Shows the initial htm for the ranking. That would be the first page
		if (command.equals("_bbsrank"))
		{
			final String content = ServerRanking.getInstance().makeServerRankingHtm(activeChar, 0);
			ShowBoard.separateAndSend(content, activeChar);
		}
		// Synerge - Shows the ranking, comes with parameters to see what category to see and if personal or server stats
		else if (command.startsWith("_bbsrank;"))
		{
			try
			{
				StringTokenizer st = new StringTokenizer(command, ";");
				st.nextToken();
				final boolean isServerStats = st.nextToken().equalsIgnoreCase("server");
				final int idTop = Integer.parseInt(st.nextToken());
								
				if (isServerStats)
				{
					final String content = ServerRanking.getInstance().makeServerRankingHtm(activeChar, idTop);
					ShowBoard.separateAndSend(content, activeChar);
				}
				else
				{
					final String content = ServerRanking.getInstance().makeSelfRankingHtm(activeChar, idTop);
					ShowBoard.separateAndSend(content, activeChar);
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	@Override
	public void onWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5)
	{}
}
