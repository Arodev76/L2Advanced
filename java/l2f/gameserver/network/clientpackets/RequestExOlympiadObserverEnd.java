package l2f.gameserver.network.clientpackets;

/**
 * format ch
 * c: (id) 0xD0
 * h: (subid) 0x2F
 */
public class RequestExOlympiadObserverEnd extends L2GameClientPacket
{
	@Override
	protected void readImpl()
	{}

	@Override
	protected void runImpl()
	{
		// ignored
	}
}