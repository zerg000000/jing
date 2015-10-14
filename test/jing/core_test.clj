(ns jing.core-test
  (:require [clojure.test :refer :all]
            [jing.fs :refer :all]
            [jing.asciidoc :refer :all]
            [jing.selmer :refer :all]
            [jing.core :refer :all]
            [jing.util :refer :all]
            [clojure.core.async :as async])
  (:import [java.nio.file Files FileSystems Paths Path LinkOption]))

(deftest fs-test
  (testing "test find-files"
    (let [pattern #".*\.edn"
          fixtures "fixtures/"
          dataset (find-files fixtures pattern)]
      (is (> (count (map identity dataset)) 0)))
  ))

(deftest fs-save-test
    (testing "test save-files!"
    (let [pattern #".*\.edn"
          fixtures "fixtures/"
          dataset (find-files fixtures pattern)
          test-file (Paths/get "target/config.edn" (make-array String 0))]
      (Files/deleteIfExists test-file)
      (dorun (map (fn [ds]
                    ((save-file! "target/")
                        (assoc ((file-to-datum fixtures) ds)
                          :response (.getBytes "Hello")
                          :out-filename "config.edn"))) dataset))
      (is (Files/exists test-file (make-array LinkOption 0))))))

(deftest asciidoc-read-meta-test
  (testing "test read-meta"
    (let [pattern #".*\.adoc"
          fixtures "fixtures/"
          dataset ((file-to-datum fixtures) (first (find-files fixtures pattern)))
          metadata ((read-meta-asciidoc) dataset)]
      (is (:meta metadata)))))


(deftest asciidoc-compile-test
  (testing "test compile"
    (let [pattern #".*\.adoc"
          fixtures "fixtures/"
          dataset ((file-to-datum fixtures) (first (find-files fixtures pattern)))
          datum ((read-meta-asciidoc) dataset)
          d     ((compile-asciidoc {"to_file" false}) datum)]
      (is (:content d)))))

(deftest common-workflow-test
  (testing "common workflow try"
    (let [cfg      (config-selmer! {:resource-path "file:fixtures/templates"
                                    :cache         false})
          pattern #".*\.adoc"
          fixtures "fixtures/"
          workflow (comp (map (file-to-datum fixtures))
                         (map (add-default-params {}))
                         (map (read-meta-asciidoc))
                         (map (compile-asciidoc {"to_file" false}))
                         (map (render-tpl-selmer "index.html"))
                         (map (set-extension "html"))
                         (map (set-relative-path-selmer "{{relative-path}}/{{generate-time|date:\"yyyy/MM/dd/\"}}"))
                         (map (save-file! "target/posts/")))
          wf-chan (async/chan 10 workflow)]
      (async/onto-chan wf-chan (find-files fixtures pattern))
      (Thread/sleep 4000))
    (let [pattern #".*\.(png|jpg|jpeg)"
          fixtures "fixtures/"
          workflow (comp (map (file-to-datum fixtures))
                         (map (copy-to! "target/")))]
      (async/onto-chan (async/chan 10 workflow) (find-files fixtures pattern)))))
