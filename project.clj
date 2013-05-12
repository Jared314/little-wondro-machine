(defproject little-wondro-machine "0.1.0-SNAPSHOT"
  :description "Library to compile a Prismatic graph into a Storm bolt-spec"
  :url "https://github.com/jared314/little-wondro-machine"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [prismatic/plumbing "0.1.0" :exclusions [org.clojure/clojure]]
                 [storm "0.8.2"]
                 [storm/storm-kestrel "0.7.2-SNAPSHOT"]]
  :profiles {:test {:dependencies [[storm-test "0.2.0"]]}}
;  :aot :all
  :min-lein-version "2.0.0")