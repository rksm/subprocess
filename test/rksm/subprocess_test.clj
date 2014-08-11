(ns rksm.subprocess-test
  (:require [clojure.test :refer :all]
            [rksm.subprocess :as subp]
            [clojure.core.async :as async]))

(deftest proc-test
  (testing "Run a simple process asynchronously"
    (let [cmd "echo 1; sleep .1; echo 2; sleep .1; echo 3"
          p (subp/async-proc "bash" "-c" cmd)]
      (is (= "1" (async/<!! (:out @p))))
      (Thread/sleep 120)
      (is (= "2" (async/<!! (:out @p))))
      (Thread/sleep 120)
      (is (= true (:exited? @p))))))
