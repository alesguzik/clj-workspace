(ns workspace.vk.auth
  (:require
   [workspace.core :as core]
   [workspace.vk :as vk]
   ))

(defn make-vk-auth-url
  [app-id permissions redirect-uri display api-version]
  (str "https://oauth.vk.com/authorize?"
       "client_id="     app-id
       "&scope="        permissions
       "&redirect_uri=" redirect-uri
       "&display="      display
       "&v="            api-version
       "&response_type=token"))

(def all-permissions
  "notify,friends,photos,audio,video,docs,notes,pages,status,offers,questions,wall,groups,messages,email,notifications,stats,ads,offline")

(def standalone-redirect-url "https://oauth.vk.com/blank.html")

(def auth-url
  (make-vk-auth-url my-app-id
                    all-permissions
                    standalone-redirect-url
                    "popup"
                    "5.21"))

(defn authorize []
  (browse/browse-url auth-url))

(defn response-to-token [response]
  (as-> response x
    (str/split x #"#")
    (peek x)
    (str/split x #"&")
    (map #(str/split % #"=") x)
    (map fix-keys x)
    (into {} x)
    :access-token))

(defn token []
  (-> (core/config) :vk :token))

(token)
