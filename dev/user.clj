(ns user
  (:require
    [clojure.core.server :as server]
    [clojure.test :as test]
    [clojure.tools.namespace.repl :as ns]))

(ns/disable-reload!)

(ns/set-refresh-dirs "src" "dev" "test")

(def lock
  (Object.))

(defn position []
  (let [trace (->> (Thread/currentThread)
                (.getStackTrace)
                (seq))
        el    ^StackTraceElement (nth trace 4)]
    (str "[" (clojure.lang.Compiler/demunge (.getClassName el)) " " (.getFileName el) ":" (.getLineNumber el) "]")))

(defn p [form]
  `(let [t# (System/currentTimeMillis)
         res# ~form]
     (locking lock
       (println (str "#p" (position) " " '~form " => (" (- (System/currentTimeMillis) t#) " ms) " res#)))
     res#))

(defn reload []
  (set! *warn-on-reflection* true)
  (let [res (ns/refresh)]
    (if (= :ok res)
      :ok
      (do
        (.printStackTrace ^Throwable res)
        (throw res))))
  :ready)

(defn test-all []
  (reload)
  (let [{:keys [fail error] :as res} (test/run-all-tests #"clj-simple-router\..*")
        res (dissoc res :type)]
    (if (pos? (+ fail error))
      (throw (ex-info "Tests failed" res))
      res)))

(defn -main [& {:as args}]
  (let [port (parse-long (get args "--port" "5555"))]
    (server/start-server
      {:name          "repl"
       :port          port
       :accept        'clojure.core.server/repl
       :server-daemon false})
    (println "Started Socket REPL server on port" port)))

(defn -test [_]
  (reload)
  (let [{:keys [fail error]} (test/run-all-tests #"clj-simple-router\..*")]
    (System/exit (+ fail error))))
