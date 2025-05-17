package daikon;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class InvariantPoolSample {

    public static List<Invariant> invariants = new ArrayList<>();

    //single method has limits of 64kb, should break this apart
    public static void installInv0()
    {
        Invariant inv0 = new Invariant(0, "zksInstance.connThrottle.DEFAULT_CONNECTION_THROTTLE_FILL_COUNT) == 1"){
            @SuppressWarnings("unchecked")
            public boolean check() {
                return (((Integer)(InvariantChecker.getValue("zksInstance.connThrottle.DEFAULT_CONNECTION_THROTTLE_FILL_COUNT"))).intValue() == 1);
            }
        };
        invariants.add(inv0);
    }

    public static void installInv1()
    {
        Invariant inv1 = new Invariant(1, "zksInstance.connThrottle.DEFAULT_CONNECTION_THROTTLE_FILL_TIME == zksInstance.connThrottle.DEFAULT_LOCAL_SESSION_WEIGHT"){
            @SuppressWarnings("unchecked")
            public boolean check() {
                return (((Integer)(InvariantChecker.getValue("zksInstance.connThrottle.DEFAULT_CONNECTION_THROTTLE_FILL_TIME"))).intValue() ==
                        ((Integer)(InvariantChecker.getValue("zksInstance.connThrottle.DEFAULT_LOCAL_SESSION_WEIGHT"))).intValue());
            }
        };
        invariants.add(inv1);
    }

    public static void installInv2()
    {
        Invariant inv2 = new Invariant(2, "daikon.Quant.size(zksInstance.zkDb.dataTree.ephemerals_FOR_ENCLOSING_USE)-1 <= daikon.Quant.size(zksInstance.zkDb.sessionsWithTimeouts_FOR_ENCLOSING_USE)-1"){
            @SuppressWarnings("unchecked")
            public boolean check() {
                return daikon.Quant.size((Set.class.cast(InvariantChecker.getValue("zksInstance.zkDb.dataTree.ephemerals"))))-1
                        <= daikon.Quant.size((Set.class.cast(InvariantChecker.getValue("zksInstance.zkDb.sessionsWithTimeouts"))))-1 ;
            }
        };
        invariants.add(inv2);
    }

    public static void installInv3()
    {
        Invariant inv3 = new Invariant(3, "daikon.Quant.subsetOf(zksInstance.zkDb.dataTree.ephemerals_FOR_ENCLOSING_USE, zksInstance.zkDb.sessionsWithTimeouts_FOR_ENCLOSING_USE)"){
            @SuppressWarnings("unchecked")
            public boolean check() {
                return daikon.Quant.subsetOf((Set.class.cast(InvariantChecker.getValue("zksInstance.zkDb.dataTree.ephemerals")).toArray(new Object[0])),
                        (Set.class.cast(InvariantChecker.getValue("zksInstance.zkDb.dataTree.ephemerals")).toArray(new Object[0])));
            }
        };
        invariants.add(inv3);
    }

    static {
        installInv0();
        installInv1();
        installInv2();
        installInv3();
    }
}
