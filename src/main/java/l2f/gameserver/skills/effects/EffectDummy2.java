package l2f.gameserver.skills.effects;

import l2f.gameserver.ai.CtrlIntention;
import l2f.gameserver.cache.Msg;
import l2f.gameserver.geodata.GeoEngine;
import l2f.gameserver.model.Effect;
import l2f.gameserver.model.Player;
import l2f.gameserver.model.entity.events.impl.SiegeEvent;
import l2f.gameserver.model.instances.SummonInstance;
import l2f.gameserver.network.serverpackets.components.SystemMsg;
import l2f.gameserver.stats.Env;
import l2f.gameserver.utils.PositionUtils;

public final class EffectDummy2 extends Effect
{
    public static final double FEAR_RANGE = 900.0;
    
    public EffectDummy2(final Env env, final EffectTemplate template) {
        super(env, template);
    }
    
    @Override
    public boolean checkCondition() {
        if (this._effected.isFearImmune()) {
            this.getEffector().sendPacket(SystemMsg.THAT_IS_AN_INCORRECT_TARGET);
            return false;
        }
        final Player player = this._effected.getPlayer();
        if (player != null) {
            final SiegeEvent<?, ?> siegeEvent = player.getEvent(SiegeEvent.class);
            if (this._effected.isSummon() && siegeEvent != null && siegeEvent.containsSiegeSummon((SummonInstance)this._effected)) {
                this.getEffector().sendPacket(SystemMsg.THAT_IS_AN_INCORRECT_TARGET);
                return false;
            }
        }
        if (this._effected.isInZonePeace()) {
            this.getEffector().sendPacket(Msg.YOU_MAY_NOT_ATTACK_IN_A_PEACEFUL_ZONE);
            return false;
        }
        return super.checkCondition();
    }
    
    @Override
	public void onStart() {
        final Player target = (Player)this.getEffected();
        if (target.getTransformation() == 303) {
            return;
        }
        super.onStart();
        if (!this._effected.startFear()) {
            this._effected.abortAttack(true, true);
            this._effected.abortCast(true, true);
            this._effected.stopMove();
        }
        this.onActionTime();
    }
    
    @Override
	public void onExit() {
        super.onExit();
        this._effected.stopFear();
        this._effected.getAI().setIntention(CtrlIntention.AI_INTENTION_ACTIVE);
    }
    
    @Override
	public boolean onActionTime() {
        final double angle = Math.toRadians(PositionUtils.calculateAngleFrom(this._effector, this._effected));
        final int oldX = this._effected.getX();
        final int oldY = this._effected.getY();
        final int x = oldX + (int)(900.0 * Math.cos(angle));
        final int y = oldY + (int)(900.0 * Math.sin(angle));
        this._effected.setRunning();
        this._effected.moveToLocation(GeoEngine.moveCheck(oldX, oldY, this._effected.getZ(), x, y, this._effected.getGeoIndex()), 0, false);
        return true;
    }
}
