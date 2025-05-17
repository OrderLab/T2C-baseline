report = "daikon_report.txt"

with open(report, 'r') as report_text:
    for text in report_text:
        if "session" in text.lower():
            print(text)
