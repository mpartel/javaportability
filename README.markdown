# Java portability tool #

Analyzes a body of Java bytecode for portability and determinism. An intended use-case is a game that uses a lock-step synchronization strategy and thus requires core game logic to behave exactly the same on all peers.

The tool finds all _unsafe_ methods transitively called from a set of root classes or methods. A method is unsafe if

* it does floating-point arithmetic and is not marked `strictfp`.
* it's marked `native` and is not whitelisted [TODO].
* it's blacklisted [TODO].

## Caveats ##

* Polymorphism: Currently if code calls a method on an interface or a superclass and a subclass overrides that method with unsafe code, the tool will not issue a warning. In general the tool might not even know about the subclass.
* Reflection: There's no way to see whether unsafe code is called through reflection.

## TODO ##

* Whitelists and blacklists - a method may always/never be called.
* Annotations for whitelisting and blacklisting.
* Native methods, unless whitelisted.
* A standard blacklist and whitelist covering some of the Java standard library.
  - For instance, the iteration order of HashMap/HashSet is not guaranteed.
    On the other hand the floating point math used on the load factor can't
    affect lookup behavior, so it's safe even thought it's not strictfp.
* A build script.
  - Currently an Eclipse project.
  - JAR release targets.
* JavaDoc
* Modify existing bytecode
  - add strictfp flags
  - Advanced: alter uses of unportable code
    - e.g. `Math` -> `StrictMath`, `new HashMap` -> `new LinkedHashMap`.

## Links ##

* http://en.wikipedia.org/wiki/Strictfp
* http://stackoverflow.com/questions/517915/when-to-use-strictfp-keyword-in-java
* http://java.sun.com/docs/books/jls/strictfp-changes.pdf
