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
    (future (.waitFor proc) (swap! proc-state assoc :exited? true) (println "done"))
    proc-state))


(comment

  (async-proc "echo" "123")
  (def x (async-proc "bash" "-c" "echo 1; sleep 3; echo 2; sleep 2; echo 3"))

  (async/<!! (:out @x))
  (.. (:proc x) hasExited)
  (.exitcode (:proc x))
  
  ;; -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-
  (require '[clojure.string :as s])
  (require '[clojure.java.io :as io])
  
  (some-> (sh "mdfind" "-name" "chrome" "-onlyin" "/Applications/")
          :out
          s/split-lines
          first
          (io/file "Contents/MacOS/Google Chrome")
          (#(when (.exists %) %))
          .getPath)
  
  ;; -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-


  (require '[cemerick.pomegranate])
  (cemerick.pomegranate/add-dependencies
   :coordinates '[[lein-cljsbuild "1.0.4-SNAPSHOT"]]
   :repositories (merge cemerick.pomegranate.aether/maven-central {"clojars" "http://clojars.org/repo"}))


  (cemerick.pomegranate/add-classpath "/Users/robert/.m2/repository/lein-cljsbuild/lein-cljsbuild/1.0.4-SNAPSHOT/lein-cljsbuild-1.0.4-SNAPSHOT.jar")
  
  (cemerick.pomegranate/add-classpath "/Users/robert/Dropbox/Projects/clojure/lein-cljsbuild/support/src")

  )
