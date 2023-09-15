(defproject io.github.tonsky/clj-simple-router "0.0.0"
  :description "A simple, order-independent Ring router"
  :license     {:name "MIT" :url "https://github.com/tonsky/clj-simple-router/blob/master/LICENSE"}
  :url         "https://github.com/tonsky/clj-simple-router"
  :dependencies
  [[org.clojure/clojure "1.11.1"]]
  :deploy-repositories
  {"clojars"
   {:url "https://clojars.org/repo"
    :username "tonsky"
    :password :env/clojars_token
    :sign-releases false}})