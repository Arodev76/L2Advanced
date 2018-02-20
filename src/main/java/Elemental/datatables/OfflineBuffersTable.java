package Elemental.datatables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import Elemental.managers.OfflineBufferManager;
import Elemental.managers.OfflineBufferManager.BufferData;
import l2f.commons.configuration.Config;
import l2f.gameserver.database.DatabaseFactory;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.Skill;

public class OfflineBuffersTable
{
	private static final Logger _log = LoggerFactory.getLogger(OfflineBuffersTable.class);

	/**
	 * Restores all the offline buff stores of the db
	 */
	public void restoreOfflineBuffers()
	{
		_log.info(getClass().getSimpleName() + ": Loading offline buffers...");

		try (Connection con = DatabaseFactory.getInstance().getConnection();
			PreparedStatement st = con.prepareStatement("SELECT * FROM character_offline_buffers WHERE charId > 0");
			ResultSet rs = st.executeQuery())
		{
			int nBuffers = 0;

			while (rs.next())
			{
				Player player = null;

				try
				{
					// Restore character and set it in offline mode
					player = Player.restore(rs.getInt("charId"));
					if (player == null)
						continue;

					player.setOfflineMode(true);
					player.setIsOnline(true);
					player.updateOnlineStatus();

					player.spawnMe();

					if (player.getClan() != null && player.getClan().getAnyMember(player.getObjectId()) != null)
						player.getClan().getAnyMember(player.getObjectId()).setPlayerInstance(player, false);

					// Create the buffer data
					final BufferData buffer = new BufferData(player, rs.getString("title"), rs.getInt("price"), null);

					// Get all the buffs from the db
					try (PreparedStatement stm_items = con.prepareStatement("SELECT * FROM character_offline_buffer_buffs WHERE charId = ?"))
					{
						stm_items.setInt(1, player.getObjectId());
						try (ResultSet skills = stm_items.executeQuery())
						{
							if (skills.next())
							{
								final String[] skillIds = skills.getString("skillIds").split(",");
								for (String skillId : skillIds)
								{
									final Skill skill = player.getKnownSkill(Integer.parseInt(skillId));
									if (skill == null)
										continue;

									buffer.getBuffs().put(skill.getId(), skill);
								}
							}
						}
					}

					// Add the buffer data to the manager
					OfflineBufferManager.getInstance().getBuffStores().put(player.getObjectId(), buffer);

					// Sit the player, put it on store and and change the colors and titles
					player.sitDown(null);

					player.setVisibleTitleColor(Config.BUFF_STORE_TITLE_COLOR);
					player.setVisibleTitle(buffer.getSaleTitle());
					player.setVisibleNameColor(Config.BUFF_STORE_OFFLINE_NAME_COLOR);

					player.setPrivateStoreType(Player.STORE_PRIVATE_BUFF);

					player.broadcastUserInfo(true);
					nBuffers++;

				}
				catch (Exception e)
				{
					_log.warn(getClass().getSimpleName() + ": Error loading buffer: " + player, e);
					if (player != null)
					{
						player.deleteMe();
					}
				}
			}

			_log.info(getClass().getSimpleName() + ": Loaded: " + nBuffers + " offline buffer(s)");
		}
		catch (Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": Error while loading offline buffer: ", e);
		}
	}

	/**
	 * Summoned when a pj loguea to your pj being in a buff store
	 *
	 * @param trader
	 */
	public synchronized void onLogin(Player trader)
	{
		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			// Remove the buffer from the manager
			OfflineBufferManager.getInstance().getBuffStores().remove(trader.getObjectId());

			// We delete the buff store
			try (PreparedStatement st = con.prepareStatement("DELETE FROM character_offline_buffers WHERE charId=?"))
			{
				st.setInt(1, trader.getObjectId());
				st.executeUpdate();
			}

			// We also delete your buffs
			try (PreparedStatement st = con.prepareStatement("DELETE FROM character_offline_buffer_buffs WHERE charId=?"))
			{
				st.setInt(1, trader.getObjectId());
				st.executeUpdate();
			}
		}
		catch (Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": Error while removing offline buffer: " + e, e);
		}
	}

	/**
	 * Summoned when a pj dislodges while being in a buff store
	 *
	 * @param trader
	 */
	public synchronized void onLogout(Player trader)
	{
		final BufferData buffer = OfflineBufferManager.getInstance().getBuffStores().get(trader.getObjectId());
		if (buffer == null)
			return;

		try (Connection con = DatabaseFactory.getInstance().getConnection())
		{
			// We save the offline buffer first
			try (PreparedStatement st = con.prepareStatement("REPLACE INTO character_offline_buffers VALUES (?,?,?)"))
			{
				st.setInt(1, trader.getObjectId());
				st.setInt(2, buffer.getBuffPrice());
				st.setString(3, buffer.getSaleTitle());
				st.executeUpdate();
			}

			// Then we save each buff from the store
			try (PreparedStatement st = con.prepareStatement("REPLACE INTO character_offline_buffer_buffs VALUES (?,?)"))
			{
				st.setInt(1, trader.getObjectId());
				st.setString(2, joinAllSkillsToString(buffer.getBuffs().values()));
				st.executeUpdate();
			}
		}
		catch (Exception e)
		{
			_log.warn(getClass().getSimpleName() + ": Error while saving offline buffer: " + e, e);
		}
	}

	/**
	 * @param skills
	 * @return Join the whole list of buffs so that they are all in a string separated by ,
	 */
	private final String joinAllSkillsToString(Collection<Skill> skills)
	{
		if (skills.isEmpty())
			return "";

		String result = "";
		for (Skill val : skills)
		{
			result += val.getId() + ",";
		}

		return result.substring(0, result.length() - 1);
	}

	public static OfflineBuffersTable getInstance()
	{
		return SingletonHolder._instance;
	}

	private static class SingletonHolder
	{
		protected static final OfflineBuffersTable _instance = new OfflineBuffersTable();
	}
}
