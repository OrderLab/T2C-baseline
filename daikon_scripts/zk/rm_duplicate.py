# This file is not used

import os
import sys
import time

input_directory = './result/t2c.dtraces'
merged_input_filename = 't2c.dtraces.merged'
report_filename = 'daikon_report.txt'

merged_declaration_filename = 't2c.declaration.merged'
merged_traces_filename = 't2c.traces.merged'
merged_traces2_filename= 't2c.traces2.merged'

fileLst = os.listdir(input_directory)
file_count = 0
for filename in fileLst:
    file_count += 1
    line_count = 0
    print("reading declaration ",file_count,"/",len(fileLst))
    trace_list = []

    with open(input_directory+'/'+filename, 'r') as ind_dtrace:
        skip=0
        phase = -1

        for line in ind_dtrace:
            line_count += 1
            if line_count==1 and line!="decl-version 2.0\n":
                print('incorrect format file'+', skip')
                break
            elif line_count==2 and line!="var-comparability none\n":
                print('incorrect format file'+', skip')
                break
            elif line_count==3 and line!="\n":
                print('incorrect format file'+', skip')
                break
            elif line_count==4 and not line.startswith("ppt "):
                print('incorrect format file'+', skip')
                print(line)
                break
            elif line_count==5 and line!="ppt-type point\n":
                print('incorrect format file'+', skip')
                break
            elif line_count>5:
                if line.endswith(":::dump"):
                    phase+=1
                    trace_list.append([line])
                    print(trace_list)
                    continue

                if line=="\n":
                    continue
                if skip>0:
                    skip -= 1
                    continue
                
                if phase>-1:
                    trace_list[phase].append(line)

        if line_count<3:
            print(f"incorrect format file with {line_count} lines")
        
    for i in range(len(trace_list)):
        for j in range(i+1, len(trace_list)):
            if trace_list[i]==trace_list[j]:
                print("Duplicate")