(ns workspace.jruby
  (:import javax.script.ScriptEngineManager))

(let [engine (.getEngineByName (ScriptEngineManager.) "jruby")]
  (defn reval [str]
    (.eval engine str)))

(reval "'Hello World'")

(reval "
require 'rubygems'
Gem.methods
")
