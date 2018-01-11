#!/bin/bash

while :;
do
	java -server -Dfile.encoding=UTF-8 -Xmx64m -cp config:./../libs/* l2f.loginserver.GameServerRegister

	[ $? -ne 2 ] && break
	sleep 10;
done
