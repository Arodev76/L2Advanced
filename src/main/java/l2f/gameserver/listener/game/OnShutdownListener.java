package l2f.gameserver.listener.game;

import l2f.gameserver.listener.GameListener;

public interface OnShutdownListener extends GameListener
{
	public void onShutdown();
}
