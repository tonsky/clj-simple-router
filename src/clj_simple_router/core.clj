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
  (let [[_ method path] (re-matches #"(?:([a-zA-Z]+)\s+)?(.*)" s)]
    (->> (str/split path #"/+")
      (cons method)
      (remove str/blank?)
      (map str/trim)
      vec)))

(defn make-matcher
  "Given set of routes, builds matcher structure. See `router`"
  [routes]
  (->> routes
    (map (fn [[mask v]] [(split mask) v]))
    (sort-by first compare-masks)))

(defn- matches? [mask path]
  (loop [mask   mask
         path   path
         params []]
    (let [m (first mask)
          p (first path)]
      (cond
        (= "**" m)        (if path
                            (conj params (str/join "/" path))
                            params)
        (= nil mask path) params
        (= nil mask)      nil
        (= nil path)      nil
        (= "*" m)         (recur (next mask) (next path) (conj params p))
        (= m p)           (recur (next mask) (next path) params)))))

(defn- match-impl [matcher path]
  (reduce
    (fn [_ [mask v]]
      (when-some [params (matches? mask path)]
        (reduced [v params])))
    nil matcher))

(defn match
  "Given matcher (see `make-matcher`) and a path, returns a vector of match and path params.
   If nothing is found, returns nil.
   
     (let [matcher (make-matcher {\"GET /a/*/b/*\" :key})]
       (match matcher \"/a/1/b/2\"))
     ;; => [:key [\"1\" \"2\"]]"
  [matcher path]
  (match-impl matcher (split path)))

(defn router
  "Given set of routes, returns ring handler. Routes are map from strings to handlers:
   
     {<route> (fn [req] {:status 200, ...})}, ...}
   
   Some route examples:
   
     \"GET /\"                 ;; will match / only
     \"GET /login\"            ;; will match /login
     \"GET /article/*\"        ;; will match /article/1, /article/xxx, but not /article or /article/1/update
     \"GET /article/*/*\"      ;; will match e.g. /article/1/update
     \"GET /article/**\"       ;; will match both /article, /article/1 and /article/1/update and deeper
     \"POST /login\"           ;; will match POST /login
     \"GET /article/*/update\" ;; single stars can be in the middle of path, but double stars can’t
     \"* /article\"            ;; method can be wildcard, too
     \"GET /**\"               ;; will match anything GET
     \"* /**\"                 ;; will match anything
   
   Routes are order-independent and sorted automatically by specificity. This allows overlapping:
   
     GET /article/*
     GET /article/new
   
   can both co-exist, and /article/new will always be checked first before falling back into /article/*.
   
   Any explicit path segment has higher specificity than a star, so this:
   
     GET /article
   
   will always be matched before this (one path segment of any):
   
     GET /*
  
   Single star, in turn, has higher specificity than double star,
   so this (any number of segments, 0-∞) will always match last:
   
     GET /**

   Values captured by * and ** will be passed down as :path-params vector:
   
     {\"GET /a/*/b/*\"
      (fn [req]
        (let [[p p2] (:path-params req)] ;; e.g. [\"1\" \"2\"] if uri was /a/1/b/2
          ...))
     
      \"GET /media/**\"
      (fn [req]
        (let [path (:path-params req)] ;; [] for /media
                                       ;; [\"abc\"] for /media/abc
                                       ;; [\"x/y/z\"] for /media/x/y/z
          ...))}
      
      \"* /article/*\"
      (fn [req]
        (let [[method id] (:path-params req)] ;; [\"GET\" \"1\"] for GET /article/1
          ...))}
   "
  [routes]
  (let [matcher (make-matcher routes)]
    (fn [req]
      (let [{:keys [request-method uri]} req
            path (cons
                   (str/upper-case (name request-method))
                   (remove str/blank? (str/split uri #"/+")))]
        (when-some [[handler params] (match-impl matcher path)]
          (handler (assoc req :path-params params)))))))

(defn wrap-routes
  "Wraps existing handler, and if no route matched, will pass control to it"
  [handler routes]
  (let [router (router routes)]
    (fn [req]
      (or
        (router req)
        (handler req)))))

(defmacro routes
  "A convenience macro that helps you define routes.
   
     body :: (<path-template> <path-params-vector> <handler-body>)+
   
   By default, `<path-params-vector>` will be bound to `(:path-params req)`. If `<path-params-vector>` is not a vector, it’ll be bound to `req` instead.
   
   Returns routes map, suitable to be passed to `router` or `wrap-routes`.
   
   Example:
   
     (routes
       \"GET /post/*/xxx/*\" [a b]
       {:status 200, :body (str a b)}
       
       \"* /**\" req
       (let [[method path] (:path-params req)]
         ...))
   "
  [& body]
  (let [req-sym 'req]
    (into {}
      (for [[path params handler] (partition 3 body)]
        [path `(fn [~req-sym]
                 (let [~params ~(if (vector? params)
                                  `(:path-params ~req-sym)
                                  req-sym)]
                   ~handler))]))))
