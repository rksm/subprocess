# subprocess

Small wrapper around Java processes. Starts a process and returns an atom map that provides access to the process object, it's exit state and core.async channels for stdout and stderr.

The main purpose of this lib is it to provide easy access to the output streams of a process, without having to think about pumping threads and such.

## Usage

Start and forget:

```clojure
(async-proc "echo" "123")
```

Listen on out with core.async:

```clojure
(let [proc (async-proc "bash" "-c" "echo 1; sleep .3; echo 2; sleep .3; echo 3")]
    (future
      (while (not (:exited? @proc))
        (println "Got output" (async/<!! (:out @proc))))
      (println "process exited")))
```

## License

Copyright © 2014,2015 Robert Krahn

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
