#! /bin/bash

script_dir="$( dirname -- "$( readlink -f -- "$0"; )"; )"
parent_dir="$( dirname -- "$( dirname -- "$( readlink -f -- "$0"; )"; )")"

echo  Start generate ZK-3832
# sudo $script_dir/bugs/generate.sh "$1" "$2" ZK-3832 || exit 1
echo  Start check ZK-3832
# sudo $script_dir/bugs/checker.sh "$1" "$2" ZK-3832 || exit 1

echo  Start generate ZK-3905
# sudo $script_dir/bugs/generate.sh "$1" "$2" ZK-3905 || exit 1
echo  Start check ZK-3905
# sudo $script_dir/bugs/checker.sh "$1" "$2" ZK-3905 || exit 1

echo  Start generate ZK-4026
# sudo $script_dir/bugs/generate.sh "$1" "$2" ZK-4026 || exit 1
echo  Start check ZK-4026
# sudo $script_dir/bugs/checker.sh "$1" "$2" ZK-4026 || exit 1

echo  Start generate ZK-4325
# sudo $script_dir/bugs/generate.sh "$1" "$2" ZK-4325 || exit 1
echo  Start check ZK-4325
# sudo $script_dir/bugs/checker.sh "$1" "$2" ZK-4325 || exit 1

echo  Start generate ZK-4362
# sudo $script_dir/bugs/generate.sh "$1" "$2" ZK-4362 || exit 1
echo  Start check ZK-4362
sudo $script_dir/bugs/checker.sh "$1" "$2" ZK-4362 || exit 1