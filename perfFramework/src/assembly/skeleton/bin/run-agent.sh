#!/bin/bash 

#  All content copyright (c) 2003-2009 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.

unset CDPATH
workdir=`dirname $0`/..
workdir=`cd "${workdir}" && pwd`
cd "${workdir}"

. ${workdir}/bin/functions.sh

console_log="$workdir/logs/console.log"

JAVA_OPTS="$JAVA_OPTS -Dfw.tc.config=$workdir/conf/fw-tc-config.xml"

$JAVA_HOME/bin/java $JAVA_OPTS  -Xmx128m \
  -cp $CLASSPATH \
  org.tc.perf.BootStrap AGENT
 
# > "$console_log" 2>&1 &
# echo Check startup log at $workdir/logs/console.log
# echo 
