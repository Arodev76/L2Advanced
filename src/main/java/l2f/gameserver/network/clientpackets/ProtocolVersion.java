package l2f.gameserver.network.clientpackets;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2f.commons.configuration.Config;
import l2f.gameserver.hwid.SmartGuard;
import l2f.gameserver.network.serverpackets.KeyPacket;
import l2f.gameserver.network.serverpackets.SendStatus;

public class ProtocolVersion extends L2GameClientPacket
{
	private static final Logger _log = LoggerFactory.getLogger(ProtocolVersion.class);

	private int protocol;
	private byte[] hwidData;

	protected void readImpl()
	{
		protocol = readD();
		
		if ((_buf.remaining() > 260) && SmartGuard.isSmartGuardEnabled())
		{
			_buf.position(_buf.position() + 260);

			hwidData = new byte[66];
			try
			{
				readB(hwidData);
			}
			catch (RuntimeException e)
			{
				hwidData = null;
			}
		}
	}

	protected void runImpl()
	{
		if (protocol == -2)
		{
			_client.closeNow(false);
			return;
		}
		else if (protocol == -3)
		{
			_log.info("Status request from IP : " + getClient().getIpAddr());
			getClient().close(new SendStatus());
			return;
		}
		else if (protocol < Config.MIN_PROTOCOL_REVISION || protocol > Config.MAX_PROTOCOL_REVISION)
		{
			_log.warn("Unknown protocol revision : " + protocol + ", client : " + _client);
			getClient().close(new KeyPacket(null));
			return;
		}
		
		if (SmartGuard.isSmartGuardEnabled())
		{
			boolean isEverythingOk;
			
			try
			{
				isEverythingOk = setHwidAndVer();
			}
			catch (RuntimeException e)
			{
				isEverythingOk = false;
			}
			
			if (!isEverythingOk)
			{
				_client.setSystemVersion(999);
				getClient().setHWID("TEMP_ERROR");
			}
		}
		else
		{
			_client.setSystemVersion(Config.LATEST_SYSTEM_VER);
			getClient().setHWID("NO-SMART-GUARD-ENABLED");
		}
		
		sendPacket(new KeyPacket(_client.enableCrypt()));
	}
	
	private boolean setHwidAndVer()
	{
		if (!SmartGuard.isSmartGuardEnabled() || hwidData == null || hwidData.length != 66)
			return false;
		
		byte[] result = _client.getDecryptedProtocol(hwidData);
		Charset encoding = Charset.forName("UTF-8");
		
		String strangeHwid = new String(Arrays.copyOfRange(result, 0, 38), encoding);
		StringBuilder builder = new StringBuilder();
		for (int i = 0;i<strangeHwid.length();i+=2)
			builder.append(strangeHwid.substring(i, i + 1));
		
		String fileId = new String(Arrays.copyOfRange(result, 40, 58), encoding);
		
		byte[] systemVerArray = Arrays.copyOfRange(result, 60, 66);
		ArrayUtils.reverse(systemVerArray);
		int systemVer = ByteBuffer.wrap(systemVerArray).getInt();
		
		if (systemVer != Config.LATEST_SYSTEM_VER || !builder.toString().contains("-"))
		{
			return false;
		}
		
		_client.setSystemVersion(systemVer);
		getClient().setHWID(builder.toString());
		getClient().setFileId(fileId);
		
		return true;
	}

	@Override
	public String getType()
	{
		return getClass().getSimpleName();
	}
}