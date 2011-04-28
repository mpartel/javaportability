package org.strictfptool.ignoreset;

import java.util.regex.Pattern;

import org.strictfptool.misc.MethodPath;

public class WildcardIgnoreSet implements IgnoreSet {
    private Pattern pattern;
    
    public WildcardIgnoreSet(String pattern) {
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
        return pattern.matcher(cls).matches();
    }
    
    @Override
    public boolean containsMethod(MethodPath path) {
        if (this.containsClass(path.getOwner())) {
            return true;
        } else {
            return false;
        }
    }
}
