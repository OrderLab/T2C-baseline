file_list = "file_list.txt"

with open(file_list, 'r') as file_list_file:
    for item in file_list_file:
        with open(item.strip(), 'r') as trace_file:
            for trace in trace_file:
                if "[..]" in trace:
                    print(trace)