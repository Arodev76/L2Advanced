package l2f.gameserver.network.serverpackets;


import l2f.gameserver.model.Player;

import org.apache.commons.lang3.StringUtils;

public class PrivateStoreMsgBuy extends L2GameServerPacket
{
	private int _objId;
	private String _name;

	/**
	 * Название личного магазина покупки
	 * @param player
	 */
	public PrivateStoreMsgBuy(Player player)
	{
		_objId = player.getObjectId();
		_name = StringUtils.defaultString(player.getBuyStoreName());
	}

	@Override
	protected final void writeImpl()
	{
		writeC(0xBF);
		writeD(_objId);
		writeS(_name);
	}
}