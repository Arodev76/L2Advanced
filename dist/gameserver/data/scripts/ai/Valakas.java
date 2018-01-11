package ai;

import java.util.HashMap;
import java.util.Map;

import l2f.commons.threading.RunnableImpl;
import l2f.commons.util.Rnd;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.ai.CtrlEvent;
import l2f.gameserver.ai.Fighter;
import l2f.gameserver.model.Creature;
import l2f.gameserver.model.Playable;
import l2f.gameserver.model.Skill;
import l2f.gameserver.model.Zone;
import l2f.gameserver.model.instances.NpcInstance;
import l2f.gameserver.network.serverpackets.components.NpcString;
import l2f.gameserver.tables.SkillTable;
import bosses.ValakasManager;

/**
 * @author pchayka
 */

public class Valakas extends Fighter
{
	// Self skills
	final Skill s_lava_skin = getSkill(4680, 1), s_fear = getSkill(4689, 1), s_defence_down = getSkill(5864, 1), s_berserk = getSkill(5865, 1), s_regen = getSkill(4691, 1);

	// Offensive damage skills
	final Skill s_tremple_left = getSkill(4681, 1), s_tremple_right = getSkill(4682, 1), s_tail_stomp_a = getSkill(4685, 1), s_tail_lash = getSkill(4688, 1), s_meteor = getSkill(4690, 1), s_breath_low = getSkill(4683, 1), s_breath_high = getSkill(4684, 1);

	// Offensive percentage skills
	final Skill s_destroy_body = getSkill(5860, 1), s_destroy_soul = getSkill(5861, 1); /* s_destroy_body2 = getSkill(5862, 1), s_destroy_soul2 = getSkill(5863, 1) */

	// Timers
	private long defenceDownTimer = Long.MAX_VALUE;

	// Timer reuses
	private final long defenceDownReuse = 120000L;

	// Vars
	private double _rangedAttacksIndex, _counterAttackIndex, _attacksIndex;
	private int _hpStage = 0;
	private int DAMAGE_COUNTER = 0;
	
	public Valakas(NpcInstance actor)
	{
		super(actor);
	}

	@Override
	protected void onEvtAttacked(Creature attacker, int damage)
	{

		NpcInstance actor = getActor();
		if (DAMAGE_COUNTER == 0)
			actor.getAI().startAITask();
		
		ValakasManager.setLastAttackTime();
		for (Playable p : ValakasManager.getZone().getInsidePlayables()) {
			notifyEvent(CtrlEvent.EVT_AGGRESSION, p, 1);
		}
		if (damage > 100)
		{
			if (attacker.getDistance(actor) > 400)
				_rangedAttacksIndex += damage / 1000D;
			else
				_counterAttackIndex += damage / 1000D;
		}
		_attacksIndex += damage / 1000D;
		DAMAGE_COUNTER += damage;
		
		super.onEvtAttacked(attacker, damage);
	}

