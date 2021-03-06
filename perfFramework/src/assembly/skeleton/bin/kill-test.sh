#!/bin/bash 

#  All content copyright (c) 2003-2009 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.

unset CDPATH
workdir=`dirname $0`/..
workdir=`cd "${workdir}" && pwd`
cd "${workdir}"

if [ $# -lt 1 ]; then
  echo "Usage: kill-test.sh <test-id>";
  echo "To list running tests, use list-tests.sh"
fi

. ${workdir}/bin/functions.sh

JAVA_OPTS="$JAVA_OPTS -Dfw.tc.config=$workdir/conf/fw-tc-config.xml"

$JAVA_HOME/bin/java $JAVA_OPTS  -Xmx64m \
  -cp $CLASSPATH \
  org.tc.perf.BootStrapClass KILL $1
