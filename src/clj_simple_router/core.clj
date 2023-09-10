(ns clj-simple-router.core
  (:require
    [clojure.string :as str]))

(defn- compare-masks [as bs]
  (let [a (first as)
        b (first bs)]
    (cond
      (= nil as bs)  0
      (= nil as)    -1
      (= nil bs)     1
      (= "**" a b)  (recur (next as) (next bs))
      (= "**" a)     1
      (= "**" b)    -1
      (= "*" a b)   (recur (next as) (next bs))
      (= "*" a)      1
      (= "*" b)     -1
      :else         (recur (next as) (next bs)))))

(defn- split [s]
  (let [[method path] (str/split s #"[/\s]+" 2)]
    (->> (cons method (str/split path #"/+"))
      (remove str/blank?)
      vec)))

(defn make-matcher [routes]
  (->> routes
    (map (fn [[mask v]] [(split mask) v]))
    (sort-by first compare-masks)))

(defn- matches? [mask path]
  (loop [mask mask
         path path]
    (let [m (first mask)
          p (first path)]
      (cond
        (= "**" m)        true
        (= nil mask path) true
        (= nil mask)      false
        (= nil path)      false
        (= m p)           (recur (next mask) (next path))
        (= "*" m)         (recur (next mask) (next path))))))

(defn match [matcher path]
  (let [path (split path)]
    (reduce
      (fn [_ [mask v]]
        (when (matches? mask path)
          (reduced v)))
      nil matcher)))
