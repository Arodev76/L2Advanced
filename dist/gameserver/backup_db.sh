#!/bin/sh

DBHOST=localhost
USER=root
PASS=
DBNAME=l2advanced

mysqlcheck -h $DBHOST -u $USER --password=$PASS -s -r $DBNAME > "log/`date +%d-%m-%Y_%H:%M:%S`-sql_check.log"
mysqldump -h $DBHOST -u $USER --password=$PASS $DBNAME | gzip -c > "backup/`date +%d-%m-%Y_%H:%M:%S`-l2advanced_$DBNAME.gz"

