package l2f.gameserver.ai;

import l2f.gameserver.Config;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Summon;

public class SummonAI extends PlayableAI
{
	public SummonAI(Summon actor)
	{
		super(actor);
	}

	@Override
	protected void thinkActive()
	{
		Summon actor = getActor();

		clearNextAction();
		if (actor.isDepressed())
		{
			setAttackTarget(actor.getPlayer());
			changeIntention(CtrlIntention.AI_INTENTION_ATTACK, actor.getPlayer(), null);
			thinkAttack(true);
		}
		else if (actor.isFollowMode())
		{
			changeIntention(CtrlIntention.AI_INTENTION_FOLLOW, actor.getPlayer(), Config.FOLLOW_RANGE);
			thinkFollow();
		}

		super.thinkActive();
	}

	@Override
	protected void thinkAttack(boolean checkRange)
	{
		Summon actor = getActor();

		if (actor.isDepressed())
			setAttackTarget(actor.getPlayer());

		super.thinkAttack(checkRange);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{
		Summon actor = getActor();
		if (attacker != null && actor.getPlayer().isDead() && !actor.isDepressed())
			Attack(attacker, false, false);
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	public Summon getActor()
	{
		return (Summon) super.getActor();
	}
}