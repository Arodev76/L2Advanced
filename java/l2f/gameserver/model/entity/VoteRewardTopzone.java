package l2f.gameserver.model.entity;

import javolution.util.FastMap;
import l2f.gameserver.Announcements;
import l2f.gameserver.Config;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;
import l2f.gameserver.utils.ItemFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class VoteRewardTopzone
{
	private static final Logger _log = LoggerFactory.getLogger(VoteRewardTopzone.class);
	
	// Configurations.
	private static String topzoneUrl = Config.TOPZONE_SERVER_LINK;
	private static String page1Url = Config.TOPZONE_FIRST_PAGE_LINK;
	private static int voteRewardVotesDifference = Config.TOPZONE_VOTES_DIFFERENCE;
	private static int firstPageRankNeeded = Config.TOPZONE_FIRST_PAGE_RANK_NEEDED;
	private static int checkTime = 60 * 1000 * Config.TOPZONE_REWARD_CHECK_TIME;
	
	// Don't-touch variables.
	private static int lastVotes = 0;
	private static FastMap<String, Integer> playerIps = new FastMap<String, Integer>();
	
	public static void updateConfigurations()
	{
		topzoneUrl = Config.TOPZONE_SERVER_LINK;
		page1Url = Config.TOPZONE_FIRST_PAGE_LINK;
		voteRewardVotesDifference = Config.TOPZONE_VOTES_DIFFERENCE;
		firstPageRankNeeded = Config.TOPZONE_FIRST_PAGE_RANK_NEEDED;
		checkTime = 60 * 1000 * Config.TOPZONE_REWARD_CHECK_TIME;
	}
	
	public static void getInstance()
	{
		_log.info("Topzone: Vote reward system initialized.");
		ThreadPoolManager.getInstance().scheduleAtFixedRate(new Runnable()
		{
			@Override
			public void run()
			{
				if (Config.ALLOW_TOPZONE_VOTE_REWARD)
				{
					reward();
				}
				else
				{
					return;
				}
			}
		}, checkTime / 2, checkTime);
	}
	
	private static void reward()
	{
		int firstPageVotes = getFirstPageRankVotes();
		int currentVotes = getVotes();
		
		if ((firstPageVotes == -1) || (currentVotes == -1))
		{
			if (firstPageVotes == -1)
			{
				_log.info("Topzone: There was a problem on getting votes from server with rank " + firstPageRankNeeded + ".");
			}
			if (currentVotes == -1)
			{
				_log.info("Topzone: There was a problem on getting server votes.");
			}
			
			return;
		}
		
		if (lastVotes == 0)
		{
			lastVotes = currentVotes;
			Announcements.getInstance().announceToAll("Topzone: Current vote count is " + currentVotes + ".");
			Announcements.getInstance().announceToAll("Topzone: We need " + ((lastVotes + voteRewardVotesDifference) - currentVotes) + " vote(s) for reward.");
			if (Config.ALLOW_TOPZONE_GAME_SERVER_REPORT)
			{
				_log.info("Server votes on topzone: " + currentVotes);
				_log.info("Votes needed for reward: " + ((lastVotes + voteRewardVotesDifference) - currentVotes));
			}
			if ((firstPageVotes - lastVotes) <= 0)
			{
				Announcements.getInstance().announceToAll("Topzone: We are in the first page of topzone, so the reward will be big.");
				if (Config.ALLOW_TOPZONE_GAME_SERVER_REPORT)
				{
					_log.info("Server is on the first page of topzone.");
				}
			}
			else
			{
				Announcements.getInstance().announceToAll("Topzone: We need " + (firstPageVotes - lastVotes) + " vote(s) to get to the first page of topzone for big reward.");
				if (Config.ALLOW_TOPZONE_GAME_SERVER_REPORT)
				{
					_log.info("Server votes needed for first page: " + (firstPageVotes - lastVotes));
				}
			}
			return;
		}
		
		if (currentVotes >= (lastVotes + voteRewardVotesDifference))
		{
			if ((firstPageVotes - currentVotes) <= 0)
			{
				if (Config.ALLOW_TOPZONE_GAME_SERVER_REPORT)
				{
					_log.info("Server votes on topzone: " + currentVotes);
					_log.info("Server is on the first page of topzone.");
					_log.info("Votes needed for next reward: " + ((currentVotes + voteRewardVotesDifference) - currentVotes));
				}
				Announcements.getInstance().announceToAll("Topzone: Everyone has been rewarded with big reward.");
				Announcements.getInstance().announceToAll("Topzone: Current vote count is " + currentVotes + ".");
				for (Player player : GameObjectsStorage.getAllPlayers())
				{
					boolean canReward = false;
					String pIp = player.getIP();
					
					if (playerIps.containsKey(pIp))
					{
						int count = playerIps.get(pIp);
						if (count < Config.TOPZONE_DUALBOXES_ALLOWED)
						{
							playerIps.remove(pIp);
							playerIps.put(pIp, count + 1);
							canReward = true;
						}
					}
					else
					{
						canReward = true;
						playerIps.put(pIp, 1);
					}
					if (canReward)
					{
						addItem(player, Config.TOPZONE_REWARD_ID, Config.TOPZONE_REWARD_COUNT);
						player.sendMessage("You have received an award for his voice to the Topzone in the amount of " + Config.TOPZONE_REWARD_COUNT);
					}
					else
					{
						player.sendMessage("Already " + Config.TOPZONE_DUALBOXES_ALLOWED + " character(s) of your ip have been rewarded, so this character won't be rewarded.");
					}
				}
				playerIps.clear();
			}
			else
			{
				if (Config.ALLOW_TOPZONE_GAME_SERVER_REPORT)
				{
					_log.info("Server votes on topzone: " + currentVotes);
					_log.info("Server votes needed for first page: " + (firstPageVotes - lastVotes));
					_log.info("Votes needed for next reward: " + ((currentVotes + voteRewardVotesDifference) - currentVotes));
				}
				Announcements.getInstance().announceToAll("Topzone: Everyone has been rewarded with small reward.");
				Announcements.getInstance().announceToAll("Topzone: Current vote count is " + currentVotes + ".");
				Announcements.getInstance().announceToAll("Topzone: We need " + (firstPageVotes - currentVotes) + " vote(s) to get to the first page of topzone for big reward.");
				for (Player player : GameObjectsStorage.getAllPlayers())
				{
					boolean canReward = false;
					String pIp = player.getIP();
					if (playerIps.containsKey(pIp))
					{
						int count = playerIps.get(pIp);
						if (count < Config.TOPZONE_DUALBOXES_ALLOWED)
						{
							playerIps.remove(pIp);
							playerIps.put(pIp, count + 1);
							canReward = true;
						}
					}
					else
					{
						canReward = true;
						playerIps.put(pIp, 1);
					}
					if (canReward)
					{
						addItem(player, Config.TOPZONE_REWARD_ID, Config.TOPZONE_REWARD_COUNT);
						player.sendMessage("You have received an award for his voice to the HOPZONE in the amount of " + Config.TOPZONE_REWARD_COUNT);
					}
					else
					{
						player.sendMessage("Already " + Config.TOPZONE_DUALBOXES_ALLOWED + " character(s) of your ip have been rewarded, so this character won't be rewarded.");
					}
				}
				playerIps.clear();
			}
			
			lastVotes = currentVotes;
		}
		else
		{
			if ((firstPageVotes - currentVotes) <= 0)
			{
				if (Config.ALLOW_TOPZONE_GAME_SERVER_REPORT)
				{
					_log.info("Server votes on topzone: " + currentVotes);
					_log.info("Server is on the first page of topzone.");
					_log.info("Votes needed for next reward: " + ((lastVotes + voteRewardVotesDifference) - currentVotes));
				}
				Announcements.getInstance().announceToAll("Topzone: Current vote count is " + currentVotes + ".");
				Announcements.getInstance().announceToAll("Topzone: We need " + ((lastVotes + voteRewardVotesDifference) - currentVotes) + " vote(s) for big reward.");
			}
			else
			{
				if (Config.ALLOW_TOPZONE_GAME_SERVER_REPORT)
				{
					_log.info("Server votes on topzone: " + currentVotes);
					_log.info("Server votes needed for first page: " + (firstPageVotes - lastVotes));
					_log.info("Votes needed for next reward: " + ((lastVotes + voteRewardVotesDifference) - currentVotes));
				}
				Announcements.getInstance().announceToAll("Topzone: Current vote count is " + currentVotes + ".");
				Announcements.getInstance().announceToAll("Topzone: We need " + ((lastVotes + voteRewardVotesDifference) - currentVotes) + " vote(s) for small reward.");
				Announcements.getInstance().announceToAll("Topzone: We need " + (firstPageVotes - currentVotes) + " vote(s) to get to the first page of topzone for big reward.");
			}
		}
	}
	
	private static int getFirstPageRankVotes()
	{
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		try
		{
			URLConnection con = new URL(page1Url).openConnection();
			con.addRequestProperty("User-Agent", "Mozilla/4.76");
			isr = new InputStreamReader(con.getInputStream());
			br = new BufferedReader(isr);
			
			String line;
			int i = 0;
			while ((line = br.readLine()) != null)
			{
				if (line.contains("<td><div align=\"center\">" + firstPageRankNeeded + "</div></td>"))
				{
					i++;
				}
				if (line.contains("<td><div align=\"center\">") && (i == 1))
				{
					i++;
				}
				if (line.contains("<td><div align=\"center\">") && (i == 2))
				{
					i = 0;
					int votes = Integer.valueOf(line.split(">")[2].replace("</div", ""));
					return votes;
				}
			}
			
			br.close();
			isr.close();
		}
		catch (Exception e)
		{
			_log.warn("Topzone: Error while getting server vote count.", e);
		}
		
		return -1;
	}
	
	private static int getVotes()
	{
		InputStreamReader isr = null;
		BufferedReader br = null;
		
		try
		{
			URLConnection con = new URL(topzoneUrl).openConnection();
			con.addRequestProperty("User-Agent", "Mozilla/4.76");
			isr = new InputStreamReader(con.getInputStream());
			br = new BufferedReader(isr);
			
			boolean got = false;
			
			String line;
			while ((line = br.readLine()) != null)
			{
				if (line.contains("<tr><td><div align=\"center\"><b><font style=\"font-size:14px;color:#018BC1;\"") && !got)
				{
					got = true;
					int votes = Integer.valueOf(line.split("=\"font-size:14px;color:#018BC1;\">")[1].replace("</font></b></div></td></tr>", ""));
					return votes;
				}
			}
			
			br.close();
			isr.close();
		}
		catch (Exception e)
		{
			_log.warn("Topzone: Error while getting server vote count.", e);
		}
		
		return -1;
	}
	
	public static void addItem(Player player, int itemId, long count)
	{
		ItemFunctions.addItem(player, itemId, count, true, "VoteRewardTopzone");
	}
}