package l2f.gameserver.model.entity.events.objects;


import l2f.gameserver.model.entity.events.GlobalEvent;

import java.io.Serializable;

public interface InitableObject extends Serializable
{
	void initObject(GlobalEvent e);
}
