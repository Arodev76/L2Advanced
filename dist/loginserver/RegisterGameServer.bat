@echo off
COLOR 0B
title Game Server Registration...
:start
echo Starting Game Server Registration.
echo.

java -Dfile.encoding=UTF-8 -XX:+UseConcMarkSweepGC -Xmx256M -cp config;./../libs/* l2f.loginserver.GameServerRegister

pause
