(ns jing.core
  (:require [jing.fs :refer :all]
            [jing.asciidoc :refer :all]
            [jing.selmer :refer :all]
            [jing.core :refer :all]
            [jing.util :refer :all]
            [clojure.core.async :as async]
            [compojure.core :refer [defroutes]]
            [compojure.route :as route]
            [compojure.handler :refer [site]]
            [ring.adapter.jetty :refer [run-jetty]])
  (:import [java.nio.file StandardCopyOption])
  (:gen-class))

(config-selmer!
  {:resource-path "file:doc/themes/jing"
   :cache         true
   :tags          [[:make-url relativize-url-tag]]})

(def site-config
  {:title "Jing Static Site Generator"
   :url   "http://www.algotizer.com/"
   :theme-pattern #".*(jpg|png|jpeg|gif|html|js|css)"
   :theme-dir "doc/themes/jing/resources"
   :dest-dir "public/"})

(def posts-config
  {:src-dir "doc/"
   :doc-pattern #".*\.adoc"
   :res-pattern #".*\.(jpg|png|jpeg|gif)"
   :publish-date-format "yyyy-MM-dd"
   :image-path "images/{{relative-path}}"
   ;:path-format "posts/{{meta.publish-date|date:\"yyyy/MM/dd/\"}}"
   :path-format "posts/"
   :layout "templates/post.html"})

(def theme-res-flow
  (comp (map (file-to-datum (:theme-dir site-config)))
        (map (copy-to! (:dest-dir site-config) StandardCopyOption/REPLACE_EXISTING))))

(def posts-flow
  (comp (map (compile-asciidoc {"to_file" false}))
        (map (render-tpl-selmer (:layout posts-config)))
        (map (save-file! (:dest-dir site-config)))))

(def posts-res-flow
  (comp (map (file-to-datum (:src-dir posts-config)))
        (map (set-relative-path-selmer (:image-path posts-config)))
        (map (copy-to! (:dest-dir site-config) StandardCopyOption/REPLACE_EXISTING))))

(def posts-list-flow
  (comp (map (file-to-datum (:src-dir posts-config)))
        (map (read-meta-asciidoc))
        (map (parse-dates "yyyy-MM-dd" [:meta :publish-date]))
        (map (add-default-params {:site site-config}))
        (map (set-extension "html"))
        (map (set-relative-path-selmer (:path-format posts-config)))
        (map (add-permlink {}))))

(defroutes main-routes
  (route/files "/" {:root (:dest-dir site-config)})
  (route/not-found "Page not found"))

(defn -main [& args]
  "generate documentation for Jing"
  (async/thread
    (loop []
    (run-loop
     (run-chans [theme-res-flow (find-files (:theme-dir site-config)
                                            (:theme-pattern site-config))]
                [posts-flow (->> (find-files (:src-dir posts-config)
                                             (:doc-pattern posts-config))
                                 (into [] posts-list-flow)
                                 (sort-by #(get-in % [:meta :publish-date]))
                                 (build-prev-next-link))]
                [posts-res-flow (find-files (:src-dir posts-config)
                                            (:res-pattern posts-config))]))
      (Thread/sleep 5000)
      (recur)))
  (run-jetty (site main-routes) {:port 8080}))
