import os

input_directory = 'hdfs.t2c.dtraces'
text_file = 'hdfs.file_list.txt'

with open(text_file, 'w') as out_file:
    fileLst = os.listdir(input_directory)
    curr_count = 0
    total_count = len(fileLst)

    skipped = 0
    skipped_size = 0
    used=0
    used_size=0
    for file in fileLst:
        # out_file.write(input_directory+"/"+file+'\n')
        # out_file.write(input_directory+"/"+file+'\n')
        used += 1
        used_size += os.stat(input_directory+"/"+file).st_size
        skip = 0
        line_count = 0
        with open(input_directory+'/'+file, 'r') as ind_dtrace:
            skip=0

            for line in ind_dtrace:
                if line!="\n":
                    line_count += 1
                
                if line_count==1 and line!="decl-version 2.0\n":
                    print('incorrect format file'+', skip')
                    break
                elif line_count==2 and line!="var-comparability none\n" and line!="\n":
                    print('incorrect format file'+', skip')
                    break
                elif line_count==3 and not line.strip().startswith("ppt") and not line.strip().endswith(":::dump"):
                    print('incorrect format file'+', skip')
                    break
                elif line_count==4 and line!="ppt-type point\n":
                    print('incorrect format file'+', skip')
                    break
                elif line_count > 4:
                    break
        if line_count>4:
            out_file.write(input_directory+"/"+file+'\n')
            used += 1
            used_size += os.stat(input_directory+"/"+file).st_size
        else:
            skipped += 1
            skipped_size += os.stat(input_directory+"/"+file).st_size
        curr_count += 1
        print(f"Writing {curr_count}/{total_count}")
    print(f"skipped {skipped}, {skipped_size/(1024*1024)}M")
    print(f"used {used}, {used_size/(1024*1024)}M")