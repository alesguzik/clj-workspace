(ns workspace.neuroph
  (:import org.neuroph.core.NeuralNetwork
           org.neuroph.nnet.MultiLayerPerceptron
           org.neuroph.core.data.DataSet
           org.neuroph.core.data.DataSetRow
           org.neuroph.util.TransferFunctionType
           java.util.ArrayList))

(defn make-dataset [data]
  (let [first-row (first data)
        inputs-number (count (first first-row))
        outputs-number (count (second first-row))
        data-set (DataSet. inputs-number outputs-number)]
    (doseq [row data]
      (.addRow data-set (DataSetRow. (into-array Double/TYPE (first row))
                                     (into-array Double/TYPE (second row)))))
    data-set))

(def dataset [[[0 0] [0]]
              [[0 1] [1]]
              [[1 0] [1]]
              [[1 1] [0]]])

(defn make-ml-perceptron [layers transfer-function-type data]
  (let [perceptron (MultiLayerPerceptron. (ArrayList. (map #(Integer. %) layers))
                                          transfer-function-type)]
    (.learn perceptron (make-dataset data))
    perceptron))

(defn use-network [network data]
  (let [row (DataSetRow. (into-array Double/TYPE data) (make-array Double/TYPE 0))
        input (.getInput row)]
    (doto network
      (.setInput input)
      (.calculate))
    (.getOutput network)))

(let [net (make-ml-perceptron [2 3 1] TransferFunctionType/TANH dataset)]
  (map #(use-network net %) (map first dataset)))
