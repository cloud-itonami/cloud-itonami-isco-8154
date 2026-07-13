(ns fabricprocessing.store
  "SSoT for the ISCO-08 8154 independent fabric processing operations
  actor (itonami actor pattern, ADR-2607011000 / CLAUDE.md Actors
  section; README's 'Robotics premise' — a plant-monitoring robot
  performs chemical-level sensing and sample collection under this
  advisor/governor pair, which never dispatches hardware itself).
  Modeled on cloud-itonami-isco-4311's bookkeeping.store.

  Domain:

    client — a registered organization (:client-id, :name)
    batch  — a registered processing batch {:batch-id :client-id
             :name :max-chemical-concentration-ppm number
             :min-process-temp-c number :max-process-temp-c number}.
             `:max-chemical-concentration-ppm` is the registered
             ceiling a proposed batch's measured chemical
             concentration must not exceed (concentration is
             measured, not judged by smell);
             `:min-process-temp-c`/`:max-process-temp-c` is the
             registered safety-envelope band a proposed batch's
             measured process temperature must fall inside.
    record — a committed operating record (approved batch process) —
             written ONLY via commit-record!.
    ledger — append-only audit trail, commit or hold."
  )

(defprotocol Store
  (client [s client-id])
  (batch [s batch-id])
  (records-of [s client-id])
  (ledger [s])
  (register-client! [s client])
  (register-batch! [s b])
  (commit-record! [s record])
  (append-ledger! [s fact]))

(defrecord MemStore [a]
  Store
  (client [_ client-id] (get-in @a [:clients client-id]))
  (batch [_ batch-id] (get-in @a [:batches batch-id]))
  (records-of [_ client-id] (filter #(= client-id (:client-id %)) (:records @a)))
  (ledger [_] (:ledger @a))
  (register-client! [s client]
    (swap! a assoc-in [:clients (:client-id client)] client) s)
  (register-batch! [s b]
    (swap! a assoc-in [:batches (:batch-id b)] b) s)
  (commit-record! [s record]
    (swap! a update :records (fnil conj []) record) s)
  (append-ledger! [s fact]
    (swap! a update :ledger (fnil conj []) fact) s))

(defn mem-store
  ([] (mem-store {}))
  ([seed] (->MemStore (atom (merge {:clients {} :batches {} :records [] :ledger []}
                                   seed)))))
