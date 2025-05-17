package t2c;

import daikon.DaikonLogger;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;

import t2c.T2CHelper;

public class TestListener extends RunListener {

    enum MODE {
        CONTINOUS, ONESHOT, NONE;
    }

    private final static MODE mode = MODE.ONESHOT;

    public void testStarted(Description description) {
        System.out.println("Test " + description.getClassName() + ":" + description.getMethodName() + " started");

        String testName = description.getClassName() + "@" + description.getMethodName();
        T2CHelper.getInstance().setLastTestName(testName);
        DaikonLogger.getLogger().clear();
    }

    public void testFinished(Description description) {
        System.out.println("Test " + description.getClassName() + ":" + description.getMethodName() + " ended");

        String testName = description.getClassName() + "@" + description.getMethodName();
        DaikonLogger.getLogger().writeTraceFile(testName);
    }
}
