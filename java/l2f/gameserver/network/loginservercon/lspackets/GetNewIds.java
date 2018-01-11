package l2f.gameserver.network.loginservercon.lspackets;

import l2f.gameserver.idfactory.IdFactory;
import l2f.gameserver.network.loginservercon.AuthServerCommunication;
import l2f.gameserver.network.loginservercon.ReceivablePacket;
import l2f.gameserver.network.loginservercon.gspackets.SendNewIds;


public class GetNewIds extends ReceivablePacket
{
	int count;

	@Override
	public void readImpl()
	{
		count = readD();
	}
	
	@Override
	protected void runImpl()
	{
		int[] ids = new int[count];
		for (int i = 0;i<count;i++)
			ids[i] = IdFactory.getInstance().getNextId();
		
		AuthServerCommunication.getInstance().sendPacket(new SendNewIds(ids));
	}
}