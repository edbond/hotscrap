(ns hotscrap.db
  (:use korma.db korma.core))

(defdb development (postgres {:db "hotscrap"
                              :user "eduard" }))


(defentity notebooks ;; By default "id"
  (entity-fields :id :name) ;; Default fields for selects
  (database development))


(defn new-notebook
  [data]
  (insert notebooks (values data)))

(defn clear
  []
  (delete notebooks))