import os,sys,time
import re

input_directory = 't2c.dtraces'
merged_directory = 't2c.dtraces.merged'
report_directory = 'daikon_report'

def feed2Daikon(filename):
    input_filename = merged_directory+'/'+filename
    report_filename = report_directory+'/'+filename
    #daikon.Daikon.undo_opts is important here to show a complete rule set, for example
    #if you turn this off, a=b and b is subset of c, in rules only a or b will be printed
    #dump_cmd = ('java -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:."  daikon.Daikon'
    #    ' --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true ' 
    #    ' --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true '
    #    ' --config_option daikon.Daikon.undo_opts=true '
    #    ' --show_progress '
    #    + input_filename + ' > '+report_filename
    #    )
    #print(dump_cmd)
    #os.system(dump_cmd)
    dump_cmd = ('java -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:."  daikon.Daikon'
        ' --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true ' 
        ' --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true '
        ' --config_option daikon.Daikon.undo_opts=true '
        ' --show_progress '
        ' --format java '
        + input_filename + ' > '+report_filename+'.java'
        )
    os.system(dump_cmd)

def parse_phase():
    mkdir_cmd = 'mkdir -p '+report_directory
    os.system(mkdir_cmd)
    
    start_time = time.time()
    print("start to parse with Daikon")
    fileLst = os.listdir(merged_directory)
    for filename in fileLst:
        feed2Daikon(filename)
        print("successfully parse for "+ filename)
    print("--- took %s seconds ---" % (time.time() - start_time))

def get_trailing_number(s):
    m = re.search(r'\d+$', s)
    return int(m.group()) if m else None

def count_category():
    hash2filelstmap = {}
    fileLst = os.listdir(input_directory)
    for filename in fileLst:
        num = get_trailing_number(filename)
        if num in hash2filelstmap:
            hash2filelstmap[num].append(filename)
        else:
            lst = []
            lst.append(filename)
            hash2filelstmap[num] = lst
    return hash2filelstmap

def merge_phase():
    print("now merging all dtraces...")
    mkdir_cmd = 'mkdir -p '+merged_directory
    os.system(mkdir_cmd)

    hash2filelstmap = count_category()
    for num in hash2filelstmap:
        merged_input_filename = merged_directory+'/'+str(num)+'.dtraces'
        with open(merged_input_filename, 'w') as merged_input_file:
            fileLst = hash2filelstmap[num]
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

