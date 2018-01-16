package l2f.commons.lang;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javolution.text.TextBuilder;
import l2f.gameserver.Config;

public class StringUtils
{
	private static final Logger _log = LoggerFactory.getLogger(StringUtils.class);
	
	public static String stripSlashes(String s)
	{
		if (s == null)
			return "";
		s = s.replace("\\'", "'");
		s = s.replace("\\\\", "\\");
		return s;
	}
	
	// TODO endure this nonsense
	public static Boolean parseBoolean(Object x)
	{
		if (x == null)
			return false;
		
		if (x instanceof Number)
			return ((Number) x).intValue() > 0;
		
		if (x instanceof Boolean)
			return (Boolean) x;
		
		if (x instanceof Double)
			return Math.abs((Double) x) < 0.00001;
		
		return !String.valueOf(x).isEmpty();
	}
	
	private static String[] tr;
	private static String[] trb;
	private static String[] trcode;
	
	public static void reload()
	{
		try
		{
			String[] pairs = FileUtils.readFileToString(new File(Config.DATAPACK_ROOT, "data/string/translit.txt")).split("\n");
			tr = new String[pairs.length * 2];
			for (int i = 0; i < pairs.length; i++)
			{
				final String[] ss = pairs[i].split(" +");
				tr[i * 2] = ss[0];
				tr[(i * 2) + 1] = ss[1];
			}
			
			pairs = FileUtils.readFileToString(new File(Config.DATAPACK_ROOT, "data/string/translit_back.txt")).split("\n");
			trb = new String[pairs.length * 2];
			for (int i = 0; i < pairs.length; i++)
			{
				final String[] ss = pairs[i].split(" +");
				trb[i * 2] = ss[0];
				trb[(i * 2) + 1] = ss[1];
			}
			
			pairs = FileUtils.readFileToString(new File(Config.DATAPACK_ROOT, "data/string/transcode.txt")).split("\n");
			trcode = new String[pairs.length * 2];
			for (int i = 0; i < pairs.length; i++)
			{
				final String[] ss = pairs[i].split(" +");
				trcode[i * 2] = ss[0];
				trcode[(i * 2) + 1] = ss[1];
			}
		}
		catch (final IOException e)
		{
			_log.error("", e);
		}
		_log.info("Loaded " + (tr.length + tr.length + trcode.length) + " translit entries.");
	}
	
	public static String fromTranslit(String s, int type)
	{
		if (type == 1)
			for (int i = 0; i < trb.length; i += 2)
				s = s.replace(trb[i], trb[i + 1]);
		else if (type == 2)
			for (int i = 0; i < trcode.length; i += 2)
				s = s.replace(trcode[i], trcode[i + 1]);
			
		return s;
	}
	
	public static String replace(String str, String regex, int flags, String replace)
	{
		return Pattern.compile(regex, flags).matcher(str).replaceAll(replace);
	}
	
	public static boolean matches(String str, String regex, int flags)
	{
		return Pattern.compile(regex, flags).matcher(str).matches();
	}
	
	public static String bbParse(String s)
	{
		if (s == null)
			return null;
		
		s = s.replace("\r", "");
		s = s.replaceAll("(\\s|\"|\'|\\(|^|\n)\\*(.*?)\\*(\\s|\"|\'|\\)|\\?|\\.|!|:|;|,|$|\n)", "$1<font color=\"LEVEL\">$2</font>$3"); // *S1*
		s = s.replaceAll("(\\s|\"|\'|\\(|^|\n)\\$(.*?)\\$(\\s|\"|\'|\\)|\\?|\\.|!|:|;|,|$|\n)", "$1<font color=\"00FFFF\">$2</font>$3");// $S1$
		s = replace(s, "^!(.*?)$", Pattern.MULTILINE, "<font color=\"FFFFFF\">$1</font>\n\n");
		s = s.replaceAll("%%\\s*\n", "<br1>");
		s = s.replaceAll("\n\n+", "<br>");
		s = replace(s, "\\[([^\\]\\|]*?)\\|([^\\]]*?)\\]", Pattern.DOTALL, "<br1><a action=\"bypass -h $1\">$2</a>");
		s = s.replaceAll(" @", "\" msg=\"");
		
		return s;
	}
	
