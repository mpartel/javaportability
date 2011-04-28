# strictfp-tool #

Analyzes a body of Java bytecode for strictfp-safety. A method that does floating-point arithmetic and is not marked strictfp-safe is considered _unsafe_. The tool is finds all unsafe methods transitively called from a set of root classes or methods.

The tool is meant to ensure code is portably deterministic. An intended use-case is a game that uses a lock-step synchronization strategy and thus requires core game logic to behave exactly the same on all peers.
 

## Caveats ##

* Not yet quite finished.
* Doesn't (and can't) understand reflection.

## TODO ##

* Ignore lists ("don't care that this method is called").
* Blacklists ("ensure that this method is not called").
* Detecting other sources of non-determinism: native methods,
  methods with ill-defined implementations,
  accessing a given static field, using random numbers etc.
