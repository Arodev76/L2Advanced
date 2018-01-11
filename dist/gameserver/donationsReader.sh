#!/bin/bash
echo "Starting Donations Script"
java -cp ../libs/DonationsReader1.0.1.jar main.Main > donationsLog.txt 2>&1 &