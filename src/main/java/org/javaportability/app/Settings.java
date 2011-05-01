package org.javaportability.app;

import java.util.LinkedList;
import java.util.List;

import org.javaportability.analysis.AnalysisSettings;

public class Settings {
    public List<String> searchPath = null;
    public List<String> configFiles = null;
    public boolean help = false;
    public boolean trace = false;
    public boolean verbose = false;
    public List<String> targets = new LinkedList<String>();
    public AnalysisSettings analysisSettings = null;
}
