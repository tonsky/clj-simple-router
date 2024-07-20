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
    (are [p key params] (= [key params] (router/match matcher p))
      "GET  /"                   :get-index   []
      "GET  /login"              :get-login   []
      "POST /login"              :post-login  []
      "HEAD /login"              :all         ["HEAD" "login"]
      "GET  /article/123"        :get-article ["123"]
      "POST /article/123"        :any-article ["POST" "123"]
      "GET  /article"            :get-any     ["article"]
      "GET  /article/123/update" :get-article-any-update ["123"]
      "GET  /any"                :get-any     ["any"]
      "GET  /any/other"          :get-all     ["any/other"]
      "POST /article/123/update" :all         ["POST" "article/123/update"])))

(deftest test-routes
  (let [routes  (router/routes
                  "GET  /"                 []            [:get-index]
                  "GET  /login"            []            [:get-login]
                  "POST /login"            []            [:post-login]
                  "GET  /article/*"        [id]          [:get-article id]
                  "GET  /article/*/update" [id]          [:get-article-any-update id]
                  "*    /article/*"        [method id]   [:any-article method id]
                  "GET  /*"                [path]        [:get-any path]
                  "GET  /**"               [path]        [:get-all path]
                  "*    /**"               [method path] [:all method path])
        router (router/router routes)]
    (are [method uri match] (= match (router {:request-method method, :uri uri}))
      :get  "/"                   [:get-index]
      :get  "/login"              [:get-login]
      :post "/login"              [:post-login]
      :head "/login"              [:all "HEAD" "login"]
      :get  "/article/123"        [:get-article "123"]
      :post "/article/123"        [:any-article "POST" "123"]
      :get  "/article"            [:get-any "article"]
      :get  "/article/123/update" [:get-article-any-update "123"]
      :get  "/any"                [:get-any "any"]
      :get  "/any/other"          [:get-all "any/other"]
      :post "/article/123/update" [:all "POST" "article/123/update"])))

(deftest test-wildcards
  (let [routes {"GET /**"   :a
                "GET /x/**" :b}
        matcher (router/make-matcher routes)]
    (are [p key params] (= [key params] (router/match matcher p))
      "GET /"    :a []
      "GET /x"   :b []
      "GET /x/y" :b ["y"])))

(deftest test-edge-cases
  (let [routes {"GET /a/*/b" :ab
                "GET /**"    :all}
        matcher (router/make-matcher routes)]
    (are [p key params] (= [key params] (router/match matcher p))
      "GET /a/*/b" :ab ["*"])))

(deftest test-router
  (let [make-handler (fn [key]
                       (fn [req]
                         [key (:path-params req)]))
        routes {"GET  /"                 (make-handler :get-index)
                "GET  /login"            (make-handler :get-login)
                "POST /login"            (make-handler :post-login)
                "GET  /article/*"        (make-handler :get-article)
                "GET  /article/*/update" (make-handler :get-article-any-update)
                "*    /article/*"        (make-handler :any-article)
                "GET  /*"                (make-handler :get-any)
                "GET  /**"               (make-handler :get-all)
                "*    /**"               (make-handler :all)}
        router (router/router routes)]
    (are [method uri key path-params] (= [key path-params]
                                        (router {:request-method method
                                                 :uri            uri}))
      :get  "/"                   :get-index   []
      :get  "/login"              :get-login   []
      :post "/login"              :post-login  []
      :head "/login"              :all         ["HEAD" "login"]
      :get  "/article/123"        :get-article ["123"]
      :post "/article/123"        :any-article ["POST" "123"]
      :get  "/article"            :get-any     ["article"]
      :get  "/article/123/update" :get-article-any-update ["123"]
      :get  "/any"                :get-any     ["any"]
      :get  "/any/other"          :get-all     ["any/other"]
      :post "/article/123/update" :all         ["POST" "article/123/update"])))

(comment
  (test/test-ns *ns*)
  (test/run-test-var #'test-edge-cases))
