package org.javaportability.callgraph.nodeset;

import java.util.regex.Pattern;

import org.javaportability.misc.MethodPath;

public class WildcardNodeSet implements NodeSet {
    private Pattern pattern;
    
    public WildcardNodeSet(String pattern) {
        String[] parts = pattern.replace('.', '/').split("\\*");
        StringBuilder regex = new StringBuilder(128);
        for (int i = 0; i < parts.length - 1; ++i) {
            if (!parts[i].isEmpty()) {
                regex.append(Pattern.quote(parts[i]));
            }
            regex.append(".*");
        }
        if (parts.length > 0) {
            regex.append(Pattern.quote(parts[parts.length - 1]));
        }
        
        if (!pattern.isEmpty() && pattern.charAt(pattern.length() - 1) == '*') {
            regex.append(".*");
        }
        
        this.pattern = Pattern.compile(regex.toString());
    }
    
    @Override
    public boolean containsClass(String cls) {
        return matches(cls);
    }
    
    @Override
    public boolean containsMethod(MethodPath path) {
        return containsClass(path.getOwner()) || matches(path.getOwner() + "::" + path.getName());
    }
    
    private boolean matches(String s) {
        return pattern.matcher(s).matches();
    }
}
