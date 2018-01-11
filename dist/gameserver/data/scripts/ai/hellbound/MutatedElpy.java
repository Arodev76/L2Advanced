package ai.hellbound;

import bosses.BelethManager;
import l2f.gameserver.ai.Fighter;
import l2f.gameserver.instancemanager.naia.NaiaCoreManager;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.Config;
import l2f.gameserver.model.Zone;
import l2f.gameserver.utils.ReflectionUtils;

public class MutatedElpy extends Fighter
{
    public MutatedElpy(NpcInstance actor) 
    {
        super(actor);
        actor.startImmobilized();
    }

    @Override
    protected void onEvtDead(Creature killer)
    {
        NaiaCoreManager.launchNaiaCore();
        BelethManager.setElpyDead();
        super.onEvtDead(killer);
    }

    @Override
    protected void onEvtAttacked(Creature attacker, int damage) 
    {
        NpcInstance actor = getActor();
        actor.doDie(attacker);
    }
}