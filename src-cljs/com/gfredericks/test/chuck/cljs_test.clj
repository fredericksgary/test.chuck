(ns com.gfredericks.test.chuck.cljs-test)

;; copied from clojure.test.check, which privatized the function in
;; recent versions.
;;
;; I think there might be plans for test.check to abstract this logic
;; into a protocol or something, so I'm not too bothered by the
;; copypasta for now.
(defmacro capture-reports [& body]
  `(let [reports# (atom [])]
     (binding [com.gfredericks.test.chuck.cljs-test/*chuck-captured-reports* reports#
               cljs.test/*current-env* (cljs.test/empty-env :com.gfredericks.test.chuck.cljs-test/chuck-capture)]
       ~@body)
     @reports#))

(defmacro checking
  "A macro intended to replace the testing macro in clojure.test with a
  generative form. To make (testing \"doubling\" (is (= (* 2 2) (+ 2 2))))
  generative, you simply have to change it to
  (checking \"doubling\" 100 [x gen/int] (is (= (* 2 x) (+ x x)))).

  For more details on this code, see http://blog.colinwilliams.name/blog/2015/01/26/alternative-clojure-dot-test-integration-with-test-dot-check/"
  [name tests bindings & body]
  `(cljs.test/testing ~name
     (let [final-reports# (atom [])]
       (com.gfredericks.test.chuck.cljs-test/report-when-failing
         (cljs.test.check/quick-check
           ~tests
           (cljs.test.check.properties/for-all ~bindings
             (let [reports# (capture-reports ~@body)]
               (swap! final-reports# com.gfredericks.test.chuck.cljs-test/save-to-final-reports reports#)
               (com.gfredericks.test.chuck.cljs-test/pass? reports#)))))
       (doseq [r# @final-reports#]
         (cljs.test/report r#)))))

(defmacro for-all
  "An alternative to clojure.test.check.properties/for-all that uses
  clojure.test-style assertions (i.e., clojure.test/is) rather than
  the truthiness of the body expression."
  [bindings & body]
  `(cljs.test.check.properties/for-all
     ~bindings
     (com.gfredericks.test.chuck.cljs-test/pass? (capture-reports ~@body))))