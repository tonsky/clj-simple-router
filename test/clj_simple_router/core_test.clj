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
    (are [path match] (= match (router/match matcher path))
      "GET /"                    :get-index
      "GET /login"               :get-login
      "POST /login"              :post-login
      "HEAD /login"              :all
      "GET /article/123"         :get-article
      "POST /article/123"        :any-article
      "GET /article"             :get-any
      "GET /article/123/update"  :get-article-any-update
      "GET /any"                 :get-any
      "GET /any/other"           :get-all
      "POST /article/123/update" :all))
  
  (testing "** matches empty too"
    (let [routes {"GET /**"   :a
                  "GET /x/**" :b}
          matcher (router/make-matcher routes)]
      (are [path match] (= match (router/match matcher path))
        "GET /" :a
        "GET /x" :b
        "GET /x/y" :b))))

(comment
  (test/test-ns *ns*)
  (test/run-test-var #'test-basics))
