package l2f.gameserver.network.loginservercon.gspackets;

import l2f.gameserver.network.loginservercon.SendablePacket;

public class SendNewIds extends SendablePacket
{
	private int[] _ids;

	public SendNewIds(int[] ids)
	{
		_ids = ids;
	}

	protected void writeImpl()
	{
		writeC(0x12);
		writeD(_ids.length);
		for (int id : _ids)
			writeD(id);
	}
}
