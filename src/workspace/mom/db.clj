(ns workspace.mom.db
  (:require
   [clojure.data.csv :as csv]
   [clojure.java.io :as io]
   [clojure.data.xml :as xml]
   [clojure.string :as str]
   [workspace.util :as u]
   [me.raynes.conch :as sh]))

(defn to-right-piped [program]
  `(defn ~(symbol (str program "|")) [& args#]
     (apply ~program (conj (vec args#) {:seq true}))))

(defn to-left-piped [program]
  `(defn ~(symbol (str "|" program)) [in# & args#]
     (apply ~program (conj (vec args#) {:in in#}))))

(defn to-both-piped [program]
  `(defn ~(symbol (str "|" program "|")) [in# & args#]
     (apply ~program (conj (vec args#) {:in in# :seq true}))))

(defmacro programs-with-pipes [& programs]
  `(do
    (sh/programs ~@programs)
    ~@(map to-right-piped programs)
    ~@(map to-left-piped  programs)
    ~@(map to-both-piped  programs)))

(programs-with-pipes ls cat grep tail cut playnote)

(def tables-path "data/mom/")
(def table-files (-> (ls| tables-path) (|grep| ".xml")))
(def table-names (map #(str/replace % #".xml" "") table-files))

(defn table-path [table-name ext]
  (str tables-path table-name "." ext))

(defn read-xml [table-name ext]
  (xml/parse-str (slurp (table-path table-name ext))))

(defn xsd-extract-name [element]
  (-> element :attrs :name keyword))

(defn xsd-extract-caption [element]
  (->> element :content first :content first :content
       (map :attrs)
      (filter #(= (-> % :name) "Caption"))
      (map :value)
      first
))

(defn table-captions [table-name]
  (->> (read-xml table-name "xsd")
       :content
       second
       :content
       second
       :content
       first
       :content
       (map (juxt xsd-extract-name xsd-extract-caption))
       (into {})))

(def all-table-captions
  (into {} (map #(vector (keyword %) (table-captions %)) table-names)))

(defn read-table [table-name]
  (read-xml table-name "xml"))

(defn extract-value [{:keys [tag content]}]
  [tag
   (case (count content)
     0 (println "Empty" tag)
     1 (first content)
     (println (str "Multiple content in " tag (vec content))))
   ])

(defn humanize-table [table]
  (->> table
       :content
       (map :content)
       (mapv #(into {} (map extract-value %)))))

(def get-table (comp humanize-table read-table))

(defn table-keys [table]
  (->> table (mapcat keys) set))

(def full-db
  (into {}
        (map #(vector (keyword %)
                      (get-table %))
             table-names)))

(def patients (full-db :Patients))
(def all-ids (map :ID patients))

(def tables-with-id (u/filter-hash #((table-keys %2) :ID) full-db))
(def tables-without-id
  (u/filter-hash (complement #((table-keys %2) :ID))
                 full-db))

(defn p [x]
  (println x)
  x)

(defn latest-by-examination-date [records]
  (->> records
      (sort #(- (apply compare (map :ExaminationDate [% %2]))))
      first))

(defn sum-all [records]
  (apply merge-with + records))

(defn sum-all-except-product-code [records]
  (dissoc (sum-all (map #(u/map-vals read-string %) records))
          :ID :ProduktCode))

(defn with-correct-phosphorus [records]
  (first (filter #(= "128.6" (% :P)) records)))

(def food (tables-without-id :FoodsList))
(def food-by-id (into {} (map #(vector (:ProductCode %) %) food)))

(defn frequency-code-recalculation-multiplier [code]
  (case code
    1 0
    2 0.05
    3 0.117
    4 0.357
    5 0.714
    6 1.5
    7 3.5
    8 5))

(defn food-record-to-hash
  "одна колонка с граммами = частота * код пересчёта * вес порции * кол-во порций"
  [{:keys [ProductCode PortionsNum ConsumptionFrequency FrequencyCode]}]
  (let [product (food-by-id ProductCode)
        all-params [product
                    ProductCode
                    PortionsNum
                    ConsumptionFrequency
                    FrequencyCode]]
    (if (reduce #(and % %2) all-params)
      {(product :ProductDescription)
       (* (read-string ConsumptionFrequency)
          (frequency-code-recalculation-multiplier (read-string FrequencyCode))
          (read-string (product :Portion))
          (read-string PortionsNum))}
      (do (println all-params)
          {}))))

(defn calculate-food-consumption [records]
  (apply merge (map food-record-to-hash records)))

(def merge-functions
  {:PhysicalFitness            latest-by-examination-date
   :ElementsConsumption        sum-all-except-product-code
   :HealthState                latest-by-examination-date
   :PhysicalDevelopment        latest-by-examination-date
   :FoodsConsumptionFrequency  calculate-food-consumption
   :Minerals                   with-correct-phosphorus
   :StomatologicalExamination  #(apply merge %)})

(defn default-merge-function [records]
  (case (count records)
    0 {}
    1 (first records)
    (do (println "Unhandled multiple records" (vec records))
        records)))

(defn merge-function [table-name]
  (merge-functions table-name default-merge-function))

(def tables-with-id-by-id
  ":TableName -> :ID -> [data]"
  (u/map-hash #(vector % (group-by :ID %2))
              tables-with-id))

(def data-by-id
  ":ID -> :TableName -> :TableKey -> value"
  (into {}
        (map (fn [id]
               (println "ID=" id)
               [id (u/map-hash #(vector (p %) ((merge-function %) (%2 id)))
                               tables-with-id-by-id)])
             all-ids)))

(defn element-hash-paths [elt]
  (->> elt
     (u/map-vals keys)
     (mapcat (fn [[tbl tbl-keys]]
               (map #(vector tbl %) tbl-keys)))))

(defn all-hash-paths [data-by-id-hash]
  (distinct (mapcat element-hash-paths (vals data-by-id-hash))))

(def columns
  (map #(vec (cons "" %)) (all-hash-paths data-by-id)))

(def columns
  [
   ["Школа" :Patients :Scool]
   ["Класс" :Patients :Class]
   ["ФИО" :Patients :ФИО]
   [nil :Patients :Address]
   [nil :Patients :AgeGroupForConsumption]
   [nil :Patients :AgeGroupForPhysicalDevelopement]
   [nil :Patients :Age]
   [nil :Patients :BirthDate]
   [nil :Patients :FirstName]
   [nil :Patients :ID]
   [nil :Patients :InfSogl]
   [nil :Patients :Patronimyc]
   [nil :Patients :Sex]
   [nil :Patients :Surname]
   [nil :Patients :dop]
   [nil :AnxietyLevel :All_level]
   [nil :AnxietyLevel :All_point]
   [nil :AnxietyLevel :ID]
   [nil :AnxietyLevel :Scale1_level]
   [nil :AnxietyLevel :Scale1_point]
   [nil :AnxietyLevel :Scale2_level]
   [nil :AnxietyLevel :Scale2_point]
   [nil :AnxietyLevel :Scale3_level]
   [nil :AnxietyLevel :Scale3_point]
   [nil :AnxietyLevel :Scale4_level]
   [nil :AnxietyLevel :Scale4_point]
   [nil :AnxietyLevel :Scale5_level]
   [nil :AnxietyLevel :Scale5_point]
   [nil :AnxietyLevel :Scale6_level]
   [nil :AnxietyLevel :Scale6_point]
   [nil :AnxietyLevel :Scale7_level]
   [nil :AnxietyLevel :Scale7_point]
   [nil :AnxietyLevel :Scale8_level]
   [nil :AnxietyLevel :Scale8_point]
   [nil :ElementsConsumption :CaloricValue]
   [nil :ElementsConsumption :Carbohydrates]
   [nil :ElementsConsumption :Carotine]
   [nil :ElementsConsumption :Cholesterol]
   [nil :ElementsConsumption :DietaryFiber]
   [nil :ElementsConsumption :Ethanol]
   [nil :ElementsConsumption :Fats]
   [nil :ElementsConsumption :Fe]
   [nil :ElementsConsumption :MFA]
   [nil :ElementsConsumption :Mg]
   [nil :ElementsConsumption :Na]
   [nil :ElementsConsumption :PUFA]
   [nil :ElementsConsumption :Polysaccharides]
   [nil :ElementsConsumption :Protein]
   [nil :ElementsConsumption :SFA]
   [nil :ElementsConsumption :SimpleSugars]
   [nil :ElementsConsumption :А]
   [nil :ElementsConsumption :В1]
   [nil :ElementsConsumption :В2]
   [nil :ElementsConsumption :Е]
   [nil :ElementsConsumption :К]
   [nil :ElementsConsumption :Р]
   [nil :ElementsConsumption :РР]
   [nil :ElementsConsumption :С]
   [nil :ElementsConsumption :Са]
   [nil :Estimations :AP_index]
   [nil :Estimations :A]
   [nil :Estimations :Al]
   [nil :Estimations :B1]
   [nil :Estimations :B2]
   [nil :Estimations :BroadJump]
   [nil :Estimations :C]
   [nil :Estimations :Ca]
   [nil :Estimations :Ca_cons]
   [nil :Estimations :CaloricValue]
   [nil :Estimations :Carbohydrates]
   [nil :Estimations :Cd]
   [nil :Estimations :Co]
   [nil :Estimations :Cr]
   [nil :Estimations :Cu]
   [nil :Estimations :DAP]
   [nil :Estimations :E]
   [nil :Estimations :Fats]
   [nil :Estimations :Fe]
   [nil :Estimations :Fe_cons]
   [nil :Estimations :Height]
   [nil :Estimations :Height_new]
   [nil :Estimations :ID]
   [nil :Estimations :IMT_index]
   [nil :Estimations :IMT_new]
   [nil :Estimations :InclinationForward]
   [nil :Estimations :KPU_index]
   [nil :Estimations :K]
   [nil :Estimations :LongDistanceRun]
   [nil :Estimations :Mg]
   [nil :Estimations :Mg_cons]
   [nil :Estimations :Mn]
   [nil :Estimations :Na]
   [nil :Estimations :Ni]
   [nil :Estimations :OHIS_index]
   [nil :Estimations :PP]
   [nil :Estimations :P]
   [nil :Estimations :P_cons]
   [nil :Estimations :Pb]
   [nil :Estimations :Protein]
   [nil :Estimations :PullingUp]
   [nil :Estimations :RightHandForce]
   [nil :Estimations :SAP]
   [nil :Estimations :Se]
   [nil :Estimations :ShortDistanceRun]
   [nil :Estimations :ShuttleRun]
   [nil :Estimations :Sr]
   [nil :Estimations :TrunkLifting]
   [nil :Estimations :Weight]
   [nil :Estimations :Weight_new]
   [nil :Estimations :Zn]
   [nil :FoodsConsumptionFrequency "Апельсины, мандарины, грейпфруты"]
   [nil :FoodsConsumptionFrequency "Баранина в любом виде"]
   [nil :FoodsConsumptionFrequency "Блины"]
   [nil :FoodsConsumptionFrequency "Бобовые в любом виде: фасоль, горох, соя"]
   [nil :FoodsConsumptionFrequency "Борщи, щи, овощные супы"]
   [nil :FoodsConsumptionFrequency "Булка сдобная"]
   [nil :FoodsConsumptionFrequency "Варенье, повидло, джем, мед"]
   [nil :FoodsConsumptionFrequency "Вишня, черешня, слива, абрикос"]
   [nil :FoodsConsumptionFrequency "Говядина в любом виде"]
   [nil :FoodsConsumptionFrequency "Кабачки, паттисоны, тыква"]
   [nil :FoodsConsumptionFrequency "Капуста квашеная"]
   [nil :FoodsConsumptionFrequency "Капуста свежая, сырая, готовая"]
   [nil :FoodsConsumptionFrequency "Картофель жареный"]
   [nil :FoodsConsumptionFrequency "Картофель отварной или пюре"]
   [nil :FoodsConsumptionFrequency "Каши или супы из круп молочные"]
   [nil :FoodsConsumptionFrequency "Кефир, простокваша, ряженка"]
   [nil :FoodsConsumptionFrequency "Колбаса вареная"]
   [nil :FoodsConsumptionFrequency "Колбаса копченая,в/к, окорок, ветчина"]
   [nil :FoodsConsumptionFrequency "Компоты домашние консервированные"]
   [nil :FoodsConsumptionFrequency "Консервы мясные тушенка"]
   [nil :FoodsConsumptionFrequency "Конфеты карамель"]
   [nil :FoodsConsumptionFrequency "Котлеты и др. блюда из рубленого мяса"]
   [nil :FoodsConsumptionFrequency "Кофе"]
   [nil :FoodsConsumptionFrequency "Крупы, каши без молока, гарнир"]
   [nil :FoodsConsumptionFrequency "Лук репчатый"]
   [nil :FoodsConsumptionFrequency "Майонез"]
   [nil :FoodsConsumptionFrequency "Макароны отварные гарнир.блюда"]
   [nil :FoodsConsumptionFrequency "Маргарин"]
   [nil :FoodsConsumptionFrequency "Масло растительное"]
   [nil :FoodsConsumptionFrequency "Масло сливочное"]
   [nil :FoodsConsumptionFrequency "Молоко сгущенное с сахаром"]
   [nil :FoodsConsumptionFrequency "Молоко"]
   [nil :FoodsConsumptionFrequency "Морковь"]
   [nil :FoodsConsumptionFrequency "Мясо птицы: курица, утка, гусь и др."]
   [nil :FoodsConsumptionFrequency "Огурцы свежие"]
   [nil :FoodsConsumptionFrequency "Орехи любые"]
   [nil :FoodsConsumptionFrequency "Пельмени из мяса"]
   [nil :FoodsConsumptionFrequency "Петрушка, укроп, салат, другая зелень"]
   [nil :FoodsConsumptionFrequency "Печень животных в любом виде"]
   [nil :FoodsConsumptionFrequency "Печенье, пряники"]
   [nil :FoodsConsumptionFrequency "Пирожки с любой начинкой"]
   [nil :FoodsConsumptionFrequency "Пирожные, торты"]
   [nil :FoodsConsumptionFrequency "Помидоры свежие"]
   [nil :FoodsConsumptionFrequency "Редька, репа, редис"]
   [nil :FoodsConsumptionFrequency "Рыба копченая, вяленая, соленая в т.ч. Сельдь"]
   [nil :FoodsConsumptionFrequency "Рыба свежая или мороженая"]
   [nil :FoodsConsumptionFrequency "Сало свиное"]
   [nil :FoodsConsumptionFrequency "Сахар"]
   [nil :FoodsConsumptionFrequency "Свекла, винегрет"]
   [nil :FoodsConsumptionFrequency "Свинина в любом виде"]
   [nil :FoodsConsumptionFrequency "Сметана, сливки"]
   [nil :FoodsConsumptionFrequency "Соки натуральные фруктовые"]
   [nil :FoodsConsumptionFrequency "Соленые и маринованные огурцы"]
   [nil :FoodsConsumptionFrequency "Сосиски, сардельки"]
   [nil :FoodsConsumptionFrequency "Сушки, баранки"]
   [nil :FoodsConsumptionFrequency "Сыр твердый, плавленый"]
   [nil :FoodsConsumptionFrequency "Творог и блюда из творога"]
   [nil :FoodsConsumptionFrequency "Хлеб белый"]
   [nil :FoodsConsumptionFrequency "Хлеб черный"]
   [nil :FoodsConsumptionFrequency "Чай"]
   [nil :FoodsConsumptionFrequency "Шоколад, конфеты шоколадные"]
   [nil :FoodsConsumptionFrequency "Яблоки свежие"]
   [nil :FoodsConsumptionFrequency "Ягоды: смородина, земляника, черника"]
   [nil :FoodsConsumptionFrequency "Яйца вареные, омлет, яичница"]
   [nil :HealthState :ExaminationDateFlag]
   [nil :HealthState :ExaminationDate]
   [nil :HealthState :Eye]
   [nil :HealthState :GymGroup]
   [nil :HealthState :HealthGroup]
   [nil :HealthState :Hearing]
   [nil :HealthState :ID]
   [nil :HealthState :Posture]
   [nil :HealthState :Sight_left_eye]
   [nil :HealthState :Sight_right_eye]
   [nil :Minerals :Al]
   [nil :Minerals :Ca]
   [nil :Minerals :Ca_x002F_Al]
   [nil :Minerals :Ca_x002F_K]
   [nil :Minerals :Ca_x002F_P]
   [nil :Minerals :Cd]
   [nil :Minerals :Co]
   [nil :Minerals :Cr]
   [nil :Minerals :Cu]
   [nil :Minerals :Fe]
   [nil :Minerals :Fe_x002F_Cu]
   [nil :Minerals :ID]
   [nil :Minerals :K]
   [nil :Minerals :Mg]
   [nil :Minerals :Mn]
   [nil :Minerals :Na]
   [nil :Minerals :Ni]
   [nil :Minerals :P]
   [nil :Minerals :Pb]
   [nil :Minerals :Se]
   [nil :Minerals :Sr]
   [nil :Minerals :Zn]
   [nil :Minerals :Zn_x002F_Cd]
   [nil :Minerals :stf]
   [nil :PediatricExamination :AccomodationSpasm]
   [nil :PediatricExamination :Alalies]
   [nil :PediatricExamination :AllergicReactions]
   [nil :PediatricExamination :AllergicRhinitis]
   [nil :PediatricExamination :AlmondsAdenoidesHypertrophy]
   [nil :PediatricExamination :AnticnemionDeformation]
   [nil :PediatricExamination :AstenoneuroticSindrome]
   [nil :PediatricExamination :Astigmatism]
   [nil :PediatricExamination :BloodHemorrhagicDiseases]
   [nil :PediatricExamination :BronchitusAsthma]
   [nil :PediatricExamination :CaseDuration]
   [nil :PediatricExamination :CasesNumber]
   [nil :PediatricExamination :CholicWays]
   [nil :PediatricExamination :ChronicOtitis]
   [nil :PediatricExamination :Cryptorchism]
   [nil :PediatricExamination :DaysNumber]
   [nil :PediatricExamination :Dermatitis]
   [nil :PediatricExamination :Diabetes]
   [nil :PediatricExamination :Diagnosis]
   [nil :PediatricExamination :ExaminationDate]
   [nil :PediatricExamination :GastritisDuodenitis]
   [nil :PediatricExamination :HealthGroup]
   [nil :PediatricExamination :HearingDeoression]
   [nil :PediatricExamination :HeartAnomaly]
   [nil :PediatricExamination :HeartHum]
   [nil :PediatricExamination :Hernia]
   [nil :PediatricExamination :Hypermetrophya]
   [nil :PediatricExamination :ID]
   [nil :PediatricExamination :LiverDiseases]
   [nil :PediatricExamination :Myopia]
   [nil :PediatricExamination :NasalSeptumCurvature]
   [nil :PediatricExamination :Nephropathy]
   [nil :PediatricExamination :NeuroticDisorders]
   [nil :PediatricExamination :Obesity]
   [nil :PediatricExamination :OcclusionAnomaly]
   [nil :PediatricExamination :PepticUlcer]
   [nil :PediatricExamination :Pharingitis]
   [nil :PediatricExamination :Phimosis]
   [nil :PediatricExamination :Phyelonephritis]
   [nil :PediatricExamination :Platypodia]
   [nil :PediatricExamination :PostureDisturbance]
   [nil :PediatricExamination :RhythmDisturbance]
   [nil :PediatricExamination :ScoliosisLordosis]
   [nil :PediatricExamination :SigtDepression]
   [nil :PediatricExamination :Sinusitis]
   [nil :PediatricExamination :Spondilitis]
   [nil :PediatricExamination :Strabismus]
   [nil :PediatricExamination :System10]
   [nil :PediatricExamination :System11]
   [nil :PediatricExamination :System12]
   [nil :PediatricExamination :System13]
   [nil :PediatricExamination :System14]
   [nil :PediatricExamination :System1]
   [nil :PediatricExamination :System2]
   [nil :PediatricExamination :System3]
   [nil :PediatricExamination :System4]
   [nil :PediatricExamination :System5]
   [nil :PediatricExamination :System6]
   [nil :PediatricExamination :System7]
   [nil :PediatricExamination :System8]
   [nil :PediatricExamination :System9]
   [nil :PediatricExamination :SystemsNum]
   [nil :PediatricExamination :ThoraxDeformation]
   [nil :PediatricExamination :ThyroidDiseases]
   [nil :PediatricExamination :Tonsillitis]
   [nil :PediatricExamination :Urarasia]
   [nil :PediatricExamination :UrinaryWaysInfection]
   [nil :PediatricExamination :WryNeck]
   [nil :PhysicalDevelopment :AP_index]
   [nil :PhysicalDevelopment :DArterialPressureAfter]
   [nil :PhysicalDevelopment :DArterialPressureRest]
   [nil :PhysicalDevelopment :ExaminationDateFlag]
   [nil :PhysicalDevelopment :ExaminationDate]
   [nil :PhysicalDevelopment :Height]
   [nil :PhysicalDevelopment :ID]
   [nil :PhysicalDevelopment :IMT_index]
   [nil :PhysicalDevelopment :PulseAfter]
   [nil :PhysicalDevelopment :PulseRest]
   [nil :PhysicalDevelopment :SArterialPressureAfter]
   [nil :PhysicalDevelopment :SArterialPressureRest]
   [nil :PhysicalDevelopment :ThoraxCircle]
   [nil :PhysicalDevelopment :TrueDate]
   [nil :PhysicalDevelopment :Weight]
   [nil :PhysicalDevelopment :АД_нагрузка]
   [nil :PhysicalDevelopment :АД_покой]
   [nil :PhysicalFitness :BroadJump]
   [nil :PhysicalFitness :ExaminationDateFlag]
   [nil :PhysicalFitness :ExaminationDate]
   [nil :PhysicalFitness :ID]
   [nil :PhysicalFitness :InclinationForward]
   [nil :PhysicalFitness :LeftHandForce]
   [nil :PhysicalFitness :LongDistanceRun]
   [nil :PhysicalFitness :Long]
   [nil :PhysicalFitness :LungCapacity]
   [nil :PhysicalFitness :PullingUp]
   [nil :PhysicalFitness :RightHandForce]
   [nil :PhysicalFitness :ShortDistanceRun]
   [nil :PhysicalFitness :ShuttleRun]
   [nil :PhysicalFitness :TrunkLifting]
   [nil :ProductsConsumption :FrequencyCode10]
   [nil :ProductsConsumption :FrequencyCode11]
   [nil :ProductsConsumption :FrequencyCode12]
   [nil :ProductsConsumption :FrequencyCode13]
   [nil :ProductsConsumption :FrequencyCode14]
   [nil :ProductsConsumption :FrequencyCode15]
   [nil :ProductsConsumption :FrequencyCode16]
   [nil :ProductsConsumption :FrequencyCode17]
   [nil :ProductsConsumption :FrequencyCode18]
   [nil :ProductsConsumption :FrequencyCode19]
   [nil :ProductsConsumption :FrequencyCode1]
   [nil :ProductsConsumption :FrequencyCode20]
   [nil :ProductsConsumption :FrequencyCode21]
   [nil :ProductsConsumption :FrequencyCode22]
   [nil :ProductsConsumption :FrequencyCode23]
   [nil :ProductsConsumption :FrequencyCode24]
   [nil :ProductsConsumption :FrequencyCode25]
   [nil :ProductsConsumption :FrequencyCode26]
   [nil :ProductsConsumption :FrequencyCode27]
   [nil :ProductsConsumption :FrequencyCode28]
   [nil :ProductsConsumption :FrequencyCode29]
   [nil :ProductsConsumption :FrequencyCode2]
   [nil :ProductsConsumption :FrequencyCode30]
   [nil :ProductsConsumption :FrequencyCode31]
   [nil :ProductsConsumption :FrequencyCode32]
   [nil :ProductsConsumption :FrequencyCode33]
   [nil :ProductsConsumption :FrequencyCode34]
   [nil :ProductsConsumption :FrequencyCode35]
   [nil :ProductsConsumption :FrequencyCode36]
   [nil :ProductsConsumption :FrequencyCode37]
   [nil :ProductsConsumption :FrequencyCode38]
   [nil :ProductsConsumption :FrequencyCode39]
   [nil :ProductsConsumption :FrequencyCode3]
   [nil :ProductsConsumption :FrequencyCode40]
   [nil :ProductsConsumption :FrequencyCode41]
   [nil :ProductsConsumption :FrequencyCode42]
   [nil :ProductsConsumption :FrequencyCode43]
   [nil :ProductsConsumption :FrequencyCode44]
   [nil :ProductsConsumption :FrequencyCode45]
   [nil :ProductsConsumption :FrequencyCode46]
   [nil :ProductsConsumption :FrequencyCode47]
   [nil :ProductsConsumption :FrequencyCode48]
   [nil :ProductsConsumption :FrequencyCode49]
   [nil :ProductsConsumption :FrequencyCode4]
   [nil :ProductsConsumption :FrequencyCode50]
   [nil :ProductsConsumption :FrequencyCode51]
   [nil :ProductsConsumption :FrequencyCode52]
   [nil :ProductsConsumption :FrequencyCode53]
   [nil :ProductsConsumption :FrequencyCode54]
   [nil :ProductsConsumption :FrequencyCode55]
   [nil :ProductsConsumption :FrequencyCode56]
   [nil :ProductsConsumption :FrequencyCode57]
   [nil :ProductsConsumption :FrequencyCode58]
   [nil :ProductsConsumption :FrequencyCode59]
   [nil :ProductsConsumption :FrequencyCode5]
   [nil :ProductsConsumption :FrequencyCode60]
   [nil :ProductsConsumption :FrequencyCode61]
   [nil :ProductsConsumption :FrequencyCode62]
   [nil :ProductsConsumption :FrequencyCode63]
   [nil :ProductsConsumption :FrequencyCode64]
   [nil :ProductsConsumption :FrequencyCode65]
   [nil :ProductsConsumption :FrequencyCode66]
   [nil :ProductsConsumption :FrequencyCode67]
   [nil :ProductsConsumption :FrequencyCode6]
   [nil :ProductsConsumption :FrequencyCode7]
   [nil :ProductsConsumption :FrequencyCode8]
   [nil :ProductsConsumption :FrequencyCode9]
   [nil :ProductsConsumption :ID]
   [nil :ProductsConsumption :PortionsNum10]
   [nil :ProductsConsumption :PortionsNum11]
   [nil :ProductsConsumption :PortionsNum12]
   [nil :ProductsConsumption :PortionsNum13]
   [nil :ProductsConsumption :PortionsNum14]
   [nil :ProductsConsumption :PortionsNum15]
   [nil :ProductsConsumption :PortionsNum16]
   [nil :ProductsConsumption :PortionsNum17]
   [nil :ProductsConsumption :PortionsNum18]
   [nil :ProductsConsumption :PortionsNum19]
   [nil :ProductsConsumption :PortionsNum1]
   [nil :ProductsConsumption :PortionsNum20]
   [nil :ProductsConsumption :PortionsNum21]
   [nil :ProductsConsumption :PortionsNum22]
   [nil :ProductsConsumption :PortionsNum23]
   [nil :ProductsConsumption :PortionsNum24]
   [nil :ProductsConsumption :PortionsNum25]
   [nil :ProductsConsumption :PortionsNum26]
   [nil :ProductsConsumption :PortionsNum27]
   [nil :ProductsConsumption :PortionsNum28]
   [nil :ProductsConsumption :PortionsNum29]
   [nil :ProductsConsumption :PortionsNum2]
   [nil :ProductsConsumption :PortionsNum30]
   [nil :ProductsConsumption :PortionsNum31]
   [nil :ProductsConsumption :PortionsNum32]
   [nil :ProductsConsumption :PortionsNum33]
   [nil :ProductsConsumption :PortionsNum34]
   [nil :ProductsConsumption :PortionsNum35]
   [nil :ProductsConsumption :PortionsNum36]
   [nil :ProductsConsumption :PortionsNum37]
   [nil :ProductsConsumption :PortionsNum38]
   [nil :ProductsConsumption :PortionsNum39]
   [nil :ProductsConsumption :PortionsNum3]
   [nil :ProductsConsumption :PortionsNum40]
   [nil :ProductsConsumption :PortionsNum41]
   [nil :ProductsConsumption :PortionsNum42]
   [nil :ProductsConsumption :PortionsNum43]
   [nil :ProductsConsumption :PortionsNum44]
   [nil :ProductsConsumption :PortionsNum45]
   [nil :ProductsConsumption :PortionsNum46]
   [nil :ProductsConsumption :PortionsNum47]
   [nil :ProductsConsumption :PortionsNum48]
   [nil :ProductsConsumption :PortionsNum49]
   [nil :ProductsConsumption :PortionsNum4]
   [nil :ProductsConsumption :PortionsNum50]
   [nil :ProductsConsumption :PortionsNum51]
   [nil :ProductsConsumption :PortionsNum52]
   [nil :ProductsConsumption :PortionsNum53]
   [nil :ProductsConsumption :PortionsNum54]
   [nil :ProductsConsumption :PortionsNum55]
   [nil :ProductsConsumption :PortionsNum56]
   [nil :ProductsConsumption :PortionsNum57]
   [nil :ProductsConsumption :PortionsNum58]
   [nil :ProductsConsumption :PortionsNum59]
   [nil :ProductsConsumption :PortionsNum5]
   [nil :ProductsConsumption :PortionsNum60]
   [nil :ProductsConsumption :PortionsNum61]
   [nil :ProductsConsumption :PortionsNum62]
   [nil :ProductsConsumption :PortionsNum63]
   [nil :ProductsConsumption :PortionsNum64]
   [nil :ProductsConsumption :PortionsNum65]
   [nil :ProductsConsumption :PortionsNum66]
   [nil :ProductsConsumption :PortionsNum67]
   [nil :ProductsConsumption :PortionsNum6]
   [nil :ProductsConsumption :PortionsNum7]
   [nil :ProductsConsumption :PortionsNum8]
   [nil :ProductsConsumption :PortionsNum9]
   [nil :RiskFactors :AgeOfFather]
   [nil :RiskFactors :AgeOfMother]
   [nil :RiskFactors :AgeOfPermanentLiving]
   [nil :RiskFactors :BirthHeight]
   [nil :RiskFactors :BirthWeight]
   [nil :RiskFactors :BreastfeedingDuration]
   [nil :RiskFactors :ChildrenNumber]
   [nil :RiskFactors :ChronicDiseasesOfMother]
   [nil :RiskFactors :ComplicationsDuringChildbirth]
   [nil :RiskFactors :DiseasesOfFather]
   [nil :RiskFactors :DuringPregnancyMedications]
   [nil :RiskFactors :DuringPregnancyStress]
   [nil :RiskFactors :EatingAtSchool]
   [nil :RiskFactors :FamilySituation]
   [nil :RiskFactors :FamilyStructure]
   [nil :RiskFactors :FatherEducation]
   [nil :RiskFactors :FatherHarmfulFactorsContactsBeforeChildBirth]
   [nil :RiskFactors :FatherSmoking]
   [nil :RiskFactors :FatherUseOfAlcohol]
   [nil :RiskFactors :FeedingMixtures]
   [nil :RiskFactors :Form]
   [nil :RiskFactors :Hardening]
   [nil :RiskFactors :HarmfulFactorsContactsBeforePregnancy]
   [nil :RiskFactors :HarmfulFactorsContactsDurationsForMother]
   [nil :RiskFactors :HarmfulFactorsContactsDuringPregnancy]
   [nil :RiskFactors :HowManyTimesEatingOnWeekends]
   [nil :RiskFactors :HowOftenChildWasIll]
   [nil :RiskFactors :HowOftenEatingBakery]
   [nil :RiskFactors :HowOftenEatingEggs]
   [nil :RiskFactors :HowOftenEatingMeat]
   [nil :RiskFactors :HowOftenEatingMilkProducts]
   [nil :RiskFactors :HowOftenEatingVegetables]
   [nil :RiskFactors :ID]
   [nil :RiskFactors :IncomePerMember]
   [nil :RiskFactors :InfectiousDiseases]
   [nil :RiskFactors :LivingConditions]
   [nil :RiskFactors :LivingSpace]
   [nil :RiskFactors :MotherEducation]
   [nil :RiskFactors :MotherSmokingBeforePregnancy]
   [nil :RiskFactors :MotherUseOfAlcohol]
   [nil :RiskFactors :Name]
   [nil :RiskFactors :NapOnSundays]
   [nil :RiskFactors :OutdoorsDuration]
   [nil :RiskFactors :Patronimyc]
   [nil :RiskFactors :PregnancyToxemia]
   [nil :RiskFactors :SleepingDuration]
   [nil :RiskFactors :SomaticDiseasesOfMother]
   [nil :RiskFactors :SportOutsideInstitution]
   [nil :RiskFactors :SummerHolidays]
   [nil :RiskFactors :Surname]
   [nil :RiskFactors :TV_andComputerDurationPerDay]
   [nil :RiskFactors :TV_andComputerFrequency]
   [nil :RiskFactors :WalksDuration]
   [nil :RiskFactors :WhenChildMakesLessons]
   [nil :RiskFactors :WhereChildDoesHomework]
   [nil :StomatologicalExamination :ExanimationDate]
   [nil :StomatologicalExamination :ID]
   [nil :StomatologicalExamination :KPI]
   [nil :StomatologicalExamination :KPP_big]
   [nil :StomatologicalExamination :KPU_big]
   [nil :StomatologicalExamination :OHIS]
   [nil :StomatologicalExamination :kpp_small]
   [nil :StomatologicalExamination :kpu_KPU]
   [nil :StomatologicalExamination :kpu_small]
])

(defn column-name [column]
  (let [[name-override table-name table-column] column]
    (or name-override
        (get-in all-table-captions [table-name table-column])
        (name table-column))))

(defn column-path [column]
  (->> column
      rest
      (map name)
      (str/join " ")))

(def header-with-names
  (cons "ID" (map column-name columns)))

(def header-with-table-name
  (cons "ID" (map (comp name second) columns)))

(def header-with-table-column
  (cons "ID" (map (comp name #(nth % 2)) columns)))



(defn make-row [[id data]]
  (apply vector
         id
         (map #(get-in data % "") (map rest columns))))

(def rows
  (map make-row data-by-id))

(def sorted-rows
  (reduce (fn [result current]
            (sort-by #(% current) result))
          rows
          (map #(.indexOf header-with-names %) ["ФИО"
                                                "Класс"
                                                "Школа"
                                                ])))

(defn write-to-file [file-name]
  (with-open [out-file (io/writer file-name)]
    (csv/write-csv out-file (concat [header-with-table-name
                                     header-with-table-column
                                     header-with-names]
                                    sorted-rows))))
