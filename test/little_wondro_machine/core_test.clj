(ns little-wondro-machine.core-test
  (:require [clojure.test :refer :all]
            [little-wondro-machine.core :refer :all]))

;(deftest a-test
;  (testing "FIXME, I fail."
;    (is (= 0 1))))


; (storm/defbolt filter-bolt ["str" "username" "body"] [tuple collector]
;                (let [tmap (.getMap tuple)
;                      m (:str tmap)
;                      i (.indexOf m ":")
;                      message (string/triml (.substring m (inc i)))
;                      username (.substring m 0 i)]
;                     (if (nil? (re-seq #"SPAM" message))
;                         (storm/emit-bolt! collector [(str username ": " message) username message] :anchor tuple))
;                     (storm/ack! collector tuple)))
;
;{"filtering" (storm/bolt-spec {"messages" :shuffle}
;                              filter-bolt)}

;(def y-graph {:testing3 (graphcore/fnk [testing filtering] true)
;              :testing2 (graphcore/fnk [testing filtering] true)
;              :testing (graphcore/fnk [filtering] true)
;              :filtering (graphcore/fnk ^{:output-schema {:str true :username true :body true}} [messages]
;                              (let [m (:str messages)
;                                    i (.indexOf m ":")
;                                    message (string/triml (.substring m (inc i)))
;                                    username (.substring m 0 i)]
;                                (if (nil? (re-seq #"SPAM" message))
;                                  {:str (str username ": " message) :username username :body message})))
;              })

;(bolt-spec-compile y-graph)