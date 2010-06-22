(ns org.rathore.amit.utils.clojure
  (:import (java.io PushbackReader StringReader)))

;; internals

(defn var-ize [var-vals]
  (loop [ret [] vvs (seq var-vals)]
    (if vvs
      (recur  (conj (conj ret `(var ~(first vvs))) (second vvs))
	      (next (next vvs)))
      (seq ret))))

;; utils

(defmacro run-and-measure-timing [expr]
  `(let [start-time# (System/currentTimeMillis)
	 response# ~expr
	 end-time# (System/currentTimeMillis)]
     {:time-taken (- end-time# start-time#) :response response# :start-time start-time# :end-time end-time#}))

;; function creators

(defmacro defmemoized [fn-name args & body]
  `(def ~fn-name (memoize (fn ~args 
			    (do
			      ~@body)))))

(defn create-runonce [function] 
  (let [sentinel (Object.)
        result (atom sentinel)] 
    (fn [& args]
      (locking sentinel 
        (if (= @result sentinel)
          (reset! result (apply function args)) 
          @result)))))

(defmacro defrunonce [fn-name args & body]
  `(def ~fn-name (create-runonce (fn ~args ~@body))))


;; language-level macros
(defmacro aif 
  ([test-form then-form]
     `(let [~'it ~test-form]
        (if ~'it ~then-form)))
  ([test-form then-form else-form]
     `(let [~'it ~test-form]
        (if ~'it ~then-form ~else-form))))

(defmacro anil?
  ([test-form then-form]
     `(let [~'it ~test-form]
        (if-not (nil? ~'it) ~then-form)))
  ([test-form then-form else-form]
     `(let [~'it ~test-form]
        (if-not (nil? ~'it) ~then-form ~else-form))))

(defmacro awhen [test-form & body]
  `(aif ~test-form (do ~@body)))

(defmacro awhile [test-expr & body]
  `(while (let [~'it ~test-expr]
            (do ~@body)
            ~'it)))

(defmacro aand [& tests]
  (if (empty? tests)
    true
    (if (empty? (rest tests))
      (first tests)
      (let [first-test (first tests)]
        `(aif ~first-test
              (aand ~@(rest tests)))))))

