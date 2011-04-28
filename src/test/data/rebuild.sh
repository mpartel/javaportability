#!/bin/sh
cd "`dirname $0`"
javac pkg_*/*.java
jar cvf withOneClass.jar pkg_in_jar
jar cvf withNoClasses.jar DummyFile
