import subprocess

#merge
subprocess.call("cd ./daikon_report && awk '!seen[$0]++' * > daikon_report.txt.java.tmp", shell=True) 

#modify: 1) formalize 2) remove illegal
with open('./daikon_report/daikon_report.txt.java.tmp', 'r') as old_file:
    with open('./daikon_report.txt.java', 'w') as new_file:
        for line in old_file.readlines():
            if 'Exiting Daikon.' in line or 'unimplemented' in line:
                continue
            new_file.write(line)





