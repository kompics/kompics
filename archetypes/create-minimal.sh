#!/bin/bash
PROG=minimal
VERSION=0.4.1-SNAPSHOT

if [ $# -ne 1 ] ; then
 echo "usage: <prog> artifactId"
 exit 1
fi

mvn archetype:create -DarchetypeGroupId=se.sics.kompics -DarchetypeArtifactId=${PROG}-template -DarchetypeVersion=${VERSION} -DgroupId=se.sics.kompics -DartifactId=$1
