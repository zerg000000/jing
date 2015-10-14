(ns jing.util
  (:require [clj-time.format :as f]
            [clj-time.core :as t]
            [jing.fs :refer [to-path]]
            [clojure.core.async :as async])
  (:import [java.net URI]))


(defn spy [datum]
  (println datum)
  datum)

(defn add-default-params [{:as params}]
  "step for adding default params to datum"
  (fn [datum]
    (merge params
           datum
           {:generate-time (t/now)})))

(defn parse-dates [ date-format & cursors ]
  "convert date string in datum to date"
  (let [custom-formatter (f/formatter date-format)]
  (fn [datum]
    (loop [cur (first cursors)
           result datum]
      (if (and cur (not (empty? cur)))
        (let [new-value (f/parse custom-formatter (get-in datum cur))]
          (recur (rest cursors) (assoc-in result cur new-value)))
        result)))))

(defn relativize [here there]
  (if-not (and here there)
    nil
    (let [here-uri (to-path here)
          there-uri (to-path there)]
      (to-path (str (.relativize (.getParent here-uri) (.getParent there-uri))) [(str (.getFileName there-uri))]))))

(defn keywordize [coll]
  "convert all keys in map to keyword"
  (into {} (map (fn [[k v]] [(keyword k) v]) coll)))

(defn add-permlink [{:as params}]
  (fn [datum]
    (let [rel-path (or (:out-relative-path datum) (:relative-path datum))
          filename (or (:out-filename datum) (:filename datum))]
      (assoc datum :permlink
        (str (or (:url params) "/")
             rel-path filename)))))

(defn build-prev-next-link
  [sorted-data]
  (let [prev-data (cons nil sorted-data)
        next-data (conj (vec (rest sorted-data)) nil)]
    (map (fn [datum prv nxt]
           (assoc datum
             :prev (:permlink prv)
             :next (:permlink nxt)))
         sorted-data prev-data next-data)))

(defn relativize-url-tag [args context-map]
  (let [permlink (:permlink context-map)
        mykey    (first args)
        resource-link (if (= "static" mykey)
                          (second args)
                          ((keyword (second args)) context-map))]
    (relativize permlink resource-link)))

(defn absolute-url-tag [args context-map]
  (let [mykey    (first args)
        resource-link (if (= "static" mykey)
                          (second args)
                          ((keyword (second args)) context-map))]
    resource-link))

(defn run-chans [& flows]
  (async/merge
    (map (fn [[f d]]
           (let [ch (async/chan 100 f)]
             (async/onto-chan ch d)
             ch)) flows)))

(defn run-loop [results-chan]
  (let [start (System/currentTimeMillis)]
  (loop []
      (let [v (async/<!! results-chan)]
        (if v
          (do
            (println "-> " (:filename v) " --> " (or (:permlink v) (:filename v)))
            (recur))
          (println "compiled in " (- (System/currentTimeMillis) start) "ms!"))))))


