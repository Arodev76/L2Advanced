package l2f.loginserver;

import java.io.File;
import java.net.InetAddress;
import java.net.ServerSocket;

import l2f.commons.net.nio.impl.SelectorConfig;
import l2f.commons.net.nio.impl.SelectorThread;
import l2f.loginserver.database.L2DatabaseFactory;
import l2f.loginserver.gameservercon.GameServerCommunication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthServer
{
	private static final Logger _log = LoggerFactory.getLogger(AuthServer.class);

	private static AuthServer authServer;

	private GameServerCommunication _gameServerListener;
	private SelectorThread<L2LoginClient> _selectorThread;

	public static AuthServer getInstance()
	{
		return authServer;
	}

	public AuthServer() throws Throwable
	{
		Config.initCrypt();
		GameServerManager.getInstance();

		L2LoginPacketHandler loginPacketHandler = new L2LoginPacketHandler();
		SelectorHelper sh = new SelectorHelper();
		SelectorConfig sc = new SelectorConfig();
		_selectorThread = new SelectorThread<L2LoginClient>(sc, loginPacketHandler, sh, sh, sh);

		_gameServerListener = GameServerCommunication.getInstance();
		_gameServerListener.openServerSocket(Config.GAME_SERVER_LOGIN_HOST.equals("*") ? null : InetAddress.getByName(Config.GAME_SERVER_LOGIN_HOST), Config.GAME_SERVER_LOGIN_PORT);
		_gameServerListener.start();
		_log.info("Listening for gameservers on " + Config.GAME_SERVER_LOGIN_HOST + ":" + Config.GAME_SERVER_LOGIN_PORT);

		_selectorThread.openServerSocket(Config.LOGIN_HOST.equals("*") ? null : InetAddress.getByName(Config.LOGIN_HOST), Config.PORT_LOGIN);
		_selectorThread.start();
		_log.info("Listening for clients on " + Config.LOGIN_HOST + ":" + Config.PORT_LOGIN);
	}

	public GameServerCommunication getGameServerListener()
	{
		return _gameServerListener;
	}

	public static void checkFreePorts() throws Throwable
	{
		ServerSocket ss = null;

		try
		{
			if (Config.LOGIN_HOST.equalsIgnoreCase("*"))
				ss = new ServerSocket(Config.PORT_LOGIN);
			else
				ss = new ServerSocket(Config.PORT_LOGIN, 50, InetAddress.getByName(Config.LOGIN_HOST));
		}
		finally
		{
			if (ss != null)
				try
				{
					ss.close();
				}
				catch (Exception e)
				{}
		}
	}

	public static void main(String[] args) throws Throwable
	{
		new File("./log/").mkdir();
		// Initialize config
		Config.load();
		// Check binding address
		checkFreePorts();
		// Initialize database
		Class.forName(Config.DATABASE_DRIVER).newInstance();
		L2DatabaseFactory.getInstance().getConnection().close();

		authServer = new AuthServer();
	}
}