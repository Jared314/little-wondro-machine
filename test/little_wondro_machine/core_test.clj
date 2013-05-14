(ns little-wondro-machine.core-test
  (:require [clojure.test :refer :all]
            [little-wondro-machine.core :as lwm]
            [storm.test.util :refer :all]
            [backtype.storm.testing :refer :all]
            [plumbing.graph :as graph]
            [plumbing.core :as graphcore]
            [backtype.storm.clojure :as storm]
            [clojure.string :as string]
            [clojure.pprint :as pprint]
            [backtype.storm.config :as storm-config])
  (:import [backtype.storm.spout KestrelThriftSpout]
           [backtype.storm.scheme StringScheme]
           [backtype.storm.tuple TupleImpl])
  (:gen-class))

(storm/defspout simple-spout ["str"] {:prepare false}
  [collector]
  (storm/emit-spout! collector ["A"]))

; Storm
(storm/defbolt filtering-bolt ["str" "username" "body"] [tuple collector]
               (let [m (:str tuple)
                     i (.indexOf m ":")
                     message (string/triml (.substring m (inc i)))
                     username (.substring m 0 i)]
                 (if (nil? (re-seq #"SPAM" message))
                   (storm/emit-bolt! collector [(str username ": " message) username message] :anchor tuple))
                 (storm/ack! collector tuple)))

(defn build-topology []
  (storm/topology {"messages" (storm/spout-spec simple-spout)}
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
  (let [g (lwm/bolt-spec-compile filtering-graph)]
    (storm/topology {"messages" (storm/spout-spec simple-spout)}
                    g)))


(def testdata [["user1: message1"]
               ["user2: message2"]
               ["user3: SPAM message"]
               ["user4: message3"]])

(def testresult [["user1: message1" "user1" "message1"]
                 ["user2: message2" "user2" "message2"]
                 ["user4: message3" "user4" "message3"]])

(deftest storm-topology-bolt-filters-spam-messages
  (with-quiet-logs
    (with-simulated-time-local-cluster [cluster :supervisors 1]
      (let [topology (build-topology)
            results (complete-topology cluster
                                       topology
                                       :mock-sources {"messages" testdata}
                                       :storm-conf {storm-config/TOPOLOGY-DEBUG true})]
        (is (ms= testresult
                 (read-tuples results "filtering")))))))

