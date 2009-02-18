#!/bin/bash
OLD_VERSION=0.4.1-SNAPSHOT

if [ $# -ne 2 ] ; then
 echo "usage: <prog> old-version-number new-version-number" 
 exit 1
fi

find . -name "pom.xml" | xargs perl -pi -e  's/<kompics.version>$1<\/kompics.version>/<kompics.version>$2<\/kompics.version>/g'
