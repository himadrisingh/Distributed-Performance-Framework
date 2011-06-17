#!/bin/bash 

#  All content copyright (c) 2003-2009 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.

unset CDPATH
workdir=`dirname $0`/..
workdir=`cd "${workdir}" && pwd`
cd "${workdir}"

. ${workdir}/bin/functions.sh

function printHelp {
  echo "Usage: runMaster.sh [load.properties]"
  echo "Sample config at conf/load.properties."
  echo "Set f/w tc-server at conf/tc-config.xml"
}


if [ "$1" = "help" ]; then
  printHelp
  exit
fi

config=conf/load.properties

if [ ! -f "$config" ]; then
  echo "Config file not found at $config"
  exit 1
else
  echo "Using config: $config"
fi

if $cygwin; then
  config=`cygpath -d "$config"`
fi

console_log="$workdir/logs/console.log"

JAVA_OPTS="$JAVA_OPTS -Dfw.tc.config=$workdir/conf/fw-tc-config.xml"

# requires large chunk of heap currently since loading tc-kit can oome.
$JAVA_HOME/bin/java $JAVA_OPTS -Xmx512m \
  -cp $CLASSPATH \
  org.tc.perf.BootStrapClass MASTER $config 
 
# > "$console_log" 2>&1 &
# echo Check startup log at $workdir/logs/console.log
# echo 
