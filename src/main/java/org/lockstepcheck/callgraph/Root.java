package org.lockstepcheck.callgraph;

import java.util.regex.Pattern;

import org.lockstepcheck.callgraph.CallGraph.MethodNode;
import org.lockstepcheck.misc.MethodPath;

public class Root {
    private static final Pattern ACCEPT_ALL = Pattern.compile(".*");
    private final String className;
    private final Pattern methodPattern;
    private final Pattern methodDescPattern;
    
    public Root(String className) {
        this(className, ACCEPT_ALL);
    }
    
    public Root(Class<?> cls) {
        this(cls.getName().replace('.', '/'));
    }
    
    public Root(String className, String methodName) {
        this(className, exactMatcher(methodName));
    }

    public Root(MethodPath methodPath) {
        this(methodPath.getOwner(), exactMatcher(methodPath.getName()), exactMatcher(methodPath.getDesc()));
    }
    
    public Root(String className, Pattern methodPattern) {
        this(className, methodPattern, ACCEPT_ALL);
    }
    
    public Root(String className, Pattern methodPattern, Pattern methodDescPattern) {
        this.className = className;
        this.methodPattern = methodPattern;
        this.methodDescPattern = methodDescPattern;
    }
    
    private static Pattern exactMatcher(String methodName) {
        return Pattern.compile(Pattern.quote(methodName));
    }
    
    public String getClassName() {
        return className;
    }
    
    public Pattern getMethodPattern() {
        return methodPattern;
    }
    
    public Pattern getMethodDescPattern() {
        return methodDescPattern;
    }

    public boolean matchesMethod(MethodNode m) {
        return methodPattern.matcher(m.getName()).matches() &&
               methodDescPattern.matcher(m.getDesc()).matches();
    }
}