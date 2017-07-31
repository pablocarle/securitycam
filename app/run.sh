#!/bin/bash

clear

HEAP_SIZE="-Xmx256m -Xms256m"
JAVA_OPTS="$HEAP_SIZE -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

echo "Start running cam manager"

java -server -jar $JAVA_OPTS cam-manager-0.0.1-SNAPSHOT.jar 