	/***
	 * Skleivalka for strings
	 * @param glueStr - string delimiter can be an empty string or null
	 * @param strings - the array of strings that need to be glued
	 * @param startIdx - the starting index, if you specify a negative it will be taken away from the number of rows
	 * @param maxCount - maximum elements if 0 - return an empty string if it is negative then take account of what is not
	 * @return
	 */
	public static String joinStrings(String glueStr, String[] strings, int startIdx, int maxCount)
	{
		String result = "";
		if (startIdx < 0)
		{
			startIdx += strings.length;
			if (startIdx < 0)
				return result;
		}
		while ((startIdx < strings.length) && (maxCount != 0))
		{
			if (!result.isEmpty() && (glueStr != null) && !glueStr.isEmpty())
				result += glueStr;
			result += strings[startIdx++];
			maxCount--;
		}
		return result;
	}
	
	/***
	 * Skleivalka for strings
	 * @param glueStr - string delimiter can be an empty string or null
	 * @param strings - the array of strings that need to be glued
	 * @param startIdx - the starting index, if you specify a negative it will be taken away from the number of rows
	 * @return
	 */
	public static String joinStrings(String glueStr, String[] strings, int startIdx)
	{
		return joinStrings(glueStr, strings, startIdx, -1);
	}
	
	/***
	 * Skleivalka for strings
	 * @param glueStr - string delimiter can be an empty string or null
	 * @param strings - the array of strings that need to be glued
	 * @return
	 */
	public static String joinStrings(String glueStr, String[] strings)
	{
		return joinStrings(glueStr, strings, 0);
	}
	
	public static String stripToSingleLine(String s)
	{
		if (s.isEmpty())
			return s;
		s = s.replaceAll("\\\\n", "\n");
		final int i = s.indexOf("\n");
		if (i > -1)
			s = s.substring(0, i);
		return s;
	}
	
	public static String htmlButton(String value, String action, int width)
	{
		return htmlButton(value, action, width, 22);
	}
	
	public static String htmlButton(String value, String action, int width, int height)
	{
		return String.format("<button value=\"%s\" action=\"%s\" back=\"L2UI_CT1.Button_DF_Small_Down\" width=%d height=%d fore=\"L2UI_CT1.Button_DF_Small\">", value, action, width, height);
	}
	
	/**
	 * Appends strings to existing string builder.
	 * @param sbString string builder
	 * @param strings strings to be appended
	 */
	public static void append(final StringBuilder sbString, final String... strings)
	{
		sbString.ensureCapacity(sbString.length() + getLength(strings));
		
		for (final String string : strings)
		{
			sbString.append(string);
		}
	}
	
	/**
	 * Counts total length of all the strings.
	 * @param strings array of strings
	 * @return total length of all the strings
	 */
	private static int getLength(final String[] strings)
	{
		int length = 0;
		
		for (final String string : strings)
		{
			if (string == null)
				length += 4;
			else
				length += string.length();
		}
		
		return length;
	}

	/**
	 * Concatenates strings.
	 * @param strings strings to be concatenated
	 * @return concatenated string
	 */
	public static String concat(final String... strings)
	{
		final TextBuilder sbString = TextBuilder.newInstance();
		
		for (final String string : strings)
		{
			sbString.append(string);
		}
		
		String result = sbString.toString();
		TextBuilder.recycle(sbString);
		return result;
	}
	
	/**
	 * Appends objects to an existing StringBuilder.
	 * @param sb : the StringBuilder to edit.
	 * @param content : parameters to append.
	 */
	public static void append(StringBuilder sb, Object... content)
	{
		for (Object obj : content)
			sb.append((obj == null) ? null : obj.toString());
	}
}