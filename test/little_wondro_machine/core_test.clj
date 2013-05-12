(ns little-wondro-machine.core-test
  (:require [clojure.test :refer :all]
            [little-wondro-machine.core :as lwm]
            [storm.test.util :refer :all]
            [backtype.storm.testing :refer :all]
            [plumbing.graph :as graph]
            [plumbing.core :as graphcore]
            [backtype.storm.clojure :as storm]
            [clojure.string :as string])
  (:import [backtype.storm.spout KestrelThriftSpout]
           [backtype.storm.scheme StringScheme]))

; Storm
(storm/defbolt filtering-bolt ["str" "username" "body"] [tuple collector]
               (let [tmap (.getMap tuple)
                     m (:str tmap)
                     i (.indexOf m ":")
                     message (string/triml (.substring m (inc i)))
                     username (.substring m 0 i)]
                 (if (nil? (re-seq #"SPAM" message))
                   (storm/emit-bolt! collector [(str username ": " message) username message] :anchor tuple))
                 (storm/ack! collector tuple)))

(defn build-topology []
    (storm/topology {"messages" (storm/spout-spec (KestrelThriftSpout. "127.0.0.1" 2229 "queuename1" (StringScheme.)))}
                    {"filtering" (storm/bolt-spec {"messages" :shuffle} filtering-bolt)}))



; Graph
(def filtering-graph {:filtering (graphcore/fnk ^{:output-schema {:str true :username true :body true}}
                                                [messages]
                                                (let [m (:str messages)
                                                      i (.indexOf m ":")
                                                      message (string/triml (.substring m (inc i)))
                                                      username (.substring m 0 i)]
                                                  (if (nil? (re-seq #"SPAM" message))
                                                    {:str (str username ": " message) :username username :body message})))})

(defn compile-graph-topology []
    (storm/topology {"messages" (storm/spout-spec (KestrelThriftSpout. "127.0.0.1" 2229 "queuename1" (StringScheme.)))}
                    (lwm/bolt-spec-compile filtering-graph)))



(def testdata [["user1: message1"]
               ["user2: message2"]
               ["user3: SPAM message"]
               ["user4: message3"]])

(deftest bolt-filters-spam-messages
  (with-quiet-logs
    (with-simulated-time-local-cluster [cluster :supervisors 1]
      (let [topology (compile-graph-topology)
            results (complete-topology cluster
                                       topology
                                       :mock-sources {"messages" testdata})]
        (is (ms= [["user1: message1" "user1" "message1"]
                  ["user2: message2" "user2" "message2"]
                  ["user4: message3" "user4" "message3"]]
                 (read-tuples results "filtering")))))))

