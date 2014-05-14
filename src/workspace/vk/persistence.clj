(ns workspace.vk.persistence
  (:require
   [clojurewerkz.neocons.rest :as nr]
   [clojurewerkz.neocons.rest.nodes :as nn]
   ))

(defvktype user
  sex
  bdate
  city
  country
  photo_50
  photo_100
  photo_200_orig
  photo_200
  photo_400_orig
  photo_max
  photo_max_orig
  online
  online_mobile
  lists
  domain
  has_mobile
  contacts
  connections
  site
  education
  universities
  schools
  can_post
  can_see_all_posts
  can_see_audio
  can_write_private_message
  status
  last_seen
  common_count
  relation
  relatives
  counters
  screen_name
  timezone)

(defvktype group
  city
  country
  place
  description
  wiki_page
  members_count
  counters
  start_date
  finish_date
  can_post
  can_see_all_posts
  activity
  status
  contacts
  links
  fixed_post
  verified
  site
  )

(defvktype photo
  id
  album_id
  owner_id
  user_id
  photo_75
  photo_130
  photo_604
  photo_807
  photo_1280
  photo_2560
  width
  height
  text
  date
  ;; extended:
  likes
  comments
  tags
  can_comment)

(defvktype audio
  id
  owner_id
  artist
  title
  duration
  url
  lyrics_id
  album_id
  genre_id)

(defvktype lyrics
  lyrics_id
  text)

(defvktype video
  id
  owner_id
  title
  description
  duration
  link
  photo_130
  photo_320
  photo_640
  date
  views
  comments
  player
  ;; extended:
  privacy_view
  privacy_comment
  can_comment
  can_repost
  likes ;; user_likes count
  repeat)

(defvktype doc
  id
  owner_id
  title
  size
  ext
  url
  photo_100
  photo_130)

(defvktype wall
  id
owner_id
from_id
date
text
reply_owner_id
reply_post_id
friends_only
comments
    count
    can_post*
likes
    count
    user_likes*
    can_like*
    can_publish*
reposts
    count
    user_reposted*
post_type
post_source*
attachments
geo
    type
    coordinates
    place
        id
        title
        latitude
        longitude
        created
        icon
        country
        city
        ;;Если место добавлено как чекин в сообщество, объект place имеет дополнительные поля:
        type
        group_id
        group_photo
        checkins
        updated
        address
signer_id
copy_history
;; extended:
wall
profiles
groups)
