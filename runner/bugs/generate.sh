#! /bin/bash

script_dir="$( dirname -- "$( readlink -f -- "$0"; )"; )"
parent_dir="$( dirname -- "$( dirname -- "$( dirname -- "$( readlink -f -- "$0"; )"; )"; )"; )"

bug_automator_dir="$2"

bug_case="$3"

# Compile Semantic-Daikon-Checker
cd "$parent_dir" || exit
rm -rf result
mkdir -p result
rm -rf daikon_report.txt* t2c*

# mvn clean package -DskipTests || exit

# Apply patch and compile zookeeper
sudo "$bug_automator_dir/zookeeper/$bug_case/install_$bug_case.sh" "$1" "$parent_dir/patch/zk/bugs/checker-$bug_case.patch" || exit

# Generate dtraces
cd "$1"
java -cp "zookeeper-server/target/*:zookeeper-server/target/lib/*" -Ddaikon.app.target=zookeeper t2c.TestEngine > $parent_dir/result/run.log

# Move dtrace files
mv t2c* $parent_dir/result
cd $parent_dir

# Generate dtrace file list
python3 ./daikon_scripts/zk/gen_file_list.py

# Generate daikon report from dtraces
java -Xmx32G -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:." daikon.Daikon --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true --no_show_progress --format java --files_from file_list.txt > daikon_report.txt || exit

# java -Xmx32G -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:." daikon.Daikon --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true --config_option daikon.Daikon.undo_opts=true --no_show_progress --format java --files_from file_list.txt > daikon_report.txt

# Generate invariant pool
rm -rf $parent_dir/src/main/java/daikon/zookeeper/*java
python3 ./daikon_scripts/zk/gen_pool.py