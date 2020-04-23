#!/bin/bash
# VARIABLES

CWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
JAVA=/usr/lib/jvm/java-1.8.0-zulu-amd64/bin/java
MAIN_CLASS=org.esa.snap.nbexec.Launcher
VM_ARGS="-Dsun.java2d.noddraw=true -Dsun.awt.nopixfmt=true -Dsun.java2d.dpiaware=false -Dorg.netbeans.level=INFO -Dsnap.debug=true -Xmx4G"
PROGRAM_ARGS="--userdir $CWD/snap-application/target/userdir --clusters $CWD/../s2tbx/s2tbx-kit/target/netbeans_clusters/s2tbx --patches $CWD/../snap-engine/$/target/classes:$CWD/../s2tbx/$/target/classes"
WORKING_DIR=$CWD/snap-application/target/snap
CLASSPATH=$CWD/nbexec/target/classes

cd $WORKING_DIR
$JAVA $VM_ARGS -cp $CLASSPATH $MAIN_CLASS $PROGRAM_ARGS