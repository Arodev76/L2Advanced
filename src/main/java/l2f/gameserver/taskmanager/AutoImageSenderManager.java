package l2f.gameserver.taskmanager;

import l2f.commons.configuration.Config;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.cache.ImagesCache;
import l2f.gameserver.model.GameObjectsStorage;
import l2f.gameserver.model.Player;

/**
 * Class containing Thread which sends most important images to the server with {@value #DELAY_BETWEEN_PICTURE} delay between each image
 */
public class AutoImageSenderManager
{
	//protected static final int[] IMAGES_SENT_ORDER = { 9011, 9012, 9013, 9021, 9022, 9023, 9811, 9812, 9813, 9821, 9822, 9823, 9311, 9312, 9313, 9321, 9322, 9323, 9211, 
	//													 9212, 9213, 9221, 9222, 9223, 9111, 9112, 9113, 9121, 9122, 9123, 9511, 9512, 9513, 9521, 9522, 9523, 9611, 9612, 
	//													 9613, 9621, 9622, 9623, 9911, 9912, 9913, 9921, 9922, 9923, 9711, 9712, 9713, 9721, 9722, 9723, };
	protected static final int[] IMAGES_SENT_ORDER = { 10000, 10001, 10002 };

	private static final long DELAY_BETWEEN_PICTURE = 1000L;

	private AutoImageSenderManager() 
	{
		// Clean
	}

	/**
	 * Checking if <code>imageId</code> is sent automatically from this class, or it should be sent in real time
	 * If image is sent automatically and player didn't receive it yet, he needs to wait.
	 * @param imageId Id of requested Image
	 * @return should player wait for the Image Thread?
	 */
	public static boolean isImageAutoSendable(int imageId)
	{
		for (int spendableId : IMAGES_SENT_ORDER)
		{
			if (spendableId == imageId)
				return true;
		}
		return false;
	}

	/**
	 * Checking if All Required images to watch Community Board were sent to the Player
	 * @param player that could receive Images
	 * @return were those images sent already?
	 */
	public static boolean wereAllImagesSent(Player player)
	{
		return !Config.ALLOW_SENDING_IMAGES || player.getLoadedImagesSize() >= IMAGES_SENT_ORDER.length;
	}

	/**
	 * Starting a Thread which sends Images to every player that didn't receive them yet
	 */
	public static void startSendingImages()
	{
		ThreadPoolManager.getInstance().schedule(new ImageSendThread(), DELAY_BETWEEN_PICTURE);
	}

	protected static class ImageSendThread implements Runnable
	{
		@Override
		public void run()
		{
			if (Config.ALLOW_SENDING_IMAGES && Config.COMMUNITYBOARD_ENABLED)
			{
				final Iterable<Player> players = GameObjectsStorage.getAllPlayersForIterate();
				for (Player player : players)
				{
					if (player != null && player.isOnline())//Check in case of No-Carrier System
					{
						int pictureToLoad = getNextPicture(player);

						if (pictureToLoad != -1)
						{
							ImagesCache.getInstance().sendImageToPlayer(player, pictureToLoad);
						}
					}
				}
			}

			ThreadPoolManager.getInstance().schedule(new ImageSendThread(), DELAY_BETWEEN_PICTURE);
		}

		/**
		 * If player didn't receive every Image yet, getting next Image Id to receive from {@link #IMAGES_SENT_ORDER} array
		 * @param player that will probably receive Image
		 * @return next Image Id. In case all images loaded: -1
		 */
		private static int getNextPicture(Player player)
		{
			if (wereAllImagesSent(player))
				return -1;

			for (int imageId : IMAGES_SENT_ORDER)
			{
				if (!player.wasImageLoaded(imageId))
					return imageId;
			}

			player.addQuickVar("AllImagesLoaded", true);
			return -1;
		}
	}
}
