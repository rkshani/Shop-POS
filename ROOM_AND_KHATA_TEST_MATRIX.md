# Room & Khata Integration Test Matrix — Batch 07

These are **planned/not executed** tests; they are not evidence of passing tests.

| ID | Test scenario | Expected DB invariants |
|---|---|---|
| DB-01 | Sale includes nonexistent product | No sale, sale items, invoice counter increment, stock movement or ledger entry |
| DB-02 | Two sale lines use same product; combined qty > stock | Entire sale rejected without partial stock change |
| DB-03 | Sale with missing Khata customer | No invoice/stock mutation |
| DB-04 | Sale customer balance close to Long.MAX_VALUE | Rollback invoice, stock, counter and ledger on overflow |
| DB-05 | Return references a deleted/missing product | No return, inventory, counter, refund or Khata changes |
| DB-06 | Return with corrupt original quantity or negative unit price | Rejected without side effects |
| DB-07 | Two returns attempt exceeding original quantity | Only allowed qty returns; other transaction fails atomically |
| DB-08 | Partial cash refund exceeds original paid amount after earlier refunds | Reject; leave all tables unchanged |
| DB-09 | Khata customer removed before return | Reject; no fabricated zero balance |
| DB-10 | Two concurrent customer payments exceed current balance | At most one commits; balance never negative |
| DB-11 | Purchase references missing product | Entire purchase rolls back |
| DB-12 | Backup restore includes invalid references | Reject before replacing original data |
| KH-01 | Later customer-level payment exists with no sale allocation | Cannot authorize extra cash refund for any specific invoice |
| KH-02 | Old database upgraded to invoice allocation schema | Migration preserves every legacy amount without guessing invoice attribution |

## Release gating

Run all relevant Room scenarios against a real in-memory Room SQLite database on JVM (or Android instrumentation if needed), assert rollback across *every* touched table, and run full builds. Document test command, platform, timestamp and test report. Do not mark as PASS from code inspection alone. Device tests are the FINAL phase.
