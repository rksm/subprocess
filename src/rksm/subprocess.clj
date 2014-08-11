(ns rksm.subprocess
  (:require [clojure.core.async :as async]
            [clojure.java.io :as io]))

(defn capture-stream
  [stream]
  (let [reader (io/reader stream)
        read-chan (async/chan)]
    (async/go-loop []
      (if-let [line (.readLine reader)]
        (do
          (async/put! read-chan line)
          (recur))
        (async/close! read-chan)))
    read-chan))

(defn async-proc
  [& cmd+args]
  (let [proc (.. Runtime getRuntime (exec (into-array String cmd+args)))
        out (capture-stream (.getInputStream proc))
        err (capture-stream (.getErrorStream proc))
        proc-state (atom {:out out :err err :proc proc :exited false})]
    (future (.waitFor proc) (swap! proc-state assoc :exited? true))
    proc-state))


(comment

  (async-proc "echo" "123")

  (let [proc (async-proc "bash" "-c" "echo 1; sleep .3; echo 2; sleep .3; echo 3")]
    (future
      (while (not (:exited? @proc))
        (println "Got output" (async/<!! (:out @proc))))
      (println "process exited")))
  )
