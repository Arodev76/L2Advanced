package l2f.gameserver.network.serverpackets;

/**
 * @author VISTALL
 */
public class ExPVPMatchUserDie extends L2GameServerPacket
{
	private int _blueKills, _redKills;

	public ExPVPMatchUserDie()
	{

	}

	@Override
	protected final void writeImpl()
	{
		writeEx(0x7F);
		writeD(_blueKills);
		writeD(_redKills);
	}
}