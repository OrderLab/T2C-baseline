package daikon;

public class GlobalState {

    public enum T2CMode {
        TEST,
        PRODUCTION,
        //to avoid recursion when asserting
        ASSERTING,
        ILLEGAL;
    }
    public volatile static T2CMode mode = T2CMode.PRODUCTION;

    public static void triggerEvent()
    {
        switch (GlobalState.mode)
        {
            case TEST:
                //CHANG: we expose states at the entry of all requests
                try {
                    DaikonLogger.getLogger().dumpStates();
                } catch (Exception e) {
                    // TODO: handle exception
                }
                break;
            case PRODUCTION:
                InvariantChecker.assertAll();
                break;
            case ASSERTING:
                break;
        }
    }

    public static class Config
    {
        public static String checkRatio = System.getProperty("daikon.checkratio");
        public static String systemName = getMainClassName();

        private static String getMainClassName()
        {
            if(System.getProperty("daikon.app.target") != null){
                return System.getProperty("daikon.app.target");
            }

            StackTraceElement[] stack = Thread.currentThread ().getStackTrace ();
            int i = 1;
            StackTraceElement main = stack[stack.length - i];
            while(main.getClassName().contains("java.lang.Thread"))
            {
                i++;
                main = stack[stack.length - i];
            }

            return main.getClassName();
        }

        public static String getSystemName()
        {
            if(GlobalState.Config.systemName.contains("zookeeper")){
                return "org.apache.zookeeper";
            } else if(GlobalState.Config.systemName.contains("hdfs")){
                return "org.apache.hadoop.hdfs";
            } else if(GlobalState.Config.systemName.contains("hbase")){
                return "org.apache.hadoop.hbase";
            }else if(GlobalState.Config.systemName.contains("hadoop")) {
                if(GlobalState.Config.systemName.contains("hdfs")){
                    return "org.apache.hadoop.hdfs";
                } else if(GlobalState.Config.systemName.contains("hbase")){
                    return "org.apache.hadoop.hbase";
                }
            } else if(GlobalState.Config.systemName.contains("cassandra")) {
                return "org.apache.cassandra";
            }

            new RuntimeException("Hey man we expect some inv switching here").printStackTrace();
            System.exit(-1);
            return null;
        }
    }
}
