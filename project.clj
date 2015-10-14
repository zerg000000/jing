(defproject jing "0.1.0-SNAPSHOT"
  :description "a static content generator"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [clj-time "0.11.0"]
                 [org.asciidoctor/asciidoctorj "1.5.2"]
                 [me.raynes/fs "1.4.6"]
                 [selmer "0.9.2"]
                 ; setup a jetty file server
                 [compojure "1.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 ; setup a watcherservice
                 ])
