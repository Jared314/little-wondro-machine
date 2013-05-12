# Little Wondro Machine

A Clojure library designed to compile a Prismatic Graph into a Storm bolt-spec.

## Usage

```clojure
(def g {...})

(storm/topology {"spout1" (storm/spout-spec ...)}
  (little-wondro-machine.core/bolt-spec-compile g))
```

## Reference

Sesame Street - Grover sells weather
http://www.youtube.com/watch?v=UbyuFzoQGV0

## License

Copyright © 2013 Jared Lobberecht
Distributed under the Eclipse Public License, the same as Clojure.
