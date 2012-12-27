(ns hotscrap.hotline
  (:require [net.cgrand.enlive-html :as html]
            [clj-http.client :as http]
            [clojure.string :as s]
            [hotscrap.db :as db])
  (:use [clojure.tools.logging :only (info error)])
  (:import [java.net URL]
           java.nio.charset.Charset))


(def start-page "http://m.hotline.ua/pr/193-3-12")

(defn median
  [coll]
  (let [c (count coll)
        m (int (/ c 2))
        s (sort coll)]
    (if (even? c)
      (int
       (/ (+ (nth s m) (nth s (dec m))) 2))
      (nth s m))))

(declare get-score)

(comment
  (http/post "http://m.hotline.ua/pr/193-3-12?r=0.08519863383844495" {:form-params {:page 2 :more 1 :quantity 20}})

  ;; end
  (http/post
   "http://m.hotline.ua/pr/1465-1482-127?r=0.38842732831835747"
   {:form-params { :page 39  :more 1 :quantity 20}
    :headers { "X-Requested-With" "XMLHttpRequest" }})


  ;; geekscore
  (http/get "http://browser.primatelabs.com/geekbench2/search"
            {:query-params
             {"utf8" "%E2%9C%93" "q" "Intel+Core+i3-3110M"}})
  )

(defn load-link-page
  "Load links from page N"
  [n]
  (let [url (format "http://m.hotline.ua/pr/193-3-12?r=%f" (rand))
        body (-> url
            (http/post
             {:form-params { :page n :more 1 :quantity 20}
              :headers { "X-Requested-With" "XMLHttpRequest" }})
            :body)]
    (-> body html/html-snippet
        (html/select [:a.list_tovar])
        (->> (map #(get-in % [:attrs :href]))))))


(defn load-info
  [relative-url]
  (let [url (format "http://m.hotline.ua%s" relative-url)]
    (-> url
        http/get
        :body)))

(defn links
  [limit]
  (flatten (pmap load-link-page (range 1 (inc limit)))))

(defn property-row
  [html n]
  (-> html
      (html/select [:table#descr_short [:tr (html/nth-child n)]
                    [:td (html/nth-last-child 1)]])
      html/texts
      first
      s/trim))

(defn props
  [link]
  (let [info (load-info link)
        html (html/html-snippet info)
        name (-> html
               (html/select [:h3.title])
               first
               :content
               last
               s/trim)
        proc (property-row html 4)
        proc-speed (-> (property-row html 5)
                       (s/replace "," ".")
                       (s/replace #"-.+" "")
                       Float/parseFloat)
        screen (property-row html 3)
        size (property-row html 2)
        price (-> html
                (html/select [:.info_pr :.orng])
                html/texts
                first
                (s/replace #"[^0-9]" "")
                s/trim
                Integer/parseInt)        
        scores (get-score proc proc-speed)
        score (if (empty? scores) 0 (float (median scores)))
        grade (if (or (zero? price) (zero? score)) 0 (float (/ score price)))]
    {
     :name name
     :proc proc
     :proc-speed proc-speed
     :screen screen
     :size size
     :price price
     :score score
     :grade grade
     }))


(defn fetch-links
  []
  (doall
   (pmap props (links 1))))


(defn load-into-db
  [pages]
  (db/clear)
  (dorun
   (pmap #(-> % props db/new-notebook) (links pages))))

(defn row-info
  "Covert row to { freq score }"
  [row]
  #_(prn row)
  {:freq (-> row (html/select [:.frequency]) html/texts
             (->> (apply str)) s/trim Integer/parseInt)
   :score (-> row (html/select [:.score]) html/texts
              (->> (apply str)) s/trim Integer/parseInt)})

(defn freq-filter
  [freq row]
  (let [scaled (* 1000 freq)
        f (row :freq)]
    (<
     (Math/abs (- f scaled))
     10)))

;; (defn a
;;   [n]
;;   (lazy-seq (concat [n]
;;                     (if (> n 10)
;;                       '()
;;                       (a (inc n))))))


(defn get-score-page
  [name freq page-no]
  ;; (println "Loading page" page-no)
  (let [page
        (http/get "http://browser.primatelabs.com/geekbench2/search"
                  {:query-params
                   {"utf8" "%E2%9C%93" "q" name "page" page-no}})
        rows (-> page
                 :body
                 html/html-snippet
                 (html/select [:tbody :tr]))
        matched-rows (->> rows
                          (map row-info)
                          (filter #(freq-filter freq %)))
        results (map :score matched-rows)]
    results))

(def memoize-get-score-page (memoize get-score-page))

;; Get score from geekbench
(defn get-score
  [name freq]
  (-> (pmap #(memoize-get-score-page name freq %) (range 1 20))
      flatten))

(defn load-score
  []
  (-> (get-score "Intel Core i3-3110M" 2.4)
      median))