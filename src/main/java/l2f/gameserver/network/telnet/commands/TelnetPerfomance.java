package l2f.gameserver.network.telnet.commands;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.MBeanServer;

import org.apache.commons.io.FileUtils;

import l2f.gameserver.Config;
import l2f.commons.lang.StatsUtils;
import l2f.commons.net.nio.impl.SelectorThread;
import l2f.commons.threading.RunnableStatsManager;
import l2f.gameserver.ThreadPoolManager;
import l2f.gameserver.geodata.PathFindBuffers;
import l2f.gameserver.network.telnet.TelnetCommand;
import l2f.gameserver.network.telnet.TelnetCommandHolder;
import l2f.gameserver.taskmanager.AiTaskManager;
import l2f.gameserver.taskmanager.EffectTaskManager;

public class TelnetPerfomance implements TelnetCommandHolder
{
	private final Set<TelnetCommand> _commands = new LinkedHashSet<>();
	
	public TelnetPerfomance()
	{
		_commands.add(new TelnetCommand("pool", "p")
		{
			@Override
			public String getUsage()
			{
				return "pool [dump]";
			}
			
			@Override
			public String handle(String[] args)
			{
				StringBuilder sb = new StringBuilder();
				
				if ((args.length == 0) || args[0].isEmpty())
				{
					sb.append(ThreadPoolManager.getInstance().getStats());
				}
				else if (args[0].equals("dump") || args[0].equals("d"))
				{
					try
					{
						new File("stats").mkdir();
						FileUtils.writeStringToFile(new File("stats/RunnableStats-" + new SimpleDateFormat("MMddHHmmss").format(System.currentTimeMillis()) + ".txt"), RunnableStatsManager.getInstance().getStats().toString());
						sb.append("Runnable stats saved.\n");
					}
					catch (IOException e)
					{
						sb.append("Exception: " + e.getMessage() + "!\n");
					}
				}
				else
				{
					return null;
				}
				
				return sb.toString();
			}
			
		});
		
		_commands.add(new TelnetCommand("mem", "m")
		{
			@Override
			public String getUsage()
			{
				return "mem";
			}
			
			@Override
			public String handle(String[] args)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(StatsUtils.getMemUsage());
				
				return sb.toString();
			}
		});
		
		_commands.add(new TelnetCommand("heap")
		{
			
			@Override
			public String getUsage()
			{
				return "heap [dump] <live>";
			}
			
			@Override
			public String handle(String[] args)
			{
				StringBuilder sb = new StringBuilder();
				
				if ((args.length == 0) || args[0].isEmpty())
				{
					return null;
				}
				else if (args[0].equals("dump") || args[0].equals("d"))
				{
					try
					{
						boolean live = (args.length == 2) && !args[1].isEmpty() && (args[1].equals("live") || args[1].equals("l"));
						new File("dumps").mkdir();
						@SuppressWarnings("unused")
						String filename = "dumps/HeapDump" + (live ? "Live" : "") + "-" + new SimpleDateFormat("MMddHHmmss").format(System.currentTimeMillis()) + ".hprof";
						
						@SuppressWarnings("unused")
						MBeanServer server = ManagementFactory.getPlatformMBeanServer();
						
						sb.append("Heap dumped.\n");
					}
					catch (Exception e)
					{
						sb.append("Exception: " + e.getMessage() + "!\n");
					}
				}
				else
				{
					return null;
				}
				
				return sb.toString();
			}
			
		});
		_commands.add(new TelnetCommand("threads", "t")
		{
			@Override
			public String getUsage()
			{
				return "threads [dump]";
			}
			
			@Override
			public String handle(String[] args)
			{
				StringBuilder sb = new StringBuilder();
				
				if ((args.length == 0) || args[0].isEmpty())
				{
					sb.append(StatsUtils.getThreadStats());
				}
				else if (args[0].equals("dump") || args[0].equals("d"))
				{
					try
					{
						new File("stats").mkdir();
						FileUtils.writeStringToFile(new File("stats/ThreadsDump-" + new SimpleDateFormat("MMddHHmmss").format(System.currentTimeMillis()) + ".txt"), StatsUtils.getThreadStats(true, true, true).toString());
						sb.append("Threads stats saved.\n");
					}
					catch (IOException e)
					{
						sb.append("Exception: " + e.getMessage() + "!\n");
					}
				}
				else
				{
					return null;
				}
				
				return sb.toString();
			}
		});
		
		_commands.add(new TelnetCommand("gc")
		{
			@Override
			public String getUsage()
			{
				return "gc";
			}
			
			@Override
			public String handle(String[] args)
			{
				StringBuilder sb = new StringBuilder();
				sb.append(StatsUtils.getGCStats());
				
				return sb.toString();
			}
		});
		
		_commands.add(new TelnetCommand("net", "ns")
		{
			@Override
			public String getUsage()
			{
				return "net";
			}
			
			@Override
			public String handle(String[] args)
			{
				StringBuilder sb = new StringBuilder();
				
				sb.append(SelectorThread.getStats());
				
				return sb.toString();
			}
			
		});
		
		_commands.add(new TelnetCommand("pathfind", "pfs")
		{
			
			@Override
			public String getUsage()
			{
				return "pathfind";
			}
			
			@Override
			public String handle(String[] args)
			{
				StringBuilder sb = new StringBuilder();
				
				sb.append(PathFindBuffers.getStats());
				
				return sb.toString();
			}
			
		});

		_commands.add(new TelnetCommand("aistats", "as")
		{
			
			@Override
			public String getUsage()
			{
				return "aistats";
			}
			
			@Override
			public String handle(String[] args)
			{
				StringBuilder sb = new StringBuilder();
				
				for (int i = 0; i < Config.AI_TASK_MANAGER_COUNT; i++)
				{
					sb.append("AiTaskManager #").append(i + 1).append("\n");
					sb.append("=================================================\n");
					sb.append(AiTaskManager.getInstance().getStats(i));
					sb.append("=================================================\n");
				}
				
				return sb.toString();
			}
			
		});
		_commands.add(new TelnetCommand("effectstats", "es")
		{
			
			@Override
			public String getUsage()
			{
				return "effectstats";
			}
			
			@Override
			public String handle(String[] args)
			{
				StringBuilder sb = new StringBuilder();
				
				for (int i = 0; i < Config.EFFECT_TASK_MANAGER_COUNT; i++)
				{
					sb.append("EffectTaskManager #").append(i + 1).append("\n");
					sb.append("=================================================\n");
					sb.append(EffectTaskManager.getInstance().getStats(i));
					sb.append("=================================================\n");
				}
				
				return sb.toString();
			}
			
		});
	}
	
	@Override
	public Set<TelnetCommand> getCommands()
	{
		return _commands;
	}
}