	@Override
	protected boolean createNewTask()
	{
		clearTasks();
		Creature target;
		if ((target = prepareTarget()) == null)
			return false;

		NpcInstance actor = getActor();
		if (actor.isDead())
			return false;

		double distance = actor.getDistance(target);

		// Buffs and stats
		double chp = actor.getCurrentHpPercents();
		if (_hpStage == 0)
		{
			actor.altOnMagicUseTimer(actor, getSkill(4691, 1));
			_hpStage = 1;
		}
		else if (chp < 80 && _hpStage == 1)
		{
			actor.altOnMagicUseTimer(actor, getSkill(4691, 2));
			defenceDownTimer = System.currentTimeMillis();
			_hpStage = 2;
		}
		else if (chp < 50 && _hpStage == 2)
		{
			actor.altOnMagicUseTimer(actor, getSkill(4691, 3));
			_hpStage = 3;
		}
		else if (chp < 30 && _hpStage == 3)
		{
			actor.altOnMagicUseTimer(actor, getSkill(4691, 4));
			_hpStage = 4;
		}
		else if (chp < 10 && _hpStage == 4)
		{
			actor.altOnMagicUseTimer(actor, getSkill(4691, 5));
			_hpStage = 5;
		}


		// Tactical Movements
		if (_counterAttackIndex > 2000)
		{
			ValakasManager.broadcastScreenMessage(NpcString.VALAKAS_HEIGHTENED_BY_COUNTERATTACKS);
			_counterAttackIndex = 0;
			return chooseTaskAndTargets(s_berserk, actor, 0);
		}
		else if (_rangedAttacksIndex > 2000)
		{
			if (Rnd.chance(60))
			{
				Creature randomHated = actor.getAggroList().getRandomHated();
				if (randomHated != null)
				{
					setAttackTarget(randomHated);
					actor.startConfused();
					ThreadPoolManager.getInstance().schedule(new RunnableImpl()
					{
						@SuppressWarnings("unused")
						@Override
						public void runImpl() throws Exception
						{
							NpcInstance actor = getActor();
							if (actor != null)
								actor.stopConfused();
							_madnessTask = null;
						}
					}, 20000L);
				}
				ValakasManager.broadcastScreenMessage(NpcString.VALAKAS_RANGED_ATTACKS_ENRAGED_TARGET_FREE);
				_rangedAttacksIndex = 0;
			}
			else
			{
				ValakasManager.broadcastScreenMessage(NpcString.VALAKAS_RANGED_ATTACKS_PROVOKED);
				_rangedAttacksIndex = 0;
				return chooseTaskAndTargets(s_berserk, actor, 0);
			}
		}
		else if (_attacksIndex > 3000)
		{
			ValakasManager.broadcastScreenMessage(NpcString.VALAKAS_PDEF_ISM_DECREACED_SLICED_DASH);
			_attacksIndex = 0;
			return chooseTaskAndTargets(s_defence_down, actor, 0);
		}
		else if (defenceDownTimer < System.currentTimeMillis())
		{
			ValakasManager.broadcastScreenMessage(NpcString.VALAKAS_FINDS_YOU_ATTACKS_ANNOYING_SILENCE);
			defenceDownTimer = System.currentTimeMillis() + defenceDownReuse + Rnd.get(60) * 1000L;
			return chooseTaskAndTargets(s_fear, target, distance);
		}

		// Basic Attack
		if (Rnd.chance(50))
			return chooseTaskAndTargets(Rnd.chance(50) ? s_tremple_left : s_tremple_right, target, distance);

		// Stage based skill attacks
		Map<Skill, Integer> d_skill = new HashMap<Skill, Integer>();
		switch (_hpStage)
		{
			case 1:
				addDesiredSkill(d_skill, target, distance, s_breath_low);
				addDesiredSkill(d_skill, target, distance, s_tail_stomp_a);
				addDesiredSkill(d_skill, target, distance, s_meteor);
				addDesiredSkill(d_skill, target, distance, s_fear);
				break;
			case 2:
			case 3:
				addDesiredSkill(d_skill, target, distance, s_breath_low);
				addDesiredSkill(d_skill, target, distance, s_tail_stomp_a);
				addDesiredSkill(d_skill, target, distance, s_breath_high);
				addDesiredSkill(d_skill, target, distance, s_tail_lash);
				addDesiredSkill(d_skill, target, distance, s_destroy_body);
				addDesiredSkill(d_skill, target, distance, s_destroy_soul);
				addDesiredSkill(d_skill, target, distance, s_meteor);
				addDesiredSkill(d_skill, target, distance, s_fear);
				break;
			case 4:
			case 5:
				addDesiredSkill(d_skill, target, distance, s_breath_low);
				addDesiredSkill(d_skill, target, distance, s_tail_stomp_a);
				addDesiredSkill(d_skill, target, distance, s_breath_high);
				addDesiredSkill(d_skill, target, distance, s_tail_lash);
				addDesiredSkill(d_skill, target, distance, s_destroy_body);
				addDesiredSkill(d_skill, target, distance, s_destroy_soul);
				addDesiredSkill(d_skill, target, distance, s_meteor);
				addDesiredSkill(d_skill, target, distance, s_fear);
				// addDesiredSkill(d_skill, target, distance, Rnd.chance(60) ? s_destroy_soul2 : s_destroy_body);
				break;
		}

		Skill r_skill = selectTopSkill(d_skill);
		if (r_skill != null && !r_skill.isOffensive())
			target = actor;

		return chooseTaskAndTargets(r_skill, target, distance);
	}

	@Override
	protected void thinkAttack()
	{
		NpcInstance actor = getActor();
		// Lava buff
		if (actor.isInZone(Zone.ZoneType.poison))
			if (actor.getEffectList() != null && actor.getEffectList().getEffectsBySkill(s_lava_skin) == null)
				actor.altOnMagicUseTimer(actor, s_lava_skin);
		super.thinkAttack();
	}

	private Skill getSkill(int id, int level)
	{
		return SkillTable.getInstance().getInfo(id, level);
	}
}