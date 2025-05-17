import re

invariant_list_file = 'inv_list_buggy.txt'
report_prefix = 'diff_4362_'
report_suffix = '.txt'

invariant_list = []
buggy = []
fixed = []

with open(invariant_list_file, 'r') as inv_file:
    for line in inv_file:
        invariant_list.append(line)

def diff(li1, li2):
    li_dif = [i for i in li1 + li2 if i not in li1 or i not in li2]
    return li_dif

buggy = []
fixed = []

max_idx = max(len(buggy), len(fixed))

for i in range(max_idx):
    with open(f"{report_prefix}{i+1}{report_suffix}", 'w') as output:
        buggy_exc = [item for item in buggy[i] if item not in fixed[i]]
        fixed_exc = [item for item in fixed[i] if item not in buggy[i]]

        output.write(f"=============================\n")
        output.write(f"BUGGY\n")
        output.write(f"=============================\n\n")

        for item in buggy_exc:
            output.write(f"{item} {invariant_list[item]}")

        output.write(f"\n\n=============================\n")
        output.write(f"FIXED\n")
        output.write(f"=============================\n")
        for item in fixed_exc:
            output.write(f"{item} {invariant_list[item]}")