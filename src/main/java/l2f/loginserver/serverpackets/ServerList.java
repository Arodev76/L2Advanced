package l2f.loginserver.serverpackets;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import javolution.util.FastMap;
import l2f.commons.net.AdvIP;
import l2f.commons.net.utils.NetUtils;
import l2f.loginserver.GameServerManager;
import l2f.loginserver.accounts.Account;
import l2f.loginserver.gameservercon.GameServer;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

public final class ServerList extends L2LoginServerPacket
{
	private Map<Integer, ServerData> _servers;
	private List<Integer> _serverIds;

	private int _lastServer;

	private static class ServerData
	{
		InetAddress ip;
		int port;
		int online;
		int maxPlayers;
		boolean status;
		boolean pvp;
		boolean brackets;
		int type;
		int ageLimit;
		int playerSize;
		int[] deleteChars;
		int serverId;

		ServerData(InetAddress ip, int port, boolean pvp, boolean brackets, int type, int online, int maxPlayers, boolean status, int size, int ageLimit, int[] d, int serverId)
		{
			this.ip = ip;
			this.port = port;
			this.pvp = pvp;
			this.brackets = brackets;
			this.type = type;
			this.online = online;
			this.maxPlayers = maxPlayers;
			this.status = status;
			this.playerSize = size;
			this.ageLimit = ageLimit;
			this.deleteChars = d;
			this.serverId = serverId;
		}
	}

	public ServerList(Account account)
	{
		_servers = new FastMap<Integer, ServerData>();

		_lastServer = account.getLastServer();

		for (GameServer gs : GameServerManager.getInstance().getGameServers())
		{
			InetAddress ip;
			try
			{
				ip = NetUtils.isInternalIP(account.getLastIP()) ? gs.getInternalHost() : gs.getExternalHost();
			}
			catch (UnknownHostException e)
			{
				continue;
			}

			// Adds original server.
			addServer(gs.getId(), ip, gs.getPort(), gs, account);

			// Adds channels
			if (gs.getAdvIP() != null)
			{
				for (AdvIP localAdvIP : gs.getAdvIP())
				{
					try
					{
						addServer(localAdvIP.channelId, InetAddress.getByName(localAdvIP.channelAdress), localAdvIP.channelPort, gs, account);

						//_log.warn("Adding server: " + localAdvIP.channelAdress + ":" + localAdvIP.channelPort + ", id: " + localAdvIP.channelId);
					}
					catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			}
		}

		_serverIds = Arrays.asList(_servers.keySet().toArray(new Integer[_servers.size()]));
		Collections.sort(_serverIds);
	}

	public void addServer(int server_id, InetAddress ip, int port, final GameServer gs, Account account)
	{
		Pair<Integer, int[]> entry = account.getAccountInfo(gs.getId());

		try
		{
			_servers.put(server_id, new ServerData(ip, port, gs.isPvp(), gs.isShowingBrackets(), gs.getServerType(), gs.getOnline(), gs.getMaxPlayers(), gs.isOnline(), entry == null ? 0 : entry.getKey(), gs.getAgeLimit(), entry == null ? ArrayUtils.EMPTY_INT_ARRAY : entry.getValue(), server_id));
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	@Override
	protected void writeImpl()
	{
		writeC(0x04);
		writeC(_servers.size());
		writeC(_lastServer);

		ServerData server;

		for (Integer serverId : _serverIds)
		{
			server = _servers.get(serverId);
			writeC(server.serverId);

			InetAddress i4 = server.ip;
			byte[] raw = i4.getAddress();
			writeC(raw[0] & 0xff);
			writeC(raw[1] & 0xff);
			writeC(raw[2] & 0xff);
			writeC(raw[3] & 0xff);

			writeD(server.port);
			writeC(server.ageLimit); // age limit
			writeC(server.pvp ? 0x01 : 0x00);
			writeH(server.online);
			writeH(server.maxPlayers);
			writeC(server.status ? 0x01 : 0x00);
			writeD(server.type);
			writeC(server.brackets ? 0x01 : 0x00);
		}

		writeH(0x00); // -??
		writeC(_servers.size());

		for (Integer serverId : _serverIds)
		{
			server = _servers.get(serverId);
			writeC(server.serverId);
			writeC(server.playerSize); // acc player size
			writeC(server.deleteChars.length);
			for (int t : server.deleteChars)
				writeD((int)(t - System.currentTimeMillis() / 1000L));
		}
	}
}