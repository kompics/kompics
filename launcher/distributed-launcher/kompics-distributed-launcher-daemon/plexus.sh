#!/bin/bash

components=META-INF/plexus/components.xml
plexus=META-INF/plexus/plexus.xml

mkdir output 2> /dev/null

for  file in $(ls -Q *.jar)  ;
do
  target=$(echo $file | sed -e "s/\"//g")
  echo "checking file: $target"

  if [ `jar tf $target | grep $components | wc -l` -gt 0 ] ; then
    echo "found components.xml in $target" 
    `jar xf $target $components`
    mkdir output/$target
    mv $components output/$target/
  fi
  if [ `jar tf $target | grep $plexus | wc -l` -gt 0 ] ; then
    echo "found plexus.xml in $target" 
    `jar xf $target $plexus`
    mkdir output/$target 2> /dev/null
    mv $plexus output/$target/
  fi
done

for f2 in $(ls -Q $output) ;
  t2=$(echo $f2 | sed -e "s/\"//g")

  cat $t2/META-INF/plexus/components.xml >> src/main/resources/META-INF/plexus/components.xml
  cat $t2/META-INF/plexus/plexus.xml >> src/main/resources/META-INF/plexus/plexus.xml
do

done

exit 0
