package l2f.gameserver.network.clientpackets;

import l2f.gameserver.hwid.ClickersDetector;
import l2f.gameserver.model.Player;


public class ThirdPartyProgramUsage extends L2GameClientPacket
{
	private int _botType;
	@Override
	protected void readImpl()
	{
		int type1 = readC();//handled by dsetup
		int type2 = readD();//handled by signatures database
		
		if (type2 != 0)
			_botType = type2;
		else
			_botType = type1;
	}

	@Override
	protected void runImpl()
	{
		Player player = getClient().getActiveChar();
				
		ClickersDetector.botPunish(player, _botType);
	}
}