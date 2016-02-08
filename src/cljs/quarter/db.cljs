(ns quarter.db)

(def default-db
  {})
(defrecord ExpectedGear [heading gear-name quantity])       ;; Heading filled in, or gear-name and quantity
(defrecord ExpectedSite [site-name numbers expected-gear])
(defrecord ActualGear [actual-quantity expected-quantity])
(defrecord Shakedown [username shake-date site-name site-number gear-quantity gear-notes]) ;; actual-gear is map from gear-name to ActualGear
(defrecord LoginToken [name picture email hash])

(defn keywordize [x parent test-fxn]
  (cond
    (map? x) (reduce-kv (fn [m k v]
                          (let [new-k (if (test-fxn parent) (keyword k) k)]
                            (assoc m new-k (keywordize v new-k test-fxn))))
                        {} x)
    (seq? x) (map #(keywordize % nil test-fxn) x)
    :else x))


(defn echo [msg x]
  (println msg x)
  x)

(defn json->Shakedown [txt]
  (if txt
    (map->LoginToken
      (keywordize (js->clj (.parse js/JSON (clj->js txt)))
                  nil
                  #(not (#{:gear-quantity :gear-notes} %))
                  ))
    nil))


(defn json->LoginToken [txt]
  (if txt
    (map->Shakedown
      (keywordize (js->clj (.parse js/JSON (clj->js txt)))
                  nil
                  (fn [_] true)
                  ))
    nil))