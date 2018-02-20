@echo off
COLOR 0B
title L2Advanced: High Five: Part 2 LS
:start
echo Starting Login Server.
echo.

set JAVA_OPTS=%JAVA_OPTS% -Xmn16m
set JAVA_OPTS=%JAVA_OPTS% -Xms64m
set JAVA_OPTS=%JAVA_OPTS% -Xmx128m

set JAVA_OPTS=%JAVA_OPTS% -Xnoclassgc
set JAVA_OPTS=%JAVA_OPTS% -XX:+AggressiveOpts
set JAVA_OPTS=%JAVA_OPTS% -XX:TargetSurvivorRatio=90
set JAVA_OPTS=%JAVA_OPTS% -XX:SurvivorRatio=16
set JAVA_OPTS=%JAVA_OPTS% -XX:MaxTenuringThreshold=12
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseParNewGC
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseConcMarkSweepGC

set JAVA_OPTS=%JAVA_OPTS% -XX:UseSSE=3
set JAVA_OPTS=%JAVA_OPTS% -XX:+UseFastAccessorMethods

java -server -Dfile.encoding=UTF-8 %JAVA_OPTS% -cp config;./../libs/* l2f.loginserver.AuthServer

REM Debug ...
REM java -Dfile.encoding=UTF-8 -cp config;./../libs/* -Xmx1G -Xnoclassgc -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=7456 l2f.gameserver.GameServer

if ERRORLEVEL 2 goto restart
if ERRORLEVEL 1 goto error
goto end
:restart
echo.
echo Server restarted ...
echo.
goto start
:error
echo.
echo Server terminated abnormaly ...
echo.
:end
echo.
echo Server terminated ...
echo.

pause