package l2f.gameserver.model.entity.events.objects;

import l2f.gameserver.model.entity.events.GlobalEvent;

import java.io.Serializable;

public interface SpawnableObject extends Serializable
{
	void spawnObject(GlobalEvent event);

	void despawnObject(GlobalEvent event);

	void refreshObject(GlobalEvent event);
}
