@echo off
COLOR 0A
title Game Server Registration...
:start
echo Starting Game Server Registration.
echo.
java -server -Xms64m -Xmx64m -cp config/xml;./../libs/* l2f.loginserver.GameServerRegister

pause
