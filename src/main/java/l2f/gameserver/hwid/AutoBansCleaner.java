package l2f.gameserver.hwid;

import l2f.gameserver.Config;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.utils.TimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Class which cleans auto bans created by Lameguard in file {@value #PATH}{@value #ORIGINAL_FILE_NAME}
 */
public class AutoBansCleaner
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AutoBansCleaner.class);
	private static final String PATH = "lameguard/";
	private static final String ORIGINAL_FILE_NAME = "banned_hwid.txt";
	private static final String TEMP_FILE_NAME = "banned_hwid_temp.txt";
	private static final String BORDER = "********************************************";

	private AutoBansCleaner() {}

	/**
	 * If File Cleaning is allowed, creates new {@link BanCleanerThread) BanCleanerThread}
	 */
	public static void startFileCleaning()
	{
		if (!isAllowed())
			return;
		ThreadPoolManager.getInstance().execute(new BanCleanerThread());
	}

	/**
	 * @return Is Auto Ban Cleaning Allowed
	 */
	private static boolean isAllowed()
	{
		return Config.ALLOW_CLEANING_AUTO_BANS;
	}

	/**
	 * If Cleaning is allowed, {@link #cleanBans() cleaning Bans} and creates new Cleaning Thread
	 */
	private static class BanCleanerThread implements Runnable
	{
		@Override
		public void run()
		{
			if (!isAllowed())
				return;
			cleanBans();
			ThreadPoolManager.getInstance().schedule(this, Config.SECONDS_BETWEEN_AUTO_BAN_CLEANING * TimeUtils.SECOND_IN_MILLIS);
		}

		/**
		 * Creating New Temporary File.
		 * Moving all Data(until and including {@link #BORDER} from original File to Temp File(@link #createTempFile).
		 * Deleting original File and putting Temporary File in it's place(@link #switchFiles).
		 */
		private static void cleanBans()
		{
			File originalFile = new File(PATH+ORIGINAL_FILE_NAME);
			File newFile = new File(PATH+TEMP_FILE_NAME);
			createTempFile(originalFile, newFile);
			switchFiles(originalFile, newFile);
		}

		/**
		 * Moving all Data(until and including {@link #BORDER} from original File to Temp File
		 * @param originalFile File to get data from
		 * @param tempFile File to put data
		 */
		private static void createTempFile(File originalFile, File tempFile)
		{
			try (BufferedReader br = new BufferedReader(new FileReader(originalFile));
			    PrintWriter pw = new PrintWriter(new FileWriter(tempFile)))
			{
				boolean foundBorder = false;
				String line = br.readLine();

				while (line != null)
				{
					if (!foundBorder)
					{
						pw.println(line);
						pw.flush();
						if (line.contains(BORDER))
							foundBorder = true;
					}
					line = br.readLine();
				}
			}
			catch (IOException e)
			{
				LOGGER.error("Error while Creating new Auto Ban File: ", e);
			}
		}

		/**
		 * Deleting original File and putting Temporary File in it's place
		 * @param original File to Remove
		 * @param newFile File to Change Name
		 */
		private static void switchFiles(File original, File newFile)
		{
			if (!original.delete())
			{
				LOGGER.error("Error while Deleting old Auto Ban File");
			}
			if (!newFile.renameTo(original))
			{
				LOGGER.error("Error while Renaming old Auto Ban File to New One");
			}
		}
	}
}
