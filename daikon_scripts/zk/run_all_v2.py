import os,sys,time

input_directory = 't2c.dtraces'
merged_input_filename = 't2c.dtraces.merged'
report_filename = 'daikon_report.txt'

def feed2Daikon(filename):
    #daikon.Daikon.undo_opts is important here to show a complete rule set, for example
    #if you turn this off, a=b and b is subset of c, in rules only a or b will be printed
    dump_cmd = ('java -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:."  daikon.Daikon'
        ' --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true ' 
        ' --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true '
        ' --config_option daikon.Daikon.undo_opts=true '
        ' --show_progress '
        + filename + ' > '+report_filename
        )
    os.system(dump_cmd)
    dump_cmd = ('java -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:."  daikon.Daikon'
        ' --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true ' 
        ' --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true '
        ' --config_option daikon.Daikon.undo_opts=true '
        ' --show_progress '
        ' --format java '
        + filename + ' > '+report_filename+'.java'
        )
    os.system(dump_cmd)

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
    with open(merged_input_filename, 'a') as merged_input_file:
        fileLst = os.listdir(input_directory)
        merged_input_file.write("decl-version 2.0\n");
        merged_input_file.write("var-comparability none\n");
        merged_input_file.write("\n");
        if_already_wrote_declaration = False
        if_still_in_declaration = True
        file_count = 0
        #we save the first file's decl section length as standard answer to filter bad files
        line_count_reference = -1
        decl_head_reference = 'DEADBEEF'
        for filename in fileLst:
            file_count += 1
            line_count = 0
            print("merging ",file_count,"/",len(fileLst))
            lines = open(input_directory+'/'+filename).readlines()[3:]
            
            #filter illegal
            if len(lines)<3:
                continue

            if decl_head_reference != 'DEADBEEF':
                if decl_head_reference != lines[2]:
                    print('incorrect format file with head '+lines[2]+', skip')
                    continue
            else:
                decl_head_reference = lines[2]
                print('set decl_head_reference to be '+decl_head_reference)
                
            if_still_in_declaration = True
            for line in lines:
                line_count += 1
                #first empty line after declaration is the starting of values
                if line == '\n':
                    if if_still_in_declaration:
                        if_already_wrote_declaration = True
                        if_still_in_declaration = False
                        if line_count_reference != -1:
                            if line_count != line_count_reference:
                                print('incorrect format file with line_count '+str(line_count)+', skip')
                                break
                        else:
                            line_count_reference = line_count
                            print('set line_count_reference to be '+str(line_count_reference))

                if if_still_in_declaration:
                    if not if_already_wrote_declaration:
                        merged_input_file.write(line)
                    else:
                        continue
                else:
                    merged_input_file.write(line)
                        
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

