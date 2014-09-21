(defproject workspace "0.1.0-SNAPSHOT"
  :description "My clojure workspace"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[cider/cider-nrepl "0.8.0-SNAPSHOT"]
            [lein-localrepo "0.5.3"]]
  :repositories {"apache-releases"
                 "http://repository.apache.org/content/repositories/releases/"}
  :jvm-opts ^:replace []
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.jruby/jruby-complete "1.7.12"]

                 ; get
                 ;
                 [http-kit "2.1.16"]
                 ;; [clj-http "0.9.1"]
                 ;; [tentacles "0.2.5"]  ; GitHub API
                 ;; [me.raynes/least "0.1.3"]  ; LastFM API
                 ;; [twitter-api "0.7.5"] ; Twitter API

                 ; parse
                 ;
                 [org.clojure/data.json "0.2.4"]
                 [org.clojure/data.xml "0.0.7"]
                 [org.clojure/data.csv "0.1.2"]
                 [org.clojure/data.zip "0.1.1"]
                 ;; [clj-tagsoup "0.3.0"]
                 [enlive "1.1.5"]
                 [clearley "0.3.0"] ; very neat parser generator
                 [instaparse "1.3.2"]
                 [com.cognitect/transit-clj "0.8.247"]
                 [com.cognitect/transit-cljs "0.8.184"]

                 ; store
                 ;
                 [org.postgresql/postgresql "9.3-1101-jdbc41"]
                 [com.h2database/h2 "1.4.178"]
                 [korma "0.3.1"]
                 [clojurewerkz/neocons "3.0.0-rc1"] ; neo4j main
                 ; [borneo "0.4.0"] ; neo4j alternative
                 ;; [clj-orient "0.7.0-SNAPSHOT"] ; OrientDB
                 [clojurewerkz/ogre "2.3.0.1"] ; nice blueprint queries

                 ; host system interaction
                 ;
                 [me.raynes/conch "0.7.0"]
                 [me.raynes/fs "1.4.4"]

                 ; analyze
                 ;
                 [incanter "1.5.5"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/core.logic "0.8.7"]
                 [org.clojure/math.combinatorics "0.0.7"]
                 [net.mikera/core.matrix "0.23.0"]
                 [fogus/bacwn "0.4.0"] ; datalog implementation
                 [org.encog/encog-core "3.2.0"]
                 [enclog "0.6.5"] ; machine-learning, neural networks
                 [clocop "0.2.0"]
                 [clojure-opennlp "0.3.2"]
                 [neuroph-core/neuroph-core "2.8"]
                 [neuroph-imgrec/neuroph-imgrec "2.8"]
                 [neuroph-ocr/neuroph-ocr "2.8"]
                 [uk.org.russet/tawny-owl "1.1.0"]

                 ; data transformation
                 ;
                 [clojurewerkz/balagan "1.0.0"] ; enlive-like datastructure transformations
                 [reagi "0.10.0"] ; FRP
                 [org.clojure/core.async "0.1.301.0-deb34a-alpha"]

                 ; display
                 ;
                 [overtone "0.9.1"]
                 [quil "2.1.0"]
                 [shadertone "0.2.3"]
                 [seesaw "1.4.4"] ; UI
                 [tikkba "0.5.0"] ; dynamic svg using analemma

                 ; documentation
                 ;
                 [clj-ns-browser "1.3.1"]
                 ])
