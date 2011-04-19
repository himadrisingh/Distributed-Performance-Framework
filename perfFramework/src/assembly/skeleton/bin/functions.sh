#!/bin/bash 

#  All content copyright (c) 2003-2009 Terracotta, Inc.,
#  except as may otherwise be noted in a separate copyright notice.
#  All rights reserved.

cygwin=false
case "`uname`" in
CYGWIN*) cygwin=true;;
esac

if [ -z "$JAVA_HOME" ]; then
  echo "JAVA_HOME environment variable must be set"
  exit 1
fi

if [ ! -d "${JAVA_HOME}" ]; then
  echo "JAVA_HOME points to non-existed path: ${JAVA_HOME}"
  exit 1
fi

makeClassPath(){
  lib_path=""
  for file in $*; do
    if $cygwin; then
      file=`cygpath -d "$file"`
      lib_path="${file};${lib_path}"
    else
      lib_path="${file}:${lib_path}"
    fi
  done
  echo $lib_path;
}

CLASSPATH=`makeClassPath ${workdir}/lib/*.jar`

if $cygwin; then
  JAVA_HOME=`cygpath -d "$JAVA_HOME"`
  workdir=`cygpath -d "$workdir"`
fi

