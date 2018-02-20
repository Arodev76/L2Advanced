package l2f.gameserver.skills.effects;
	
import l2f.gameserver.model.Effect;
import l2f.gameserver.model.Player;
import l2f.gameserver.stats.Env;
	
public final class EffectDummy3 extends Effect
{
	  public EffectDummy3(Env env, EffectTemplate template)
	  {
		  super(env, template);
	  }
	
	  public boolean checkCondition()
	  {
		  if (this._effected.isParalyzeImmune())
			  return false;
		  return super.checkCondition();
	  }
	
	  public void onStart()
	  {
		  Player target = (Player)getEffected();
		  if (target.getTransformation() == 303) {
			  return;
	    }
		  super.onStart();
	
		  this._effected.startParalyzed();
		  this._effected.abortAttack(true, true);
		  this._effected.abortCast(true, true);
	  }
	
	  public void onExit()
	  {
		  super.onExit();
		  this._effected.stopParalyzed();
	  }
	
	  public boolean onActionTime()
	  {
		  return false;
	  }
	}