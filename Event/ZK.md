# ZK-4325 and ZK-4646

## Common step (you only need to do it once)
1. Edit `system_dir_path` in conf/samples/zk-3.6.1.properties to zookeeper path
```
#Required (user-specific):
system_dir_path=/home/chang/zookeeper/ #edit this line
ticket_collection_path=${ok_dir}/conf/samples/zk-collections

#Required (customized rule-related):
time_window_length_in_millis=5000
#select instrumentation range: strict-selective, relaxed-selective, specified_selective, full
gentrace_instrument_mode=strict-selective
verify_survivor_mode=true

```
2. Edit `ticket_collection_path` in conf/samples/zk-3.6.1.properties to `${ok_dir}/conf/samples/zk-collections-basic`
```
#Required (user-specific):
system_dir_path=/home/chang/zookeeper/
ticket_collection_path=${ok_dir}/conf/samples/zk-collections-basic #It will look like this

#Required (customized rule-related):
time_window_length_in_millis=5000
#select instrumentation range: strict-selective, relaxed-selective, specified_selective, full
gentrace_instrument_mode=strict-selective
verify_survivor_mode=true
```
3. Generate traces (~15h)
```
nohup bash ./run_engine.sh runall_foreach conf/samples/zk-3.6.1.properties &> ./log_zk
```
4. Copy traces
```
cp -r inv_verify_output inv_verify_output_bk
```

## ZK-4325
1. Use zookeeper version 3.6.1
```
cd <zookeeper>
git checkout release-3.6.1
mvn clean package -DskipTests
```
2. Install oathkeeper runtime on zookeeper
```
cd ~/OKLib
./run_engine.sh install conf/samples/zk-3.6.1.properties zookeeper
rm -rf inv_prod_input && mkdir inv_prod_input && cp -r inv_verify_output_bk/ZK-* inv_prod_input/
```
3. Create zoo.cfg
```
cp <T2C>experiments/detection/zookeeper/ZK-4325/zoo.cfg <zookeeper>/conf
echo dataDir=<zookeeper> >> <zookeeper>/conf
```
4. Run bug automation script
```
<T2C>/experiments/detection/zookeeper/ZK-4325/trigger_ZK-4325.sh <zookeeper>
```

## ZK-4646
You need 3-nodes cluster to do this.
0. Clone T2C repo on each nodes
1. Copy the entire oathkeeper repository to all nodes (including the rules that were generated)
```
# this example is for if the trace generation was done on node0
rsync -Pavz ~/OKLib <node1>:~
rsync -Pavz ~/OKLib <node2>:~
```
2. Use zookeeper 3.6.1 on ALL NODES
```
cd <zookeeper>
git checkout release-3.6.1
```
3. Create <zookeeper>/conf/zoo.cfg on ALL NODES containing these lines. Make sure to change dataDir to zookeeper path (appended with /data) and node0, node1, node2 to each node's hostname
```
tickTime=2000
dataDir=/users/dimas/zookeeper/data
clientPort=2181
initLimit=5
syncLimit=2
server.1=node0:2888:3888
server.2=node1:2888:3888
server.3=node2:2888:3888
```
4. Create myid to enable zookeeper cluster mode
```
# run this on 1 node
# Create myid in nodes
ssh -t $user@$hostname1 "mkdir -p $zookeeper_dir/data ; cd $zookeeper_dir/data ; echo 1 > myid"
ssh -t $user@$hostname2 "mkdir -p $zookeeper_dir/data ; cd $zookeeper_dir/data ; echo 2 > myid"
ssh -t $user@$hostname3 "mkdir -p $zookeeper_dir/data ; cd $zookeeper_dir/data ; echo 3 > myid"
```
5. Apply patch, build
```
# run this on 1 node
for hostname in $hostname1 $hostname2
do
    scp follower.patch $user@$hostname:$zookeeper_dir/
    ssh -t $user@$hostname "cd $zookeeper_dir/ ; git apply <T2C>/experiments/detection/zookeeper/ZK-4646/follower.patch ; mvn install -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true"
done

scp leader.patch $user@$hostname3:$zookeeper_dir/
ssh -t $user@$hostname3 "cd $zookeeper_dir/ ; git apply <T2C>/experiments/detection/zookeeper/ZK-4646/leader.patch ; mvn install -DskipTests -Dmaven.test.skip=true -Dmaven.site.skip=true -Dmaven.javadoc.skip=true"
```
6. Trigger the bug. Change user and hostname variable in trigger_ZK-4646.sh first.
```
# run this on 1 node
<T2C>/experiments/detection/zookeeper/ZK-4646/trigger_ZK-4646.sh <zookeeper>
```