# Oathkeeper

## Install oathkeeper
1. Clone the repository
```
cd ~
git clone https://github.com/OrderLab/OKLib.git
cd OKLib
git submodule update --init --recursive
```
2. Build oathkeeper
```
cd ~/OKLib && mvn package
```

## Detect successful cases
1. [ZK-4325 and ZK-4646](./ZK.md)
2. [HDFS-14699, HDFS-16942, HDFS-16633](./HDFS.md)