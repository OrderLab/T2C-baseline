package t2c;


import java.io.*;

public class T2CHelper {
    private static T2CHelper helper = new T2CHelper();

    private static final String LOGFILEPREFIX = "t2clog_";
    private String lastTestName = "";

    //log for each test case
    private Writer logwriter;
    private String logFileName;

    //shared log for all test cases
    private Writer globalLogwriter;
    private String globalLogFileName = "t2ctests.summary";

    //runtime log in production
    private Writer productionLogwriter;
    private String productionLogFileName = "t2c.prod.log";

    private void init() {
        // init in lazyway because we need to wait until test case to feed us log name
        try {
            //in cassandra we didn't get per-test name, we have to run one-by-one manually
            logFileName = LOGFILEPREFIX + lastTestName;
            logwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(logFileName, true)));

            globalLogwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(globalLogFileName, true)));

            productionLogwriter = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(productionLogFileName )));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static T2CHelper getInstance() {
        return helper;
    }

    void logInfoInternal(String str) {
        //if we move to another test, re-init
        if (logFileName == null || !logFileName.equals(LOGFILEPREFIX + lastTestName))
            init();

        try {
            logwriter.write(str + "\n");
            logwriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void logInfo(String str) {
        getInstance().logInfoInternal(str);

    }

    void globallogInfoInternal(String str) {
        if (globalLogwriter == null)
            init();

        try {
            globalLogwriter.write(str + "\n");
            globalLogwriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public PrintWriter getGlobalLogWriter()
    {
        return new PrintWriter(globalLogwriter);
    }

    public static void globalLogInfo(String str) {
        getInstance().globallogInfoInternal(str);

    }

    void prodlogInfoInternal(String str) {
        if (productionLogwriter == null)
            init();

        try {
            productionLogwriter.write(str + "\n");
            productionLogwriter.flush();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static void prodLogInfo(String str) {
        getInstance().prodlogInfoInternal(str);

    }

    public void setLastTestName(String name) {
        lastTestName = name;
    }
}