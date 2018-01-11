package ai.hellbound;

import java.util.HashMap;
import java.util.Map;

import l2f.gameserver.ai.Fighter;
import l2f.gameserver.instancemanager.naia.NaiaCoreManager;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.instances.NpcInstance;

public class NaiaSpore extends Fighter
{
	private static Map<Integer, Integer> epidosIndex = new HashMap<Integer, Integer>();

	static
	{
		resetEpidosStats();
	}
	
	public static void resetEpidosStats()
	{
		epidosIndex.put(1, 0);
		epidosIndex.put(2, 0);
		epidosIndex.put(3, 0);
		epidosIndex.put(4, 0);
	}

	public NaiaSpore(NpcInstance actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtDead(Creature killer)
	{
		NpcInstance actor = getActor();
		int npcId = actor.getNpcId();
		switch (npcId)
		{
			//fire
			case 25605:
			{
				epidosIndex.put(1, epidosIndex.get(1) + 1);
				break;
			}
				//water
			case 25606:
			{
				epidosIndex.put(2, epidosIndex.get(2) + 1);
				break;
			}
				//wind
			case 25607:
			{
				epidosIndex.put(3, epidosIndex.get(3) + 1);
				break;
			}
				//earth
			case 25608:
			{
				epidosIndex.put(4, epidosIndex.get(4) + 1);
				break;
			}
			default:
				break;
		}
		
		if (isBossSpawnCondMet() != 0 && !NaiaCoreManager.isBossSpawned())
			NaiaCoreManager.spawnEpidos(isBossSpawnCondMet());

		super.onEvtDead(killer);
	}

	private int isBossSpawnCondMet()
	{
		for (int i = 1; i < 5; i++)
		{
			if (epidosIndex.get(i) >= 100) // 100
			{
				return i;
			}
		}

		return 0;
	}
}