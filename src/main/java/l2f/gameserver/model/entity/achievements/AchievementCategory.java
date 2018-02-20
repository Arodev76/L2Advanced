package l2f.gameserver.model.entity.achievements;

import java.util.List;

import javolution.util.FastTable;
import l2f.gameserver.cache.HtmCache;
import l2f.gameserver.model.Player;

/**
 * 
 * @author Midnex
 * @author Promo (htmls)
 * @author Nik (total rework)
 *
 */
public class AchievementCategory
{
	private static final int BAR_MAX = 24;
	private List<Achievement> _achievements = new FastTable<>();
	private final int _categoryId;
	//private String _html;
	private final String _name;
	private final String _icon;
	private final String _desc;
	
	public AchievementCategory(int categoryId, String categoryName, String categoryIcon, String categoryDesc)
	{
		_categoryId = categoryId;
		_name = categoryName;
		_icon = categoryIcon;
		_desc = categoryDesc;
	}
	
	public String getHtml(Player player)
	{
		return getHtml(Achievements.getAchievementLevelSum(player, getCategoryId()));
	}

	public String getHtml(int totalPlayerLevel)
	{
		int greenbar = 0;

		if (totalPlayerLevel > 0)
		{
			greenbar = BAR_MAX * (totalPlayerLevel * 100 / _achievements.size()) / 100;
			greenbar = Math.min(greenbar, BAR_MAX);
		}
		
		String temp = HtmCache.getInstance().getNullable("achievements/AchievementsCat.htm", null);
		
		temp = temp.replaceFirst("%bg%", getCategoryId() % 2 == 0 ? "090908" : "0f100f");
		temp = temp.replaceFirst("%desc%", getDesc());
		temp = temp.replaceFirst("%icon%", getIcon());
		temp = temp.replaceFirst("%name%", getName());
		temp = temp.replaceFirst("%id%", "" + getCategoryId());

		temp = temp.replaceFirst("%caps1%", greenbar > 0 ? "Gauge_DF_Food_Left" : "Gauge_DF_Exp_bg_Left");
		temp = temp.replaceFirst("%caps2%", greenbar >= 24 ? "Gauge_DF_Food_Right" : "Gauge_DF_Exp_bg_Right");

		temp = temp.replaceAll("%bar1%", "" + greenbar);
		temp = temp.replaceAll("%bar2%", "" + (24 - greenbar));
		return temp;
	}

	public int getCategoryId()
	{
		return _categoryId;
	}

	public List<Achievement> getAchievements()
	{
		return _achievements;
	}

	public String getDesc()
	{
		return _desc;
	}

	public String getIcon()
	{
		return _icon;
	}

	public String getName()
	{
		return _name;
	}
}
