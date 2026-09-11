(ns fabricprocessing.governor
  "FabricProcessingGovernor — the independent safety/traceability
  layer named in this repository's README/business-model.md, gating
  the robot-dispensed physical work (chemical-level sensing, sample
  collection) an advisor may propose. The governor never dispatches
  hardware itself. Modeled on cloud-itonami-isco-4311's
  bookkeeping.governor. Batch twist: a proposed batch's measured
  chemical concentration is arithmetic comparison against the
  registered ceiling — concentration is measured, not judged by
  smell — and the measured process temperature must fall inside the
  registered safety-envelope band.

  HARD invariants (:hard? true, ALWAYS :hold, never overridable):
    1. client provenance — the organization must be registered.
    2. no-actuation      — proposal :effect must be :propose (the
                           governor never dispatches hardware; it only
                           gates what the robot may execute).
    3. batch basis          — a process approval must cite a
                           REGISTERED batch belonging to this client.
    4. chemical-concentration ceiling — the proposed measured
                           concentration must not exceed the batch's
                           registered :max-chemical-concentration-ppm
                           (measured, not judged by smell).
    5. process-temperature envelope — the proposed measured process
                           temperature must fall inside the batch's
                           registered [:min-process-temp-c,
                           :max-process-temp-c] band.
  ESCALATION invariants (:escalate? true, ALWAYS human sign-off per
  business-model.md's Trust Controls — these are :high/
  :safety-critical regardless of confidence):
    6. :op :approve-concentrated-chemical-handling (no concentrated-
                           chemical handling without the governor
                           gate).
    7. :op :approve-pressurized-equipment-operation (pressurized-
                           equipment operation requires human
                           sign-off).
    8. low confidence (< `confidence-floor`)."
  (:require [fabricprocessing.store :as store]))

(def confidence-floor 0.6)

(def ^:private always-escalate-ops #{:approve-concentrated-chemical-handling
                                     :approve-pressurized-equipment-operation})

(defn- hard-violations [{:keys [request proposal]} client-record b]
  (let [{:keys [op chemical-concentration-ppm process-temp-c]} proposal
        process? (= :approve-batch-process op)]
    (cond-> []
      (nil? client-record)
      (conj {:rule :no-client :detail "未登録 client"})

      (not= :propose (:effect proposal))
      (conj {:rule :no-actuation :detail "effect は :propose のみ許可（governor はハードウェアを直接起動しない）"})

      (and process? (nil? b))
      (conj {:rule :unknown-batch :detail "未登録 batch への処理承認は不可"})

      (and process? b (not= (:client-id b) (:client-id request)))
      (conj {:rule :batch-wrong-client :detail "batch が別 client のもの"})

      (and process? b (number? chemical-concentration-ppm)
           (> chemical-concentration-ppm (:max-chemical-concentration-ppm b)))
      (conj {:rule :chemical-concentration-exceeds-ceiling
             :detail (str "測定薬品濃度 " chemical-concentration-ppm "ppm > 登録済み上限 "
                          (:max-chemical-concentration-ppm b) "ppm（濃度は測定であって匂いの判断ではない）")})

      (and process? b (number? process-temp-c)
           (or (< process-temp-c (:min-process-temp-c b))
               (> process-temp-c (:max-process-temp-c b))))
      (conj {:rule :process-temperature-out-of-envelope
             :detail (str "処理温度 " process-temp-c "℃ が登録済み安全域 ["
                          (:min-process-temp-c b) ", " (:max-process-temp-c b)
                          "]℃ の外")}))))

(defn check
  "Assess a proposal against `request`/`context`/`proposal` and a
  `store` implementing `fabricprocessing.store/Store`. Pure — never
  mutates the store, never dispatches the robot."
  [request context proposal store]
  (let [client-record (store/client store (:client-id request))
        b (some->> (:batch-id proposal) (store/batch store))
        hard (hard-violations {:request request :proposal proposal}
                              client-record b)
        hard? (boolean (seq hard))
        conf (or (:confidence proposal) 0.0)
        low? (< conf confidence-floor)
        always-risky? (contains? always-escalate-ops (:op proposal))]
    {:ok? (and (not hard?) (not low?) (not always-risky?))
     :violations hard
     :confidence conf
     :hard? hard?
     :escalate? (and (not hard?) (or low? always-risky?))}))
