#! /bin/bash

# Uncomment and change if JAVA_HOME or mvn or java not detected
#PATH=$PATH:/home/dparikesit/.sdkman/candidates/java/current/bin:/home/dparikesit/.sdkman/candidates/maven/current/bin
#JAVA_HOME=/home/dparikesit/.sdkman/candidates/java/current

script_dir="$( dirname -- "$( readlink -f -- "$0"; )"; )"
parent_dir="$( dirname -- "$( dirname -- "$( readlink -f -- "$0"; )"; )")"

cd "$parent_dir" || exit
# rm -rf result
# mkdir -p result
# rm -rf daikon_report.txt* InvariantPool.java t2c*
mvn clean package -DskipTests || exit

cd "$1" || exit

sudo rm -rf logs/ version-2/

# git stash
# git checkout tags/release-3.6.1
git reset --hard
git clean -fd
git clean -fx
git checkout daikon-3.6.1

git apply $parent_dir/patch/zk/checker-3.6.1.patch || exit

mvn clean install -DskipTests -Dmaven.site.skip=true -Dmaven.javadoc.skip=true || exit
chmod +x -R bin/

cp $script_dir/zoo.cfg ./conf
echo dataDir=$1 >> ./conf/zoo.cfg

sed -i "1 a JAVA_HOME=$JAVA_HOME" ./bin/zkEnv.sh

echo Build successful

sudo /home/dparikesit/Projects/garudaAce/bug-automator/zookeeper/ZK-4325/all_ZK-4325.sh "$1"

#java -cp "zookeeper-server/target/*:zookeeper-server/target/lib/*" -Ddaikon.app.target=zookeeper t2c.TestEngine
# java -cp "zookeeper-server/target/*:zookeeper-server/target/lib/*" -Ddaikon.app.target=zookeeper t2c.TestEngine > $parent_dir/result/run.log

#mv t2c* $parent_dir/result

#cd $parent_dir

#python3 ./daikon_scripts/zk/gen_file_list.py

# java -Xmx32G -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:." daikon.Daikon --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true --config_option daikon.Daikon.undo_opts=true --no_show_progress --format java --files_from file_list.txt > daikon_report.txt

# java -Xmx32G -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:." daikon.Daikon --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true --no_show_progress --format java --files_from file_list.txt > daikon_report.txt