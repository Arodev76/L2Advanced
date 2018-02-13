package l2f.commons.threading;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author VISTALL
 * @date 03:13/12.02.2018
 */
public abstract class RunnableImpl implements Runnable
{
	public static final Logger _log = LoggerFactory.getLogger(RunnableImpl.class);
	
	public abstract void runImpl() throws Exception;
	
	@Override
	public final void run()
	{
		try
		{
			runImpl();
		}
		catch (final Exception e)
		{
			_log.error("Exception: RunnableImpl.run(): " + e, e);
		}
	}
}
