package org.portablejava.app;

import java.util.LinkedList;
import java.util.List;

public class Settings {
    public List<String> searchPath = null;
    public boolean help = false;
    public boolean trace = false;
    public boolean verbose = false;
    public List<String> targets = new LinkedList<String>();
}
