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
        proc-state (atom {:out out :err err :proc proc :exited? false})]
    (future (.waitFor proc) (swap! proc-state assoc :exited? true))
    proc-state))

(defn process-obj [proc]
  (:proc @proc))

(defn pid [proc]
  ;; seriously? Java has no way of official getting a pid of a process???
  (let [p (process-obj proc)]
    (if-let [pid-field (.getDeclaredField (class p) (name "pid"))]
      (-> pid-field
          (doto (.setAccessible true))
          (.get p)))))

(defn exited? [proc]
  (:exited? @proc))

(defn exit-code [proc]
  (:code @proc))

(defn- read-chan [proc chan-name]
  (loop [val ""]
    (if val
      (recur (str val (async/<!!(chan-name @proc))))
      val)))

(defn stdout [proc]
  (read-chan proc :out))

(defn stderr [proc]
  (read-chan proc :err))

(defn signal
  ([proc] (signal proc "SIGTERM"))
  ([proc sig]
     (print "signaling... " "kill" "-s" sig (pid proc))
     (if (exited? proc)
       (exit-code proc)
       (if-let [proc-id (pid proc)]
         (let [kill-proc (async-proc "kill" (format "-s %s %s" sig proc-id))]
           (println "signaling... " "kill" "-s" sig proc-id)
           (println "signaling... " (process-obj kill-proc))
           (.waitFor (process-obj kill-proc))
           (exit-code proc))
         (-> (format "cannot kill process %s" proc) Exception. throw)))))

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
