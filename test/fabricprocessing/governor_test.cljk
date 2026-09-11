(ns fabricprocessing.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [fabricprocessing.store :as store]
            [fabricprocessing.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Textile Finishers"})
    (store/register-batch! st {:batch-id "B-1" :client-id "client-1"
                               :name "denim-lot-9"
                               :max-chemical-concentration-ppm 50
                               :min-process-temp-c 40
                               :max-process-temp-c 60})
    st))

(defn- process-batch [concentration temp]
  {:op :approve-batch-process :effect :propose :batch-id "B-1"
   :chemical-concentration-ppm concentration :process-temp-c temp
   :confidence 0.9 :stake :low})

(def ^:private req {:client-id "client-1"})

(deftest ok-within-concentration-and-temperature-envelope
  (let [st (fresh-store)
        v (governor/check req {} (process-batch 30 50) st)]
    (is (:ok? v))))

(deftest ok-at-exact-ceiling-and-envelope-edges
  (testing "the concentration ceiling and temperature envelope boundaries are inclusive"
    (let [st (fresh-store)]
      (is (:ok? (governor/check req {} (process-batch 50 40) st)))
      (is (:ok? (governor/check req {} (process-batch 50 60) st))))))

(deftest hard-on-chemical-concentration-exceeds-ceiling
  (testing "concentration is measured, not judged by smell"
    (let [st (fresh-store)
          v (governor/check req {} (assoc (process-batch 100 50) :confidence 0.99) st)]
      (is (:hard? v))
      (is (some #(= :chemical-concentration-exceeds-ceiling (:rule %)) (:violations v))))))

(deftest hard-on-process-temperature-out-of-envelope
  (let [st (fresh-store)
        v (governor/check req {} (assoc (process-batch 30 90) :confidence 0.99) st)]
    (is (:hard? v))
    (is (some #(= :process-temperature-out-of-envelope (:rule %)) (:violations v)))))

(deftest hard-on-unknown-batch
  (let [st (fresh-store)
        v (governor/check req {} (assoc (process-batch 30 50) :batch-id "B-ghost") st)]
    (is (:hard? v))
    (is (some #(= :unknown-batch (:rule %)) (:violations v)))))

(deftest hard-on-foreign-batch
  (let [st (fresh-store)]
    (store/register-client! st {:client-id "client-2" :name "Other"})
    (let [v (governor/check {:client-id "client-2"} {} (process-batch 30 50) st)]
      (is (:hard? v))
      (is (some #(= :batch-wrong-client (:rule %)) (:violations v))))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        v (governor/check {:client-id "nobody"} {} (process-batch 30 50) st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-actuation-violation
  (let [st (fresh-store)
        v (governor/check req {} (assoc (process-batch 30 50) :effect :direct-write) st)]
    (is (:hard? v))
    (is (some #(= :no-actuation (:rule %)) (:violations v)))))

(deftest always-escalates-concentrated-chemical-handling-even-at-high-confidence
  (testing "no concentrated-chemical handling without the governor gate"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-concentrated-chemical-handling :effect :propose
                                    :batch-id "B-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest always-escalates-pressurized-equipment-operation-even-at-high-confidence
  (testing "pressurized-equipment operation requires human sign-off"
    (let [st (fresh-store)
          v (governor/check req {} {:op :approve-pressurized-equipment-operation :effect :propose
                                    :batch-id "B-1" :confidence 0.99 :stake :low} st)]
      (is (not (:hard? v)))
      (is (:escalate? v)))))

(deftest escalates-low-confidence
  (let [st (fresh-store)
        v (governor/check req {} (assoc (process-batch 30 50) :confidence 0.3) st)]
    (is (not (:hard? v)))
    (is (:escalate? v))))
