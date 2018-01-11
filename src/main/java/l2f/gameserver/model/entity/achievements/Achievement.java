package l2f.gameserver.model.entity.achievements;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import l2f.gameserver.data.htm.HtmCache;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.items.ItemInstance;
import l2f.gameserver.model.reward.RewardItemResult;
import l2f.gameserver.network.serverpackets.InventoryUpdate;
import l2f.gameserver.network.serverpackets.MagicSkillUse;
import l2f.gameserver.network.serverpackets.components.CustomMessage;
import l2f.gameserver.utils.Log;

/**
 * 
 * @author Midnex
 * @author Promo (htmls)
 * @author Nik (total rework)
 *
 */
public class Achievement
{
	private final int _id;
	private final int _level;
	private final String _name;
	private final int _categoryId;
	private final String _icon;
	private final String _desc;
	private final long _pointsToComplete;
	private final String _achievementType;
	private final int _fame;
	private final List<RewardItemResult> _rewards;
	
	public Achievement(int id, int level, String name, int categoryId, String icon, String desc, long pointsToComplete, String achievementType, int fame)
	{
		_id = id;
		_level = level;
		_name = name;
		_categoryId = categoryId;
		_icon = icon;
		_desc = desc;
		_pointsToComplete = pointsToComplete;
		_achievementType = achievementType;
		_fame = fame;
		_rewards = new LinkedList<RewardItemResult>();
	}

	public boolean isDone(long playerPoints)
	{
		return playerPoints >= _pointsToComplete;
	}

	public String getNotDoneHtml(Player pl, int playerPoints)
	{
		String oneAchievement = HtmCache.getInstance().getNotNull("achievements/oneAchievement.htm", pl);

		int greenbar = (int) (24 * (playerPoints * 100 / _pointsToComplete) / 100);
		greenbar = Math.max(greenbar, 0);

		if (greenbar > 24)
		{
			pl.sendMessage(new CustomMessage("l2r.gameserver.achievements.iachievement.applying_fix", pl));
			return "";
		}

		oneAchievement = oneAchievement.replaceFirst("%fame%", "" + _fame);
		oneAchievement = oneAchievement.replaceAll("%bar1%", "" + greenbar);
		oneAchievement = oneAchievement.replaceAll("%bar2%", "" + (24 - greenbar));

		oneAchievement = oneAchievement.replaceFirst("%cap1%", greenbar > 0 ? "Gauge_DF_Food_Left" : "Gauge_DF_Exp_bg_Left");
		oneAchievement = oneAchievement.replaceFirst("%cap2%", "Gauge_DF_Exp_bg_Right");

		oneAchievement = oneAchievement.replaceFirst("%desc%", _desc.replaceAll("%need%", "" + Math.max(0, _pointsToComplete - playerPoints)));

		oneAchievement = oneAchievement.replaceFirst("%bg%", _id % 2 == 0 ? "090908" : "0f100f");
		oneAchievement = oneAchievement.replaceFirst("%icon%", _icon);
		oneAchievement = oneAchievement.replaceFirst("%name%", _name + (_level > 1 ? (" Lv. " + _level) : ""));
		return oneAchievement;
	}
	
	public String getDoneHtml()
	{
		String oneAchievement = HtmCache.getInstance().getNullable("achievements/oneAchievement.htm", null);

		oneAchievement = oneAchievement.replaceFirst("%fame%", "" + _fame);
		oneAchievement = oneAchievement.replaceAll("%bar1%", "24");
		oneAchievement = oneAchievement.replaceAll("%bar2%", "0");

		oneAchievement = oneAchievement.replaceFirst("%cap1%", "Gauge_DF_Food_Left");
		oneAchievement = oneAchievement.replaceFirst("%cap2%", "Gauge_DF_Food_Right");

		oneAchievement = oneAchievement.replaceFirst("%desc%", "Done.");

		oneAchievement = oneAchievement.replaceFirst("%bg%", _id % 2 == 0 ? "090908" : "0f100f");
		oneAchievement = oneAchievement.replaceFirst("%icon%", _icon);
		oneAchievement = oneAchievement.replaceFirst("%name%", _name + (_level > 1 ? (" Lv. " + _level) : ""));
		return oneAchievement;
	}
	
	public void reward(Player player)
	{
		synchronized (player.getAchievements())
		{
			player.sendChatMessage(player.getObjectId(), 20, "Achievement Completed!", getName());
			player.getAchievements().put(getId(), getLevel());
			
			player.setFame(player.getFame() + getFame());
			Log.add("game", "Achievements: Player " + player.getName() + " recived " + getFame() + " fame from achievement " + getName());
			
			InventoryUpdate iu = new InventoryUpdate();
			for (ItemInstance item : getRewards().stream().map(r -> r.createItem()).collect(Collectors.toList()))
			{
				player.getInventory().addItem(item, "Achievement:" + getName());
				iu.addNewItem(item);
			}
			
			player.broadcastPacket(iu, new MagicSkillUse(player, player, 2528, 1, 0, 500));
		}
	}

	public List<RewardItemResult> getRewards()
	{
		return _rewards;
	}

	public String getName()
	{
		return _name;
	}

	public String getDesc()
	{
		return _desc;
	}

	public int getId()
	{
		return _id;
	}

	public int getLevel()
	{
		return _level;
	}

	public void addReward(int itemId, long itemCount)
	{
		_rewards.add(new RewardItemResult(itemId, itemCount));
	}

	public String getType()
	{
		return _achievementType;
	}

	public long getPointsToComplete()
	{
		return _pointsToComplete;
	}

	public int getCategoryId()
	{
		return _categoryId;
	}

	public String getIcon()
	{
		return _icon;
	}

	public int getFame()
	{
		return _fame;
	}
}
