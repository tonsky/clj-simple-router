(ns clj-simple-router.core-test
  (:require
    [clj-simple-router.core :as router]
    [clojure.test :as test :refer [is are deftest testing]]))


(deftest test-basics
  (let [routes  {"GET  /"                 :get-index
                 "GET  /login"            :get-login
                 "POST /login"            :post-login
                 "GET  /article/*"        :get-article
                 "GET  /article/*/update" :get-article-any-update
                 "*    /article/*"        :any-article
                 "GET  /*"                :get-any
                 "GET  /**"               :get-all
                 "*    /**"               :all}
        matcher (router/make-matcher routes)]
    (are [p m] (= m (router/match matcher p))
      "GET  /"                   [:get-index   []]
      "GET  /login"              [:get-login   []]
      "POST /login"              [:post-login  []]
      "HEAD /login"              [:all         ["HEAD" "login"]]
      "GET  /article/123"        [:get-article ["123"]]
      "POST /article/123"        [:any-article ["POST" "123"]]
      "GET  /article"            [:get-any     ["article"]]
      "GET  /article/123/update" [:get-article-any-update ["123"]]
      "GET  /any"                [:get-any     ["any"]]
      "GET  /any/other"          [:get-all     ["any/other"]]
      "POST /article/123/update" [:all         ["POST" "article/123/update"]])))

(deftest test-wildcards
  (let [routes {"GET /**"   :a
                "GET /x/**" :b}
        matcher (router/make-matcher routes)]
    (are [p m] (= m (router/match matcher p))
      "GET /"    [:a []]
      "GET /x"   [:b []]
      "GET /x/y" [:b ["y"]])))

(deftest test-edge-cases
  (let [routes {"GET /a/*/b" :ab
                "GET /**"    :all}
        matcher (router/make-matcher routes)]
    (are [p m] (= m (router/match matcher p))
      "GET /a/*/b" [:ab ["*"]])))

(comment
  (test/test-ns *ns*)
  (test/run-test-var #'test-edge-cases))
