# State checker

## Installing Daikon
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