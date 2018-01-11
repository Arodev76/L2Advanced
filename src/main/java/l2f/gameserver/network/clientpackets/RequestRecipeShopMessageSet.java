package l2f.gameserver.network.clientpackets;

import l2f.gameserver.model.Player;

public class RequestRecipeShopMessageSet extends L2GameClientPacket
{
	// format: cS
	private String _name;

	@Override
	protected void readImpl()
	{
		_name = readS(16);
	}

	@Override
	protected void runImpl()
	{
		Player activeChar = getClient().getActiveChar();
		if (activeChar == null)
			return;

		activeChar.setManufactureName(_name);
	}
}