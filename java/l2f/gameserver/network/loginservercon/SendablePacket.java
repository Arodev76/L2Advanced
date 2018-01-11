package l2f.gameserver.network.loginservercon;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public abstract class SendablePacket extends l2f.commons.net.nio.SendablePacket<AuthServerCommunication>
{
	private static final Logger _log = LoggerFactory.getLogger(SendablePacket.class);
	
	@Override
	public AuthServerCommunication getClient()
	{
		return AuthServerCommunication.getInstance();
	}
	
	@Override
	protected ByteBuffer getByteBuffer()
	{
		return getClient().getWriteBuffer();
	}
	
	public boolean write()
	{
		try
		{
			writeImpl();
		}
		catch (RuntimeException e)
		{
			_log.error("Loginserver SendablePacket ", e);
		}
		return true;
	}
	
	protected abstract void writeImpl();
}
