if [[ $1 == "install" ]]
then
    echo "install test checker to target system"
    echo "usage: ./run_engine.sh install zookeeper/hdfs"

    if [[ $2 == "zookeeper" ]]
    then
        echo "install test checker to target system"
        cd ~/zookeeper
        git apply ~/Semantic-Daikon-Checker/patch/zk/install.patch
    elif [[ $2 == "zookeeper-3.6.1" ]]
    then
        echo "install test checker to target system"
        cd ~/zookeeper
        git apply ~/Semantic-Daikon-Checker/patch/zk/install-3.6.1.patch
    elif [[ $2 == "zookeeper-2355" ]]
    then
        echo "install test checker to target system"
        cd ~/zookeeper
        git apply ~/Semantic-Daikon-Checker/patch/zk/install-2355.patch
    elif [[ $2 == "hdfs" ]]
    then
        cd ~/hadoop
        git apply ~/Semantic-Daikon-Checker/patch/hdfs/install.patch
    fi

    echo "succeed"

fi