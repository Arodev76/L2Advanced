	package l2f.gameserver.skills.effects;
	
	import l2f.gameserver.ai.CharacterAI;
	import l2f.gameserver.ai.CtrlIntention;
	import l2f.gameserver.cache.Msg;
	import l2f.gameserver.geodata.GeoEngine;
	import l2f.gameserver.model.Creature;
	import l2f.gameserver.model.Effect;
	import l2f.gameserver.model.Player;
	import l2f.gameserver.model.entity.events.impl.SiegeEvent;
	import l2f.gameserver.model.instances.SummonInstance;
	import l2f.gameserver.network.serverpackets.components.SystemMsg;
	import l2f.gameserver.stats.Env;
	import l2f.gameserver.utils.PositionUtils;
	
	public final class EffectDummy2 extends Effect
	{
	  public static final double FEAR_RANGE = 900.0D;
	
	  public EffectDummy2(Env env, EffectTemplate template)
	  {
		  super(env, template);
	  }
	
	  public boolean checkCondition()
	  {
		  if (this._effected.isFearImmune())
		  {
			  getEffector().sendPacket(SystemMsg.THAT_IS_AN_INCORRECT_TARGET);
			  return false;
		  }
	
		  Player player = this._effected.getPlayer();
		  if (player != null)
		  {
			  SiegeEvent siegeEvent = (SiegeEvent)player.getEvent(SiegeEvent.class);
			  if ((this._effected.isSummon()) && (siegeEvent != null) && (siegeEvent.containsSiegeSummon((SummonInstance)this._effected)))
			  {
				  getEffector().sendPacket(SystemMsg.THAT_IS_AN_INCORRECT_TARGET);
				  return false;
			  }
	    }
	
		  if (this._effected.isInZonePeace())
	    {
			getEffector().sendPacket(Msg.YOU_MAY_NOT_ATTACK_IN_A_PEACEFUL_ZONE);
			return false;
	    }
	
		return super.checkCondition();
	  }
	
	  public void onStart()
	  {
		  Player target = (Player)getEffected();
		  if (target.getTransformation() == 303) {
			  return;
		  }
		  super.onStart();
	
		  if (!this._effected.startFear())
		  {
			  this._effected.abortAttack(true, true);
			  this._effected.abortCast(true, true);
			  this._effected.stopMove();
		  }
	
		  onActionTime();
	  }
	
	  public void onExit()
	  {
		  super.onExit();
		  this._effected.stopFear();
		  this._effected.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
	  }
	
	  public boolean onActionTime()
	  {
		  double angle = Math.toRadians(PositionUtils.calculateAngleFrom(this._effector, this._effected));
		int oldX = this._effected.getX();
		int oldY = this._effected.getY();
		int x = oldX + (int)(900.0D * Math.cos(angle));
		int y = oldY + (int)(900.0D * Math.sin(angle));
		this._effected.setRunning();
		this._effected.moveToLocation(GeoEngine.moveCheck(oldX, oldY, this._effected.getZ(), x, y, this._effected.getGeoIndex()), 0, false);
		return true;
	  }
	}