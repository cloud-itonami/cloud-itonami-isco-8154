(ns fabricprocessing.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [fabricprocessing.actor :as actor]
            [fabricprocessing.store :as store]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "client-1" :name "Kobo Textile Finishers"})
    (store/register-batch! st {:batch-id "B-1" :client-id "client-1"
                               :name "denim-lot-9"
                               :max-chemical-concentration-ppm 50
                               :min-process-temp-c 40
                               :max-process-temp-c 60})
    st))

(deftest commits-an-in-ceiling-in-envelope-batch-process
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-batch-process :stake :low
                 :batch-id "B-1" :chemical-concentration-ppm 30 :process-temp-c 50}
        result (actor/run-request! graph request {} "thread-1")]
    (is (= :done (:status result)))
    (is (some? (get-in result [:state :record])))
    (is (= 1 (count (store/records-of st "client-1"))))))

(deftest holds-an-out-of-envelope-batch-process
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-batch-process :stake :low
                 :batch-id "B-1" :chemical-concentration-ppm 30 :process-temp-c 90}
        result (actor/run-request! graph request {} "thread-2")]
    (is (= :hold (:disposition (:state result))))
    (is (empty? (store/records-of st "client-1")))))

(deftest interrupts-then-approves-pressurized-equipment-on-human-approval
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        request {:client-id "client-1" :op :approve-pressurized-equipment-operation :stake :low
                 :batch-id "B-1"}
        interrupted (actor/run-request! graph request {} "thread-3")]
    (is (= :interrupted (:status interrupted)))
    (is (empty? (store/records-of st "client-1")))
    (let [resumed (actor/approve! graph "thread-3")]
      (is (= :done (:status resumed)))
      (is (= 1 (count (store/records-of st "client-1")))))))
