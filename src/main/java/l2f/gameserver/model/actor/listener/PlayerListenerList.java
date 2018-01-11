package l2f.gameserver.model.actor.listener;

import l2f.commons.listener.Listener;
import l2f.gameserver.listener.actor.player.OnPlayerEnterListener;
import l2f.gameserver.listener.actor.player.OnPlayerExitListener;
import l2f.gameserver.listener.actor.player.OnPlayerPartyInviteListener;
import l2f.gameserver.listener.actor.player.OnPlayerPartyLeaveListener;
import l2f.gameserver.listener.actor.player.OnQuestionMarkListener;
import l2f.gameserver.listener.actor.player.OnTeleportListener;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.Reflection;

public class PlayerListenerList extends CharListenerList
{
	public PlayerListenerList(Player actor)
	{
		super(actor);
	}

	@Override
	public Player getActor()
	{
		return (Player) actor;
	}

	public void onEnter()
	{
		if (!global.getListeners().isEmpty())
			for (Listener<Creature> listener : global.getListeners())
				if (OnPlayerEnterListener.class.isInstance(listener))
					((OnPlayerEnterListener) listener).onPlayerEnter(getActor());

		if (!getListeners().isEmpty())
			for (Listener<Creature> listener : getListeners())
				if (OnPlayerEnterListener.class.isInstance(listener))
					((OnPlayerEnterListener) listener).onPlayerEnter(getActor());
	}
	
	public void onExit()
	{
		if (!global.getListeners().isEmpty())
			for (Listener<Creature> listener : global.getListeners())
				if (OnPlayerExitListener.class.isInstance(listener))
					((OnPlayerExitListener) listener).onPlayerExit(getActor());

		if (!getListeners().isEmpty())
			for (Listener<Creature> listener : getListeners())
				if (OnPlayerExitListener.class.isInstance(listener))
					((OnPlayerExitListener) listener).onPlayerExit(getActor());
	}

	public void onTeleport(int x, int y, int z, Reflection reflection)
	{
		if (!global.getListeners().isEmpty())
			for (Listener<Creature> listener : global.getListeners())
				if (OnTeleportListener.class.isInstance(listener))
					((OnTeleportListener) listener).onTeleport(getActor(), x, y, z, reflection);

		if (!getListeners().isEmpty())
			for (Listener<Creature> listener : getListeners())
				if (OnTeleportListener.class.isInstance(listener))
					((OnTeleportListener) listener).onTeleport(getActor(), x, y, z, reflection);
	}

	public void onPartyInvite()
	{
		if (!global.getListeners().isEmpty())
			for (Listener<Creature> listener : global.getListeners())
				if (OnPlayerPartyInviteListener.class.isInstance(listener))
					((OnPlayerPartyInviteListener) listener).onPartyInvite(getActor());

		if (!getListeners().isEmpty())
			for (Listener<Creature> listener : getListeners())
				if (OnPlayerPartyInviteListener.class.isInstance(listener))
					((OnPlayerPartyInviteListener) listener).onPartyInvite(getActor());
	}

	public void onPartyLeave()
	{
		if (!global.getListeners().isEmpty())
			for (Listener<Creature> listener : global.getListeners())
				if (OnPlayerPartyLeaveListener.class.isInstance(listener))
					((OnPlayerPartyLeaveListener) listener).onPartyLeave(getActor());

		if (!getListeners().isEmpty())
			for (Listener<Creature> listener : getListeners())
				if (OnPlayerPartyLeaveListener.class.isInstance(listener))
					((OnPlayerPartyLeaveListener) listener).onPartyLeave(getActor());
	}
	
	public void onQuestionMarkClicked(int questionMarkId)
	{
		if (!global.getListeners().isEmpty())
			for (Listener<Creature> listener : global.getListeners())
				if (OnQuestionMarkListener.class.isInstance(listener))
					((OnQuestionMarkListener) listener).onQuestionMarkClicked(getActor(), questionMarkId);
		
		if (!getListeners().isEmpty())
			for (Listener<Creature> listener : getListeners())
				if (OnQuestionMarkListener.class.isInstance(listener))
					((OnQuestionMarkListener) listener).onQuestionMarkClicked(getActor(), questionMarkId);
	}
}
