(ns jing.selmer
  (:require [selmer.parser :as parser]
            [selmer.filters :as f]
            [jing.util :as u]))

(defn config-selmer! [config]
  "this function will globally config the selmer behavour"
  (if (:resource-path config)
    (parser/set-resource-path! (:resource-path config)))
  (if (:cache config)
    (parser/cache-on!)
    (parser/cache-off!))
  (if (:filters config)
    (dorun (map (fn [[k v]] (f/add-filter! k v)) (:filters config))))
  (if (:tags config)
    (dorun (map (fn [tag] (if (> (count tag) 2)
                              (parser/add-tag! (first tag) (second tag) (nth 2 tag))
                              (parser/add-tag! (first tag) (second tag)))) (:tags config)))))

(defn render-tpl-selmer [tpl]
  "
  return a render function, which using specified template
  "
  (fn [datum]
    (let [content (parser/render-file tpl datum)]
      (assoc datum :content content :response (.getBytes content)))))

(defn set-relative-path-selmer [template-string]
  "return a string render function for creating relative-path according to datum"
  (fn [datum]
    (assoc datum :out-relative-path (parser/render template-string datum))))

