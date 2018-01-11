package l2f.gameserver.handler.bypass;

public class BypassHandler
{
	private static final BypassHandler _instance = new BypassHandler();

	public static BypassHandler getInstance()
	{
		return _instance;
	}

	public void registerBypass(IBypassHandler bypass)
	{

	}
}
