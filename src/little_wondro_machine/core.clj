(ns little-wondro-machine.core
  (:require [plumbing.graph :as graph]
            [plumbing.core :as graphcore]
            [backtype.storm.clojure :as storm]
            [clojure.string :as string]
            [plumbing.fnk.pfnk :as pfnk])
  (:import [backtype.storm.topology IBasicBolt BasicOutputCollector]
           [backtype.storm.task TopologyContext]
           [backtype.storm.tuple Tuple]
           [java.util Map]))

(defn graph-fnk->bolt-execute [node-name body passthrough-fields]
  (let [[input-schema _] (pfnk/io-schemata body)
        input-ns (keys input-schema)]
    (fn [tuple collector]
      (try
        (letfn [(map->hmap [data] (reduce #(let [[a b] %2 n (keyword a)]
                                             (assoc-in %1 (remove nil? [(keyword (namespace n)) (keyword (name n))]) {} b)) data))
                (nsmap [ns data] (reduce #(assoc %1 (str ns "/" (name (first %2))) (second %2)) {} data))]
          (let [input (.getMap tuple)
                data (select-keys (map->hmap input) input-ns)
                result (body data)
                mapresult (if (and (map? result) (not (nil? result))) (nsmap node-name result) {node-name result})]
            (storm/emit-bolt! collector
                              (merge input mapresult)
                              :anchor tuple)
            (storm/ack! collector tuple)))
        (catch Exception e (storm/fail! collector tuple))))))

(defn graph-fnk->bolt
  ([node-name body] (graph-fnk->bolt node-name body nil))
  ([node-name body passthrough-fields]
   (let [[input-schema output-schema] (pfnk/io-schemata body)
         has-output-schema (map? output-schema)
         output-vec (concat (map #(str node-name "/" (name %)) (keys (if has-output-schema output-schema))) passthrough-fields)
         bolt (graph-fnk->bolt-execute node-name body passthrough-fields)]
     (reify
       IBasicBolt
       (^void prepare [this ^Map stormConf ^TopologyContext context] nil)
       (^void execute [this ^Tuple input ^BasicOutputCollector collector]
              (bolt input collector))
       (^void cleanup [this] nil)))))

;Current Limitations:
;  Compiles the graph nodes into a linear graph of bolts, sorted by dependency
;  Only handles a single input spout
;  Does not use the bolt coordination features
(defn bolt-spec-compile [g]
  (let [k (if (fn? g) g (graph/->graph g))
        input-key (first (keys (first (pfnk/io-schemata k))))
        input (cons input-key (keys k))
        s (map #(conj %1 %2) k input)]
    (reduce #(let [[a b c] %2 n (name a)]
               (assoc %1 n (storm/bolt-spec {(name c) :shuffle} (graph-fnk->bolt n b))))
            {}
            s)))
