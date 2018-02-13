package l2f.commons.dbutils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import l2f.gameserver.database.DatabaseFactory;

/**
 * A collection of JDBC helper methods. This class is thread safe.
 */
public class DbUtils
{
	private static final Logger _log = LoggerFactory.getLogger(DbUtils.class);
	
	public static boolean set(String query, Object... vars)
	{
		return setEx(query, vars);
	}
	
	/**
	 * Performs a simple sql queries where unnecessary control parameters <BR>
	 * NOTE: In this method, the parameters passed are not valid for SQL-injection!
	 * @param query
	 * @param vars
	 * @return
	 */
	public static boolean setEx(String query, Object... vars)
	{
		Connection con = null;
		Statement statement = null;
		PreparedStatement pstatement = null;
		boolean result = false;
		
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			if (vars.length == 0)
			{
				statement = con.createStatement();
				statement.executeUpdate(query);
			}
			else
			{
				pstatement = con.prepareStatement(query);
				setVars(pstatement, vars);
				
				pstatement.executeUpdate();
			}
			
			result = true;
		}
		catch (final SQLException e)
		{
			e.printStackTrace();
			_log.warn("Could not execute update '" + query + "' ", e);
		}
		finally
		{
			closeQuietly(con, statement);
			closeQuietly(pstatement);
		}
		return result;
	}
	
	public static void setVars(PreparedStatement statement, Object... vars) throws SQLException
	{
		Number n;
		long long_val;
		double double_val;
		for (int i = 0; i < vars.length; i++)
			if (vars[i] instanceof Number)
			{
				n = (Number) vars[i];
				long_val = n.longValue();
				double_val = n.doubleValue();
				if (long_val == double_val)
					statement.setLong(i + 1, long_val);
				else
					statement.setDouble(i + 1, double_val);
			}
			else if (vars[i] instanceof String)
				statement.setString(i + 1, (String) vars[i]);
	}
	
	public static int getTheNumberThatNotExist(String ret_field, String table, String where, int start, int end)
	{
		int number;
		for (number = start; number <= end; number++)
			if (!simple_get_if_exist(ret_field, table, where + "=" + number))
				break;
			
		return number;
	}
	
	public static boolean simple_get_if_exist(String ret_field, String table, String where)
	{
		final String query = "SELECT " + ret_field + " FROM `" + table + "` WHERE " + where + " LIMIT 1;";
		
		boolean res = false;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();
			
			if (rset.next())
				res = true;
		}
		catch (final SQLException e)
		{
			_log.warn("Error in query '" + query + "':", e);
		}
		finally
		{
			closeQuietly(con, statement, rset);
		}
		
		return res;
	}
	
	public static Object get(String query)
	{
		Object ret = null;
		Connection con = null;
		Statement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery(query + " LIMIT 1");
			final ResultSetMetaData md = rset.getMetaData();
			
			if (rset.next())
				if (md.getColumnCount() > 1)
				{
					final Map<String, Object> tmp = new HashMap<>();
					for (int i = md.getColumnCount(); i > 0; i--)
						tmp.put(md.getColumnName(i), rset.getObject(i));
					ret = tmp;
				}
				else
					ret = rset.getObject(1);
				
		}
		catch (final Exception e)
		{
			_log.warn("Could not execute query '" + query + "': " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return ret;
	}
	
	public static List<Map<String, Object>> getAll(String query)
	{
		final List<Map<String, Object>> ret = new ArrayList<>();
		Connection con = null;
		Statement statement = null;
		ResultSet rset = null;
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.createStatement();
			rset = statement.executeQuery(query);
			final ResultSetMetaData md = rset.getMetaData();
			
			while (rset.next())
			{
				final Map<String, Object> tmp = new HashMap<>();
				for (int i = md.getColumnCount(); i > 0; i--)
					tmp.put(md.getColumnName(i), rset.getObject(i));
				ret.add(tmp);
			}
		}
		catch (final Exception e)
		{
			_log.warn("Could not execute query '" + query + "': " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return ret;
	}
	
	public static List<Object> get_array(DatabaseFactory db, String query)
	{
		final List<Object> ret = new ArrayList<>();
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		try
		{
			if (db == null)
				db = DatabaseFactory.getInstance();
			con = db.getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();
			final ResultSetMetaData md = rset.getMetaData();
			
			while (rset.next())
				if (md.getColumnCount() > 1)
				{
					final Map<String, Object> tmp = new HashMap<>();
					for (int i = 0; i < md.getColumnCount(); i++)
						tmp.put(md.getColumnName(i + 1), rset.getObject(i + 1));
					ret.add(tmp);
				}
				else
					ret.add(rset.getObject(1));
		}
		catch (final Exception e)
		{
			_log.warn("Could not execute query '" + query + "': " + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		return ret;
	}
	
	public static List<Object> get_array(String query)
	{
		return get_array(null, query);
	}
	
	public static int simple_get_int(String ret_field, String table, String where)
	{
		final String query = "SELECT " + ret_field + " FROM `" + table + "` WHERE " + where + " LIMIT 1;";
		
		int res = 0;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rset = null;
		
		try
		{
			con = DatabaseFactory.getInstance().getConnection();
			statement = con.prepareStatement(query);
			rset = statement.executeQuery();
			
			if (rset.next())
				res = rset.getInt(1);
		}
		catch (final Exception e)
		{
			_log.warn("mSGI: Error in query '" + query + "':" + e);
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con, statement, rset);
		}
		
		return res;
	}
	
	/**
	 * Close a <code>Connection</code>, avoid closing if null.
	 * @param conn Connection to close.
	 * @throws SQLException if a database access error occurs
	 */
	public static void close(Connection conn) throws SQLException
	{
		if (conn != null)
			conn.close();
	}
	
	/**
	 * Close a <code>ResultSet</code>, avoid closing if null.
	 * @param rs ResultSet to close.
	 * @throws SQLException if a database access error occurs
	 */
	public static void close(ResultSet rs) throws SQLException
	{
		if (rs != null)
			rs.close();
	}
	
	/**
	 * Close a <code>Statement</code>, avoid closing if null.
	 * @param stmt Statement to close.
	 * @throws SQLException if a database access error occurs
	 */
	public static void close(Statement stmt) throws SQLException
	{
		if (stmt != null)
			stmt.close();
	}
	
	/**
	 * Close a <code>Statement</code> and <code>ResultSet</code>, avoid closing if null.
	 * @param stmt Statement to close.
	 * @param rs ResultSet to close.
	 * @throws SQLException if a database access error occurs
	 */
	public static void close(Statement stmt, ResultSet rs) throws SQLException
	{
		close(stmt);
		close(rs);
	}
	
	/**
	 * Close a <code>Connection</code>, avoid closing if null and hide any SQLExceptions that occur.
	 * @param conn Connection to close.
	 */
	public static void closeQuietly(Connection conn)
	{
		try
		{
			close(conn);
		}
		catch (final SQLException e)
		{
			// quiet
		}
	}
	
	/**
	 * Close a <code>Connection</code> and <code>Statement</code>. Avoid closing if null and hide any SQLExceptions that occur.
	 * @param conn Connection to close.
	 * @param stmt Statement to close.
	 */
	public static void closeQuietly(Connection conn, Statement stmt)
	{
		try
		{
			closeQuietly(stmt);
		}
		finally
		{
			closeQuietly(conn);
		}
	}
	
	/**
	 * Close a <code>Statement</code> and <code>ResultSet</code>. Avoid closing if null and hide any SQLExceptions that occur.
	 * @param stmt Statement to close.
	 * @param rs ResultSet to close.
	 */
	public static void closeQuietly(Statement stmt, ResultSet rs)
	{
		try
		{
			closeQuietly(stmt);
		}
		finally
		{
			closeQuietly(rs);
		}
	}
	
	/**
	 * Close a <code>Connection</code>, <code>Statement</code> and <code>ResultSet</code>. Avoid closing if null and hide any SQLExceptions that occur.
	 * @param conn Connection to close.
	 * @param stmt Statement to close.
	 * @param rs ResultSet to close.
	 */
	public static void closeQuietly(Connection conn, Statement stmt, ResultSet rs)
	{
		
		try
		{
			closeQuietly(rs);
		}
		finally
		{
			try
			{
				closeQuietly(stmt);
			}
			finally
			{
				closeQuietly(conn);
			}
		}
	}
	
	/**
	 * Close a <code>ResultSet</code>, avoid closing if null and hide any SQLExceptions that occur.
	 * @param rs ResultSet to close.
	 */
	public static void closeQuietly(ResultSet rs)
	{
		try
		{
			close(rs);
		}
		catch (final SQLException e)
		{
			// quiet
		}
	}
	
	/**
	 * Close a <code>Statement</code>, avoid closing if null and hide any SQLExceptions that occur.
	 * @param stmt Statement to close.
	 */
	public static void closeQuietly(Statement stmt)
	{
		try
		{
			close(stmt);
		}
		catch (final SQLException e)
		{
			// quiet 35214
		}
	}
}
