#!/bin/bash

KOMPICS_HOME=~/.kompics/
DAEMON=daemon-with-dependencies.jar


if [ ! -d $KOMPICS_HOME ] ; then
   mkdir $KOMPICS_HOME
   if [ $? -ne 0 ] ; then
      echo "Problem creating KOMPICS_HOME: $KOMPICS_HOME"
      exit -1
   fi
fi

java -jar $DAEMON

exit 0
