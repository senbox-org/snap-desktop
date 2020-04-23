#/bin/sh
# VARIABLES
JAVA=/usr/lib/jvm/java-1.8.0-zulu-amd64/bin/java

CWD=$PWD
MAIN_CLASS=org.esa.snap.runtime.Launcher
VM_ARGS="-Dsnap.mainClass=org.esa.snap.core.gpf.main.GPT -Dsnap.home=$CWD/snap-application/target/snap -Dsnap.extraClusters=$CWD/../s2tbx/s2tbx-kit/target/netbeans_clusters/s2tbx -Dsnap.debug=true -Xmx4G"
PROGRAM_ARGS="$@"
WORKING_DIR=snap-application/target/snap
CLASSPATH=$CWD/../snap-engine/snap-runtime/target/classes

cd $WORKING_DIR
$JAVA $VM_ARGS -cp $CLASSPATH $MAIN_CLASS $PROGRAM_ARGS