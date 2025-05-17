#! /bin/bash

source /home/dparikesit/.bash_aliases

gotify java -Xmx90G -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:." daikon.PrintInvariants --output_num_samples --output daikon_report_cassandra.txt --format java ./cassandra/cassandra.inv.gz && gotify java -Xmx90G -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:." daikon.PrintInvariants --output_num_samples --output daikon_report_hbase.txt --format java ./hbase/hbase.inv.gz && gotify java -Xmx90G -cp "$DAIKONDIR/java/:$DAIKONDIR/java/lib/*:." daikon.Daikon --config_option daikon.inv.binary.twoSequence.SuperSet.enabled=true --config_option daikon.inv.binary.twoSequence.SubSet.enabled=true --show_detail_progress --config_option daikon.Daikon.progress_display_width=250 --format java --files_from file_list_hdfs.txt -o hdfs.inv.gz