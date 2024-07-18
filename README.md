# A simple, order-independent Ring router

## Using

Add this to deps.edn:

```
io.github.tonsky/clj-simple-router {:mvn/version "0.1.0"}
```

## Rationale

clj-simple-router was born out of the desire to have all three:

- Overlapping routes
- Order-independent routes declarations
- Minimal set of features

### Overlapping routes

Imagine you have a URL scheme that has posts under:

```
/post/:id
```

and your ids are, for example, always numerical. So, `/post/123` and such.

Now, you need a URL to create a new post. Why not have `/post/new`? Technically, it will never collide with post id, because those are numerical.

Sometimes you are even okay with overlapping, e.g. `/post/new` is treated as special, meaning you’ll never be able to create a post with `:id = "new"`, but you are ok with that. And there are a million possible scenarios like this. The important thing is, some amount of overlap is ok.

### Order-independent route declarations

Traditional routers solve this by forcing you to specify URLs in order. If you specify it like

```
/post/new
/post/:id
```

It will work as expected. If, however, you specify it in reverse order:

```
/post/:id
/post/new
```

then `/post/new` will never be reached.

This breaks modularity. If you split your app into modules, each defining its own routes, it might be hard to bring them together in the correct order. Even worse, the order you list your routes/imports becomes _an invisible dependency_ that’s too easy to break.

### Solution

clj-simple-router solves this by allowing you to define your routes in any order and sorting them for you so that more specific routes always come before less specific ones.

## How to use

Require:

```
(require '[clj-simple-router.core :as router])
```

Route definitions are maps from a path template to a handler, like this:

```
{"GET /"
 (fn [req]
   {:status 200, :body ...})

 "GET /post/*"
 (fn [req]
   (let [[id] (:path-params req)]
     {:status 200, :body ...}))

 ...}
```

Since they are maps, it doesn’t matter in what order you define them. Feel free to pass those around, merge, generate programmatically, etc.

Path templates:

- Method is part of it, separated by space: `GET /a/b/c`, `POST /x`, etc.
- Wildcard `*` replaces a single path segment: `GET /post/*` will match `/post/123` but not `/post/123/update`
- Double wildcard `**` replaces any number of path segments, including zero. `GET /post/**` will match `/post`, `/post/123`, `/post/123/update`, etc.
- You can use wildcards on a method, too: `* /post/123` means any method with URI `/post/123`.
- You can’t use wildcards as a part of the path segment. This will NOT work: `GET /post*`

The way paths are sorted is hopefully very intuitive, but it’s simple, too: the path is first split into segments, and then sorted lexicographically, with the condition that any specific string goes before `*` and `*` goes before `**`.

Inside handlers, path segments that matched wildcards will be assigned to a vector stored in `:path-params`. So

```
GET /post/*/xxx/*
```

matched against `/post/1/xxx/3` will have in request:

```
{:path-params ["1" "3"]}
```

Double wildcards match the entire string, so

```
GET /post/**
```

matched against `/post/1/xxx/3` will have in request:

```
{:path-params ["1/xxx/3"]}
```

And against `/post` it will have:

```
{:path-params [""]}
```

There’s a helper macro, `router/routes`, that helps you build routes. It returns the same map:

```
(router/routes
  "GET  /" []
  {:status 200, :body ...}

  "GET  /article/*" [id]
  {:status 200, :body (str id ...)}

  "GET  /article/*/*/*" [x y z]
  ...

  "* /article/*" [method id]
  ...

  "* /**" req
  (let [[method path] (:path-params req)}]
    ...))
```

By default, you specify `<path-template> <path-params-vector> <handler-body>`. But if `<path-params-vector>` is not a vector, it’ll be bound to `req` instead.

### Use with Ring

To use routes with Ring, you have two options:

```
(router/router routes)
```

builds a terminal handler. It’ll try to process everything you throw at it.

```
(router/wrap-routes handler routes)
```

wraps an existing handler, and if no route matched, will pass control to it.

### A simple complete example

This is a simple namespace that sets up a map of routes and starts a Jetty server using them.

```clojure
(ns my-namespace
  (:require
    [clj-simple-router.core :as router]
    [ring.adapter.jetty :as jetty]
    [ring.util.response :as response]))

(defn render-page
  ([page-name]
    ...)
  ([page-name id]
    ...))

(def routes
  (router/routes
    "GET /" []
    {:status 200
     :body "<html><body><h1>It’s working!</h1></body></html>"}
    
    ;; inline parameter
    "GET /pages/*" [page-name]
    (render-page page-name)
    
    ;; parameter from request
    "GET /pages/*/*" req
    (let [[page-name id] (:path-params req)]
      (render-page page-name id))
    
    ;; wildcard parameter
    "GET /files/**" [path]
    (response/file-response path {:root "files"})))

(defn handler []
  (router/router routes))

(defn -main [& _args]
  (jetty/run-jetty (handler) {:port 8000}))
```

## Scope

What’s not in scope:

- Named wildcard path segments. There are usually 1-3 of them tops, no need to invent names just to immediately match them back to args.
- Wildcard coercion (like `"GET /post/*"` to int id) and
- regular expressions (like `"GET /post/\d+"`). Both these features complicate the router too much, interfere with sorting, etc. Just do it in the handler.
- Reverse routing. I never understood what it is for.

## ClojureScript

Maybe? The algorithm is fairly cross-platform. The request method is internally just another path segment. PRs welcome!

## License

Copyright © 2023 Nikita Prokopov

Licensed under [MIT License](LICENSE).
