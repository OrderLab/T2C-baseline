import os,sys,time

input_directory = './result/t2c.dtraces'
merged_input_filename = 't2c.dtraces.merged'
report_filename = 'daikon_report.txt'

merged_declaration_filename = 't2c.declaration.merged'
merged_traces_filename = 't2c.traces.merged'
merged_traces2_filename= 't2c.traces2.merged'

def feed2Daikon(filename):
    #daikon.Daikon.undo_opts is important here to show a complete rule set, for example
    #if you turn this off, a=b and b is subset of c, in rules only a or b will be printed
    dump_cmd = ('java -Xmx20480m -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:."  daikon.Daikon'
        ' --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true ' 
        ' --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true '
        ' --config_option daikon.Daikon.undo_opts=true '
        ' --show_progress '
        + filename + ' > '+report_filename
        )
    os.system(dump_cmd)
    dump_cmd = ('java -Xmx20480m -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:."  daikon.Daikon'
        ' --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true ' 
        ' --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true '
        ' --config_option daikon.Daikon.undo_opts=true '
        ' --show_progress '
        ' --format java '
        + filename + ' > '+report_filename+'.java'
        )
    os.system(dump_cmd)

def equality_check(arr1, arr2):
   if (len(arr1) != len(arr2)):
      return False
   arr1.sort()
   arr2.sort()
   for i in range(0, len(arr2)):
      if (arr1[i] != arr2[i]):
         return False
   return True

def parse_phase():
    start_time = time.time()
    print("start to parse with Daikon")
    filename = merged_input_filename
    feed2Daikon(filename)
    print("successfully parse for "+ filename)
    print("--- took %s seconds ---" % (time.time() - start_time))

def merge_phase():
    print("now merging all dtraces...")
    if os.path.exists(merged_input_filename):
        os.remove(merged_input_filename)
    if os.path.exists(merged_declaration_filename):
        os.remove(merged_declaration_filename)
    if os.path.exists(merged_traces_filename):
        os.remove(merged_traces_filename)
    if os.path.exists(merged_traces2_filename):
        os.remove(merged_traces2_filename)

    var_list_decl = set()
    var_list_traces = set()
    var_list_traces2 = set()
    
    file_decl = open(merged_declaration_filename, 'a')
    file_traces = open(merged_traces_filename, 'a')
    file_traces2 = open(merged_traces2_filename, 'a')

    file_decl.write("decl-version 2.0\n")
    file_decl.write("var-comparability none\n")
    file_decl.write("\n")
    file_decl.write("ppt T2C:::dump\n")
    file_decl.write("ppt-type point\n")

    file_traces.write("T2C:::dump\n")
    file_traces.write("this_invocation_nonce\n")
    file_traces.write("1\n")

    file_traces2.write("T2C:::dump\n")
    file_traces2.write("this_invocation_nonce\n")
    file_traces2.write("1\n")

    fileLst = os.listdir(input_directory)
    file_count = 0
    for filename in fileLst:
        file_count += 1
        line_count = 0
        print("merging ",file_count,"/",len(fileLst))

        with open(input_directory+'/'+filename, 'r') as ind_dtrace:
            phase = 0
            skip=0

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
                elif line_count==4 and line!="ppt T2C:::dump\n":
                    print('incorrect format file'+', skip')
                    break
                elif line_count==5 and line!="ppt-type point\n":
                    print('incorrect format file'+', skip')
                    break
                elif line_count>5:
                    if line=='T2C:::dump\n':
                        if phase==0:
                            phase=1
                        elif phase==1:
                            phase=2
                        else:
                            break
                        continue
                    
                    if line=="this_invocation_nonce\n":
                        skip=1
                        continue
                    if line=="\n":
                        continue
                    if skip>0:
                        skip -= 1
                        continue

                    if phase==0 and line.split()[0]=="variable":
                        var_name = line.split()[1]
                        if var_name in var_list_decl:
                            skip = 4
                            continue
                        else:
                            var_list_decl.add(line.split()[1])
                    
                    if phase==1:
                        try:
                            float(line.strip())
                        except ValueError:
                            if line.strip() in var_list_traces:
                                skip=2
                                continue
                            else:
                                if line.strip()!="true" and line.strip()!="false" and line.strip()!="null":
                                    var_list_traces.add(line.strip())
                    
                    if phase==2:
                        try:
                            float(line.strip())
                        except ValueError:
                            if line.strip() in var_list_traces2:
                                skip=2
                                continue
                            else:
                                if line.strip()!="true" and line.strip()!="false" and line.strip()!="null":
                                    var_list_traces2.add(line.strip())

                    if phase==0:
                        file_decl.write(line)
                    elif phase==1:
                        file_traces.write(line)
                    elif phase==2:
                        file_traces2.write(line)
                        
            if line_count<3:
                print('incorrect format file'+', skip')
        
    file_decl.close()
    file_traces.close()
    file_traces2.close()

    file_merged = open(merged_input_filename, 'a')

    with open(merged_declaration_filename, 'r') as file_decl:
        for line in file_decl:
            file_merged.write(line)

    file_merged.write('\n')

    with open(merged_traces_filename, 'r') as file_traces:
        for line in file_traces:
            file_merged.write(line)

    file_merged.write('\n')
    file_merged.close()
                    
    print("done.")

def main():

    if len(sys.argv)>1:
        if sys.argv[1] == 'parse':
            parse_phase()
            return
    merge_phase()
    parse_phase()

if __name__ == "__main__":
    main()

