package services.community;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.StringTokenizer;

import l2f.commons.dbutils.DbUtils;
import l2f.gameserver.Config;
import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.database.DatabaseFactory;
import l2f.gameserver.handler.bbs.CommunityBoardManager;
import l2f.gameserver.handler.bbs.ICommunityBoardHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.base.ClassId;
import l2f.gameserver.network.serverpackets.ShowBoard;
import l2f.gameserver.scripts.ScriptFile;
import l2f.gameserver.tables.ClanTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankingCommunity implements ScriptFile, ICommunityBoardHandler
{
	private static final Logger _log = LoggerFactory.getLogger(RankingCommunity.class);
	private static final int SECONDS_TO_REFRESH = 300;

	@Override
	public void onLoad()
	{
		if (Config.COMMUNITYBOARD_ENABLED)
		{
			_log.info("CommunityBoard: Ranking Top Players.");
			CommunityBoardManager.getInstance().registerHandler(this);
		}
	}

	@Override
	public void onReload()
	{
		if (Config.COMMUNITYBOARD_ENABLED) CommunityBoardManager.getInstance().removeHandler(this);
	}

	@Override
	public String[] getBypassCommands()
	{
		return new String[] { "_bbsloc" };
	}

	private enum RANKING_TYPE
	{
		PVP("pvpkills"), PK("pkkills"), ONLINE("onlinetime");

		String db;

		private RANKING_TYPE(String db)
		{
			this.db = db;
		}
	}

	@Override
	public void onBypassCommand(Player player, String bypass)
	{
		StringTokenizer st = new StringTokenizer(bypass, "_");
		String cmd = st.nextToken();
		player.setSessionVar("add_fav", null);
		if ("bbsloc".equals(cmd))
		{
			int type = 0;
			if (st.hasMoreTokens())
				try
				{
					type = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{

				}
			String html = HtmCache.getInstance().getNotNull("scripts/services/community/bbs_region_list.htm", player);
			// html = BbsUtil.htmlAll(html, player);
			html = html.replace("%ranking%", getRanking(player, RANKING_TYPE.values()[type]));
			html = html.replace("%type%", "" + type);
			ShowBoard.separateAndSend(html, player);
		}
	}

	private long _lastCheck = 0;
	private final RankingInfos[][] _infos = new RankingInfos[3][15];

	private String getRanking(Player player, RANKING_TYPE type)
	{
		if (_lastCheck < System.currentTimeMillis())
		{
			_lastCheck = System.currentTimeMillis() + SECONDS_TO_REFRESH * 1000;
			updateInfos(player);
		}

		StringBuilder builder = new StringBuilder();
		int index = 0;
		for (RankingInfos info : _infos[type.ordinal()])
		{
			if (info == null)
				continue;
			builder.append("<table width=760 border=0 cellpadding=0 cellspacing=0 bgcolor=").append(getTableColor(index)).append(" height=25>");
			builder.append("	<tr>");
			builder.append("		<td width=35>");
			builder.append("			<font color=").append(getTextColor(index)).append("><center>").append(index + 1).append(".</center></font>");
			builder.append("		</td>");
			builder.append("		<td width=210>");
			builder.append("			<font color=").append(getTextColor(index)).append("><center>").append(info.playerName).append("</center></font>");
			builder.append("		</td>");
			builder.append("		<td width=90>");
			builder.append("			<font color=").append(getTextColor(index)).append("><center>").append(type == RANKING_TYPE.ONLINE ? getOnlineTime(info.score) : info.score).append("</center></font>");
			builder.append("		</td>");
			builder.append("		<td width=90>");
			builder.append("			<center>").append(getOnlineStatus(info.online)).append("</center>");
			builder.append("		</td>");
			builder.append("		<td width=155>");
			builder.append("			<font color=").append(getTextColor(index)).append("><center>").append(info.mainClass).append("</center></font>");
			builder.append("		</td>");
			builder.append("		<td width=190>");
			builder.append("			<font color=").append(getTextColor(index)).append("><center>").append(ClanTable.getInstance().getClanName(info.clanId)).append("</center></font>");
			builder.append("		</td>");
			builder.append("	</tr>");
			builder.append("</table>");
			index++;
		}

		return builder.toString();
	}

	private static String getTextColor(int index)
	{
		if (index == 0)
			return "FF0000 name=hs12";
		else if (index == 1)
			return "FF6633 name=hs12";
		else if (index == 2)
			return "FF9933 name=hs12";
		else
			return "868685 name=__SYSTEMWORLDFONT";
	}

	private void updateInfos(Player player)
	{
		Connection con = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			for (RANKING_TYPE type : RANKING_TYPE.values())
			{
				PreparedStatement statement = null;
				ResultSet rset = null;
				try
				{
					statement = con.prepareStatement("SELECT * FROM characters WHERE accesslevel = 0 ORDER BY " + type.db + " DESC LIMIT 14");
					rset = statement.executeQuery();
					int index = 0;
					while (rset.next())
					{
						RankingInfos info = new RankingInfos();
						int charId = rset.getInt("obj_Id");
						info.playerName = rset.getString("char_name");
						info.score = rset.getInt(type.db);
						info.online = rset.getInt("online") == 1 ? true : false;
						info.clanId = rset.getInt("clanid");

						PreparedStatement statement2 = null;
						ResultSet rset2 = null;

						try
						{
							statement2 = con.prepareStatement("SELECT class_id FROM character_subclasses WHERE char_obj_id=" + charId + " AND isBase=1");
							rset2 = statement2.executeQuery();
							if (rset2.next())
							{
								info.mainClass = getFullClassName(player, ClassId.values()[rset2.getInt("class_id")]);
							}
						}
						catch (Exception e)
						{

							_log.error("Error in updateInfos 3a, RegionCommunity:", e);
						}
						finally
						{
							DbUtils.close(statement2, rset2);
						}

						_infos[type.ordinal()][index] = info;
						index++;
					}
				}
				catch (Exception e)
				{
					_log.error("Error in updateInfos 2, RegionCommunity:", e);
				}
				finally
				{
					DbUtils.close(statement, rset);
				}
			}
		} catch (Exception e)
		{
			_log.error("Error in updateInfos, RegionCommunity:", e);
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}

	private static String getFullClassName(Player player, ClassId classIndex)
	{
		switch (classIndex.getId())
		{
		case 0:
			return "Human Fighter";
		case 1:
			return "Warrior";
		case 2:
			return "Gladiator";
		case 3:
			return "Warlord";
		case 4:
			return "Human Knight";
		case 5:
			return "Paladin";
		case 6:
			return "Dark Avenger";
		case 7:
			return "Rogue";
		case 8:
			return "Treasure Hunter";
		case 9:
			return "Hawkeye";
		case 10:
			return "Human Mystic";
		case 11:
			return "Human Wizard";
		case 12:
			return "Sorcerer";
		case 13:
			return "Necromancer";
		case 14:
			return "Warlock";
		case 15:
			return "Cleric";
		case 16:
			return "Bishop";
		case 17:
			return "Prophet";
		case 18:
			return "Elven Fighter";
		case 19:
			return "Elven Knight";
		case 20:
			return "Temple Knight";
		case 21:
			return "Sword Singer";
		case 22:
			return "Elven Scout";
		case 23:
			return "Plains Walker";
		case 24:
			return "Silver Ranger";
		case 25:
			return "Elven Mystic";
		case 26:
			return "Elven Wizard";
		case 27:
			return "Spellsinger";
		case 28:
			return "Elemental Summoner";
		case 29:
			return "Elven Oracle";
		case 30:
			return "Elven Elder";
		case 31:
			return "Dark Fighter";
		case 32:
			return "Palus Knight";
		case 33:
			return "Shillien Knight";
		case 34:
			return "Bladedancer";
		case 35:
			return "Assassin";
		case 36:
			return "Abyss Walker";
		case 37:
			return "Phantom Ranger";
		case 38:
			return "Dark Mystic";
		case 39:
			return "Dark Wizard";
		case 40:
			return "Spellhowler";
		case 41:
			return "Phantom Summoner";
		case 42:
			return "Shillien Oracle";
		case 43:
			return "Shillien Elder";
		case 44:
			return "Orc Fighter";
		case 45:
			return "Orc Raider";
		case 46:
			return "Destroyer";
		case 47:
			return "Monk";
		case 48:
			return "Tyrant";
		case 49:
			return "Orc Mystic";
		case 50:
			return "Orc Shaman";
		case 51:
			return "Overlord";
		case 52:
			return "Warcryer";
		case 53:
			return "Dwarven Fighter";
		case 54:
			return "Scavenger";
		case 55:
			return "Bounty Hunter";
		case 56:
			return "Artisan";
		case 57:
			return "Warsmith";
		case 88:
			return "Duelist";
		case 89:
			return "Dreadnought";
		case 90:
			return "Phoenix Knight";
		case 91:
			return "Hell Knight";
		case 92:
			return "Sagittarius";
		case 93:
			return "Adventurer";
		case 94:
			return "Archmage";
		case 95:
			return "Soultaker";
		case 96:
			return "Arcana Lord";
		case 97:
			return "Cardinal";
		case 98:
			return "Hierophant";
		case 99:
			return "Eva's Templar";
		case 100:
			return "Sword Muse";
		case 101:
			return "Wind Rider";
		case 102:
			return "Moonlight Sentinel";
		case 103:
			return "Mystic Muse";
		case 104:
			return "Elemental Master";
		case 105:
			return "Eva's Saint";
		case 106:
			return "Shillien Templar";
		case 107:
			return "Spectral Dancer";
		case 108:
			return "Ghost Hunter";
		case 109:
			return "Ghost Sentinel";
		case 110:
			return "Storm Screamer";
		case 111:
			return "Spectral Master";
		case 112:
			return "Shillien Saint";
		case 113:
			return "Titan";
		case 114:
			return "Grand Khavatari";
		case 115:
			return "Dominator";
		case 116:
			return "Doom Cryer";
		case 117:
			return "Fortune Seeker";
		case 118:
			return "Maestro";
		case 123:
			return "Kamael Soldier";
		case 124:
			return "Kamael Soldier";
		case 125:
			return "Trooper";
		case 126:
			return "Warder";
		case 127:
			return "Berserker";
		case 128:
			return "Soul Breaker";
		case 129:
			return "Soul Breaker";
		case 130:
			return "Arbalester";
		case 131:
			return "Doombringer";
		case 132:
			return "Soul Hound";
		case 133:
			return "Soul Hound";
		case 134:
			return "Trickster";
		case 135:
			return "Inspector";
		case 136:
			return "Judicator";
		default:
			return "None";
		}
	}

	private class RankingInfos
	{
		String playerName;
		int score;
		boolean online;
		String mainClass;
		int clanId;
	}

	private String getOnlineTime(int score)
	{
		return (int) Math.ceil((double) score / 86400) + " Days";
	}

	private String getOnlineStatus(boolean online)
	{
		if (online)
			return "<font color=13a91e>Online</font>";
		else
			return "<font color=a91313>Offline</font>";
	}

	private static String getTableColor(int index)
	{
		return (index % 2 == 0 ? "18191e" : "22181a");
	}

	@Override
	public void onShutdown()
	{

	}

	@Override
	public void onWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5)
	{

	}
}
