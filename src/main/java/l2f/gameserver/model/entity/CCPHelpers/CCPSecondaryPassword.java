package l2f.gameserver.model.entity.CCPHelpers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2f.gameserver.cache.HtmCache;
import l2f.gameserver.database.LoginDatabaseFactory;
import l2f.gameserver.instancemanager.QuestManager;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.quest.Quest;
import l2f.gameserver.network.serverpackets.MagicSkillUse;
import l2f.gameserver.network.serverpackets.NpcHtmlMessage;
import l2f.gameserver.network.serverpackets.SocialAction;
import l2f.gameserver.network.serverpackets.TutorialShowHtml;
import l2f.gameserver.skills.AbnormalEffect;

public class CCPSecondaryPassword
{
	private static final Logger _log = LoggerFactory.getLogger(CCPSecondaryPassword.class);

	public static void startSecondaryPasswordSetup(Player player, String text)
	{
		StringTokenizer st = new StringTokenizer(text, "|");
		String[] args = new String[st.countTokens()];
		for (int i = 0;i<args.length;i++)
			args[i] = st.nextToken().trim();

		String pageIndex = args[0].substring(args[0].length()-1);

		if (pageIndex.equals("F"))
		{
			if (hasPassword(player))
				sendHtml(player, HtmCache.getInstance().getNotNull("command/cfgSPSecondaryChange.htm", player));
			else
				sendHtml(player, HtmCache.getInstance().getNotNull("command/cfgSPSecondarySet.htm", player));
			return;
		}
		if (args.length < 2)
		{
			player.sendMessage("Incorrect values!");
			return;
		}

		switch (pageIndex)
		{
			case "C"://Change Password
				String currentPass = args[1];
				String newPass = args.length > 2 ? args[2] : "";

				if (getSecondaryPass(player).equals(currentPass))
				{
					if(!checkConditions(newPass))
					{
						player.sendMessage("Password should contain minimum a UpperCase, LowerCase, Symbol and a Digit!");
						return;
					}
					setSecondaryPassword(player, player.getAccountName(), newPass);
				}
				else
				{
					//player.stopAbnormalEffect(AbnormalEffect.FIREROOT_STUN);
					player.logout();
				}
				break;
			case "S":// Setup Password
				if(!checkConditions(args[1]))
				{
					sendHtml(player, HtmCache.getInstance().getNotNull("command/cfgSPSecondarySet.htm", player));
					player.sendMessage("Password should contain minimum a UpperCase, LowerCase, Symbol and a Digit!");
					return;
				}
				setSecondaryPassword(player, player.getAccountName(), args[1]);
				break;
		}

	}
	
	private static boolean checkConditions(String password)
	{
		boolean containsDigit = false;
		boolean containsUpperCase = false;
		boolean containsLowerCase = false;
		boolean hasSpecialChar = false;
		for(char c : password.toCharArray())
		{
			if(Character.isDigit(c))
			{
				containsDigit = true;
				continue;
			}
			if(Character.isUpperCase(c))
			{
				containsUpperCase = true;
				continue;
			}
			if(Character.isLowerCase(c))
			{
				containsLowerCase = true;	
				continue;
			}	
			
			switch(c)
			{
				case '!':
				case '@':
				case '#':
				case '$':
				case '%':
				case '^':
				case '&':
				case '*':
				case '(':
				case ')':
				case '-':
				case '=':
				{
					hasSpecialChar = true;
					continue;
				}
			}
		}
		return containsDigit && containsUpperCase && containsLowerCase && hasSpecialChar;
	}

	public static void setSecondaryPassword(Player player, String accountName, String password)
	{
		final boolean hadPassword = hasPassword(player);

		if (!validateString(password))
		{
			player.sendMessage("Invalid characters in Password!");

			if (hadPassword)
				sendHtml(player, HtmCache.getInstance().getNotNull("command/cfgSPSecondaryChange.htm", player));
			else
				sendHtml(player, HtmCache.getInstance().getNotNull("command/cfgSPSecondarySet.htm", player));
			return;
		}

		try (Connection con = LoginDatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("UPDATE accounts SET secondaryPassword=? WHERE login=?"))
		{
			statement.setString(1, password);
			statement.setString(2, accountName);

			statement.execute();
		}
		catch (SQLException e)
		{
			_log.info("Error setSecondaryPassword ", e);
		}

		player.sendMessage("Password successfully saved!");
		if (player.isBlocked())
		{
			//player.stopAbnormalEffect(AbnormalEffect.FIREROOT_STUN);
			player.broadcastPacket(new SocialAction(player.getObjectId(), SocialAction.VICTORY));
			final MagicSkillUse msu = new MagicSkillUse(player, player, 6463, 1, 0, 500);
			player.broadcastPacket(msu);
			player.broadcastCharInfo();
			player.unblock();
		}

		// Synerge - Show the first special tutorial window after setting for the first time the secondary password if character is under level 70
		if (!hadPassword && player.getLevel() < 70 && player.getActiveClassId() == player.getBaseClassId())
		{
			if (player.isMageClass())
				player.sendPacket(new TutorialShowHtml(HtmCache.getInstance().getNotNull("SpecialTutorial/AfterSecondaryPasswordMage.htm", player)));
			else
				player.sendPacket(new TutorialShowHtml(HtmCache.getInstance().getNotNull("SpecialTutorial/AfterSecondaryPasswordWarrior.htm", player)));
		}
		else
		{
			Quest q = QuestManager.getQuest(255);
			if (q != null)
			{
				player.processQuestEvent(q.getName(), "OpenClassMaster", null, false);
			}
		}
	}

	public static boolean tryPass(Player player, String pass)
	{
		String correctPass = getSecondaryPass(player);
		if (pass.equals(correctPass))
		{
			return true;
		}
		return false;
	}

	public static boolean hasPassword(Player player)
	{
		String pass = getSecondaryPass(player);
		if (pass != null && pass.length() > 0)
			return true;
		return false;
	}

	private static void sendHtml(Player player, String html)
	{
		html = html.replace("%online%", CCPSmallCommands.showOnlineCount());
		NpcHtmlMessage msg = new NpcHtmlMessage(0);
		msg.setHtml(html);
		player.sendPacket(msg);
	}

	private static String getSecondaryPass(Player player)
	{
		try (Connection con = LoginDatabaseFactory.getInstance().getConnection();
				PreparedStatement statement = con.prepareStatement("SELECT secondaryPassword FROM accounts WHERE login='" + player.getAccountName() + "'");
				ResultSet rset = statement.executeQuery())
		{
			while (rset.next())
			{
				return rset.getString("secondaryPassword");
			}
		}
		catch (SQLException e)
		{
			_log.error("Error in getSecondaryPass ", e);
		}
		return null;
	}

	private static boolean validateString(String s)
	{
		// Sanity check
		if (s == null || s.length() < 3 || s.length() > 30 || s.equalsIgnoreCase("Lineage2!"))
		{
			return false;
		}
		
		// a-z A-Z 0-9 Symbols: - ! $ % ^ & * ( ) _ + 
		String allowedChars = "^[a-zA-Z0-9-!@#$%^&*()_+=-]+$";
		if(!s.matches(allowedChars))
		{
			return false;
		}
		return true;
	}
}
