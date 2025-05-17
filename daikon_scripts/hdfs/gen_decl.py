import os,sys,time
import re

#this file is used to generate a merged declaration file from dumped traces,
#as the test cases would only dump part of them

#this is to get the type info of states
merged_input_dir = 't2c.dtraces.merged.dir'
merged_output = 't2c.dtraces.merged'

def check_phase():
    if not os.path.isdir(merged_input_dir):
        print("ERROR! "+merged_input_dir+" not exists! You should rename original t2c.dtraces.merged to be "+merged_input_dir)
        exit(-1)

def merge_phase():
    with open(merged_output, 'w') as merged_output_file:
        fileLst = os.listdir(merged_input_dir)
        for filename in fileLst:
            if filename.endswith(".dtraces"):
                with open(merged_input_dir+'/'+filename, 'r') as decl_file:
                    line = decl_file.readline()
                    while line:
                        line = decl_file.readline()
                        if 'this_invocation_nonce' in line:
                            break
                        merged_output_file.write(line)

def main():
    check_phase()
    merge_phase()

if __name__ == "__main__":
    main()


