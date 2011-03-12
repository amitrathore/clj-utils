(ns org.rathore.amit.utils.maps
  (:use clojure.contrib.generic.functor))

(defn- kv-updater-for-val [val-tester val-updater]
  #(if (val-tester %) (val-updater %) %))

(defn update-map-vals [m val-tester val-updater]
  (fmap (kv-updater-for-val val-tester val-updater) m))

(defn- kv-updater-for-key [key-tester key-updater [k v]]
  (if (key-tester k)
    {(key-updater k) v}
    {k v}))

(defn update-map-keys [m key-tester key-updater]
  (apply merge (map (partial kv-updater-for-val key-tester key-updater) m)))