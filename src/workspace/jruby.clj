#_(comment "
")
(ns workspace.jruby
  (:import javax.script.ScriptEngineManager
           javax.script.ScriptEngine
           java.io.StringWriter))

(def manager (ScriptEngineManager.))
(def engine (.getEngineByName manager "jruby"))
(def context (.getContext engine))

(defn reval [str & [binding]]
  (if binding
    (.eval engine str binding)
    (.eval engine str)))

(.getMethods (.getClass engine))

(defn gem [& args]
  (let [binding (.createBindings engine)
        writer (StringWriter.)]
    (.put binding "args" args)
    (.put binding "writer" writer)
    (.eval engine "
require 'rubygems'
require 'rubygems/gem_runner'
require 'rubygems/exceptions'
require 'stringio'
out = StringIO.new
$stdout=out
$stderr=out
begin
  Gem::GemRunner.new.run $args.to_a
rescue Gem::SystemExitException => e
  puts e.exit_code
end
out.rewind
$writer.write out.read
" binding)
    (.flush writer)
    (.toString writer)))

(gem "install" "rails")

#_(comment "
")
