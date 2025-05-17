import os,sys,time

input_directory = 't2c.dtraces'
output_directory = 'daikon_outputs/'
report_filename = 'daikon_report.txt'

def feed2Daikon(filename):
    #daikon.Daikon.undo_opts is important here to show a complete rule set, for example
    #if you turn this off, a=b and b is subset of c, in rules only a or b will be printed
    dump_cmd = ('java -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:."  daikon.Daikon'
        ' --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true ' 
        ' --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true '
        ' --config_option daikon.Daikon.undo_opts=true '
        + input_directory+'/'+filename + ' > daikon_outputs/' + filename + '.output'
        )
    os.system(dump_cmd)

def parse_phase():
    count = 0
    fileLst = os.listdir(input_directory)
    for filename in fileLst:
        if filename.startswith("t2c.dtrace"):
            start_time = time.time()
            feed2Daikon(filename)
            count += 1
            print("successfully parse for "+ filename+' '+str(count)+'/'+str(len(fileLst)))
            print("--- took %s seconds ---" % (time.time() - start_time))
        else:
            continue

def merge_phase():
    print("now merging all reports...")
    fileLst = os.listdir(output_directory)
    ifFirst = True
    f = set()
    retained = set()
    for filename in fileLst:
        f = set(open(output_directory+'/'+filename).readlines()[5:-1])
        if ifFirst:
            ifFirst = False
            retained = f
            continue
        retained  = retained.intersection(f)
    if os.path.exists(report_filename):
        os.remove(report_filename)
    with open(report_filename, 'a') as report_file:
        for line in retained:
            #we skip enclosing vars
            if "_FOR_ENCLOSING_USE" not in line:
                report_file.write(line)
    print("done.")

def main():
    mkdir_cmd = 'mkdir -p '+output_directory
    os.system(mkdir_cmd)

    if len(sys.argv)>1:
        if sys.argv[1] == 'merge':
            merge_phase()
            return

    parse_phase()
    merge_phase()

if __name__ == "__main__":
    main()

