package l2f.loginserver.clientpackets;

import l2f.loginserver.L2LoginClient;
import l2f.loginserver.SessionKey;
import l2f.loginserver.serverpackets.ServerList;
import l2f.loginserver.serverpackets.ServerListFake;
import l2f.loginserver.serverpackets.LoginFail.LoginFailReason;

/**
 * Format: ddc
 * d: fist part of session id
 * d: second part of session id
 * c: ?
 */
public class RequestServerList extends L2LoginClientPacket
{
	private int _loginOkID1;
	private int _loginOkID2;
	private boolean _loginFake = false;

	public RequestServerList(boolean login) 
	{
		_loginFake = login;
	}

	@Override
	protected void readImpl()
	{
		_loginOkID1 = readD();
		_loginOkID2 = readD();
	}

	@Override
	protected void runImpl()
	{
		L2LoginClient client = getClient();
		if (_loginFake)
		{
			client.sendPacket(new ServerListFake(client.getAccount()));
			return;
		}
		SessionKey skey = client.getSessionKey();
		
		if (skey == null || !skey.checkLoginPair(_loginOkID1, _loginOkID2))
		{
			client.close(LoginFailReason.REASON_ACCESS_FAILED);
			return;
		}

		client.sendPacket(new ServerList(client.getAccount()));
	}
}