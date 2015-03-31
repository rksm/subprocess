(ns rksm.subprocess
  (:require [clojure.core.async
             :as async
             :refer [go go-loop chan <! >! <!! >!! alt!! timeout close! put!]]
            [clojure.java.io :as io]))

(defn capture-stream
  [stream]
  (let [reader (io/reader stream)
        read-chan (chan)]
    (go-loop []
      (if-let [line (.readLine reader)]
        (do
          (put! read-chan line)
          (recur))
        (close! read-chan)))
    read-chan))

(defn async-proc
  [& cmd+args]
  (let [proc (.. Runtime getRuntime (exec (into-array String cmd+args)))
        out (capture-stream (.getInputStream proc))
        err (capture-stream (.getErrorStream proc))
        proc-state (atom {:out out :err err :proc proc :exited? false})]
    (future (.waitFor proc) (swap! proc-state assoc :exited? true))
    proc-state))

(defn process-obj [proc]
  (:proc @proc))

(defn- get-field
  [proc field-name]
  (let [p (process-obj proc)]
    (if-let [pid-field (.getDeclaredField (class p) (name field-name))]
      (-> pid-field
        (doto (.setAccessible true))
        (.get p)))))

(defn pid
  "seriously? Java has no way of official getting a pid of a process???"
  [proc]
  (get-field proc "pid"))

(defn exited? [proc] (:exited? @proc))

(defn exit-code [proc] (get-field proc "exitcode"))

(defn- read-chan [proc chan-name]
  (loop [val ""]
    (if val
      (recur (str val (<!! (chan-name @proc))))
      val)))

(defn stdout [proc]
  (read-chan proc :out))

(defn stderr [proc]
  (read-chan proc :err))

(defn signal
  ([proc] (signal proc "KILL"))
  ([proc sig]
   (if (exited? proc)
     (exit-code proc)
     (let [kill-proc (.exec (java.lang.Runtime/getRuntime)
                            (format "kill -s %s %s" sig (pid proc)))
           exit-chan (chan)]
       (async/go
        (.waitFor (process-obj proc))
        (>! exit-chan (or (exit-code proc) :exited)))
       (alt!!
        (timeout 100) :timeout
        exit-chan ([code] code))))))

(comment

  (def p (async-proc "ls"))
  (def chan-name :out)
  (async/<!!(chan-name @p))

  (stdout p)
  (stderr p)
  123
  p
  (async-proc "echo" "123")

  (let [proc (async-proc "bash" "-c" "echo 1; sleep .3; echo 2; sleep .3; echo 3")]
    (future
      (while (not (:exited? @proc))
        (println "Got output" (async/<!! (:out @proc))))
      (println "process exited")))
  )
