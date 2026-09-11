(ns fabricprocessing.advisor
  "ProcessAdvisor — the advisor named in this repository's README,
  proposing a batch operation (approve a batch process, approve
  concentrated-chemical handling, approve pressurized-equipment
  operation) from a production order, chemical handling protocol and
  safety envelope. Swappable mock/llm; the advisor ONLY proposes —
  `fabricprocessing.governor` checks the chemical-concentration
  ceiling and process-temperature envelope independently and always
  escalates concentrated-chemical/pressurized-equipment decisions.
  Modeled on cloud-itonami-isco-4311's advisor.

  A proposal: {:op :approve-batch-process|:approve-concentrated-chemical-handling|:approve-pressurized-equipment-operation
               :effect :propose :batch-id str
               :chemical-concentration-ppm number :process-temp-c number
               :stake kw :confidence n :rationale str}"
  ;; clojure.edn, not clojure.core/read-string: this parses untrusted
  ;; advisor output, and the core reader executes #=(...) at read time.
  (:require [clojure.edn :as edn]))

(defprotocol Advisor
  (-advise [advisor store request] "request -> proposal map"))

(defn- infer [_store {:keys [op stake batch-id chemical-concentration-ppm process-temp-c] :as request}]
  {:op op
   :effect :propose
   :batch-id batch-id
   :chemical-concentration-ppm chemical-concentration-ppm
   :process-temp-c process-temp-c
   :stake (or stake :low)
   :confidence (case (or stake :low) :high 0.7 :medium 0.85 :low 0.95)
   :rationale (str "proposed " (name op) " for client " (:client-id request))})

(defn mock-advisor []
  (reify Advisor
    (-advise [_ store request] (infer store request))))

(def ^:private system-prompt
  "You are a fabric-processing advisor. Given a request, propose an
   :op, the :batch-id, :chemical-concentration-ppm and
   :process-temp-c, an honest :confidence and a :stake. Never call an
   over-ceiling chemical concentration or an out-of-envelope process
   temperature conforming — the governor checks both against the
   registered batch record. Concentrated-chemical and pressurized-
   equipment decisions always require human sign-off regardless of
   confidence.")

(defn- parse-proposal [content]
  (try
    (let [p (edn/read-string content)]
      (if (map? p)
        (assoc p :effect :propose)
        {:op :unknown :effect :propose :confidence 0.0 :stake :high
         :rationale "unparseable LLM response"}))
    (catch #?(:clj Exception :cljs js/Error) _
      {:op :unknown :effect :propose :confidence 0.0 :stake :high
       :rationale "LLM response parse failure"})))

(defn llm-advisor
  [chat-model model-generate-fn gen-opts]
  (reify Advisor
    (-advise [_ _store request]
      (let [msgs [{:role :system :content system-prompt}
                  {:role :user :content (str "operation request: " (pr-str request))}]
            resp (model-generate-fn chat-model msgs gen-opts)]
        (parse-proposal (:content resp))))))
