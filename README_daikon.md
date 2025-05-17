# Semantic-Daikon-Checker
Initial version refers to https://github.com/OrderLab/zookeeper/compare/release-3.6.1...daikon-3.6.1
https://github.com/OrderLab/hadoop/compare/branch-3.2.1...daikon-3.2.1#diff-7307c722d7f96937e46bff5ca5048df559c89747fd36f09efe3bd7a78c98b62e

## How to run
### Install Git LFS
Provided dtrace files are pushed to git lfs. You have to install git lfs extension to pull them
```
# For ubuntu/debian
(. /etc/lsb-release &&
curl -s https://packagecloud.io/install/repositories/github/git-lfs/script.deb.sh |
sudo env os=ubuntu dist="${DISTRIB_CODENAME}" bash)
```
### Generating Invariant Pool
0. Make sure Semantic-Daikon-Checker and zookeeper is in the same dir level. Make sure you have installed Daikon v5.8.20
1. Compile this program
```
mvn clean package -DskipTests
```
2. Checkout zookeeper 3.4.11
```
git checkout tags/release-3.4.11
```
3. Apply patch
```
git apply <Semantic-Daikon-Checker absolute path>/patch/zk-3.4.11.patch
```
4. Compile zookeeper
```
ant compile
ant compile-test
```
5. Run this command. You can change the heap size parameter, `-Xmx16G` in TestEngine.java if you don't have enough memory (default heap size often causes GC overhead limit exceeded)
```
ZK_ABS_PATH=<insert zk absolute path>
SEMANTIC_DAIKON_ABS_PATH=<insert this repo absolute path>
java -cp "$SEMANTIC_DAIKON_ABS_PATH/target/*:$ZK_ABS_PATH/build/*:$ZK_ABS_PATH/build/lib/*" -Ddaikon.app.target=zookeeper t2c.TestEngine
```
6. Move all t2c result to result folder
```
mv t2c* $SEMANTIC_DAIKON_ABS_PATH/result
```
7. Generate dtraces file list -> We will get file_list.txt in <Semantic-Daikon-Checker> root folder
```
cd $SEMANTIC_DAIKON_ABS_PATH
python3 ./daikon_scripts/zk/gen_file_list.py
```
8. Feed the dtraces to daikon -> We will get daikon_report_java.txt in <Semantic-Daikon-Checker> root folder
```
java -Xmx32G -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:." daikon.Daikon --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true --no_show_progress --format java --files_from file_list.txt > daikon_report.txt
```
9. Generate invariants java class -> 
```
python3 ./daikon_scripts/zk/gen_pool.py
```

### Running checker
0. Make sure the InvariantPool.java has been generated correctly
1. Compile this program
```
mvn clean package -DskipTests
```
2. Checkout zookeeper 3.4.11
```
git checkout tags/release-3.4.11
```
3. Apply patch
```
git apply <Semantic-Daikon-Checker absolute path>/patch/checker-3.4.11.patch
```
4. Compile zookeeper
```
ant compile
ant compile-test
```
5. Run the automated script
```
# Make sure install script is commented -> just run the trigger and kill script
sudo ~/bug-automator/zookeeper/ZK-4325/all_ZK-4325.sh "$1"
```

## How to run script
```
cd <Semantic-Daikon-Checker absolute path>

# For example, sudo ./runner/bugs/generate.sh /home/dparikesit/Projects/garudaAce/zookeeper /home/dparikesit/Projects/garudaAce/bug-automator ZK-4325
sudo ./runner/bugs/generate.sh <zookeeper absolute path> <bug automator abs path> <bug case>

# For example, sudo ./runner/bugs/checker.sh /home/dparikesit/Projects/garudaAce/zookeeper /home/dparikesit/Projects/garudaAce/bug-automator ZK-4325
sudo ./runner/checker.sh <zookeeper absolute path> <bug automator abs path> <bug case>
```

