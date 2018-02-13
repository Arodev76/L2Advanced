package l2f.commons.lang;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.LockInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.MonitorInfo;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

public final class StatsUtils
{
    private static final MemoryMXBean memMXbean;
    private static final ThreadMXBean threadMXbean;
    
    public static long getMemUsed() {
        return StatsUtils.memMXbean.getHeapMemoryUsage().getUsed();
    }
    
    public static String getMemUsedMb() {
        return getMemUsed() / 1048576L + " Mb";
    }
    
    public static long getMemMax() {
        return StatsUtils.memMXbean.getHeapMemoryUsage().getMax();
    }
    
    public static String getMemMaxMb() {
        return getMemMax() / 1048576L + " Mb";
    }
    
    public static long getMemFree() {
        final MemoryUsage heapMemoryUsage = StatsUtils.memMXbean.getHeapMemoryUsage();
        return heapMemoryUsage.getMax() - heapMemoryUsage.getUsed();
    }
    
    public static String getMemFreeMb() {
        return getMemFree() / 1048576L + " Mb";
    }
    
    public static CharSequence getMemUsage() {
        final double maxMem = StatsUtils.memMXbean.getHeapMemoryUsage().getMax() / 1024.0;
        final double allocatedMem = StatsUtils.memMXbean.getHeapMemoryUsage().getCommitted() / 1024.0;
        final double usedMem = StatsUtils.memMXbean.getHeapMemoryUsage().getUsed() / 1024.0;
        final double nonAllocatedMem = maxMem - allocatedMem;
        final double cachedMem = allocatedMem - usedMem;
        final double useableMem = maxMem - usedMem;
        final StringBuilder list = new StringBuilder();
        list.append("AllowedMemory: ........... ").append((int)maxMem).append(" KB").append("\n");
        list.append("     Allocated: .......... ").append((int)allocatedMem).append(" KB (").append(Math.round(allocatedMem / maxMem * 1000000.0) / 10000.0).append("%)").append("\n");
        list.append("     Non-Allocated: ...... ").append((int)nonAllocatedMem).append(" KB (").append(Math.round(nonAllocatedMem / maxMem * 1000000.0) / 10000.0).append("%)").append("\n");
        list.append("AllocatedMemory: ......... ").append((int)allocatedMem).append(" KB").append("\n");
        list.append("     Used: ............... ").append((int)usedMem).append(" KB (").append(Math.round(usedMem / maxMem * 1000000.0) / 10000.0).append("%)").append("\n");
        list.append("     Unused (cached): .... ").append((int)cachedMem).append(" KB (").append(Math.round(cachedMem / maxMem * 1000000.0) / 10000.0).append("%)").append("\n");
        list.append("UseableMemory: ........... ").append((int)useableMem).append(" KB (").append(Math.round(useableMem / maxMem * 1000000.0) / 10000.0).append("%)").append("\n");
        return list;
    }
    
    public static CharSequence getThreadStats() {
        final StringBuilder list = new StringBuilder();
        final int threadCount = StatsUtils.threadMXbean.getThreadCount();
        final int daemonCount = StatsUtils.threadMXbean.getThreadCount();
        final int nonDaemonCount = threadCount - daemonCount;
        final int peakCount = StatsUtils.threadMXbean.getPeakThreadCount();
        final long totalCount = StatsUtils.threadMXbean.getTotalStartedThreadCount();
        list.append("Live: .................... ").append(threadCount).append(" threads").append("\n");
        list.append("     Non-Daemon: ......... ").append(nonDaemonCount).append(" threads").append("\n");
        list.append("     Daemon: ............. ").append(daemonCount).append(" threads").append("\n");
        list.append("Peak: .................... ").append(peakCount).append(" threads").append("\n");
        list.append("Total started: ........... ").append(totalCount).append(" threads").append("\n");
        list.append("=================================================").append("\n");
        return list;
    }
    
    public static CharSequence getThreadStats(final boolean lockedMonitors, final boolean lockedSynchronizers, final boolean stackTrace) {
        final StringBuilder list = new StringBuilder();
        for (final ThreadInfo info : StatsUtils.threadMXbean.dumpAllThreads(lockedMonitors, lockedSynchronizers)) {
            list.append("Thread #").append(info.getThreadId()).append(" (").append(info.getThreadName()).append(")").append("\n");
            list.append("=================================================\n");
            list.append("\tgetThreadState: ...... ").append(info.getThreadState()).append("\n");
            for (final MonitorInfo monitorInfo : info.getLockedMonitors()) {
                list.append("\tLocked monitor: ....... ").append(monitorInfo).append("\n");
                list.append("\t\t[").append(monitorInfo.getLockedStackDepth()).append(".]: at ").append(monitorInfo.getLockedStackFrame()).append("\n");
            }
            for (final LockInfo lockInfo : info.getLockedSynchronizers()) {
                list.append("\tLocked synchronizer: ...").append(lockInfo).append("\n");
            }
            if (stackTrace) {
                list.append("\tgetStackTace: ..........\n");
                for (final StackTraceElement trace : info.getStackTrace()) {
                    list.append("\t\tat ").append(trace).append("\n");
                }
            }
            list.append("=================================================\n");
        }
        return list;
    }
    
    public static CharSequence getGCStats() {
        final StringBuilder list = new StringBuilder();
        for (final GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            list.append("GarbageCollector (").append(gcBean.getName()).append(")\n");
            list.append("=================================================\n");
            list.append("getCollectionCount: ..... ").append(gcBean.getCollectionCount()).append("\n");
            list.append("getCollectionTime: ...... ").append(gcBean.getCollectionTime()).append(" ms").append("\n");
            list.append("=================================================\n");
        }
        return list;
    }
    
    static {
        memMXbean = ManagementFactory.getMemoryMXBean();
        threadMXbean = ManagementFactory.getThreadMXBean();
    }
}
