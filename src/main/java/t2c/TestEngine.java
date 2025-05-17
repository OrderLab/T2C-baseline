package t2c;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.junit.internal.TextListener;
import org.junit.runner.Computer;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import daikon.GlobalState;

//how to run: 1) mvn package -DskipTests 2) run our engine
//java -cp "zookeeper-server/target/*:zookeeper-server/target/lib/*"  org.apache.zookeeper.t2c.TestEngine

/*
order here is extremely important!
previously we met a bug that some methods cannot be found, but they are indeed in the package, later
we found out somehow in the build/lib/jars/ there are some same packages but older versions, cause cannot find methods
*/

/**
 * the offline test case engine
 * the purpose of this part is to track each test and do two things
 * 1) divide execution of each test so we are when we have some outputs what are
 * the current running test, by
 * printing out the test class and method name
 * 2) start our checker at the beginning and do periodically checking
 */
public class TestEngine {
    static final String OUPUTSTREAM_FILE_NAME = "t2c.out";

    static PrintStream stream;
    static PrintStream stream2;
    static {
        try {
            stream = new PrintStream(new FileOutputStream(OUPUTSTREAM_FILE_NAME, true), true);
            stream2 = new PrintStream(new FileOutputStream("t2c.done", true), true);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static int spawnProcess(Class klass) throws IOException,
            InterruptedException {
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome +
                File.separator + "bin" +
                File.separator + "java";
        String classpath = System.getProperty("java.class.path");
        String className = null;
        if (GlobalState.Config.getSystemName().contains("zookeeper") || GlobalState.Config.getSystemName().contains("cassandra") || GlobalState.Config.getSystemName().contains("hdfs") || GlobalState.Config.getSystemName().contains("hbase")){
            className = "t2c.TestEngine";// klass.getName();
        } else {
            System.err.println("Hey man we expect some inv switching here");
            System.exit(-1);
        }

        String rootPath = System.getProperty("user.dir");
        String customDaikonAppTarget = System.getProperty("daikon.app.target") != null
                ? ("-Ddaikon.app.target=" + System.getProperty("daikon.app.target"))
                : "";

        ProcessBuilder builder = new ProcessBuilder(
                javaBin, "-Xmx48G", "-cp", classpath,
                "-Dt2c.testname=" + klass.getName(), customDaikonAppTarget, className);

        Process process = builder.inheritIO().start();
        process.waitFor(3, TimeUnit.MINUTES);
        return process.exitValue();
    }

    public static void execSingleTest(String className) {
        GlobalState.mode = GlobalState.T2CMode.TEST;

        JUnitCore core = new JUnitCore();
        core.addListener(new TestListener());
        core.addListener(new TextListener(System.out));

        try {
            // iterate
            System.out.println("DIMAS: Starting " + className);
            stream2.print('"' + className + '"' + ',');
            Instant start = Instant.now();
            stream.println(className + " start: " + start.toString());

            Result result = core.run(Computer.serial(), TestClassPool.getClass(className));

            Instant finish = Instant.now();
            stream.println(className + " start: " + start.toString() + ", finish: " + finish.toString() + ", duration: " + Duration.between(start, finish).toString());
            T2CHelper.globalLogInfo(className + " failures: " + result.getFailureCount() + "/" + result.getRunCount());

            // do clean exit
            System.exit(0);

        } catch (Exception ex) {
            System.err.println("Exception when executing test class:" + className);
            stream.println("Exception when executing test class:" + className);
            ex.printStackTrace();
        }
    }

    public static void main(String... args) {

        // spawned process
        if (System.getProperty("t2c.testname") != null) {
            execSingleTest(System.getProperty("t2c.testname"));
        } else {
            // main entry
            try {
                // register all test classes by default
                TestClassPool.registerAllClass();
                // TestClassPool.registerSpecificClasses();

                int count = 0;
                for (Class clazz : TestClassPool.getClasses()) {
                    System.out.println("Spawn test for " + (count + 1) + "/" + TestClassPool.getClasses().size());
                    // if(clazz.getName().toLowerCase().contains("cnx") || clazz.getName().toLowerCase().contains("client") || clazz.getName().toLowerCase().contains("sasl") || clazz.getName().toLowerCase().contains("ssl") || clazz.getName().toLowerCase().contains("auth")){
                    //     spawnProcess(clazz);
                    // }
                    // if(clazz.getName().toLowerCase().contains("nioservercnxntest")){
                    // }
                    spawnProcess(clazz);
                    count++;
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}