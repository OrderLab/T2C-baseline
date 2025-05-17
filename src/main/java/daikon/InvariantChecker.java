package daikon;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import daikon.zookeeper.InvariantPool;

public class InvariantChecker {

    public static boolean DUMP_SUPPRESS_INV_LIST_WHEN_CHECKING = false;

    public static boolean ifLoadedSuppressList = false;
    public static List<Integer> suppressedInvs = new ArrayList<>();

    public static long totalSuccess = 0l;
    public static long totalFail = 0l;
    public static long totalSkip = 0l;

    public static void loadSuppressedInvs()
    {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("daikon.suppress.input"));
            while(scanner.hasNextInt()){
                suppressedInvs.add(scanner.nextInt());
            }
            System.out.println("Loaded "+suppressedInvs.size()+" suppressable invs from file: daikon.suppress.input");
        } catch (FileNotFoundException e) {
            System.out.println("No suppress file found.");
        }
        ifLoadedSuppressList = true;
    }

    public static void dumpSuppressedInvs(List<Integer> invs)
    {
        if(invs.isEmpty())
        {
            //our intuition is to avoid namenode and datanode all write to same file
            System.out.println("Skip dumpSuppressedInv due to empty list.");
            return;
        }

        FileWriter writer = null;
        try {
            File file = new File("daikon.suppress.output");
            boolean result = Files.deleteIfExists(file.toPath());

            writer = new FileWriter("daikon.suppress.output");
            for (Integer inv:invs)
            {
                writer.write(inv + " ");
            }
            writer.close();
            System.out.println("Dumped "+invs.size()+" invs to suppress later.");
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    static class AssertTask implements Runnable{
        @Override
        public void run() {
            if(!ifLoadedSuppressList)
                loadSuppressedInvs();

            System.out.println("CHANG: start to assertAll");
            GlobalState.mode = GlobalState.T2CMode.ASSERTING;
            long start = System.currentTimeMillis();
            int passCount = 0, failCount = 0, abortCount = 0;
            int passCountAfterSuppress = 0, failCountAfterSuppress = 0, abortCountAfterSuppress = 0;
            List<Integer> failedInvs = new ArrayList<>();
            List<Invariant> invs = null;

            int failThreshold = 3;
            int cooldown = 100;

            if (GlobalState.Config.systemName.contains("zookeeper")) {
                invs = InvariantPool.invariants;
                failThreshold = 3;
                cooldown = 100;
            } else if (GlobalState.Config.systemName.contains("hbase")) {
                invs = daikon.hbase.InvariantPool.invariants;
            } else if (GlobalState.Config.systemName.contains("hdfs")) {
                invs = daikon.hdfs.InvariantPool.invariants;
            } else if (GlobalState.Config.systemName.contains("cassandra")) {
                try {
                    invs = daikon.cassandra.InvariantPool.invariants;
                    System.out.println("DIMAS2: asserting cassandra");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.err.println("Hey man we expect some inv switching here");
                System.exit(-1);
            }

            for(Invariant inv: invs)
            {
                //long firstTime = System.currentTimeMillis();
                try{
                    if(inv.success==0L && inv.fail > failThreshold) {
                        if(inv.cooldown==0){
                            inv.cooldown=cooldown;
                            inv.fail=0L;
                        } else{
                            inv.cooldown--;
                        }
                        throw new Exception("Skipped");
                    }
                    boolean ret = inv.check();
                    //assert fails
                    if (!ret)
                    {
                        //LOG.trace("CHANG: invariant #"+inv.id+" FAIL!");
                        //LOG.trace("invariant #"+inv.id+": "+ inv.desp);
                        failCount++;
                        failedInvs.add(inv.id);
                        inv.fail += 1L;
                        totalFail++;


                        if(!suppressedInvs.contains(inv.id))
                            failCountAfterSuppress++;
                    }
                    else
                    {
                        //LOG.trace("CHANG: invariant #"+inv.id+" PASS.");
                        passCount++;
                        totalSuccess++;
                        inv.success += 1L;

                        if(!suppressedInvs.contains(inv.id))
                            passCountAfterSuppress++;
                    }

                } catch (Exception ex)
                {
                    //LOG.trace("CHANG: invariant #"+inv.id+" aborts with exception:");
                    abortCount++;
                    totalSkip++;
                    
                    if(!suppressedInvs.contains(inv.id))
                        abortCountAfterSuppress++;
                    //ex.printStackTrace();
                }
                //long lastTime = System.currentTimeMillis() - firstTime;
                //if(lastTime>0)
                //    LOG.info("CHANG: inv"+inv.id+" took "+lastTime+" ms");
            }
            long elapsedTime = System.currentTimeMillis() - start;
            StringBuilder sb = new StringBuilder();
            sb.append("DAIKON: assertAll finished, took " + elapsedTime + " ms to iterate through " + invs.size()
                    + " invariants" + "\n");
            sb.append("passCount:"+(passCount+abortCount)+", failCount:"+failCount+"\n");
            // sb.append("After suppressing, passCount:"+passCountAfterSuppress+", failCount:"+failCountAfterSuppress+", abortCount:"+abortCountAfterSuppress+"\n");
            sb.append("Total, passCount:"+(totalSuccess+totalSkip)+", failCount:"+totalFail+"\n");
            System.out.println(sb.toString());
            // System.out.println("failed invs: "+Arrays.toString(failedInvs.toArray()));
            GlobalState.mode = GlobalState.T2CMode.PRODUCTION;

            if(DUMP_SUPPRESS_INV_LIST_WHEN_CHECKING)
                dumpSuppressedInvs(failedInvs);
        }
    }

    static boolean rollDice()
    {
        double checkRatio = 0;
        if(GlobalState.Config.checkRatio==null)
        {
            checkRatio = 1;
        }
        else {
            checkRatio = Double.valueOf(GlobalState.Config.checkRatio);
        }

        Random r = new Random();
        double randomValue = r.nextDouble();
        return randomValue<checkRatio;
    }

    public static void assertAll()
    {
        if(!rollDice())
        {
            //LOG.info("Bad luck on dice, skip checking.");
            return;
        }

        try {
            DaikonLogger.getLogger().dumpStates();
        } catch (Exception e) {
            // TODO: handle exception
            return;
        }

        new Thread(new AssertTask()).start();

        if(GlobalState.Config.systemName.contains("hbase")){
            for(int i=0; i<750; i++){
                try {
                    DaikonLogger.getLogger().dumpStates();
                } catch (Exception e) {
                    // TODO: handle exception
                    try {
                        Thread.sleep(1000);
                    } catch (Exception ex) {
                        // TODO: handle exception

                    }
                }

                new Thread(new AssertTask()).start();
            }
        }
        
    }

    public static Object getValue(String key) {

        return DaikonLogger.getLogger().accessState(key);
    }
}
