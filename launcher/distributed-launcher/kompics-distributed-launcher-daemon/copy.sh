#!/bin/bash

for file in $( ls lib ) ;
do
 echo "Checking $file"

  if [ -e lib/$file/components.xml ] ; then
    echo "Concatinating $file/components.xml"
    `cat lib/$file/components.xml >> ./src/main/resources/META-INF/plexus/components.xml`

  fi
  if [ -e lib/$file/plexus.xml ] ; then
    echo "Concatinating $file/plexus.xml"
     `cat lib/$file/plexus.xml >> src/main/resources/META-INF/plexus/plexus.xml`
  fi
done
