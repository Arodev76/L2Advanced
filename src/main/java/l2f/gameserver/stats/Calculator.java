package l2f.gameserver.stats;

import l2f.commons.lang.ArrayUtils;
import l2f.gameserver.model.Creature;
import l2f.gameserver.stats.funcs.Func;
import l2f.gameserver.stats.funcs.FuncOwner;

public final class Calculator
{
	private Func[] _functions;
	
	private double _base;
	private double _last;
	
	public final Stats _stat;
	public final Creature _character;
	
	public Calculator(Stats stat, Creature character)
	{
		_stat = stat;
		_character = character;
		_functions = Func.EMPTY_FUNC_ARRAY;
	}
	
	/**
	 * Return the number of Funcs in the Calculator.<BR>
	 * <BR>
	 * @return
	 */
	public int size()
	{
		return _functions.length;
	}
	
	/**
	 * Add a Func to the Calculator.<BR>
	 * <BR>
	 * @param f
	 */
	public void addFunc(Func f)
	{
		_functions = ArrayUtils.add(_functions, f);
		ArrayUtils.eqSort(_functions);
	}
	
	/**
	 * Remove a Func from the Calculator.<BR>
	 * <BR>
	 * @param f
	 */
	public void removeFunc(Func f)
	{
		_functions = ArrayUtils.remove(_functions, f);
		if (_functions.length == 0)
			_functions = Func.EMPTY_FUNC_ARRAY;
		else
			ArrayUtils.eqSort(_functions);
	}
	
	/**
	 * Remove each Func with the specified owner of the Calculator.<BR>
	 * <BR>
	 * @param owner
	 */
	public void removeOwner(Object owner)
	{
		final Func[] tmp = _functions;
		for (final Func element : tmp)
			if (element.owner == owner)
				removeFunc(element);
	}
	
	/**
	 * Run each Func of the Calculator.<BR>
	 * <BR>
	 * @param env
	 */
	@SuppressWarnings("unused")
	public void calc(Env env)
	{
		final Func[] funcs = _functions;
		_base = env.value;
		
		boolean overrideLimits = false;
		for (final Func func : funcs)
		{
			if (func == null)
				continue;
			
			if (func.owner instanceof FuncOwner)
			{
				if (!((FuncOwner) func.owner).isFuncEnabled())
					continue;
				if (((FuncOwner) func.owner).overrideLimits())
					overrideLimits = true;
			}
			if ((func.getCondition() == null) || func.getCondition().test(env))
				func.calc(env);
		}
		
		if (!overrideLimits)
			env.value = _stat.validate(env.value);
		
		if (env.value != _last)
		{
			final double last = _last; // TODO [G1ta0] найти приминение в StatsChangeRecorder
			_last = env.value;
		}
	}
	
	/**
	 * Для отладки
	 * @return
	 */
	public Func[] getFunctions()
	{
		return _functions;
	}
	
	public double getBase()
	{
		return _base;
	}
	
	public double getLast()
	{
		return _last;
	}
}