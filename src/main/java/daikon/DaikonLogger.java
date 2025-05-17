package daikon;


import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class DaikonLogger {
    static DaikonLogger logger = new DaikonLogger();

    static final String LOGNAME = "t2c.dtrace.";
    static final String ENTRYNAME = "T2C:::dump";
    static final String DIRNAME = "t2c.dtraces";

    static final String SYS_PACKAGE_NAME = GlobalState.Config.getSystemName();
    static final int SEARCH_DEPTH_THRESHOLD = 3;

    //the starting points of system instances
    static Map<String, Object> rootNodes = new HashMap<>();

    static Set<String> duplicate = new HashSet();

    Set<Var> varSet = new TreeSet<>();
    //used in production
    Point currentPoint = null;
    //used in testing
    List<Point> points =
            new ArrayList<>();

    Map<String, String> redirectMap = new HashMap<>();

    Writer writer = null;
    static String LOGSUFFIX;

    public static class Point
    {
        Map<String, Var> vars = new HashMap<>();
        Map<String, Object> objs = new HashMap<>();
        //a counter to collect useful metrics, like final field
        public int counter = 0;

        public Point() {
        }

        public Point(Map<String, Var> vars, Map<String, Object> objs) {
            this.vars = vars;
            this.objs = objs;
        }

        public Object getObj(String varName)
        {
            return objs.get(varName);
        }

        public void add(Var var, Object obj)
        {
            vars.put(var.varName,var);
            objs.put(var.varName,obj);
        }

        public void addAll(Point point)
        {
            vars.putAll(point.vars);
            objs.putAll(point.objs);
            counter += point.counter;
        }


        public void increCounter()
        {
            counter++;
        }

        public int size()
        {
            if (vars.size()!=objs.size())
                throw new RuntimeException("size does not match!");
            return vars.size();
        }

        public void dump()
        {
            System.out.println("Dumping vars: "+vars.size());
            // System.out.println(Arrays.toString(vars.keySet().toArray()));
        }
    }

    public static class Var implements Comparable<Var>
    {
        protected String varName;
        protected String varType;

        public Var(String varName, String varType) {
            this.varName = varName;
            this.varType = varType;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Var var = (Var) o;

            return varName.equals(var.varName);
        }

        @Override
        public int hashCode() {
            return varName.hashCode();
        }

        @Override
        public int compareTo(Var o2) {
            return this.varName.compareTo(o2.varName);
        }
    };

    private static Object getInstance(String clazzName, String fieldName) throws Exception
    {
        try{
            Class<?> zkClazz = Class.forName(clazzName);
            Field field = zkClazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object instance = field.get(zkClazz);

            return instance;
        }
        catch (Throwable ex)
        {
            // ex.printStackTrace();
            throw ex;
        }
    }

    //this cannot be put in the static block because we need to wait until these instances inited
    void initrootNodes() throws Exception {
        if (GlobalState.Config.getSystemName().contains("zookeeper")) {
            rootNodes.put("zksInstance", getInstance("org.apache.zookeeper.server.ZooKeeperServer", "zksInstance"));
        } else if (GlobalState.Config.getSystemName().contains("hbase")) {
            rootNodes.put("modifyTableProcedureInstance", getInstance("org.apache.hadoop.hbase.master.procedure.ModifyTableProcedure", "modifyTableProcedureInstance"));
            rootNodes.put("reopenTableRegionsProcedureInstance", getInstance("org.apache.hadoop.hbase.master.procedure.ReopenTableRegionsProcedure", "reopenTableRegionsProcedureInstance"));
            rootNodes.put("regionStatesInstance", getInstance("org.apache.hadoop.hbase.master.assignment.RegionStates", "regionStatesInstance"));
            rootNodes.put("HMasterInstance", getInstance("org.apache.hadoop.hbase.master.HMaster", "HMasterInstance"));
            rootNodes.put("RegionServerInstance", getInstance("org.apache.hadoop.hbase.regionserver.HRegionServer", "HRegionServerInstance"));
        } else if (GlobalState.Config.getSystemName().contains("hdfs")) {
            rootNodes.put("DataNode.instance", getInstance("org.apache.hadoop.hdfs.server.datanode.DataNode", "instance"));
            rootNodes.put("NameNode.instance", getInstance("org.apache.hadoop.hdfs.server.namenode.NameNode", "instance"));
            rootNodes.put("blockManagerInstance", getInstance("org.apache.hadoop.hdfs.server.blockmanagement.BlockManager", "blockManagerInstance"));
        } else if (GlobalState.Config.getSystemName().contains("cassandra")) {
            rootNodes.put("cfsInstance", getInstance("org.apache.cassandra.db.ColumnFamilyStore", "cfsInstance"));
            rootNodes.put("resultSetInstance", getInstance("org.apache.cassandra.cql3.ResultSet", "resultSetInstance"));
            rootNodes.put("fromResultListInstance", getInstance("org.apache.cassandra.cql3.UntypedResultSet$FromResultList", "fromResultListInstance"));
            rootNodes.put("fromPagerInstance", getInstance("org.apache.cassandra.cql3.UntypedResultSet$FromPager", "fromPagerInstance"));
            rootNodes.put("CFMetadataInstance", getInstance("org.apache.cassandra.config.CFMetaData", "CFMetadataInstance"));
            rootNodes.put("StorageService.instance", getInstance("org.apache.cassandra.service.StorageService", "instance"));
            rootNodes.put("Schema.instance", getInstance("org.apache.cassandra.config.Schema", "instance"));
            rootNodes.put("CommitLog.instance", getInstance("org.apache.cassandra.db.commitlog.CommitLog", "instance"));
            rootNodes.put("QueryProcessor.instance", getInstance("org.apache.cassandra.cql3.QueryProcessor", "instance"));
            rootNodes.put("SizeEstimatesRecorder.instance", getInstance("org.apache.cassandra.db.SizeEstimatesRecorder", "instance"));
        } else {
            System.err.println("Hey man we expect some inv switching here");
            System.exit(-1);
        }
    }

    public DaikonLogger() {
        //init dtrace dir
        File directory = new File(DIRNAME);
        if (! directory.exists()){
            directory.mkdir();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }
        //for redirect map
        //redirectMap.put("zksInstance.sessionTracker.","zksInstance.sessionTracker.localSessionTracker.");
    }


    public static DaikonLogger getLogger() {
        return logger;
    }

    public void addPoint(Point point)
    {
        //System.out.println("CHANG: addPoint");
        for(Map.Entry<String, Var> entry:point.vars.entrySet())
        {
            Var var = entry.getValue();

            boolean ifAdd = varSet.contains(var);
            if(points.size()>0 && ifAdd)
                //disable warning for performance
                //throw new RuntimeException("still add new vars after first point, need to check");
                //System.out.println("still add new vars after first point, need to check");
                continue;
            else
                varSet.add(var);
        }

        //according to offline or online mode, we need to keep whole point list or just recent point
        switch (GlobalState.mode)
        {
            case TEST:
                points.add(point);
                break;
            case PRODUCTION:
            case ASSERTING:
                //for debugging use
                point.dump();
                currentPoint = point;
                break;
        }
    }

    public Object accessState(String varName)
    {
        varName = redirectAlias(varName);

        long start = System.currentTimeMillis();
        Object obj = currentPoint.getObj(varName);
        long elapsedTime = System.currentTimeMillis() - start;
        if(elapsedTime>1)
            System.out.println("accessState took "+elapsedTime+" ms");
        return obj;
    }

    public void clear()
    {
        //remember to clear for each method
        varSet.clear();
        points.clear();
    }

    public void writeTraceFile(String logSuffix) {
        LOGSUFFIX = logSuffix;
        updateLogger();

        try {
            if(varSet.isEmpty()){
                writeEmptyDeclaration();
            } else{
                writeDeclaration(LOGSUFFIX+varSet.hashCode());
                writeValues(LOGSUFFIX+varSet.hashCode());
            }

            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    //we observe server instance in test and production sometimes could be different,
    //for example, in zookeeper  test there is sessiontrackerimpl,
    // but in prod there is a leaderSessiontracker and learnerSessionTracker,
    // in the leaderSessiontracker there is a globalSessionTracker, that is a sessiontrackerimpl
    // so we need to do some redirection here
    String redirectAlias(String name)
    {
        for(String key:redirectMap.keySet())
        {
            if(name.startsWith(key))
            {
                String newPrefix = redirectMap.get(key);
                //to avoid replace something like zksInstance.sessionTracker.localSessionTracker.sessionExpiryQueue.expiryMap
                // zksInstance.sessionTracker.localSessionTracker.sessionExpiryQueue.expiryMap
                //if(name.startsWith(newPrefix))
                //    continue;

                return name.replace(key,newPrefix);
            }
        }

        return name;
    }

    void updateLogger()
    {
        try {
            // writer = new BufferedWriter(new OutputStreamWriter(
            //         new FileOutputStream(DIRNAME+"/"+LOGNAME+LOGSUFFIX+varSet.hashCode())));
            writer = new FileWriter(DIRNAME+"/"+LOGNAME+LOGSUFFIX+varSet.hashCode());
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            System.out.println("DIMAS: Writer error on" + DIRNAME+"/"+LOGNAME+LOGSUFFIX+varSet.hashCode());
            ex.printStackTrace();
        }

    }


    void writeEmptyDeclaration() throws IOException {
        writer.write("decl-version 2.0\n");
        writer.write("var-comparability none\n");
        writer.write("\n");
        writer.write("This report is marked as invalid.\n");
    }

    //More sample codes, you can refer to dinv:
    //https://bitbucket.org/bestchai/dinv/src/default/logmerger/logs.go
    //https://plse.cs.washington.edu/daikon/download/doc/developer/File-formats.html
    void writeDeclaration(String className) throws IOException {
        writer.write("decl-version 2.0\n");
        writer.write("var-comparability none\n");
        writer.write("\n");

        writer.write("ppt " + className + ":::dump\n");
        writer.write("ppt-type point\n");
        for (Var var : varSet) {
            String name = var.varName;
            //Daikon cannot handle byte
            if (!var.varType.equals("byte") && !var.varType.equals("long") && !var.varType.equals("int")) {
                if (var.varType.equals("double") || var.varType.equals("float")) {
                    var.varType = "double";
                }
            } else {
                var.varType = "int";
            }
//            else if (var.varType.equals("double") || var.varType.equals("long")) {
//                var.varType = "float";
//            }

            if (var.varType.equals("java.lang.Object[]")) {
                //this enclosing var is a must, cannot omit
                writer.write("variable " + name + "_FOR_ENCLOSING_USE" + "\n");
                writer.write("var-kind variable\n");
                writer.write("dec-type java.lang.Object[]\n");
                writer.write("rep-type hashcode\n");
                writer.write("comparability -1\n");

                writer.write("variable " + name + "[..]\n");
                writer.write("var-kind array\n");
                writer.write("enclosing-var " + name + "_FOR_ENCLOSING_USE" + "\n");
                writer.write("array 1\n");
                writer.write("dec-type java.lang.Object[]\n");
                if (name.startsWith("DataNode.instance.directoryScanner.diffs")) {
                    this.writer.write("rep-type java.lang.String[]\n");
                } else {
                    this.writer.write("rep-type hashcode[]\n");
                }

                writer.write("comparability -1\n");
            } else {
                writer.write("variable " + name + "\n");
                writer.write("var-kind variable\n");
                writer.write("dec-type " + var.varType + "\n");
                writer.write("rep-type " + var.varType + "\n");
                writer.write("comparability -1\n");
            }
        }

        writer.write("\n");

    }

    void writeValues(String className) throws IOException {

        for (int i = 0; i < points.size(); ++i) {
            Point point = points.get(i);

            writer.write(className.replaceAll(" ", "") + ":::dump\n");
            writer.write("this_invocation_nonce\n");
            writer.write("1\n");
            for (Var var:varSet){
                Object value = point.getObj(var.varName);
                String name= var.varName;                

                if(var.varType.equals("java.lang.Object[]"))
                {
                    //this enclosing var is a must, cannot omit
                    writer.write(name + "_FOR_ENCLOSING_USE" + "\n");
                    writer.write("1\n");
                    writer.write("1\n");

                    writer.write(name + "[..]\n");
                    writer.write("[");

                    if (value!=null) {
                        for (Object subobj : (Collection)value)
                            writer.write(subobj + " ");
                    }

                    writer.write("]\n");
                    writer.write("1\n");
                }
                else
                {
                    // Change null values to nonsensical
                    // http://plse.cs.washington.edu/daikon/download/doc/developer.html#Nonsensical-values
                    Boolean isNonsense = false;
                    if (value==null && (var.varType.equals("float") || var.varType.equals("long") || var.varType.equals("double") || var.varType.equals("boolean") || var.varType.equals("int"))){
                        value = "nonsensical";
                        isNonsense = true;
                    }
                    writer.write(name + "\n");
                    writer.write(value + "\n");
                    if (isNonsense){
                        writer.write("2\n");
                    } else{
                        writer.write("1\n");
                    }
                }
            }
            
            writer.write("\n");
        }
    }

    @SuppressWarnings("unchecked")
    public static Point registerAllFields(Object parentObj, String layout)
    {
        Point point = new Point();

        if(parentObj==null)
        {
            System.out.println(layout +"is null");
            return point;
        }

        Class clazz = parentObj.getClass();

        while(clazz!=null && clazz.getName().startsWith(SYS_PACKAGE_NAME)) {
            try {
                //wisdom borrowed from https://stackoverflow.com/questions/17095628/loop-over-all-fields-in-a-java-class
                //
                Field[] fields = clazz.getDeclaredFields();
                for (Field f : fields) {
                    //System.out.println("CHANG: put layout:f " + layout + " " + f.getName());
                    Class t = f.getType();
                    f.setAccessible(true);
                    Object v = f.get(parentObj);

                    String varName = layout + "." + f.getName();
                    if(varName.contains("sessionTracker.sessionsWithTimeout") || varName.contains("sessionTracker.expirer") || varName.contains("serverStats.provider")) {
                        continue;
                    }
                    if(varName.contains("DEFAULT_TICK_TIME") || varName.contains("VERSION") || varName.contains("TXNLOG_MAGIC")){
                        continue;
                    }
                    if(varName.contains("params.") || varName.contains("$assertionsDisabled")){
                        continue;
                    }
                    if(varName.contains("directoryScanner.dataset") || varName.contains("saslServer.dnConf")){
                        continue;
                    }
                    if(!varName.contains("this$0")) {
                        if (SYS_PACKAGE_NAME.contains("hdfs")) {
                            if (varName.contains(".dn.") || varName.contains(".nn.") || varName.contains("blockManager.") || varName.contains("DataNode.") && varName.contains(".datanode.") || varName.contains("blockManagerInstance.") && varName.contains(".datanode.") || !varName.contains("NameNode.instance.namesystem.") && varName.contains(".namesystem.") || countSubstr(varName, ".namesystem.") > 1 || countSubstr(varName, ".dir.") > 1 || checkDuplicate(varName)) {
                                duplicate.add(varName);
                                continue;
                            }
                        } else if (SYS_PACKAGE_NAME.contains("hbase") && (!varName.contains("activeMasterManager.master.") && varName.contains(".master.") || varName.contains(".stopper.") || varName.contains(".abortable.") || varName.contains(".hMaster.") || varName.contains(".masterServices.") || varName.contains(".services.") || varName.contains(".server.") || !varName.contains("HMasterInstance.serverManager.") && varName.contains(".serverManager.") || checkDuplicate(varName))) {
                            duplicate.add(varName);
                            continue;
                        }

                        if (t.isPrimitive() || t.getName().toLowerCase().contains("atomic") && !t.getName().toLowerCase().contains("updater") && !t.getName().toLowerCase().contains("adder")) {
                            //no need to copy for primitive type
                            if(t.getName().toLowerCase().contains("atomic")){
                                if (t.getName().toLowerCase().contains("integer")) {
                                    point.add(new Var(varName, "int"),
                                            ((AtomicInteger) v).intValue());
                                } else if (t.getName().toLowerCase().contains("boolean")) {
                                    point.add(new Var(varName, "boolean"),
                                            ((AtomicBoolean) v).get());
                                }  else if (t.getName().toLowerCase().contains("long")) {
                                    point.add(new Var(varName, "long"),
                                            ((AtomicLong) v).longValue());
                                }
                            } else{
                                point.add(new Var(varName, t.getName()),
                                        v);
                            }

                            //System.out.println("CHANG: put varName " + varName);
                        } else if (!t.isPrimitive()){//(Map.class.isAssignableFrom(t)) {
                            //temp disable
                            //more details refer to https://coderanch.com/t/383648/java/java-reflection-element-type-List
                            Type type = f.getGenericType();
                            // System.out.println("DIMAS 2: " + type.getTypeName());
                            if (type instanceof ParameterizedType) {
                                ParameterizedType pt = (ParameterizedType) type;
                                if (pt.getActualTypeArguments().length < 1)
                                    throw new RuntimeException("Weird! This should never be smaller than 1!");

                                if (!pt.getActualTypeArguments()[0].getTypeName().startsWith("java.lang.") && !varName.contains("resultSetInstance.rows")) {
                                    continue;
                                }

                                if (pt.getActualTypeArguments()[0].getTypeName().startsWith("java.lang.String"))
                                    //skip for string
                                    continue;
                            }

                            // System.out.println("DIMAS 4: ");
                            if(v!=null && v instanceof Map){
                                //key set is dynamic with original map, so we need to copy them
                                Set set = new HashSet<Object>(((Map) v).keySet());
                                point.add(new Var(varName, "java.lang.Object[]"), set);
                            }

                            if (v != null && v instanceof List && varName.contains("resultSetInstance.rows")) {
                                if (varName.contains("resultSetInstance.rows")) {
                                    System.out.println("DIMAS: resultSet 6");
                                }

                                List<Object> vList = (List)v;
                                Set<Object> set = new HashSet((Collection)vList.stream().map((item) -> item.hashCode()).collect(Collectors.toList()));
                                point.add(new Var(varName, "java.lang.Object[]"), set);
                            }

                            // else
                            //     System.out.println(varName+" is null");

                            // point.add(new Var(varName, "java.lang.Object[]"), set);
                            //System.out.println("CHANG: put varName " + varName);
                        }
                    }
                }

            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }

            clazz = clazz.getSuperclass();
        }

        return point;
    }

    public static Map<String, Object> listNestedClassMember(Object instance, String layout, String layoutClass, int layer)
    {
        //we do not dive too deep than n layer
        if(layer>=SEARCH_DEPTH_THRESHOLD)
            return new HashMap<>();

        Map<String, Object> map = new HashMap<>();
        //System.out.println("CHANG: put layout"+layout);
        map.put(layout, instance);

        if(instance==null) {
            System.out.println(layout + "is null");
            return map;
        }

        Class clazz = instance.getClass();

        while(clazz!=null && clazz.getName().startsWith(SYS_PACKAGE_NAME)) {
            try {
                // System.out.println("DIMAS: 1 "+clazz.getName());
                Field[] fields = clazz.getDeclaredFields();
                for (Field f : fields) {
                    Class t = f.getType();
                    f.setAccessible(true);
                    Object v = f.get(instance);

                    if (v == null) {
                        //System.out.println("CHANG: skip v==null");
                        continue;
                    }

                    if (!t.getName().startsWith(SYS_PACKAGE_NAME)) {
                        //System.out.println("!t.getName().startsWith "+SYS_PACKAGE_NAME);
                        continue;
                    }

                    //skip ourself to avoid e.g. zksInstance.zksInstance.self.minSessionTimeout
                    if (f.getName().equals("zksInstance") || f.getName().equals("cfsInstance") || f.getName().equals("resultSetInstance") || f.getName().equals("fromResultListInstance") || f.getName().equals("fromPagerInstance") || f.getName().equals("CFMetadataInstance") || f.getName().equals("StorageService.instance") || f.getName().equals("Schema.instance") || f.getName().equals("CommitLog.instance") || f.getName().equals("QueryProcessor.instance") || f.getName().equals("SizeEstimatesRecorder.instance") || f.getName().equals("DataNode.instance") || f.getName().equals("NameNode.instance") || f.getName().equals("instance") || f.getName().equals("modifyTableProcedureInstance") || f.getName().equals("reopenTableRegionsProcedureInstance") || f.getName().equals("regionStatesInstance") || f.getName().equals("HMasterInstance") || f.getName().equals("HRegionServerInstance") || f.getName().equals("blockPoolManagerInstance") || f.getName().equals("blockManagerInstance") || f.getName().equals("dataXceiverServerInstance"))
                    {
                        continue;
                    }

                    //System.out.println("CHANG: dive in "+layout + "." + f.getName());

                    if (t.getName().startsWith(SYS_PACKAGE_NAME) && layoutClass.contains(t.getName())) {
                        // Skip circular reference
                        continue;
                    }
                    map.putAll(listNestedClassMember(v, layout + "." + f.getName(), layoutClass+"-"+t.getName(), layer + 1));
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            clazz = clazz.getSuperclass();
        }
        return map;
    }

    //CHANG: a state exposer
    public void dumpStates() throws Exception
    {
        initrootNodes();

        Point point = new Point();
        long start = System.currentTimeMillis();
        for(Map.Entry<String, Object> rootEntry: rootNodes.entrySet()) {
            String instanceName = rootEntry.getKey();
            Object instanceValue = rootEntry.getValue();
            if (instanceValue == null) {
                System.out.println(instanceName+" is null");
            }

            try {
                //manual version
                //lst.addAll(DaikonLogger.registerAllFields(zksInstance, "zksInstance"));
                //lst.addAll(DaikonLogger.registerAllFields(zksInstance.sessionTracker, "zksInstance.sessionTracker"));
                //lst.addAll(DaikonLogger.registerAllFields(zksInstance.zkDb.dataTree, "zksInstance.zkDb.dataTree"));
                for (Map.Entry<String, Object> entry : DaikonLogger.listNestedClassMember(instanceValue, instanceName, instanceValue.getClass().getName(), 0).entrySet()) {
                    point.addAll(DaikonLogger.registerAllFields(entry.getValue(), entry.getKey()));
                }

            } catch (Throwable e)
            {
                System.out.println("Exception happened when dumping states, skipped");
                e.printStackTrace();
            }
        }
        DaikonLogger.getLogger().addPoint(point);

        long elapsedTime = System.currentTimeMillis() - start;
        System.out.println("CHANG: dumpStates finished, took " + elapsedTime + " ms to iterate through " + point.size() + " states");
        System.out.println("CHANG: final fields in total: "+ point.counter);
    }

    private static int countSubstr(String original, String subStr) {
        int lastIndex = 0;
        int count = 0;

        while(lastIndex != -1) {
            lastIndex = original.indexOf(subStr, lastIndex);
            if (lastIndex != -1) {
                ++count;
                lastIndex += subStr.length();
            }
        }

        return count;
    }

    private static boolean checkDuplicate(String varName) {
        String[] token = varName.split(".");
        int len = token.length;

        for(int i = 0; i < len; ++i) {
            String item = token[i];
            if (item != "instance" && countSubstr(varName, item) > 1) {
                return true;
            }
        }

        return false;
    }
}