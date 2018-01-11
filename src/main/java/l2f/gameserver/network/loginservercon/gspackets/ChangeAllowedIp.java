package l2f.gameserver.network.loginservercon.gspackets;

import l2f.gameserver.network.loginservercon.SendablePacket;

public class ChangeAllowedIp extends SendablePacket
{

	private String account;
	private String ip;

	public ChangeAllowedIp(String account, String ip)
	{
		this.account = account;
		this.ip = ip;
	}

	@Override
	protected void writeImpl()
	{
		writeC(0x07);
		writeS(account);
		writeS(ip);
	}
}