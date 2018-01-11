package l2f.gameserver.model.reward;

public class RewardItem
{
	public final int itemId;
	public long count;
	public boolean isAdena;

	public RewardItem(int itemId)
	{
		this.itemId = itemId;
		count = 1;
	}
}
