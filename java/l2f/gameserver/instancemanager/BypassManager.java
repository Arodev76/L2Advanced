package l2f.gameserver.instancemanager;

import l2f.gameserver.handler.bbs.ICommunityBoardHandler;
import l2f.gameserver.model.Player;
import l2f.gameserver.utils.Log;
import l2f.gameserver.utils.Strings;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class BypassManager
{
	private static final Pattern p = Pattern.compile("\"(bypass +-h +)(.+?)\"");

	public static enum BypassType
	{
		ENCODED,
		ENCODED_BBS,
		SIMPLE,
		SIMPLE_BBS,
		SIMPLE_DIRECT
	}

	public static BypassType getBypassType(String bypass)
	{
		switch (bypass.charAt(0))
		{
			case '0':
				return BypassType.ENCODED;
			case '1':
				return BypassType.ENCODED_BBS;
			default:
				if (Strings.matches(bypass, "^(_mrsl|_diary|_match|manor_menu_select|_match|_olympiad).*", Pattern.DOTALL))
					return BypassType.SIMPLE;
//				if (CommunityBoardManager.getInstance().getCommunityHandler(bypass) != null)
//					return BypassType.SIMPLE_BBS;
				return BypassType.SIMPLE_DIRECT;
		}
	}

	public static String encode(String html, List<String> bypassStorage, boolean bbs)
	{
		Matcher m = p.matcher(html);
		StringBuffer sb = new StringBuffer();

		while (m.find())
		{
			String bypass = m.group(2);
			String code = bypass;
			String params = "";
			int i = bypass.indexOf(" $");
			boolean use_params = i >= 0;
			if (use_params)
			{
				code = bypass.substring(0, i);
				params = bypass.substring(i).replace("$", "\\$");
			}

			if (bbs)
				m.appendReplacement(sb, "\"bypass -h 1" + Integer.toHexString(bypassStorage.size()) + params + "\"");
			else
				m.appendReplacement(sb, "\"bypass -h 0" + Integer.toHexString(bypassStorage.size()) + params + "\"");

			bypassStorage.add(code);
		}

		m.appendTail(sb);
		return sb.toString();
	}

	public static DecodedBypass decode(String bypass, List<String> bypassStorage, boolean bbs, Player player)
	{
		synchronized (bypassStorage)
		{
			String[] bypass_parsed = bypass.split(" ");
			int idx = Integer.parseInt(bypass_parsed[0].substring(1), 16);
			String bp;

			try
			{
				bp = bypassStorage.get(idx);
			}
			catch (RuntimeException e)
			{
				bp = null;
			}

			if (bp == null)
			{
				Log.add("Can't decode bypass (bypass not exists): " + (bbs ? "[bbs] " : "") + bypass + " / Player: " + player.getName() + " / Npc: " + (player.getLastNpc() == null ? "null" : player.getLastNpc().getName()), "debug_bypass");
				return null;
			}

			DecodedBypass result = null;
			result = new DecodedBypass(bp, bbs);
			for (int i = 1; i < bypass_parsed.length; i++)
				result.bypass += " " + bypass_parsed[i];
			result.trim();

			return result;
		}
	}

	public static class DecodedBypass
	{
		public String bypass;
		public boolean bbs;
		public ICommunityBoardHandler handler;

		public DecodedBypass(String _bypass, boolean _bbs)
		{
			bypass = _bypass;
			bbs = _bbs;
		}

		public DecodedBypass(String _bypass, ICommunityBoardHandler _handler)
		{
			bypass = _bypass;
			handler = _handler;
		}

		public DecodedBypass trim()
		{
			bypass = bypass.trim();
			return this;
		}
	}
}