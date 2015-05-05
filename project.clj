(defproject rksm/subprocess "0.1.3"
  :description "Spawn subprocesses and listen to stdout and stderr via core.async."
  :url "https://github.com/rksm/subprocess"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]
  :scm {:url "git@github.com:rksm/subprocess.git"}
  :pom-addition [:developers [:developer
                              [:name "Robert Krahn"]
                              [:url "http://robert.kra.hn"]
                              [:email "robert.krahn@gmail.com"]
                              [:timezone "-9"]]])
