#! /bin/bash
##################
# VARIABLES      #
##################
workspace=`pwd`
userdir=$workspace/snap-application/target/userdir
cwd=$workspace/snap-application/target/snap

main_class="org.esa.snap.nbexec.Launcher"
main_target=$workspace/nbexec/target/classes

clusters=(s2tbx)
patches_arg="--patches $workspace/../snap-engine/$/target/classes"
clusters_arg=""

for index in ${!clusters[*]} 
do
	cluster=${clusters[$index]}
	patch="$workspace/../$cluster/$/target/classes"
	patches_arg="$patches_arg:$patch"
	cluster_path="$workspace/../$cluster/${cluster}-kit/target/netbeans_clusters/$cluster"
	if [ $index -eq 0 ]; then
		clusters_arg="--clusters $cluster_path"
	else
		clusters_arg="$cluster_arg:$cluster_path"
	fi
done

jvm=java
vmargs="-Dsun.java2d.noddraw=true -Dsun.awt.nopixfmt=true -Dsun.java2d.dpiaware=false -Dorg.netbeans.level=INFO -Dsnap.debug=true -Xmx4G"


##################
# EXECUTE        #
##################
cd $cwd
$jvm $vmargs -cp $main_target $main_class --userdir $userdir $clusters_arg $patches_arg
cd $workspace