(ns workspace.vk.api
  (:require
   [workspace.vk :as vk :refer [make-api-call]]
   [workspace.vk.auth :as vk-auth]
   ))

(defn fix-keys [[k v]]
  [(-> k (str/replace "_" "-") keyword) v])

(def default-options
  {:access-token (vk-auth/token)
   :v "5.21"})

(defn to-param-key [key]
  (-> key
      name
      (str/replace #"-" "_")))

(defn from-param-key [key]
  (-> key
      (str/replace #"_" "-")
      keyword))

(defn convert-to-params [options]
  (let [key-renaming (into {}
                           (mapv (juxt identity to-param-key)
                                 (keys options)))]
    (set/rename-keys options key-renaming)))

(defn api-method-url [method]
  (str "https://api.vk.com/method/" method))

(defn request-options [options]
  (-> (merge default-options options)
      convert-to-params))

(defn make-async-api-call [method options]
  (future
    (when-let [response
               @(http/get (api-method-url method)
                         {:query-params (request-options options)})]
      (-> response
          :body
          (json/read-str :key-fn from-param-key)))))

(defn make-api-call [method options]
  @(make-async-api-call method options))

(def all-user-fields
  "sex,bdate,city,country,photo_50,photo_100,photo_200_orig,photo_200,photo_400_orig,photo_max,photo_max_orig,online,online_mobile,lists,domain,has_mobile,contacts,connections,site,education,universities,schools,can_post,can_see_all_posts,can_see_audio,can_write_private_message,status,last_seen,common_count,relation,relatives,counters,screen_name,timezone")

(make-api-call "users.get" {:user-id 3885655})
