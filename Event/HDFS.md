# HDFS-14699, HDFS-16942, HDFS-16633

## Common step (you only need to do it once)
1. Edit `system_dir_path` in conf/samples/hdfs-3.2.1.properties to hadoop path
```
#Required (user-specific):
system_dir_path=/home/chang/hadoop/ #edit this line
ticket_collection_path=${ok_dir}/conf/samples/hdfs-collections

#Required (customized rule-related):
time_window_length_in_millis=5000
#select instrumentation range: strict-selective, relaxed-selective, specified_selective, full
gentrace_instrument_mode=strict-selective
verify_survivor_mode=true

```
2. Edit `ticket_collection_path` in conf/samples/hdfs-3.2.1.properties to `${ok_dir}/conf/samples/hdfs-collections-basic`
```
#Required (user-specific):
system_dir_path=/home/chang/hadoop/
ticket_collection_path=${ok_dir}/conf/samples/hdfs-collections-basic #It will look like this

#Required (customized rule-related):
time_window_length_in_millis=5000
#select instrumentation range: strict-selective, relaxed-selective, specified_selective, full
gentrace_instrument_mode=strict-selective
verify_survivor_mode=true
```
3. Generate traces (~15h)
```
nohup bash ./run_engine.sh runall_foreach conf/samples/hdfs-3.2.1.properties &> ./log_hdfs
```
4. Copy traces
```
cp -r inv_verify_output inv_verify_output_bk
```

## HDFS-14699
1. Follow the readme in ~/OKLib/experiments/notes/HDFS-14699.md

## HDFS-16942
1. Modify `experiments/detection/hdfs/HDFS16942/install_HDFS-16942.sh`
```
#! /bin/bash
# run this scripts on the server, reproducing HDFS16942 only need one server
# version: 3.1.3
# $1 should be the directory of Hadoop source code
script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1
# git checkout tags/rel/release-3.1.3 # -> comment this line
git apply $script_dir/hook_HDFS-16942.patch
mvn clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar
version='3.2.1' # -> change this line from 3.1.3 to 3.2.1
cp $script_dir/core-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $script_dir/hdfs-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/

# configure JAVA_HOME in hadoop-env.sh
# change 3.1.3 to 3.2.1
sed -i 's/# export JAVA_HOME=/export JAVA_HOME=\/usr\/lib\/jvm\/java-1.8.0-openjdk-amd64\//g' hadoop-dist/target/hadoop-3.2.1/etc/hadoop/hadoop-env.sh
```
2. Modify `experiments/detection/hdfs/HDFS16942/trigger_HDFS-16942.sh`
```
# run this script on the server, reproducing HDFS14942 only need one server
# $1 should be the directory of Hadoop source code
cd $1
version='3.2.1' # -> Change this line from 3.1.3 to 3.2.1

cd hadoop-dist/target/hadoop-${version}/
./bin/hdfs namenode -format
./sbin/hadoop-daemon.sh start namenode
./sbin/hadoop-daemon.sh start datanode
echo "sleep 8 seconds"
sleep 8
echo "stop cluster"
./sbin/hadoop-daemon.sh stop datanode
./sbin/hadoop-daemon.sh stop namenode
echo "now there should be a WARN message 'BR lease 0x{} is not valid for DN{}, because the lease has expired.' in logs/hadoop-username-namenode-hostname.log" 
```
3. Apply runtime
```
cd ~/OKLib
./run_engine.sh install conf/samples/hdfs-3.2.1.properties hdfs
rm -rf inv_prod_input && mkdir inv_prod_input && cp -r inv_verify_output_bk/HDFS-* inv_prod_input/
```
4. Run the script
```
cd <T2C>/experiments/detection/hdfs/HDFS16942
./install_HDFS-16942.sh <hadoop dir>
./trigger_HDFS-16942.sh <hadoop dir>
```

## HDFS-16633
1. Modify `experiments/detection/hdfs/HDFS16633/install_HDFS-16633.sh`
```
#! /bin/bash
# run this scripts on the server, reproducing HDFS16633 only need one server
# version: 3.1.2
# $1 should be the directory of Hadoop source code
script_dir=$( dirname -- "$( readlink -f -- "$0"; )"; )

cd "$1" || exit 1
version='3.2.1' # -> change from 3.1.2 to 3.2.1
# git checkout tags/rel/release-$version # -> comment this
git apply $script_dir/forOathkeeper.patch # -> change this to forOathkeeper.patch
mvn clean package -Pdist -DskipTests -Dmaven.javadoc.skip=true -Dtar
cp $script_dir/core-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
cp $script_dir/hdfs-site.xml hadoop-dist/target/hadoop-${version}/etc/hadoop/
# configure JAVA_HOME in hadoop-env.sh
sed -i 's/# export JAVA_HOME=/export JAVA_HOME=\/usr\/lib\/jvm\/java-1.8.0-openjdk-amd64\//g' hadoop-dist/target/hadoop-${version}/etc/hadoop/hadoop-env.sh
```
2. Modify `experiments/detection/hdfs/HDFS16633/trigger_HDFS-16633.sh`
```
# run this script on the server, reproducing HDFS16633 only need one server
# $1 should be the directory of Hadoop source code
cd $1
version='3.2.1' # -> change from 3.1.2 to 3.2.1
cd hadoop-dist/target/hadoop-${version}/
./bin/hdfs namenode -format
./sbin/hadoop-daemon.sh start namenode
./sbin/hadoop-daemon.sh start datanode
echo "sleep 8 seconds"
sleep 8
echo "running work load"
./bin/hadoop org.apache.hadoop.hdfs.NNBenchWithoutMR -operation createWrite -baseDir /benchmarks -numFiles 100 -blocksPerFile 16 -bytesPerBlock 1048576
sleep 3
echo "now there should be messages 'ERROR org.apache.hadoop.hdfs.server.datanode.DataNode: ubuntu:9866:DataXceiver error processing WRITE_BLOCK operation' and 'CHANG: inject IOException!' in logs/hadoop-username-datanode-hostname.log" 
echo "now the issue is reproduced"
./sbin/hadoop-daemon.sh stop datanode
./sbin/hadoop-daemon.sh stop namenode
```
3. Apply runtime
```
cd ~/OKLib
./run_engine.sh install conf/samples/hdfs-3.2.1.properties hdfs
rm -rf inv_prod_input && mkdir inv_prod_input && cp -r inv_verify_output_bk/HDFS-* inv_prod_input/
```
4. Run the script
```
cd <T2C>/experiments/detection/hdfs/HDFS16633
./install_HDFS-16633.sh <hadoop dir>
./trigger_HDFS-16633.sh <hadoop dir>
```
