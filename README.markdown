# lockstepcheck #

Analyzes a body of Java bytecode for portability and determinism. An intended use-case is a game that uses a lock-step synchronization strategy and thus requires core game logic to behave exactly the same on all peers.

The tool is finds all _unsafe_ methods transitively called from a set of root classes or methods. A method is unsafe if

* it does floating-point arithmetic and is not marked `strictfp`.
* it's marked `native` and is not whitelisted [TODO].
* it's blacklisted [TODO].

## Caveats ##

* Polymorphism: Currently if code calls a method on an interface or a superclass and a subclass overrides that method with unsafe code, the tool will not issue a warning. In general the tool might not even know about the subclass.
* Reflection: There's no way to see whether unsafe code is called through reflection.

## TODO ##

* Ignore lists ("don't care that this method is called").
* Native methods, unless whitelisted.
* Blacklists ("ensure that this method is not called").
* A standard blacklist covering some of the Java standard library.
