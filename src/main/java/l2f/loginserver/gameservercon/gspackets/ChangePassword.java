package l2f.loginserver.gameservercon.gspackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Logger;

import l2f.commons.dbutils.DbUtils;
import l2f.loginserver.database.L2DatabaseFactory;
import l2f.loginserver.gameservercon.ReceivablePacket;
import l2f.loginserver.gameservercon.lspackets.ChangePasswordResponse;

public class ChangePassword extends ReceivablePacket
{
	private static final Logger log = Logger.getLogger(ChangePassword.class.getName());

	private String accname;
	String oldPass;
	String newPass;
	String hwid;
	
	@Override
	protected void readImpl()
	{
		accname = readS();
		oldPass = readS();
		newPass = readS();
		hwid = readS();
	}
	
	@Override
	protected void runImpl()
	{
		String dbPassword = null;
		Connection con = null;
		PreparedStatement statement = null;
		ResultSet rs = null;
		try
		{
			con = L2DatabaseFactory.getInstance().getConnection();
			try
			{
				statement = con.prepareStatement("SELECT * FROM accounts WHERE login = ?");
				statement.setString(1, accname);
				rs = statement.executeQuery();
				if (rs.next())
					dbPassword = rs.getString("password");
			}
			catch (Exception e)
			{
				log.warning("Can't recive old password for account " + accname + ", exciption :" + e);
			}
			finally
			{
				DbUtils.closeQuietly(statement, rs);
			}

			//Encode old password and compare it to sended one, send packet to determine changed or not.
			try
			{
				if (!oldPass.equals(dbPassword))
				{
					ChangePasswordResponse cp1 = new ChangePasswordResponse(accname, false);
					sendPacket(cp1);
				}
				else
				{
					statement = con.prepareStatement("UPDATE accounts SET password = ? WHERE login = ?");
					statement.setString(1, newPass);
					statement.setString(2, accname);
					int result = statement.executeUpdate();
					
					ChangePasswordResponse cp1 = new ChangePasswordResponse(accname, result != 0);
					sendPacket(cp1);
				}
			}
			catch (Exception e1)
			{
				e1.printStackTrace();
			}
			finally
			{
				DbUtils.closeQuietly(statement);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			DbUtils.closeQuietly(con);
		}
	}
}
