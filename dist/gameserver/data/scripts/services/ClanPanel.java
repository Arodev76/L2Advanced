//package services;
//
//import l2f.commons.configuration.Config;
//import l2f.gameserver.dao.ClanDataDAO;
//import l2f.gameserver.data.htm.HtmCache;
//import l2f.gameserver.model.Player;
//import l2f.gameserver.model.base.ClassId;
//import l2f.gameserver.model.base.ClassType2;
//import l2f.gameserver.model.base.Race;
//import l2f.gameserver.model.pledge.Clan;
//import l2f.gameserver.scripts.Functions;
//import l2f.gameserver.tables.ClanTable;
//import l2f.gameserver.utils.Util;
//
//public class ClanPanel extends Functions
//{
//	public void editDescription()
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		Player player = getSelf();
//
//		if (player == null)
//			return;
//
//		Clan clan = player.getClan();
//
//		if (clan == null || clan.getLeader().getPlayer() != player)
//			return;
//
//		String html = HtmCache.getInstance().getNotNull("scripts/services/ClanPanel/edit.htm", player);
//		show(html, player);
//	}
//
//	public void doEdit()
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		doEdit(new String[] { "" });
//	}
//
//	public void doEdit(String[] param)
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		if (param == null)
//			return;
//
//		Player player = getSelf();
//		Clan clan = player.getClan();
//		String description = Util.ArrayToString(param, 0);
//
//		if (checkDescription(player, clan, description))
//		{
//			ClanDataDAO.getInstance().updateDescription(clan.getClanId(), description);
//			clan.setDescription(description);
//			Util.communityNextPage(player, "_bbsclan:clan:id:" + clan.getClanId());
//		}
//	}
//
//	public void addDescription()
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		Player player = getSelf();
//
//		if (player == null)
//			return;
//		Clan clan = player.getClan();
//
//		if (clan == null || clan.getLeader().getPlayer() != player)
//			return;
//
//		String html = HtmCache.getInstance().getNotNull("scripts/services/ClanPanel/add.htm", player);
//		show(html, player);
//	}
//
//	public void doAdd()
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		doAdd(new String[] { "" });
//	}
//
//	public void doAdd(String[] param)
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		if (param == null)
//			return;
//
//		Player player = getSelf();
//		Clan clan = player.getClan();
//		String description = Util.ArrayToString(param, 0);
//
//		if (checkDescription(player, clan, description))
//		{
//			ClanDataDAO.getInstance().insertDescription(clan.getClanId(), description);
//			clan.setDescription(description);
//			Util.communityNextPage(player, "_bbsclan:clan:id:" + clan.getClanId());
//		}
//	}
//
//	private boolean checkDescription(Player player, Clan clan, String description)
//	{
//		if (player == null || clan == null || clan.getLeader().getPlayer() != player)
//			return false;
//
//		int min = 6;
//		if (description.length() < min)
//		{
//			player.sendMessage("Descriptions size must be minimum " + min + " symbols!");
//			return false;
//		}
//
//		return true;
//	}
//
//	public void addRequest()
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		addRequest(new String[] { "-1" });
//	}
//
//	public void addRequest(String[] param)
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		Player player = getSelf();
//
//		if (player == null)
//			return;
//
//		if (player.getClan() != null)
//			return;
//		int clan = Integer.parseInt(param[0]);
//
//		if (clan == -1)
//			return;
//
//		String html = HtmCache.getInstance().getNotNull("scripts/services/ClanPanel/request.htm", player);
//		html = html.replace("<?clan?>", String.valueOf(clan));
//
//		show(html, player);
//	}
//
//	public void sendRequest(String[] param)
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		Player player = getSelf();
//
//		if (player == null)
//			return;
//
//		if (player.getClan() != null)
//			return;
//
//		int clan = Integer.parseInt(param[0]);
//		String note = param.length > 1 ? Util.ArrayToString(param, 1) : "...";
//		Util.communityNextPage(player, "_bbsclan:invite:" + clan + " " + note);
//	}
//
//	public void online()
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		online(new String[] { "-1", "1" });
//	}
//
//	public void online(String[] param)
//	{
//		if (!Config.COMMUNITYBOARD_CLAN_ENABLED)
//			return;
//
//		if (param.length < 1)
//			return;
//
//		Player player = getSelf();
//
//		if (player == null)
//			return;
//
//		int clan_id = Integer.parseInt(param[0]);
//
//		int page = 1;
//
//		if (param.length > 1)
//			page = Integer.parseInt(param[1]);
//
//		Clan clan = ClanTable.getInstance().getClan(clan_id);
//
//		if (clan != null)
//		{
//			String html = HtmCache.getInstance().getNotNull("scripts/services/ClanPanel/clan_online.htm", player);
//			String template = HtmCache.getInstance().getNotNull("scripts/services/ClanPanel/clan_online_template.htm", player);
//			String list = "";
//			String data = "";
//			int current = 1;
//			int start = (page - 1) * 20;
//			int end = Math.min(page * 20, clan.getOnlineMembers(0).size());
//			for (int i = start; i < end; i++)
//			{
//				Player member = clan.getOnlineMembers(0).get(i);
//				list = template;
//				list = list.replace("<?name?>", member.getName());
//				list = list.replace("<?level?>", String.valueOf(member.getLevel()));
//				list = list.replace("<?color?>", (current % 2 == 0 ? "666666" : "999999"));
//				list = list.replace("<?icon?>", getClanClassIcon(member.getClassId()));
//				String unity = member.getClan().getUnitName(member.getPledgeType());
//				list = list.replace("<?unity?>", (unity.length() > 10 ? (unity.substring(0, 8) + "...") : unity));
//				data += list;
//				current++;
//			}
//
//			html = html.replace("<?navigate?>", parseNavigate(clan, page));
//			html = html.replace("<?data?>", data);
//			html = html.replace("<?name?>", clan.getName());
//			html = html.replace("<?count?>", String.valueOf(clan.getOnlineMembers(0).size()));
//			show(html, player);
//		}
//	}
//
//	private String parseNavigate(Clan clan, int page)
//	{
//		StringBuilder pg = new StringBuilder();
//
//		double size = clan.getOnlineMembers(0).size();
//		double inpage = 20;
//
//		if (size > inpage)
//		{
//			double max = Math.ceil(size / inpage);
//
//			pg.append("<center><table width=25 border=0><tr>");
//			int line = 1;
//
//			for (int current = 1; current <= max; current++)
//			{
//				if (page == current)
//					pg.append("<td width=50 align=center><button value=\"[").append(current).append("]\" width=38 height=25 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td>");
//				else
//					pg.append("<td width=50 align=center><button value=\"").append(current).append("\" action=\"bypass -h htmbypass_services.ClanPanel:online " + clan.getClanId() + " ").append(current).append("\" width=28 height=25 back=\"L2UI_ct1.button_df_down\" fore=\"L2UI_ct1.button_df\"></td>");
//
//				if (line == 22)
//				{
//					pg.append("</tr><tr>");
//					line = 0;
//				}
//				line++;
//			}
//
//			pg.append("</tr></table></center>");
//		}
//
//		return pg.toString();
//	}
//
//	private String getClanClassIcon(ClassId classid) //TODO
//	{
//		String icon = "L2UI_CH3.party_styleicon";
//		switch (classid)
//		{
//			case fighter:
//				icon += "1_1";
//				break;
//			case warrior:
//				icon += "1_1";
//				break;
//			case gladiator:
//				icon += "1";
//				break;
//			case warlord:
//				icon += "1";
//				break;
//			case knight:
//				icon += "1";
//				break;
//			case paladin:
//				icon += "1";
//				break;
//			case darkAvenger:
//				icon += "1";
//				break;
//			case rogue:
//				icon += "1";
//				break;
//			case treasureHunter:
//				icon += "1";
//				break;
//			case hawkeye:
//				icon += "2";
//				break;
//			case mage:
//				icon += "1_2";
//				break;
//			case wizard:
//				icon += "1_2";
//				break;
//			case sorceror:
//				icon += "4";
//				break;
//			case necromancer:
//				icon += "4";
//				break;
//			case warlock:
//				icon += "7";
//				break;
//			case cleric:
//				icon += "6";
//				break;
//			case bishop:
//				icon += "6";
//				break;
//			case prophet:
//				icon += "6";
//				break;
//			case elvenFighter:
//				icon += "1";
//				break;
//			case elvenKnight:
//				icon += "3";
//				break;
//			case templeKnight:
//				icon += "3";
//				break;
//			case swordSinger:
//				icon += "3";
//				break;
//			case elvenScout:
//				icon += "3";
//				break;
//			case plainsWalker:
//				icon += "3";
//				break;
//			case silverRanger:
//				icon += "3";
//				break;
//			default:
//				icon += "1_1";
//				break;
//		}
//
//		return icon;
//	}
//}
