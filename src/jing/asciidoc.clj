(ns jing.asciidoc
  (:require [jing.util :refer :all])
  (:import [java.nio.file Files FileSystems Paths Path LinkOption]
           [java.nio.file.attribute FileAttribute]
           [java.net URI]
           [org.asciidoctor Asciidoctor$Factory Options]))


(def asciidoctor (Asciidoctor$Factory/create))

(defn read-meta-asciidoc []
  (fn [datum]
    (let [metadata (with-open [rdr ((:new-reader datum))]
                     (-> asciidoctor
                       (.readDocumentHeader rdr)
                       (.getAttributes)))]
      (assoc datum :meta (keywordize metadata)))))

(defn compile-asciidoc [options]
  (fn [datum]
    (with-open [rdr ((:new-reader datum))
                wtr ((:new-writer datum))]
      (.convert asciidoctor rdr  wtr (assoc options "jing-meta" (:meta datum)))
      (let [content (.toString wtr)]
        (assoc datum :content content :response (.getBytes content))))
      ))
