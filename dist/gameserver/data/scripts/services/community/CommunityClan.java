package services.community;

import gnu.trove.map.hash.TIntObjectHashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import javolution.util.FastMap;
import l2f.gameserver.Config;
import l2f.gameserver.cache.Msg;
import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.data.xml.holder.ItemHolder;
import l2f.gameserver.data.xml.holder.ResidenceHolder;
import l2f.gameserver.database.DatabaseFactory;
import l2f.gameserver.handler.bbs.CommunityBoardManager;
import l2f.gameserver.handler.bbs.ICommunityBoardHandler;
import l2f.gameserver.listener.actor.player.OnPlayerEnterListener;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.SubClass;
import l2f.gameserver.model.actor.listener.CharListenerList;
import l2f.gameserver.model.base.ClassId;
import l2f.gameserver.model.entity.SevenSigns;
import l2f.gameserver.model.entity.residence.Residence;
import l2f.gameserver.model.items.Inventory;
import l2f.gameserver.model.items.PcInventory;
import l2f.gameserver.model.pledge.Clan;
import l2f.gameserver.model.pledge.Clan.SinglePetition;
import l2f.gameserver.model.pledge.SubUnit;
import l2f.gameserver.model.pledge.UnitMember;
import l2f.gameserver.network.serverpackets.NpcHtmlMessage;
import l2f.gameserver.network.serverpackets.ShowBoard;
import l2f.gameserver.network.serverpackets.SystemMessage;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.scripts.Functions;
import l2f.gameserver.scripts.ScriptFile;
import l2f.gameserver.tables.ClanTable;
import l2f.gameserver.templates.item.ItemTemplate;
import l2f.gameserver.utils.BbsUtil;
import l2f.gameserver.utils.Util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommunityClan extends Functions implements ScriptFile, ICommunityBoardHandler
{
	private static final Logger _log = LoggerFactory.getLogger(CommunityClan.class);
	private static final int CLANS_PER_PAGE = 6;
	private static final int MEMBERS_PER_PAGE = 17;
	private static final String[] ALL_CLASSES =
	{
		"Duelist",
		"Dreadnought",
		"PhoenixKnight",
		"HellKnight",
		"Adventurer",
		"Saggitarius",
		"Archmage",
		"SoulTaker",
		"ArcanaLord",
		"Cardinal",
		"Hierophant",
		"EvaTemplar",
		"SwordMuse",
		"WindRider",
		"MoonlightSentinel",
		"MysticMuse",
		"ElementalMaster",
		"EvaSaint",
		"ShillienTemplar",
		"SpectralDancer",
		"GhostHunter",
		"GhostSentinel",
		"StormScreamer",
		"SpectralMaster",
		"ShillienSaint",
		"Titan",
		"GrandKhauatari",
		"Dominator",
		"Doomcryer",
		"FortuneSeeker",
		"Maestro"
	};
	private static final int[] SLOTS =
	{
		Inventory.PAPERDOLL_RHAND,
		Inventory.PAPERDOLL_LHAND,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_BACK,
		Inventory.PAPERDOLL_UNDER,
		Inventory.PAPERDOLL_BELT,
		Inventory.PAPERDOLL_LFINGER,
		Inventory.PAPERDOLL_RFINGER,
		Inventory.PAPERDOLL_LEAR,
		Inventory.PAPERDOLL_REAR,
		Inventory.PAPERDOLL_NECK,
		Inventory.PAPERDOLL_LBRACELET
	};
	private static final String[] NAMES =
	{
		"Weapon",
		"Shield",
		"Helmet",
		"Chest",
		"Legs",
		"Gloves",
		"Boots",
		"Cloak",
		"Shirt",
		"Belt",
		"Ring",
		" Ring",
		"Earring",
		"Earring",
		"Necklace",
		"Bracelet"
	};

	private static TIntObjectHashMap<String[]> _clanSkillDescriptions = new TIntObjectHashMap<>();
	private final Listener _listener = new Listener();

	@Override
	public void onLoad()
	{
		CharListenerList.addGlobal(_listener);
		if (Config.COMMUNITYBOARD_ENABLED)
		{
			_log.info("CommunityBoard: Clan Community service loaded.");
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
	{
	}

	@Override
	public String[] getBypassCommands()
	{
		return new String[]
		{
			"_bbsclan",
			"_clbbsclan_",
			"_clbbslist_",
			"_clbbsmanage",
			"_bbsclanjoin",
			"_clbbspetitions",
			"_clbbsplayerpetition",
			"_clbbsplayerinventory",
			"_bbsclanmembers",
			"_clbbssinglemember",
			"_clbbsskills",
			"_mailwritepledgeform",
			"_announcepledgewriteform",
			"_announcepledgeswitchshowflag",
			"_announcepledgewrite"
		};
	}

	@Override
	public void onBypassCommand(Player player, String bypass)
	{
		StringTokenizer st = new StringTokenizer(bypass, "_");
		String cmd = st.nextToken();
		player.setSessionVar("add_fav", null);
		String html = null;

		if ("bbsclan".equals(cmd))
		{
			Clan clan = player.getClan();
			if (clan != null)
			{
				onBypassCommand(player, "_clbbsclan_" + player.getClanId());
			}
			else
			{
				onBypassCommand(player, "_clbbslist_0");
			}
			return;
		}
		else if ("clbbslist".equals(cmd))
		{
			int page = Integer.parseInt(st.nextToken());
			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clanlist.htm", player);
			html = html.replace("%rank%", getAllClansRank(player, page));
			html = html.replace("%myClan%", (player.getClan() != null ? "_clbbsclan_" + player.getClanId() : "_clbbslist_0"));
		}
		else if ("clbbsclan".equals(cmd))
		{
			int clanId = Integer.parseInt(st.nextToken());
			if (clanId == 0)
			{
				player.sendPacket(new SystemMessage(SystemMessage.NOT_JOINED_IN_ANY_CLAN));
				onBypassCommand(player, "_clbbslist_0");
				return;
			}

			Clan clan = ClanTable.getInstance().getClan(clanId);
			if (clan == null)
			{
				onBypassCommand(player, "_clbbslist_0");
				return;
			}

			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clan.htm", player);
			html = getMainClanPage(player, clan, html);
		}
		else if ("clbbsmanage".equals(cmd))// _clbbsmanage_btn
		{
			String actionToken = st.nextToken();
			int action = Integer.parseInt(actionToken.substring(0, 1));

			if (action != 0)
			{
				boolean shouldReturn = manageRecrutationWindow(player, action, actionToken);
				if (shouldReturn)
					return;
			}
			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clanrecruit.htm", player);
			html = getClanRecruitmentManagePage(player, html);
		}
		else if ("bbsclanjoin".equals(cmd))
		{
			int clanId = Integer.parseInt(st.nextToken());

			Clan clan = ClanTable.getInstance().getClan(clanId);
			if (clan == null)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_clbbslist_0");
				return;
			}

			// Dont allow the player to join another clan if it has penalty
			if (!player.canJoinClan())
			{
				player.sendPacket(SystemMsg.AFTER_LEAVING_OR_HAVING_BEEN_DISMISSED_FROM_A_CLAN_YOU_MUST_WAIT_AT_LEAST_A_DAY_BEFORE_JOINING_ANOTHER_CLAN);
				return;
			}

			String next = st.nextToken();
			if (Integer.parseInt(next.substring(0, 1)) == 1)
			{
				try
				{
					if (!manageClanJoinWindow(player, clan, next.substring(2)))
					{
						sendInfoMessage(player, "You have already sent petition to this clan!", "_clbbsclan_" + clan.getClanId(), true);
						return;
					}
				}
				catch (Exception e)
				{
					sendErrorMessage(player, "The petition you tried to send is incorrect!", "_bbsclanjoin_" + clan.getClanId() + "_0");
					return;
				}
				sendInfoMessage(player, "Your petition has been submitted!", "_clbbsclan_" + clan.getClanId(), false);
				return;
			}
			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clanjoin.htm", player);
			html = getClanJoinPage(player, clan, html);
		}
		else if ("clbbspetitions".equals(cmd))
		{
			int clanId = Integer.parseInt(st.nextToken());

			Clan clan = ClanTable.getInstance().getClan(clanId);
			if (clan == null)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_clbbslist_0");
				return;
			}

			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clanpetitions.htm", player);
			html = getClanPetitionsPage(player, clan, html);
		}
		else if ("clbbsplayerpetition".equals(cmd))
		{
			int senderId = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens())
			{
				int action = Integer.parseInt(st.nextToken());
				managePlayerPetition(player, senderId, action);
				return;
			}
			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clanplayerpetition.htm", player);

			Player sender = GameObjectsStorage.getPlayer(senderId);
			if (sender != null)
				html = getClanSinglePetitionPage(player, sender, html);
			else
				html = getClanSinglePetitionPage(player, senderId, html);
		}
		else if ("clbbsplayerinventory".equals(cmd))
		{
			int senderId = Integer.parseInt(st.nextToken());
			Player sender = GameObjectsStorage.getPlayer(senderId);

			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clanplayerinventory.htm", player);

			if (sender != null)
				html = getPlayerInventoryPage(sender, html);
			else
				html = getPlayerInventoryPage(senderId, html);
		}
		else if ("bbsclanmembers".equals(cmd))
		{
			int clanId = Integer.parseInt(st.nextToken());
			if (clanId == 0)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_clbbslist_0");
				return;
			}

			int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;

			Clan clan = ClanTable.getInstance().getClan(clanId);
			if (clan == null)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_clbbslist_0");
				return;
			}

			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clanmembers.htm", player);
			html = getClanMembersPage(player, clan, html, page);
		}
		else if ("clbbssinglemember".equals(cmd))
		{
			int playerId = Integer.parseInt(st.nextToken());

			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clansinglemember.htm", player);

			Player member = GameObjectsStorage.getPlayer(playerId);
			if (member != null)
				html = getClanSingleMemberPage(member, html);
			else
				html = getClanSingleMemberPage(playerId, html);
		}
		else if ("clbbsskills".equals(cmd))
		{
			int clanId = Integer.parseInt(st.nextToken());
			if (clanId == 0)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_clbbslist_0");
				return;
			}

			Clan clan = ClanTable.getInstance().getClan(clanId);
			if (clan == null)
			{
				sendErrorMessage(player, "Such clan cannot be found!", "_clbbslist_0");
				return;
			}

			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clanskills.htm", player);
			html = getClanSkills(clan, html);
		}
		else if ("mailwritepledgeform".equals(cmd))
		{
			Clan clan = player.getClan();
			if (clan == null || clan.getLevel() < 2 || !player.isClanLeader())
			{
				onBypassCommand(player, "_clbbsclan_" + player.getClanId());
				return;
			}

			html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_pledge_mail_write.htm", player);

			html = html.replace("%PLEDGE_ID%", String.valueOf(clan.getClanId()));
			html = html.replace("%pledge_id%", String.valueOf(clan.getClanId()));
			html = html.replace("%pledge_name%", clan.getName());

			html = BbsUtil.htmlBuff(html, player);
		}
		else if ("announcepledgewriteform".equals(cmd))
		{
			Clan clan = player.getClan();
			if (clan == null || clan.getLevel() < 2 || !player.isClanLeader())
			{
				onBypassCommand(player, "_clbbsclan_" + player.getClanId());
				return;
			}

			HashMap<Integer, String> tpls = Util.parseTemplate(HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "bbs_clanannounce.htm", player));
			html = tpls.get(0);

			String notice = "";
			int type = 0;
			try (Connection con = DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("SELECT * FROM `bbs_clannotice` WHERE `clan_id` = ? and type != 2"))
			{
				statement.setInt(1, clan.getClanId());

				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						notice = rset.getString("notice");
						type = rset.getInt("type");
					}
				}
			}
			catch (Exception e)
			{
				_log.error("While selecting bbs_clannotice:", e);
			}

			if (type == 0)
			{
				html = html.replace("%off%", "Disabled");
				html = html.replace("%on%", "<a action=\"bypass _announcepledgeswitchshowflag_1\">Enable</a>");
			}
			else
			{
				html = html.replace("%off%", "<a action=\"bypass _announcepledgeswitchshowflag_0\">Disable</a>");
				html = html.replace("%on%", "Enabled");

			}
			html = html.replace("%flag%", String.valueOf(type));

			List<String> args = new ArrayList<String>();
			args.add("0");
			args.add("0");
			args.add("0");
			args.add("0");
			args.add("0");
			args.add("0"); // account data ?
			args.add("");
			args.add("0"); // account data ?
			args.add("");
			args.add("0"); // account data ?
			args.add("");
			args.add("");
			args.add(notice);
			args.add("");
			args.add("");
			args.add("0");
			args.add("0");
			args.add("");

			player.sendPacket(new ShowBoard(html, "1001", player));
			player.sendPacket(new ShowBoard(args));
			return;
		}
		else if ("announcepledgeswitchshowflag".equals(cmd))
		{
			Clan clan = player.getClan();
			if (clan == null || clan.getLevel() < 2 || !player.isClanLeader())
			{
				onBypassCommand(player, "_clbbsclan_" + player.getClanId());
				return;
			}

			int type = Integer.parseInt(st.nextToken());

			try (Connection con = DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("UPDATE `bbs_clannotice` SET type = ? WHERE `clan_id` = ? and type = ?"))
			{
				statement.setInt(1, type);
				statement.setInt(2, clan.getClanId());
				statement.setInt(3, type == 1 ? 0 : 1);
				statement.execute();
			}
			catch (Exception e)
			{
				_log.error("While updating bbs_clannotice:", e);
			}

			clan.setNotice(type == 0 ? "" : null);
			onBypassCommand(player, "_announcepledgewriteform");
			return;
		}
		html = BbsUtil.htmlAll(html, player);
		ShowBoard.separateAndSend(html, player);
	}

	private String getMainClanPage(Player player, Clan clan, String html)
	{
		html = html.replace("%clanName%", clan.getName());
		html = html.replace("%clanId%", String.valueOf(clan.getClanId()));
		html = html.replace("%position%", "#" + clan.getRank());
		html = html.replace("%clanLeader%", clan.getLeaderName());
		html = html.replace("%allyName%", (clan.getAlliance() != null ? clan.getAlliance().getAllyName() : "No Alliance"));
		html = html.replace("%crp%", Util.formatAdena(clan.getReputationScore()));
		html = html.replace("%membersCount%", String.valueOf(clan.getAllMembers().size()));
		html = html.replace("%clanLevel%", String.valueOf(clan.getLevel()));
		html = html.replace("%raidsKilled%", String.valueOf(0)); // NOT DONE
		html = html.replace("%epicsKilled%", String.valueOf(0)); // NOT DONE

		Residence clanHall = ResidenceHolder.getInstance().getResidence(clan.getHasHideout());
		html = html.replace("%clanHall%", (clanHall != null ? getResidenceName(clanHall) : "No"));
		Residence castle = ResidenceHolder.getInstance().getResidence(clan.getCastle());
		html = html.replace("%castle%", (castle != null ? castle.getName() : "No"));
		Residence fortress = ResidenceHolder.getInstance().getResidence(clan.getHasFortress());
		html = html.replace("%fortress%", (fortress != null ? getResidenceName(fortress) : "No"));

		int[] data = getMainClanPageData(clan);

		html = html.replace("%pvps%", String.valueOf(data[0]));
		html = html.replace("%pks%", String.valueOf(data[1]));
		html = html.replace("%nobleCount%", String.valueOf(data[2]));
		html = html.replace("%heroCount%", String.valueOf(data[3]));
		html = html.replace("%clan_avarage_level%", Util.formatAdena(clan.getAverageLevel()));
		html = html.replace("%clan_online%", Util.formatAdena(clan.getOnlineMembers(0).size()));

		// Synerge - Code to replace the white border of the crests with a black one
		//html = html.replace("%clan_crest%", clan.hasCrest() ? "Crest.crest_" + Config.REQUEST_ID + "_" + clan.getCrestId() : "L2UI_CH3.ssq_bar1back");
		//html = html.replace("%ally_crest%", clan.getAlliance() != null && clan.getAlliance().getAllyCrestId() > 0 ? "Crest.crest_" + Config.REQUEST_ID + "_" + clan.getAlliance().getAllyCrestId() : "L2UI_CH3.ssq_bar2back");
		String clanCrest = "";
		if ((clan.getAlliance() != null && clan.getAlliance().getAllyCrestId() > 0) || clan.hasCrest())
		{
			clanCrest += "<td width=46 align=center>";
			clanCrest += "<table fixwidth=24 fixheight=12 cellpadding=0 cellspacing=0>";
			clanCrest += "<tr>";

			if (clan.getAlliance() != null && clan.getAlliance().getAllyCrestId() > 0)
			{
				clanCrest += "<td>";
				clanCrest += "<table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + Config.REQUEST_ID + "_" + clan.getAlliance().getAllyCrestId() + ">";
				clanCrest += "<tr><td fixwidth=8><img height=4 width=8 src=L2UI.SquareBlack>&nbsp;</td></tr>";
				clanCrest += "</table></td>";
			}

			if (clan.hasCrest())
			{
				clanCrest += "<td>";
				clanCrest += "<table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + Config.REQUEST_ID + "_" + clan.getCrestId() + ">";
				clanCrest += "<tr><td fixwidth=16><img height=4 width=16 src=L2UI.SquareBlack>&nbsp;</td></tr>";
				clanCrest += "</table></td>";
			}

			clanCrest += "</tr></table></td>";
		}
		else
			clanCrest += "<td width=46>&nbsp;</td>";
		html = html.replace("%clan_crest%", clanCrest);

		String alliances = "";
		if (clan.getAlliance() != null)
		{
			alliances = "<table border=0 width=765 cellspacing=0 cellpadding=0 height=20 bgcolor=333333><tr>";
			alliances += "<td align=center><font color=LEVEL name=hs9>Clan Ally:</font></td>";
			for (Clan memberClan : clan.getAlliance().getMembers())
			{
				alliances += "<td align=center>";
				alliances += "<button action=\"bypass _clbbslist_0\" value=\"" + memberClan.getName() + "\" width=150 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.button_df\">";
				alliances += "</td>";
			}
			alliances += "</tr></table>";
		}
		html = html.replace("%alliances%", alliances);

		String wars = "<tr>";
		int index = 0;
		for (Clan warClan : clan.getEnemyClans())
		{
			if (index == 5)
			{
				wars += "</tr><tr>";
				index = 0;
			}
			wars += "<td align=center>";
			wars += "<button action=\"bypass _clbbsclan_" + warClan.getClanId() + "\" value=\"" + warClan.getName() + "\" width=130 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.button_df\">";
			wars += "</td>";
			index++;
		}

		wars += "</tr>";

		html = html.replace("%wars%", wars);

		String joinClan = "";
		if (player.getClan() == null)
		{
			joinClan = "<tr><td width=200 align=\"center\">";
			joinClan += "<button action=\"bypass _bbsclanjoin_" + clan.getClanId() + "_0\" value=\"Join Clan\" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">";
			joinClan += "</td></tr>";
		}
		html = html.replace("%joinClan%", joinClan);

		String manageRecruitment = "";
		String managePetitions = "";
		String manageNotice = "";
		if (player.getClan() != null && player.getClan().equals(clan) && player.getClan().getLeaderId() == player.getObjectId())
		{
			manageRecruitment = "<tr><td width=200 align=\"center\">";
			manageRecruitment += "<button action=\"bypass _clbbsmanage_0\" value=\"Manage Recruitment\" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">";
			manageRecruitment += "</td></tr>";

			managePetitions = "<tr><td width=200 align=\"center\">";
			managePetitions += "<button action=\"bypass _clbbspetitions_" + clan.getClanId() + "\" value=\"Manage Petitions\" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">";
			managePetitions += "</td></tr>";

			manageNotice = "<tr><td width=200 align=\"center\">";
			manageNotice += "<button action=\"bypass _announcepledgewriteform\" value=\"Manage Notice\" width=200 height=22 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_ct1.Button_DF\">";
			manageNotice += "</td></tr>";
		}

		html = html.replace("%manageRecruitment%", manageRecruitment);
		html = html.replace("%managePetitions%", managePetitions);
		html = html.replace("%manageNotice%", manageNotice);

		return html;
	}

	private String getClanMembersPage(Player player, Clan clan, String html, int page)
	{
		html = html.replace("%clanName%", clan.getName());
		List<UnitMember> members = clan.getAllMembers();

		StringBuilder builder = new StringBuilder();
		int index = 0;
		int max = Math.min(MEMBERS_PER_PAGE + MEMBERS_PER_PAGE * page, members.size());
		for (int i = MEMBERS_PER_PAGE * page; i < max; i++)
		{
			UnitMember member = members.get(i);
			builder.append("<tr>");
			builder.append("<td width=100><font name=hs12 color=\"f1b45d\">").append(index + 1).append(".</font></td>");
			builder.append("<td width=150><font name=hs12 color=\"FFFFFF\">").append(member.getName()).append("</font></td>");
			//builder.append("<button action=\"bypass _clbbssinglemember_").append(member.getObjectId()).append("\" value=\"").append(member.getName()).append("\" width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\">");
			builder.append("<td align=center width=100>").append(member.getPlayer() != null ? "<font name=hs12 color=6a9b54>True</font>" : "<font name=hs12 color=FF6666>False</font>").append("</td>");
			builder.append("<td align=center width=100>").append(member.isSubLeader() != 0 || member.isClanLeader() ? "<font name=hs12 color=6a9b54>True</font>" : "<font name=hs12 color=FF6666>False</font>").append("</td>");
			builder.append("<td align=center width=75><font name=hs12 color=\"BBFF44\">").append(getUnitName(member.getSubUnit().getType())).append("</font></td>");
			builder.append("<td align=center width=75></td>");
			builder.append("</tr>");
			index++;
		}

		html = html.replace("%members%", builder.toString());

		// Restarting Builder
		builder = new StringBuilder();

		builder.append("<table width=750><tr><td align=center width=350>");

		if (page > 0)
			builder.append("<button action=\"bypass _bbsclanmembers_").append(clan.getClanId()).append("_").append(page - 1).append("\" value=\"Previous\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\">");

		builder.append("</td><td align=center width=350>");

		if (members.size() > MEMBERS_PER_PAGE + MEMBERS_PER_PAGE * page)
			builder.append("<center><button action=\"bypass _bbsclanmembers_").append(clan.getClanId() + "_" + (page + 1)).append("\" value=\"Next\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\"></center>");

		builder.append("</td></tr></table>");

		html = html.replace("%nextPages%", builder.toString());
		return html;
	}

	private String getClanSingleMemberPage(Player member, String html)
	{
		html = html.replace("%playerName%", member.getName());
		html = html.replace("%playerId%", String.valueOf(member.getObjectId()));
		html = html.replace("%clanName%", member.getClan() != null ? member.getClan().getName() : "");
		html = html.replace("%online%", "<font color=6a9b54>True</font>");
		html = html.replace("%title%", member.getTitle());
		html = html.replace("%pvpPoints%", String.valueOf(member.getPvpKills()));
		html = html.replace("%pkPoints%", String.valueOf(member.getPkKills()));
		html = html.replace("%rank%", "Level " + (member.getClan() != null ? member.getClan().getAnyMember(member.getObjectId()).getPowerGrade() : 0));
		html = html.replace("%onlineTime%", getConvertedTime(member.getOnlineTime()));
		html = html.replace("%leader%", member.getSubUnit() != null ? (member.getSubUnit().getLeaderObjectId() == member.getObjectId() ? "True" : "False") : "False");
		html = html.replace("%subpledge%", getUnitName(member.getSubUnit().getType()));
		html = html.replace("%nobless%", member.isNoble() ? "True" : "False");
		html = html.replace("%hero%", member.isHero() ? "True" : "False");
		html = html.replace("%adena%", getConvertedAdena(member.getAdena()));
		html = html.replace("%recs%", String.valueOf(member.getRecomHave()));
		html = html.replace("%sevenSigns%", SevenSigns.getCabalShortName(SevenSigns.getInstance().getPlayerCabal(member)));
		html = html.replace("%fame%", String.valueOf(member.getFame()));

		Collection<SubClass> classes = member.getSubClasses().values();
		int subIndex = 0;
		for (SubClass sub : classes)
		{
			String replacement = "";
			if (sub.isBase())
			{
				replacement = "mainClass";
			}
			else
			{
				if (subIndex == 0)
					replacement = "firstSub";
				else if (subIndex == 1)
					replacement = "secondSub";
				else
					replacement = "thirdSub";
				subIndex++;
			}

			html = html.replace("%" + replacement + "%", ClassId.values()[sub.getClassId()].getName() + "(" + sub.getLevel() + ")");
		}
		html = html.replace("%firstSub%", "");
		html = html.replace("%secondSub%", "");
		html = html.replace("%thirdSub%", "");

		html = html.replace("%clanId%", String.valueOf(member.getClanId()));

		return html;
	}

	private String getClanSingleMemberPage(int playerId, String html)
	{
		OfflineSinglePlayerData data = getSinglePlayerData(playerId);

		html = html.replace("%playerName%", data.char_name);
		html = html.replace("%playerId%", String.valueOf(playerId));
		html = html.replace("%clanName%", data.clan_name);
		html = html.replace("%online%", "<font color=9b5454>False</font>");
		html = html.replace("%title%", data.title == null ? "" : data.title);
		html = html.replace("%pvpPoints%", "" + data.pvpKills);
		html = html.replace("%pkPoints%", "" + data.pkKills);
		html = html.replace("%onlineTime%", getConvertedTime(data.onlineTime));
		html = html.replace("%leader%", Util.boolToString(data.isClanLeader));
		html = html.replace("%subpledge%", getUnitName(data.pledge_type));
		html = html.replace("%nobless%", Util.boolToString(data.isNoble));
		html = html.replace("%hero%", Util.boolToString(data.isHero));
		html = html.replace("%adena%", getConvertedAdena(data.adenaCount));
		html = html.replace("%recs%", "" + data.rec_have);
		html = html.replace("%sevenSigns%", SevenSigns.getCabalShortName(data.sevenSignsSide));
		html = html.replace("%fame%", "" + data.fame);
		html = html.replace("%clanId%", "" + data.clanId);

		String[] otherSubs =
		{
			"%firstSub%",
			"%secondSub%",
			"%thirdSub%"
		};
		int index = 0;
		for (int[] sub : data.subClassIdLvlBase)
		{
			if (sub[2] == 1)
				html = html.replace("%mainClass%", ClassId.values()[sub[0]].getName() + "(" + sub[1] + ")");
			else
				html = html.replace(otherSubs[index], ClassId.values()[sub[0]].getName() + "(" + sub[1] + ")");
		}
		// In case player doesn't have all subclasses
		for (String sub : otherSubs)
			html = html.replace(sub, "<br>");

		return html;
	}

	private String getClanSkills(Clan clan, String html)
	{
		html = html.replace("%clanName%", clan.getName());
		html = html.replace("%clanId%", String.valueOf(clan.getClanId()));

		String skills = "";
		for (Skill clanSkill : clan.getSkills())
		{
			skills += "<tr><td width=30></td>";
			skills += "<td width=55><br>";
			skills += "<img src=\"" + clanSkill.getIcon() + "\" height=30 width=30>";
			skills += "</td><td width=660><br><table width=660><tr><td><font name=\"hs12\" color=\"00BBFF\">";
			skills += clanSkill.getName() + " - <font name=\"hs12\" color=\"FFFFFF\">Level " + clanSkill.getLevel() + " </font>";
			skills += "</font></td></tr><tr><td>";
			String[] descriptions = _clanSkillDescriptions.get(clanSkill.getId());
			if (descriptions == null || descriptions.length < clanSkill.getLevel() - 1)
			{
				_log.warn("cannot find skill id:" + clanSkill.getId() + " in Clan Community Skills descriptions!");
			}
			else
			{
				skills += "<font color=\"FFFF11\">" + descriptions[clanSkill.getLevel() - 1] + "</font>";
			}
			skills += "</td></tr></table></td></tr>";
		}

		html = html.replace("%skills%", skills);

		return html;
	}

	private String getClanSinglePetitionPage(Player leader, Player member, String html)
	{
		html = html.replace("%clanId%", String.valueOf(leader.getClan().getClanId()));
		html = html.replace("%playerId%", String.valueOf(member.getObjectId()));
		html = html.replace("%playerName%", member.getName());
		html = html.replace("%online%", "<font color=6a9b54>True</font>");
		html = html.replace("%onlineTime%", getConvertedTime(member.getOnlineTime()));
		html = html.replace("%pvpPoints%", String.valueOf(member.getPvpKills()));
		html = html.replace("%pkPoints%", String.valueOf(member.getPkKills()));
		html = html.replace("%fame%", String.valueOf(member.getFame()));
		html = html.replace("%adena%", getConvertedAdena(member.getAdena()));

		Collection<SubClass> classes = member.getSubClasses().values();
		int subIndex = 0;
		for (SubClass sub : classes)
		{
			String replacement = "";
			if (sub.isBase())
			{
				replacement = "mainClass";
			}
			else
			{
				if (subIndex == 0)
					replacement = "firstSub";
				else if (subIndex == 1)
					replacement = "secondSub";
				else
					replacement = "thirdSub";
				subIndex++;
			}

			html = html.replace("%" + replacement + "%", ClassId.values()[sub.getClassId()].getName() + "(Level: " + sub.getLevel() + ")");
		}
		html = html.replace("%firstSub%", "");
		html = html.replace("%secondSub%", "");
		html = html.replace("%thirdSub%", "");

		int index = 1;
		for (String question : leader.getClan().getQuestions())
		{
			html = html.replace("%question" + index + "%", question != null && question.length() > 2 ? question + "?" : "");
			index++;
		}

		SinglePetition petition = leader.getClan().getPetition(member.getObjectId());
		index = 1;
		for (String answer : petition.getAnswers())
		{
			html = html.replace("%answer" + index + "%", answer != null && answer.length() > 2 ? answer : "");
			index++;
		}

		html = html.replace("%comment%", petition.getComment());

		return html;
	}

	private String getClanSinglePetitionPage(Player leader, int playerId, String html)
	{
		PetitionPlayerData data = getSinglePetitionPlayerData(playerId);

		html = html.replace("%clanId%", String.valueOf(leader.getClanId()));
		html = html.replace("%playerId%", String.valueOf(playerId));
		html = html.replace("%online%", "<font color=9b5454>False</font>");
		html = html.replace("%playerName%", data.char_name);
		html = html.replace("%onlineTime%", getConvertedTime(data.onlineTime));
		html = html.replace("%pvpPoints%", "" + data.pvpKills);
		html = html.replace("%pkPoints%", "" + data.pkKills);
		html = html.replace("%fame%", "" + data.fame);
		html = html.replace("%adena%", getConvertedAdena(data.adenaCount));
		// Subclasses
		String[] otherSubs =
		{
			"%firstSub%",
			"%secondSub%",
			"%thirdSub%"
		};
		int index = 0;
		for (int[] sub : data.subClassIdLvlBase)
		{
			if (sub[2] == 1)
				html = html.replace("%mainClass%", ClassId.values()[sub[0]].getName() + "(" + sub[1] + ")");
			else
				html = html.replace(otherSubs[index], ClassId.values()[sub[0]].getName() + "(" + sub[1] + ")");
		}
		// In case player doesn't have all subclasses
		for (String sub : otherSubs)
			html = html.replace(sub, "<br>");

		index = 1;
		for (String question : leader.getClan().getQuestions())
		{
			html = html.replace("%question" + index + "%", question != null && question.length() > 2 ? question : "");
			index++;
		}

		SinglePetition petition = leader.getClan().getPetition(playerId);
		index = 1;
		for (String answer : petition.getAnswers())
		{
			html = html.replace("%answer" + index + "%", answer != null && answer.length() > 2 ? answer : "");
			index++;
		}

		html = html.replace("%comment%", petition.getComment());

		return html;
	}

	private String getClanRecruitmentManagePage(Player player, String html)
	{
		Clan clan = player.getClan();
		if (clan == null)
			return html;

		html = html.replace("%clanName%", clan.getName());
		boolean firstChecked = clan.getClassesNeeded().size() == ALL_CLASSES.length;
		html = html.replace("%checked1%", firstChecked ? "_checked" : "");
		html = html.replace("%checked2%", firstChecked ? "" : "_checked");

		String[] notChoosenClasses = getNotChosenClasses(clan);
		html = html.replace("%firstClassGroup%", notChoosenClasses[0]);
		html = html.replace("%secondClassGroup%", notChoosenClasses[1]);

		String list = "<tr>";
		int index = -1;
		for (Integer clas : clan.getClassesNeeded())
		{
			if (index % 4 == 3)
				list += "</tr><tr>";
			index++;

			list += "<td align=center width=100><button value=\"" + ALL_CLASSES[clas - 88] + "\" action=\"bypass  _clbbsmanage_5 " + ALL_CLASSES[clas - 88] + "\" back=\"l2ui_ct1.button.button_df_small_down\" width=105 height=20 fore=\"l2ui_ct1.button.button_df_small\"></td>";
		}
		list += "</tr>";

		html = html.replace("%choosenClasses%", list);

		for (int i = 0; i < 8; i++)
		{
			String clanQuestion = clan.getQuestions()[i];
			html = html.replace("%question" + (i + 1) + "%", clanQuestion != null && clanQuestion.length() > 0 ? clanQuestion : "Question " + (i + 1) + ":");
		}

		html = html.replace("%recrutation%", clan.isRecruting() ? "Stop" : "Start");
		return html;
	}

	private String getClanJoinPage(Player player, Clan clan, String html)
	{
		html = html.replace("%clanId%", String.valueOf(clan.getClanId()));
		html = html.replace("%clanName%", clan.getName());
		for (int i = 0; i < 8; i++)
		{
			String question = clan.getQuestions()[i];
			if (question != null && question.length() > 2)
			{
				html = html.replace("%question" + (i + 1) + "%", question);
				html = html.replace("%answer" + (i + 1) + "%", "<edit var=\"answer" + (i + 1) + "\" width=275 height=15>");
			}
			else
			{
				html = html.replace("%question" + (i + 1) + "%", "");
				html = html.replace("%answer" + (i + 1) + "%", "");
				html = html.replace("$answer" + (i + 1), " ");
			}
		}

		boolean canJoin = false;

		String classes = "<tr>";
		int index = -1;
		for (int classNeeded : clan.getClassesNeeded())
		{
			index++;
			if (index == 6)
			{
				classes += "</tr><tr>";
				index = 0;
			}
			boolean goodClass = player.getSubClasses().keySet().contains(classNeeded);

			if (goodClass)
				canJoin = true;

			classes += "<td width=130><font color=\"" + (goodClass ? "00FF00" : "9b5454") + "\">";
			classes += ClassId.values()[classNeeded].getName();
			classes += "</font></td>";
		}
		classes += "</tr>";

		html = html.replace("%classes%", classes);

		if (canJoin)
			html = html.replace("%joinClanButton%", "<br><center><button action=\"bypass _bbsclanjoin_" + clan.getClanId() + "_1 | $answer1 | $answer2 | $answer3 | $answer4 | $answer5 | $answer6 | $answer7 | $answer8 | $comment |\" value=\"Send\" width=320 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\"></center>");
		else
			html = html.replace("%joinClanButton%", "");

		return html;
	}

	private String getClanPetitionsPage(Player player, Clan clan, String html)
	{
		html = html.replace("%clanName%", clan.getName());

		String petitions = "";
		int index = 1;
		List<SinglePetition> _petitionsToRemove = new ArrayList<>();

		for (SinglePetition petition : clan.getPetitions())
		{
			ClanPetitionData data = getClanPetitionsData(petition.getSenderId());
			if (data == null)
			{
				_petitionsToRemove.add(petition);
				continue;
			}
			petitions += "<tr><td width=30><font name=\"hs12\" color=\"f1b45d\">";
			petitions += index;
			petitions += ".</font></font></td><td align=center width=150>";
			petitions += "<button action=\"bypass _clbbsplayerpetition_" + petition.getSenderId() + "\" value=\"" + data.char_name + "\" width=150 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\">";
			petitions += "</td><td width=100><center>";
			petitions += data.online;
			petitions += "</td><td width=95><font color=\"f1b45d\"><center>";
			petitions += data.pvpKills;
			petitions += "</center></font></td><td width=100><font color=\"f1b45d\"><center>";
			petitions += getConvertedTime(data.onlineTime);
			petitions += "</center></font></td><td width=75><font color=\"f1b45d\"><center>";
			petitions += Util.boolToString(data.isNoble);
			petitions += "</center></font></td></tr>";
			index++;
		}

		for (SinglePetition petitionToRemove : _petitionsToRemove)
			clan.deletePetition(petitionToRemove);

		html = html.replace("%petitions%", petitions);

		return html;
	}

	private String getPlayerInventoryPage(Player player, String html)
	{
		html = html.replace("%playerName%", player.getName());
		html = html.replace("%back%", (player.getClan() != null ? "_clbbssinglemember_" + player.getObjectId() : "_clbbsplayerpetition_" + player.getObjectId()));

		PcInventory pcInv = player.getInventory();
		String inventory = "<tr>";
		for (int i = 0; i < SLOTS.length; i++)
		{
			if (i % 2 == 0)
				inventory += "</tr><tr>";
			inventory += "<td><table><tr><td height=40>";
			inventory += pcInv.getPaperdollItem(SLOTS[i]) != null ? "<img src=" + pcInv.getPaperdollItem(SLOTS[i]).getTemplate().getIcon() + " width=32 height=32>" : "<img src=\"Icon.low_tab\" width=32 height=32>";
			inventory += "</td><td width=150><font color=\"FFFFFF\">";
			inventory += pcInv.getPaperdollItem(SLOTS[i]) != null ? pcInv.getPaperdollItem(SLOTS[i]).getTemplate().getName() + " +" + pcInv.getPaperdollItem(SLOTS[i]).getEnchantLevel() : "No " + NAMES[i];
			inventory += "</font></td></tr></table></td>";
		}
		inventory += "</tr>";

		html = html.replace("%inventory%", inventory);

		return html;
	}

	private String getPlayerInventoryPage(int playerId, String html)
	{
		OfflinePlayerInventoryData data = getPlayerInventoryData(playerId);
		html = html.replace("%playerName%", data.char_name);
		html = html.replace("%back%", (data.clanId != 0 ? "_clbbssinglemember_" + playerId : "_clbbsplayerpetition_" + playerId));

		String inventory = "<tr>";
		for (int i = 0; i < SLOTS.length; i++)
		{
			if (i % 2 == 0)
				inventory += "</tr><tr>";
			int[] item = data.itemIdAndEnchantForSlot.get(i);
			ItemTemplate template = null;
			if (item != null && item[0] > 0)
				template = ItemHolder.getInstance().getTemplate(item[0]);
			inventory += "<td><table><tr><td height=40>";
			inventory += template != null ? "<img src=" + template.getIcon() + " width=32 height=32>" : "<img src=\"Icon.low_tab\" width=32 height=32>";
			inventory += "</td><td width=150><font color=\"bc7420\">";
			inventory += template != null ? template.getName() + " +" + item[1] : "No " + NAMES[i];
			inventory += "</font></td></tr></table></td>";
		}
		inventory += "</tr>";

		html = html.replace("%inventory%", inventory);

		return html;
	}

	private class OfflinePlayerInventoryData
	{
		String char_name;
		int clanId;
		Map<Integer, int[]> itemIdAndEnchantForSlot = new FastMap<>();
	}

	private OfflinePlayerInventoryData getPlayerInventoryData(int playerId)
	{
		OfflinePlayerInventoryData data = new OfflinePlayerInventoryData();

		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("SELECT char_name,clanid FROM characters WHERE obj_Id = '" + playerId + "'"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					data.char_name = rset.getString("char_name");
					data.clanId = rset.getInt("clanid");
				}
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT item_id, loc_data, enchant_level FROM items WHERE owner_id = '" + playerId + "' AND loc='PAPERDOLL'"); ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					int loc = rset.getInt("loc_data");
					for (int i = 0; i < SLOTS.length; i++)
					{
						if (loc == SLOTS[i])
						{
							int[] itemData =
							{
								rset.getInt("item_id"),
								rset.getInt("enchant_level")
							};
							data.itemIdAndEnchantForSlot.put(i, itemData);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			_log.error("Error in getPlayerInventoryData:", e);
		}

		return data;
	}

	private int[] getMainClanPageData(Clan clan)
	{
		int[] data = new int[5];

		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("SELECT SUM(pvpkills), SUM(pkkills) FROM characters WHERE characters.clanid = '" + clan.getClanId() + "'"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					data[0] = rset.getInt("SUM(pvpkills)");
					data[1] = rset.getInt("SUM(pkkills)");
				}
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT count(`characters`.`obj_Id`) FROM `characters` join `olympiad_nobles` ON `characters`.`obj_Id` = `olympiad_nobles`.`char_id` where `characters`.`clanid` = '" + clan.getClanId() + "'");
				ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
					data[2] = rset.getInt("count(`characters`.`obj_Id`)");
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT count(`characters`.`obj_Id`) FROM `characters` join `heroes` on `characters`.`obj_Id` = `heroes`.`char_id` where `characters`.`clanid` = '" + clan.getClanId() + "'"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
					data[3] = rset.getInt("count(`characters`.`obj_Id`)");
			}
		}
		catch (Exception e)
		{
			_log.error("Error in getMainClanPageData:", e);
		}

		return data;
	}

	private class OfflineSinglePlayerData
	{
		String char_name;
		String title = "";
		int pvpKills;
		int pkKills;
		long onlineTime;
		int rec_have;
		int sevenSignsSide = 0;
		int fame;
		int clanId;
		@SuppressWarnings("unused")
		int pledgeRank;
		String clan_name = "";
		int pledge_type = 0;
		boolean isClanLeader = false;
		boolean isNoble = false;
		boolean isHero = false;
		long adenaCount = 0L;
		List<int[]> subClassIdLvlBase = new ArrayList<>();
	}

	private OfflineSinglePlayerData getSinglePlayerData(int playerId)
	{
		OfflineSinglePlayerData data = new OfflineSinglePlayerData();

		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("SELECT char_name,title,pvpkills,pkkills,onlinetime,rec_have,fame,clanid,pledge_rank FROM characters WHERE obj_Id = '" + playerId + "'"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					data.char_name = rset.getString("char_name");
					data.title = rset.getString("title");
					data.pvpKills = rset.getInt("pvpkills");
					data.pkKills = rset.getInt("pkkills");
					data.onlineTime = rset.getLong("onlinetime");
					data.rec_have = rset.getInt("rec_have");
					data.fame = rset.getInt("fame");
					data.clanId = rset.getInt("clanid");
					data.pledgeRank = rset.getInt("pledge_rank");
				}
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT cabal FROM seven_signs WHERE char_obj_id='" + playerId + "'"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
					data.sevenSignsSide = SevenSigns.getCabalNumber(rset.getString("cabal"));
			}

			// If player have clan
			if (data.clanId > 0)
			{
				try (PreparedStatement statement = con.prepareStatement("SELECT type,name,leader_id FROM `clan_subpledges` where `clan_id` = '" + data.clanId + "'"); ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						data.clan_name = rset.getString("name");
						data.pledge_type = rset.getInt("type");
						data.isClanLeader = rset.getInt("leader_id") == playerId;
					}
				}
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT olympiad_points FROM `olympiad_nobles` where `char_id` = '" + playerId + "'"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
					data.isNoble = true;
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT count FROM `heroes` where `char_id` = '" + playerId + "'"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
					data.isHero = true;
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT count FROM `items` where `owner_id` = '" + playerId + "' AND item_id=57"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
					data.adenaCount = rset.getLong("count");
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT class_id,level,isBase FROM `character_subclasses` where `char_obj_id` = '" + playerId + "'"); ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					int[] sub = new int[3];
					sub[0] = rset.getInt("class_id");
					sub[1] = rset.getInt("level");
					sub[2] = rset.getInt("isBase");
					data.subClassIdLvlBase.add(sub);
				}
			}
		}
		catch (Exception e)
		{
			_log.error("Error in getSinglePlayerData:", e);
		}

		return data;
	}

	private class PetitionPlayerData
	{
		String char_name;
		long onlineTime;
		int pvpKills;
		int pkKills;
		int fame;
		long adenaCount = 0L;
		List<int[]> subClassIdLvlBase = new ArrayList<>();
	}

	private PetitionPlayerData getSinglePetitionPlayerData(int playerId)
	{
		PetitionPlayerData data = new PetitionPlayerData();

		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			try (PreparedStatement statement = con.prepareStatement("SELECT char_name,onlinetime,pvpkills,pkkills,fame FROM characters WHERE obj_Id = '" + playerId + "'"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					data.char_name = rset.getString("char_name");
					data.onlineTime = rset.getLong("onlinetime");
					data.pvpKills = rset.getInt("pvpkills");
					data.pkKills = rset.getInt("pkkills");
					data.fame = rset.getInt("fame");
				}
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT count FROM `items` WHERE `owner_id` = '" + playerId + "' AND item_id=57"); ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
					data.adenaCount = rset.getLong("count");
			}

			try (PreparedStatement statement = con.prepareStatement("SELECT class_id,level,isBase FROM `character_subclasses` WHERE `char_obj_id` = '" + playerId + "'"); ResultSet rset = statement.executeQuery())
			{
				while (rset.next())
				{
					int[] sub = new int[3];
					sub[0] = rset.getInt("class_id");
					sub[1] = rset.getInt("level");
					sub[2] = rset.getInt("isBase");
					data.subClassIdLvlBase.add(sub);
				}
			}
		}
		catch (Exception e)
		{
			_log.error("Error in getSinglePetitionPlayerData:", e);
		}

		return data;
	}

	private class ClanPetitionData
	{
		String char_name;
		String online;
		int pvpKills;
		long onlineTime;
		boolean isNoble;
	}

	private ClanPetitionData getClanPetitionsData(int senderId)
	{
		ClanPetitionData data = new ClanPetitionData();
		Player sender = GameObjectsStorage.getPlayer(senderId);
		boolean haveclan = false;
		if (sender != null)
		{
			data.char_name = sender.getName();
			data.online = "<font color=6a9b54>True</font>";
			data.pvpKills = sender.getPvpKills();
			data.onlineTime = sender.getOnlineTime();
			data.isNoble = sender.isNoble();
		}
		else
		{
			try (Connection con = DatabaseFactory.getInstance().getConnection())
			{
				try (PreparedStatement statement = con.prepareStatement("SELECT char_name,pvpkills,onlinetime,clanid FROM characters WHERE obj_Id = '" + senderId + "'"); ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						data.char_name = rset.getString("char_name");
						data.online = "<font color=9b5454>False</font>";
						data.pvpKills = rset.getInt("pvpkills");
						data.onlineTime = rset.getLong("onlinetime");
						if (rset.getInt("clanid") > 0)
							haveclan = true;
					}
				}

				try (PreparedStatement statement = con.prepareStatement("SELECT char_id FROM olympiad_nobles WHERE char_id = '" + senderId + "'"); ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						data.isNoble = true;
					}
				}
			}
			catch (Exception e)
			{
				_log.error("Error in getClanPetitionsData:", e);
			}
		}

		if (haveclan)
			return null;
		else
			return data;
	}

	private String getConvertedTime(long seconds)
	{
		int days = (int) (seconds / 86400);
		seconds -= days * 86400;
		int hours = (int) (seconds / 3600);
		seconds -= hours * 3600;
		int minutes = (int) (seconds / 60);

		boolean includeNext = true;
		String time = "";
		if (days > 0)
		{
			time = days + " Days ";
			if (days > 5)
				includeNext = false;
		}
		if (hours > 0 && includeNext)
		{
			if (time.length() > 0)
				includeNext = false;
			time += hours + " Hours ";
			if (hours > 10)
				includeNext = false;
		}
		if (minutes > 0 && includeNext)
		{
			time += minutes + " Mins";
		}
		return time;
	}

	private String getConvertedAdena(long adena)
	{
		String text = "";
		String convertedAdena = String.valueOf(adena);
		int ks = (convertedAdena.length() - 1) / 3;
		long firstValue = adena / (long) (Math.pow(1000, ks));
		text = firstValue + getKs(ks);
		if ((convertedAdena.length() - 2) / 3 < ks)
		{
			adena -= firstValue * (long) (Math.pow(1000, ks));
			if (adena / (long) (Math.pow(1000, (ks - 1))) > 0)
				text += " " + adena / (int) (Math.pow(1000, (ks - 1))) + getKs(ks - 1);
		}
		return text;
	}

	private String getKs(int howMany)
	{
		String x = "";
		for (int i = 0; i < howMany; i++)
			x += "k";
		return x;
	}

	public String getUnitName(int type)
	{
		String subUnitName = "";
		switch (type)
		{
			case Clan.SUBUNIT_MAIN_CLAN:
				subUnitName = "Main Clan";
				break;
			case Clan.SUBUNIT_ROYAL1:
			case Clan.SUBUNIT_ROYAL2:
				subUnitName = "Royal Guard";
				break;
			default:
				subUnitName = "Order of Knight";
		}
		return subUnitName;
	}

	private void sendErrorMessage(Player player, String message, String backPage)
	{
		sendInfoMessage(player, message, backPage, true);
	}

	private void sendInfoMessage(Player player, String message, String backPage, boolean error)
	{
		String html = "<html><head><title>Clan Recruitment</title></head><body>";
		html += "<table border=0 cellpadding=0 cellspacing=0 width=700><tr><td><br><br>";
		html += "<center><font color = \"" + (error ? "9b5454" : "6a9b54") + "\">";
		html += message;
		html += "</font><br><br><br>";
		html += "<button action=\"bypass " + backPage + "\" value=\"Back\" width=130 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\">";
		html += "</center></td></tr></table></body></html>";

		ShowBoard.separateAndSend(html, player);
	}

	private String getMainStatsTableColor(int index)
	{
		return index % 2 == 0 ? "222320" : "191919";
	}

	private String getAllClansRank(Player player, int page)
	{
		Clan[] clans = ClanTable.getInstance().getClans();
		Arrays.sort(clans, _clansComparator);

		// Making first row
		String text = "<table border=0 width=760>";
		text += "<tr><td align=center height=30>";
		text += "<table border=0 width=760 bgcolor=" + getMainStatsTableColor(0) + " height=30><tr>";
		text += "<td align=left width=40><font color=\"FFFFFF\">Rank</font></td>";
		text += "<td align=center width=220><font color=\"FFFFFF\">Clan Information</font></td>";
		text += "<td align=center width=120><font color=\"FFFFFF\">Leader</font></td>";
		text += "<td align=center width=120><font color=\"FFFFFF\">Alliance</font></td>";
		text += "<td align=center width=50><font color=\"FFFFFF\">Level</font></td>";
		text += "<td align=center width=230><font color=\"FFFFFF\">Recruitment</font></td>";
		text += "</tr></table></td></tr>";

		int max = Math.min(CLANS_PER_PAGE + CLANS_PER_PAGE * page, clans.length);
		int index = 0;
		for (int i = CLANS_PER_PAGE * page; i < max; i++)
		{
			Clan clan = clans[i];
			text += "<tr><td align=center height=30>";
			text += "<table border=0 width=760 bgcolor=\"" + getMainStatsTableColor(index + 1) + "\" height=30>";
			text += "<tr><td width=40>";
			text += "<font name=\"__SYSTEMWORLDFONT\" color=FFFFFF>#" + (i + 1) + "</font></center>";
			text += "</td><td align=center width=210>";

			text += "<table cellspacing=0 cellpadding=0><tr>";

			/*
			text += "<td width=8 align=right valign=center>";
			text += "<img src=" + (clan.getAlliance() != null && clan.getAlliance().getAllyCrestId() > 0 ? "Crest.crest_" + Config.REQUEST_ID + "_" + clan.getAlliance().getAllyCrestId() : "L2UI_CH3.ssq_bar2back") + " width=8 height=16 />";
			text += "</td>";
			text += "<td width=22 align=left valign=center>";
			text += "<img src=" + (clan.hasCrest() ? "Crest.crest_" + Config.REQUEST_ID + "_" + clan.getCrestId() : "L2UI_CH3.ssq_bar1back") + " width=16 height=16 />";
			text += "</td>";
			*/
			// Synerge - Code to replace the white border of the crests with a black one
			if ((clan.getAlliance() != null && clan.getAlliance().getAllyCrestId() > 0) || clan.hasCrest())
			{
				text += "<td width=46 align=center>";
				text += "<table fixwidth=24 fixheight=12 cellpadding=0 cellspacing=0>";
				text += "<tr><td height=12></td></tr>";
				text += "<tr>";

				if (clan.getAlliance() != null && clan.getAlliance().getAllyCrestId() > 0)
				{
					text += "<td>";
					text += "<table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + Config.REQUEST_ID + "_" + clan.getAlliance().getAllyCrestId() + ">";
					text += "<tr><td fixwidth=8><img height=4 width=8 src=L2UI.SquareBlack>&nbsp;</td></tr>";
					text += "</table></td>";
				}

				if (clan.hasCrest())
				{
					text += "<td>";
					text += "<table height=8 cellpadding=0 cellspacing=0 background=Crest.crest_" + Config.REQUEST_ID + "_" + clan.getCrestId() + ">";
					text += "<tr><td fixwidth=16><img height=4 width=16 src=L2UI.SquareBlack>&nbsp;</td></tr>";
					text += "</table></td>";
				}

				text += "</tr></table></td>";
			}
			else
				text += "<td width=46>&nbsp;</td>";

			text += "<td width=130 align=left valign=bottom>";
			text += "<button action=\"bypass _clbbsclan_" + clan.getClanId() + "\" value=\"" + clan.getName() + "\" width=160 height=27 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></center>";
			text += "</td>";
			text += "</tr></table>";

			text += "</td><td width=115>";
			text += "<center><font name=\"__SYSTEMWORLDFONT\" color=\"FFFFFF\">" + clan.getLeaderName() + "</font></center>";
			text += "</td><td width=115>";
			text += "<center><font name=\"__SYSTEMWORLDFONT\" color=\"FFFFFF\">" + (clan.getAlliance() != null ? clan.getAlliance().getAllyName() : "<font name=\"__SYSTEMWORLDFONT\" color=\"A18C70\">No</fomt>") + "</font>";
			text += "</td><td width=60>";
			text += "<center><font name=\"__SYSTEMWORLDFONT\" color=\"FFFFFF\">" + clan.getLevel() + "</font></center>";
			text += "</td><td width=220>";

			if (!clan.isRecruting() || clan.isFull())
				text += "<center><button action=\"bypass _clbbslist_" + page + "\" value=\"Recrutation Closed\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_Apply_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Apply\"></center>";
			else if (player.getClan() != null)
				text += "<center><button action=\"bypass _clbbslist_" + page + "\" value=\"Recrutation Opened\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_Apply_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Apply\"></center>";
			else
				text += "<center><button action=\"bypass _bbsclanjoin_" + clan.getClanId() + "_0\" value=\"Join Clan\" width=200 height=31 back=\"L2UI_CT1.OlympiadWnd_DF_Apply_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Apply\"></center>";

			text += "</td></tr></table>";
			text += "</td></tr>";
			index++;
		}

		text += "</table>";
		text += "<table width=700><tr><td width=350>";
		if (page > 0)
			text += "<center><button action=\"bypass _clbbslist_" + (page - 1) + "\" value=\"Previous\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\"></center>";
		text += "</td><td width=350>";
		if (clans.length > CLANS_PER_PAGE + CLANS_PER_PAGE * page)
			text += "<center><button action=\"bypass _clbbslist_" + (page + 1) + "\" value=\"Next\" width=140 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.button_df\"></center>";
		text += "</td></tr></table>";

		return text;
	}

	private boolean manageRecrutationWindow(Player player, int actionToken, String wholeText)
	{
		Clan clan = player.getClan();

		boolean failedAction = false;
		switch (actionToken)
		{
			case 1:
				clan.getClassesNeeded().clear();
				for (int i = 88; i <= 118; i++)
					clan.addClassNeeded(i);
				break;
			case 2:
				clan.getClassesNeeded().clear();
				break;
			case 3:
				if (wholeText.length() > 2)
				{
					String clazz = wholeText.substring(2);
					for (int i = 0; i < ALL_CLASSES.length; i++)
					{
						if (ALL_CLASSES[i].equals(clazz))
						{
							clan.addClassNeeded(88 + i);
							break;
						}
					}
				}
				break;
			case 5:
				String clazz = wholeText.substring(2);
				for (int i = 0; i < ALL_CLASSES.length; i++)
				{
					if (ALL_CLASSES[i].equals(clazz))
					{
						clan.deleteClassNeeded(88 + i);
						break;
					}
				}
				break;
			case 6:
				String[] questions = clan.getQuestions();
				StringTokenizer st = new StringTokenizer(wholeText.substring(2), "|");
				for (int i = 0; i < 8; i++)
				{
					String question = st.nextToken();
					if (question.length() > 3)
						questions[i] = question;
					clan.setQuestions(questions);
				}
				break;
			case 7:
				clan.setRecrutating(!clan.isRecruting());
				break;
			default:
				failedAction = true;
		}

		if (!failedAction)
			clan.updateRecrutationData();

		return false;
	}

	private boolean manageClanJoinWindow(Player player, Clan clan, String text)
	{
		StringTokenizer st = new StringTokenizer(text, "|");
		String[] answers = new String[8];
		for (int i = 0; i < 8; i++)
		{
			String answer = st.nextToken();
			answers[i] = answer;
		}
		String comment = st.nextToken();
		return clan.addPetition(player.getObjectId(), answers, comment);
	}

	private void managePlayerPetition(Player player, int senderId, int action)
	{
		Player sender = GameObjectsStorage.getPlayer(senderId);
		Clan clan = player.getClan();
		switch (action)
		{
			case 1:
				int type = -1;
				for (SubUnit unit : clan.getAllSubUnits())
					if (clan.getUnitMembersSize(unit.getType()) < clan.getSubPledgeLimit(unit.getType()))
						type = unit.getType();

				if (type == -1)
				{
					sendErrorMessage(player, "Clan is full!", "_clbbsplayerpetition_" + senderId);
					return;
				}
				if (sender != null)
				{
					player.getClan().addMember(sender, type);
				}
				else
				{
					try (Connection con = DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("UPDATE characters SET clanid=" + clan.getClanId() + ", pledge_type=" + type + " WHERE obj_Id=" + senderId + " AND clanid=0"))
					{
						statement.execute();
					}
					catch (Exception e)
					{
						_log.error("Error in managePlayerPetition:", e);
					}
					player.getClan().getSubUnit(type).addUnitMember(getSubUnitMember(clan, type, senderId));
				}
				sendInfoMessage(player, "Member has been added!", "_clbbspetitions_" + clan.getClanId(), false);
			case 2:
				clan.deletePetition(senderId);
				if (action == 2)
					sendInfoMessage(player, "Petition has been deleted!", "_clbbspetitions_" + clan.getClanId(), false);
				break;
		}
	}

	private UnitMember getSubUnitMember(Clan clan, int type, int memberId)
	{
		UnitMember member = null;
		try (Connection con = DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement(//
		"SELECT `c`.`char_name` AS `char_name`," + //
		"`s`.`level` AS `level`," + //
		"`s`.`class_id` AS `class_id`," + //
		"`c`.`title` AS `title`," + //
		"`c`.`pledge_rank` AS `pledge_rank`," + //
		"`c`.`sex` AS `sex` " + //
		"FROM `characters` `c` " + //
		"LEFT JOIN `character_subclasses` `s` ON (`s`.`char_obj_id` = `c`.`obj_Id` AND `s`.`isBase` = '1') " + //
		"WHERE `c`.`obj_Id`=?");)
		{
			statement.setInt(1, memberId);

			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					member = new UnitMember(clan, rset.getString("char_name"), rset.getString("title"), rset.getInt("level"), rset.getInt("class_id"), memberId, type, rset.getInt("pledge_rank"), 0, rset.getInt("sex"), Clan.SUBUNIT_NONE);
				}
			}
		}
		catch (Exception e)
		{
			_log.error("Error in managePlayerPetition:", e);
		}

		return member;
	}

	private final ClanComparator _clansComparator = new ClanComparator();

	private class ClanComparator implements Comparator<Clan>
	{
		@Override
		public int compare(Clan o1, Clan o2)
		{
			if (o1.getLevel() > o2.getLevel())
				return -1;
			if (o2.getLevel() > o1.getLevel())
				return 1;
			if (o1.getReputationScore() > o2.getReputationScore())
				return -1;
			if (o2.getReputationScore() > o1.getReputationScore())
				return 1;
			return 0;
		}
	}

	private String[] getNotChosenClasses(Clan clan)
	{
		String[] splited =
		{
			"",
			""
		};

		ArrayList<Integer> classes = clan.getClassesNeeded();

		for (int i = 0; i < ALL_CLASSES.length; i++)
		{
			if (!classes.contains(i + 88))
			{
				int x = 1;
				if (i % 2 == 0)
					x = 0;
				if (!splited[x].equals(""))
					splited[x] += ";";
				splited[x] += ALL_CLASSES[i];
			}
		}
		return splited;
	}

	@Override
	public void onWriteCommand(Player player, String bypass, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		StringTokenizer st = new StringTokenizer(bypass, "_");
		String cmd = st.nextToken();
		if ("announcepledgewrite".equals(cmd))
		{
			Clan clan = player.getClan();
			if (clan == null || clan.getLevel() < 2 || !player.isClanLeader())
			{
				onBypassCommand(player, "_clbbsclan_" + player.getClanId());
				return;
			}

			if (arg3 == null || arg3.isEmpty())
			{
				onBypassCommand(player, "_announcepledgewriteform");
				return;
			}

			// arg3 = removeIllegalText(arg3);
			arg3 = arg3.replace("<", "");
			arg3 = arg3.replace(">", "");
			arg3 = arg3.replace("&", "");
			arg3 = arg3.replace("$", "");

			if (arg3.isEmpty())
			{
				onBypassCommand(player, "_announcepledgewriteform");
				return;
			}

			if (arg3.length() > 3000)
				arg3 = arg3.substring(0, 3000);

			int type = Integer.parseInt(st.nextToken());

			try (Connection con = DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("REPLACE INTO `bbs_clannotice`(clan_id, type, notice) VALUES(?, ?, ?)"))
			{
				statement.setInt(1, clan.getClanId());
				statement.setInt(2, type);
				statement.setString(3, arg3);
				statement.execute();
			}
			catch (Exception e)
			{
				e.printStackTrace();
				onBypassCommand(player, "_announcepledgewriteform");
				return;
			}

			if (type == 1)
				clan.setNotice(arg3.replace("\n", "<br1>"));
			else
				clan.setNotice("");

			player.sendPacket(Msg.NOTICE_HAS_BEEN_SAVED);
			onBypassCommand(player, "_announcepledgewriteform");
		}
	}

	private class Listener implements OnPlayerEnterListener
	{
		@Override
		public void onPlayerEnter(Player player)
		{
			Clan clan = player.getClan();
			if (clan == null || clan.getLevel() < 2)
				return;

			if (clan.getNotice() == null)
			{
				String notice = "";
				int type = 0;
				try (Connection con = DatabaseFactory.getInstance().getConnection(); PreparedStatement statement = con.prepareStatement("SELECT * FROM `bbs_clannotice` WHERE `clan_id` = ? and type != 2"))
				{
					statement.setInt(1, clan.getClanId());

					try (ResultSet rset = statement.executeQuery())
					{
						if (rset.next())
						{
							notice = rset.getString("notice");
							type = rset.getInt("type");
						}
					}
				}
				catch (Exception e)
				{
					_log.error("While updating bbs_clannotice:", e);
				}

				clan.setNotice(type == 1 ? notice.replace("\n", "<br1>\n") : "");
			}

			if (!clan.getNotice().isEmpty())
			{
				String html = HtmCache.getInstance().getNotNull(Config.BBS_HOME_DIR + "clan_popup.htm", player);
				html = html.replace("%pledge_name%", clan.getName());
				html = html.replace("%content%", clan.getNotice());

				player.sendPacket(new NpcHtmlMessage(0).setHtml(html));
			}
		}
	}

	private static String getResidenceName(Residence r)
	{
		int id = r.getId();
		StringTokenizer st = new StringTokenizer(r.getName());
		if (id >= 101 && id <= 121 || id >= 22 && id <= 30 || id == 30 || id == 34 || id == 35 || id >= 47 && id <= 58 || id == 61 || id == 62)
			return st.nextToken();

		switch (id)
		{
			case 31: // second
			case 32:
			case 33:
			case 36:
			case 37:
			case 38:
			case 41:
			case 42:
			case 43:
			case 44:
			case 45:
				st.nextToken();
				return st.nextToken();
			case 21:// third
				st.nextToken();
				st.nextToken();
				return st.nextToken();

			case 59:// first & second
			case 60:
			case 63:
				return st.nextToken() + " " + st.nextToken();
			default:
				return r.getName();
		}
	}

	{
		_clanSkillDescriptions.put(370, new String[]
		{
			"Increases clan members' Max HP by 3%. It only affects those who are of an Heir rank or higher.",
			"Increases clan members' Max HP by 5%. It only affects those who are of an Heir rank or higher.",
			"Increases clan members' Max HP by 6%. It only affects those who are of an Heir rank or higher."
		});
		_clanSkillDescriptions.put(371, new String[]
		{
			"Increases clan members' Max CP by 6%. It only affects those who are of a Baron rank or higher.",
			"Increases clan members' Max CP by 10%. It only affects those who are of a Baron rank or higher.",
			"Increases clan members' Max CP by 12%. It only affects those who are of a Baron rank or higher."
		});
		_clanSkillDescriptions.put(372, new String[]
		{
			"Increases clan members' Max MP by 3%. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Max MP by 5%. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Max MP by 6%. It only affects those who are of a Viscount rank or higher."
		});
		_clanSkillDescriptions.put(373, new String[]
		{
			"Increases clan members' HP regeneration by 3%. It only affects those who are of an Heir rank or higher.",
			"Increases clan members' HP regeneration by 5%. It only affects those who are of an Heir rank or higher.",
			"Increases clan members' HP regeneration by 6%. It only affects those who are of an Heir rank or higher."
		});
		_clanSkillDescriptions.put(374, new String[]
		{
			"Increases clan members' CP regeneration by 6%. It only affects those who are of an Elder rank or higher.",
			"Increases clan members' CP regeneration by 10%. It only affects those who are of an Elder rank or higher.",
			"Increases clan members' CP regeneration by 12%. It only affects those who are of an Elder rank or higher."
		});
		_clanSkillDescriptions.put(375, new String[]
		{
			"Increases clan members' MP regeneration by 3%. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' MP regeneration by 5%. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' MP regeneration by 6%. It only affects those who are of a Viscount rank or higher."
		});
		_clanSkillDescriptions.put(376, new String[]
		{
			"Increases clan members' P. Atk. by 3%. It only affects those who are of a Knight rank or higher.",
			"Increases clan members' P. Atk. by 5%. It only affects those who are of a Knight rank or higher.",
			"Increases clan members' P. Atk. by 6%. It only affects those who are of a Knight rank or higher."
		});
		_clanSkillDescriptions.put(377, new String[]
		{
			"Increases clan members' P. Def. by 3%. It only affects those who are of a Knight rank or higher.",
			"Increases clan members' P. Def. by 5%. It only affects those who are of a Knight rank or higher.",
			"Increases clan members' P. Def. by 6%. It only affects those who are of a Knight rank or higher."
		});
		_clanSkillDescriptions.put(378, new String[]
		{
			"Increases clan members' M. Atk by 6%. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' M. Atk by 10%. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' M. Atk by 12%. It only affects those who are of a Viscount rank or higher."
		});
		_clanSkillDescriptions.put(379, new String[]
		{
			"Increases clan members' M. Def by 6%. It only affects those who are of an Heir rank or higher.",
			"Increases clan members' M. Def by 10%. It only affects those who are of an Heir rank or higher.",
			"Increases clan members' M. Def by 12%. It only affects those who are of an Heir rank or higher."
		});
		_clanSkillDescriptions.put(380, new String[]
		{
			"Increases clan members' Accuracy by 1. It only affects those who are of a Baron rank or higher.",
			"Increases clan members' Accuracy by 2. It only affects those who are of a Baron rank or higher.",
			"Increases clan members' Accuracy by 3. It only affects those who are of a Baron rank or higher."
		});
		_clanSkillDescriptions.put(381, new String[]
		{
			"Increases clan members' Evasion by 1. It only affects those who are of a Baron rank or higher.",
			"Increases clan members' Evasion by 2. It only affects those who are of a Baron rank or higher.",
			"Increases clan members' Evasion by 3. It only affects those who are of a Baron rank or higher."
		});
		_clanSkillDescriptions.put(382, new String[]
		{
			"Increases clan members' Shield Defense by 12%. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Shield Defense by 20%. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Shield Defense by 24%. It only affects those who are of a Viscount rank or higher."
		});
		_clanSkillDescriptions.put(383, new String[]
		{
			"Increases clan members' Shield Defense by 24%. It only affects those who are of a Baron rank or higher.",
			"Increases clan members' Shield Defense by 40%. It only affects those who are of a Baron rank or higher.",
			"Increases clan members' Shield Defense by 48%. It only affects those who are of a Baron rank or higher."
		});
		_clanSkillDescriptions.put(384, new String[]
		{
			"Increases clan members' Resistance to Water/Wind attacks by 3. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Water/Wind attacks by 5. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Water/Wind attacks by 6. It only affects those who are of a Viscount rank or higher."
		});
		_clanSkillDescriptions.put(385, new String[]
		{
			"ncreases clan members' Resistance to Fire/Earth attacks by 3. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Fire/Earth attacks by 5. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Fire/Earth attacks by 6. It only affects those who are of a Viscount rank or higher."
		});
		_clanSkillDescriptions.put(386, new String[]
		{
			"Increases clan members' Resistance to Stun attacks by 12. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Stun attacks by 20. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Stun attacks by 24. It only affects those who are of a Viscount rank or higher."
		});
		_clanSkillDescriptions.put(387, new String[]
		{
			"Increases clan members' Resistance to Hold attacks by 12. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Hold attacks by 20. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Hold attacks by 24. It only affects those who are of a Viscount rank or higher."
		});
		_clanSkillDescriptions.put(388, new String[]
		{
			"Increases clan members' Resistance to Sleep attacks by 12. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Sleep attacks by 20. It only affects those who are of a Viscount rank or higher.",
			"Increases clan members' Resistance to Sleep attacks by 24. It only affects those who are of a Viscount rank or higher."
		});
		_clanSkillDescriptions.put(389, new String[]
		{
			"Increases clan members' Speed by 3. It only affects those who are of a Count rank or higher.",
			"Increases clan members' Speed by 5. It only affects those who are of a Count rank or higher.",
			"Increases clan members' Speed by 6. It only affects those who are of a Count rank or higher."
		});
		_clanSkillDescriptions.put(390, new String[]
		{
			"Decreases clan members' experience loss and the chance of other death penalties when killed by a monster or player. It only affects those who are of an Heir rank or higher.",
			"Decreases clan members' experience loss and the chance of other death penalties when killed by a monster or player. It only affects those who are of an Heir rank or higher.",
			"Decreases clan members' experience loss and the chance of other death penalties when killed by a monster or player. It only affects those who are of an Heir rank or higher."
		});
		_clanSkillDescriptions.put(391, new String[]
		{
			"Grants the privilege of Command Channel formation. It only effects Sage / Elder class and above."
		});
		_clanSkillDescriptions.put(590, new String[]
		{
			"The Max HP of clan members in residence increases by 222."
		});
		_clanSkillDescriptions.put(591, new String[]
		{
			"The Max CP of clan members in residence increases by 444."
		});
		_clanSkillDescriptions.put(592, new String[]
		{
			"The Max MP of clan members in residence increases by 168."
		});
		_clanSkillDescriptions.put(593, new String[]
		{
			"The HP Recovery Bonus of clan members in residence increases by 1.09."
		});
		_clanSkillDescriptions.put(594, new String[]
		{
			"CP recovery bonus of clan members in residence increases by 1.09."
		});
		_clanSkillDescriptions.put(595, new String[]
		{
			"The MP Recovery Bonus of clan members in residence increases by 0.47."
		});
		_clanSkillDescriptions.put(596, new String[]
		{
			"P. Atk. of clan members in residence increases by 34.6."
		});
		_clanSkillDescriptions.put(597, new String[]
		{
			"P. Def. of clan members in residence increases by 54.7."
		});
		_clanSkillDescriptions.put(598, new String[]
		{
			"M. Atk. of clan members in residence increases by 40.4."
		});
		_clanSkillDescriptions.put(599, new String[]
		{
			"The M. Def. of clan members in residence increases by 44."
		});
		_clanSkillDescriptions.put(600, new String[]
		{
			"Accuracy of clan members in residence increases by 4."
		});
		_clanSkillDescriptions.put(601, new String[]
		{
			"Evasion of clan members in residence increases by 4."
		});
		_clanSkillDescriptions.put(602, new String[]
		{
			"Shield Defense of clan members in residence increases by 54.7."
		});
		_clanSkillDescriptions.put(603, new String[]
		{
			"Shield Defense. of clan members in residence increases by 225."
		});
		_clanSkillDescriptions.put(604, new String[]
		{
			"Resistance to Water and Wind attacks of clan members in residence increases by 10."
		});
		_clanSkillDescriptions.put(605, new String[]
		{
			"Resistance to Fire and Earth attacks of clan members in residence increases by 10."
		});
		_clanSkillDescriptions.put(606, new String[]
		{
			"Resistance to Stun attacks of clan members in residence increases by 10."
		});
		_clanSkillDescriptions.put(607, new String[]
		{
			"Resistance to Hold attacks of clan members in residence increases by 10."
		});
		_clanSkillDescriptions.put(608, new String[]
		{
			"Resistance to Sleep attacks of clan members in residence increases by 10."
		});
		_clanSkillDescriptions.put(609, new String[]
		{
			"The Speed of clan members in residence increases by 6."
		});
		_clanSkillDescriptions.put(610, new String[]
		{
			"When a clan member within the residence is killed by PK/ordinary monster, the Exp. points consumption rate and the probability of incurring a death after-effect are decreased."
		});
		_clanSkillDescriptions.put(611, new String[]
		{
			"The corresponding troops' P. Atk. increase by 17.3.",
			"The corresponding troops' P. Atk. increase by 17.3 and Critical Rate increase by 15.",
			"The corresponding troops' P. Atk. increase by 17.3, Critical Rate increase by 15, and Critical Damage increase by 100."
		});
		_clanSkillDescriptions.put(612, new String[]
		{
			"The corresponding troops' P. Def. increase by 27.3.",
			"The corresponding troops' P. Def. increase by 27.3 and M. Def. increase by 17.6.",
			"The corresponding troops' P. Def. increase by 27.3, M. Def. increase by 17.6, and Shield Defense. increase by 6%."
		});
		_clanSkillDescriptions.put(613, new String[]
		{
			"The corresponding troops' Accuracy increase by 2.",
			"The corresponding troops' Accuracy increase by 2 and Evasion increase by 2.",
			"The corresponding troops' Accuracy increase by 2, Evasion increase by 2, and Speed increase by 3."
		});
		_clanSkillDescriptions.put(614, new String[]
		{
			"The corresponding troops' M. Def. increase by 17.",
			"The corresponding troops' M. Def. increase by 31.1.",
			"The corresponding troops' M. Def. increase by 44."
		});
		_clanSkillDescriptions.put(615, new String[]
		{
			"The corresponding troops' heal power increase by 20.",
			"The corresponding troops' heal power increase by 20 and Max MP increase by 30%.",
			"The corresponding troops' heal power increase by 20, Max MP increase by 30%, and MP consumption decreases by 5%."
		});
		_clanSkillDescriptions.put(616, new String[]
		{
			"The corresponding troops' M. Atk. increase by 7.17.",
			"The corresponding troops' M. Atk. increase by 19.32.",
			"The corresponding troops' M. Atk. increase by 19.32 and magic Critical Damage rate increases by 1%."
		});
		_clanSkillDescriptions.put(848, new String[]
		{
			"STR+1 / INT+1"
		});
		_clanSkillDescriptions.put(849, new String[]
		{
			"DEX+1 / WIT+1"
		});
		_clanSkillDescriptions.put(850, new String[]
		{
			"STR+1 / MEN+1"
		});
		_clanSkillDescriptions.put(851, new String[]
		{
			"CON+1 / MEN+1"
		});
		_clanSkillDescriptions.put(852, new String[]
		{
			"DEX+1 / MEN+1"
		});
		_clanSkillDescriptions.put(853, new String[]
		{
			"CON+1 / INT+1"
		});
		_clanSkillDescriptions.put(854, new String[]
		{
			"DEX+1 / INT+1"
		});
		_clanSkillDescriptions.put(855, new String[]
		{
			"STR+1 / WIT+1"
		});
		_clanSkillDescriptions.put(856, new String[]
		{
			"CON+1 / WIT+1"
		});
	}
}
