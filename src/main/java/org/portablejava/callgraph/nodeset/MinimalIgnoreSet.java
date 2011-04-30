package org.portablejava.callgraph.nodeset;

import org.portablejava.analysis.AnalysisSettings;
import org.portablejava.misc.MethodPath;

/**
 * Contains array classes, since they can never be discovered.
 * This is automatically included by {@link AnalysisSettings}.
 */
public class MinimalIgnoreSet implements NodeSet {

    @Override
    public boolean containsClass(String className) {
        return className.startsWith("[");
    }

    @Override
    public boolean containsMethod(MethodPath path) {
        return containsClass(path.getOwner());
    }
    
}
