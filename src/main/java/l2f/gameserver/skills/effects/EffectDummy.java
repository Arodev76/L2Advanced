package l2f.gameserver.skills.effects;

import l2f.gameserver.model.Effect;
import l2f.gameserver.model.Player;
import l2f.gameserver.stats.Env;
/*    */ 
public class EffectDummy extends Effect
{
	public EffectDummy(Env env, EffectTemplate template)
	{
		super(env, template);
	}

	public void onStart()
	{
		Player target = (Player)getEffected();
		if (target.getTransformation() == 303) {
			return;
		}
		super.onStart();
	}

	public void onExit()
	{
		super.onExit();
	}
 
	public boolean onActionTime()
	{
		return false;
	}
}