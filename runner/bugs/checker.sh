#! /bin/bash

script_dir="$( dirname -- "$( readlink -f -- "$0"; )"; )"
parent_dir="$( dirname -- "$( dirname -- "$( dirname -- "$( readlink -f -- "$0"; )"; )"; )"; )"

bug_automator_dir="$2"

bug_case="$3"

cd "$parent_dir" || exit
# mvn clean package -DskipTests || exit

# Apply patch and compile zookeeper
cd "$bug_automator_dir" || exit
sudo "zookeeper/$bug_case/all_$bug_case.sh" "$1" "$parent_dir/patch/zk/bugs/checker-$bug_case.patch" || exit

# Get zookeeper log
cp -R $1/logs/*.out "$script_dir/$bug_case.log" || exit

