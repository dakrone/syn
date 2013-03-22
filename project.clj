(defproject syn "1.0.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public/"}
  ;; TODO: OSS or closed source?
  ;; :license {:name "Eclipse Public License"
  ;;           :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [lamina "0.5.0-beta14"]
                 [clj-http "0.6.5"]
                 ;; Broken for some reason with lamina unless I specify it here
                 [com.yammer.metrics/metrics-core "3.0.0-SNAPSHOT"
                  :exclusions [[org.slf4j/slf4j-api]
                               [com.yammer.metrics/metrics-annotation]]]]
  :min-lein-version "2.0.0"
  :plugins [[lein-bikeshed "0.1.0"]
            [lein-pprint "1.1.1"]])
