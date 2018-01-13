#!/bin/bash

while :;
do
java -Xbootclasspath/p:./jsr167.jar -server -Dfile.encoding=UTF-8 -Xmx15G -XX:PermSize=3G -XX:+UseConcMarkSweepGC -XX:+UseTLAB -XX:+CMSIncrementalMode -XX:+CMSIncrementalPacing -cp config:./lameguard-1.9.5.jar:./../libs/* com.lameguard.LameGuard l2f.gameserver.GameServer > log/stdout.log 2>&1
        [ $? -ne 2 ] && break
        sleep 30;
done

