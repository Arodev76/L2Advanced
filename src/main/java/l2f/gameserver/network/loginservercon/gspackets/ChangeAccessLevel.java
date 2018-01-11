package l2f.gameserver.network.loginservercon.gspackets;

import l2f.gameserver.network.loginservercon.SendablePacket;

public class ChangeAccessLevel extends SendablePacket
{
	private String account;
	private int level;
	private int banExpire;
	
	public ChangeAccessLevel(String account, int level, int banExpire)
	{
		this.account = account;
		this.level = level;
		this.banExpire = banExpire;
	}
	
	protected void writeImpl()
	{
		writeC(0x11);
		writeS(account);
		writeD(level);
		writeD(banExpire);
	}
}
