(ns user
  (:require
    [duti.core :as duti]))

(duti/set-dirs "src" "test")

(def reload
  duti/reload)

(def -main
  duti/-main)

(defn test-all []
  (duti/test #"clj-simple-router\..*-test"))

(defn -test-main [_]
  (duti/test-exit #"clj-simple-router\..*-test"))
