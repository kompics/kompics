#!/bin/bash
mvn assembly:assembly -Dmaven.test.skip=true
echo "moving daemon.jar to ~/.kompics"
mv target/kompics-experiments-daemon-0.0.1-SNAPSHOT-jar-with-dependencies.jar ~/.kompics/daemon.jar
