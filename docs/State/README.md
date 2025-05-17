# State checker

## Install daikon
1. Clone repo
```
cd ~
git clone https://github.com/codespecs/daikon.git
git checkout tags/v5.8.20
```
2. Append to bashrc
```
export DAIKONDIR=~/daikon >> ~/.bashrc
source $DAIKONDIR/scripts/daikon.bashrc >> ~/.bashrc
source ~/.bashrc
```
3. Compile daikon
```
cd $DAIKONDIR
make compile
make -C $DAIKONDIR/java dcomp_rt.jar
```

## Detect successful cases
Follow each readme to use it
1. [ZK1208](./ZK1208.md)
2. [ZK2355](./ZK2355.md)
3. [ZK4362](./ZK4362.md)

## Throughput and false positive experiments
For throughput and false positive experiments, use run system based on the versions in Table 2, and then run the workload as usual