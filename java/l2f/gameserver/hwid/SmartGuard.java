package l2f.gameserver.hwid;

import l2f.gameserver.Config;
import l2f.gameserver.crypt.BlowfishEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author FireMoon
 */
public class SmartGuard
{
	private static final Logger LOG = LoggerFactory.getLogger(SmartGuard.class);
	
	protected static byte[] _key = new byte[16];
	
	public static byte[] getKey(byte[] key)
	{
		byte[] bfkey = {5, 5, 17, 11, 22, 32, 9, 9, 1, 17, -1, -2, 44, 45, -28, -12}; //STATIC KEY
		
		try
		{
			BlowfishEngine bf = new BlowfishEngine();
			bf.init(true, bfkey);
			bf.processBlock(key, 0, _key, 0);
			bf.processBlock(key, 8, _key, 8);
		}
		catch (IOException e)
		{
			LOG.warn("Incorrect Key!", e);
		}
		
		return _key;
	}
	
	public static byte[] getHwidKey(byte[] data)
	{
		byte[] bfkey = {15, 63, 19, 124, 120, 2, 91, 112, 74, 59, 4, 67, 32, 98, 44, 54}; //STATIC KEY
		
		byte[] result = null;
		
		try
		{
			BlowfishEngine bf = new BlowfishEngine();
			bf.init(false, bfkey);
			
			result = new byte[data.length];
			
			for (int i = 0; i <= data.length / 8; i++)
				bf.processBlock(data, i * 8, result, i * 8);

		}
		catch (IOException e)
		{
			//_log.warning("Incorrect Key 2!");
		}
		
		return result;
	}
	
	public static boolean isSmartGuardEnabled()
	{
		if (Config.ALLOW_SMARTGUARD)
			return true;
		
		return false;
	}
	
	public static int getHWIDOption()
	{
		return Config.GET_CLIENT_HWID;
	}
}