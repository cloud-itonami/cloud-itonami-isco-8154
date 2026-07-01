# cloud-itonami-isco-8154

Open Occupation Blueprint for **ISCO-08 8154**: Bleaching, Dyeing and Fabric Cleaning Machine Operators.

This repository designs a forkable OSS business for an independent fabric-processing plant operator: a plant-monitoring robot performs chemical-level sensing and sampling under a governor-gated actor, so the operator keeps their own process and safety records instead of renting a closed plant-control SaaS.

## Robotics premise

All cloud-itonami verticals are designed on the premise that a **robot performs
the physical domain work**. Here a plant-monitoring robot performs chemical-level sensing and sample collection near dyeing/bleaching equipment under an actor that proposes
actions and an independent **Fabric Processing Governor** that gates them. The governor never
dispatches hardware itself; `:high`/`:safety-critical` actions (such as
handling concentrated bleaching/dyeing chemicals, or operating near pressurized equipment) require human sign-off.

A live sample of the operator console (robotics safety console, shared template) is rendered in [docs/samples/operator-console.html](docs/samples/operator-console.html) — pure-data HTML output of `kotoba.robotics.ui`.

## Core Contract

```text
production order + chemical handling protocol + safety envelope
        |
        v
Process Advisor -> Fabric Processing Governor -> process/monitor, or human sign-off
        |
        v
robot actions (gated) + operating records + audit ledger
```

No automated advice can dispatch a robot action the governor refuses, suppress
an operating record, or disclose sensitive data without governor approval and
audit evidence.

## Capability layer

Resolves via [`kotoba-lang/occupation`](https://github.com/kotoba-lang/occupation)
(ISCO-08 `8154`). Required capabilities:

- :robotics
- :telemetry
- :dmn
- :bpmn
- :audit-ledger

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md).

## License

AGPL-3.0-or-later.
