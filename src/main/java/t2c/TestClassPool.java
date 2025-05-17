package t2c;

import java.util.*;

import daikon.GlobalState;
import jdk.nashorn.internal.objects.Global;
import org.reflections8.*;
import org.reflections8.scanners.*;
import org.reflections8.util.*;

public class TestClassPool {

    static private Map<String, Class> classMap = new HashMap<>();

    static public void registerAllClass() throws Exception
    {
        // System.out.println(System.getProperty("java.class.path"));
        Reflections reflections = new Reflections(GlobalState.Config.getSystemName(), new SubTypesScanner(false));

        Set<Class<? extends Object>> allClasses =
                reflections.getSubTypesOf(Object.class);
        
        System.out.println(allClasses.size());
        for(Class clazz:allClasses)
        {
            if(clazz.getName().toLowerCase().contains("test")){
                System.out.println(clazz.getName());
            }

            if (GlobalState.Config.getSystemName().contains("hbase") && clazz.getName().contains("Test") && !clazz.getName().contains("$")) {
                System.out.println("registering for test class " + clazz.getName());
                register(clazz);
            } else if (GlobalState.Config.getSystemName().contains("hdfs") && clazz.getName().contains("Test") && (clazz.getName().contains("namenode") || clazz.getName().contains("datanode"))) {
                System.out.println("registering for test class " + clazz.getName());
                register(clazz);
            } else if (clazz.getName().endsWith("Test")) {
                System.out.println("registering for test class " + clazz.getName());
                register(clazz);
            }
            // if(!clazz.getName().endsWith("Test"))
            //     continue;
            //
            // System.out.println("registering for test class "+clazz.getName());
            // register(clazz);
        }
    }

    static public void register(Class clazz)
    {
        classMap.put(clazz.getName(),clazz);
    }

    static public Class getClass(String classname) throws Exception
    {
        return Class.forName(classname);
    }

    static Collection<Class> getClasses()
    {
        return classMap.values();
    }
}