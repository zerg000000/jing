(ns jing.fs
  (:import [java.nio.file Files FileSystems Paths Path LinkOption CopyOption]
           [java.nio.file.attribute FileAttribute]
           [java.nio.charset Charset]
           [java.net URI]
           [java.io File]
           [org.asciidoctor Asciidoctor$Factory Options])
  (:require [me.raynes.fs :as fs]))

(defprotocol ToPathable
  (to-path [this] [this others]))

(extend-protocol ToPathable
  java.lang.String
  (to-path
   ([this]
    (to-path this []))
   ([this others]
    (Paths/get this (into-array String others))))
  java.net.URI
  (to-path
   ([this] (Paths/get this))
   ([this others]
     (Paths/get this)))
  java.io.File
  (to-path
    ([this]
      (to-path (.toURI this)))
    ([this others]
      (to-path (.toURI this))))
  )

(defn find-files [^String base ^String pattern]
  (let [base-path (to-path base)]
    (map (fn [d] (to-path d))
    (filter
     fs/file?
     (fs/find-files base pattern))
      )))

(defn save-file! [^String base & options]
  (fn [datum]
    (let [base-path (to-path base)
          rel-path (or (:out-relative-path datum) (:relative-path datum))
          resp     (:response datum)
          dest-path (.resolve base-path rel-path)
          filename (or (:out-filename datum) (:filename datum))]
      (Files/createDirectories dest-path (make-array FileAttribute 0))
      (Files/write (.resolve dest-path filename) resp (into-array LinkOption options))
      datum)))

(defn copy-to! [^String base & options]
  (fn [datum]
  (let [base-path (to-path base)
        rel-path (or (:out-relative-path datum) (:relative-path datum))
        dest-path (.resolve base-path rel-path)
        filename (or (:out-filename datum) (:filename datum))]
    (Files/createDirectories dest-path (make-array FileAttribute 0))
    (Files/copy (to-path (:uri datum))
                (.resolve dest-path filename) (into-array CopyOption options))
    datum)))

(defn file-to-datum [^String base]
  (fn [^Path src]
    (let [uri       (.toUri src)
          base-path (.toAbsolutePath (to-path base))
          src-dir (.normalize (.getParent src))
          rel-path (.normalize (.relativize base-path src-dir))]
      {:uri uri
       :new-reader #(Files/newBufferedReader src (Charset/forName "UTF-8"))
       :new-writer #(java.io.StringWriter.)
       :relative-path rel-path
       :filename (.toString (.getFileName src))})))

(defn set-extension [^String ext]
  (fn [datum]
    (let [filename (:filename datum)]
      (assoc datum :out-filename
        (clojure.string/replace
          filename #"^(.*)\.(.*)$" (str "$1." ext))))))

