package l2f.gameserver.network.serverpackets;

public class KeyPacket extends L2GameServerPacket
{
	//private final int[] _signatures;
	private byte[] _key;

	public KeyPacket(byte key[])
	{
		_key = key;
		//_signatures = ClickersSignatureDao.getInstance().getSignatures();
	}
	
	@Override
	public void writeImpl()
	{
		writeC(0x2E);
		if (_key == null || _key.length == 0)
		{
			writeC(0x00);
			return;
		}
		writeC(0x01);
		writeB(_key);
		writeD(0x01);
		writeD(0x00);
		writeC(0x00);
		writeD(0x00); // Seed (obfuscation key)
		
		//writeD(_signatures.length);

		//for(int sig : _signatures)
		//{
		//	writeD(sig);
		//}
	}
}