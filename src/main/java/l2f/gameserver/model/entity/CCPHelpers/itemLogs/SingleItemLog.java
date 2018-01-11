package l2f.gameserver.model.entity.CCPHelpers.itemLogs;

public class SingleItemLog
{
	private final int itemTemplateId;
	private final long itemCount;
	private final int itemEnchantLevel;
	private final int itemObjectId;
	private String receiverName;
	private boolean nameChanged;

	public SingleItemLog(int itemTemplateId, long itemCount, int itemEnchantLevel, int itemObjectId)
	{
		this(itemTemplateId, itemCount, itemEnchantLevel, itemObjectId, "");
	}

	public SingleItemLog(int itemTemplateId, long itemCount, int itemEnchantLevel, int itemObjectId, String receiverName)
	{
		this.itemTemplateId = itemTemplateId;
		this.itemCount = itemCount;
		this.itemEnchantLevel = itemEnchantLevel;
		this.itemObjectId = itemObjectId;
		this.receiverName = receiverName;
		this.nameChanged = false;
	}

	public int getItemTemplateId()
	{
		return this.itemTemplateId;
	}

	public long getItemCount()
	{
		return this.itemCount;
	}

	public int getItemEnchantLevel()
	{
		return this.itemEnchantLevel;
	}

	public int getItemObjectId()
	{
		return this.itemObjectId;
	}

	public void setReceiverName(String receiverName)
	{
		this.receiverName = receiverName;
		this.nameChanged = true;
	}

	public String getReceiverName()
	{
		return this.receiverName;
	}

	public boolean didNameChange()
	{
		return this.nameChanged;
	}
}