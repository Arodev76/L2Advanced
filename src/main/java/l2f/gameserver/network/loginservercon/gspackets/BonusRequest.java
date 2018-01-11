package l2f.gameserver.network.loginservercon.gspackets;

import l2f.gameserver.network.loginservercon.SendablePacket;

public class BonusRequest extends SendablePacket
{
	private String account;
	private double bonus;
	private int bonusExpire;
	
	public BonusRequest(String account, double bonus, int bonusExpire)
	{
		this.account = account;
		this.bonus = bonus;
		this.bonusExpire = bonusExpire;
	}
	
	protected void writeImpl()
	{
		writeC(0x10);
		writeS(account);
		writeF(bonus);
		writeD(bonusExpire);
	}
}