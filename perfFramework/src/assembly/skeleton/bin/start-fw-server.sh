#!/bin/bash 

#  All content copyright (c) 2003-2009 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.

unset CDPATH
workdir=`dirname $0`/..
workdir=`cd "${workdir}" && pwd`
cd "${workdir}"

. ${workdir}/bin/functions.sh

JAVA_OPTS="-Xms1g -Xmx1g"

echo "Starting server at 8510 port"
$JAVA_HOME/bin/java $JAVA_OPTS \
	-Dtc.install-root=${workdir}/lib \
	-Dcom.sun.management.jmxremote \
	-Dsun.rmi.dgc.server.gcInterval=31536000 \
	-XX:+HeapDumpOnOutOfMemoryError \
	-cp $CLASSPATH \
	com.tc.server.TCServerMain -f $workdir/conf/fw-tc-config.xml &

sleep 10
