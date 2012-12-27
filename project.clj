(defproject hotscrap "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [ch.qos.logback/logback-classic "1.0.1"]
                 [org.clojure/tools.logging "0.2.3"]
                 [korma "0.3.0-beta7"]
                 [postgresql/postgresql "9.1-901.jdbc4"]
                 [ragtime "0.3.2"]
                 [ragtime/ragtime.sql.files "0.3.2"]
                 ;; [org.clojure/core.memoize "0.5.2"]
                 [clj-http "0.6.2"]
                 [enlive "1.0.1"]]
  :ragtime {:migrations ragtime.sql.files/migrations
            :database "jdbc:postgresql://localhost:5432/hotscrap"}
  :plugins [[ragtime/ragtime.lein "0.3.2"]])
