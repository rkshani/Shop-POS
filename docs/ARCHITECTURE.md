# Architecture

## The shape

Clean Architecture in three layers, MVVM at the top, one Gradle module.

```
      Compose screen  ──uses──▶  ViewModel  ──calls──▶  UseCase
                                     ▲                     │
                                     │                     ▼
                                 StateFlow            Repository (interface)
                                                            ▲
                                                            │ implements
                                                   RepositoryImpl ──▶ Room DAO
```

`domain/` has no Room, Compose or Koin imports. That constraint is what makes
`CheckoutUseCaseTest` and `CartTest` run in milliseconds with no device.

## Why one `shared` module, not twelve

A `:core:*` / `:domain` / `:data` / `:feature:*` split is the textbook answer and it
buys real things — enforced boundaries, parallel builds, faster incremental compiles.
It also multiplies Gradle config across KMP targets and makes Room's KSP wiring
fiddly for every module that touches an entity.

For a codebase this size the boundary is enforced by package structure and `internal`
visibility instead. The package layout maps one-to-one onto modules, so splitting later
is mechanical: move `presentation/khata/` to `:feature:khata`, add a build file, done.
Split when build times start hurting, not before.

## Decisions worth knowing

**Money is `Long` paisa, never `Double`.** `Money` is a value class, so there is no
allocation cost, but the type system stops anyone adding a `Double` price to a balance.
A khata that drifts by a paisa a day loses trust in a month.

**Sales snapshot their prices and costs.** `SaleItem` stores `unitPrice` *and*
`unitCost` at the time of sale. If you later change a product's cost, last month's
profit report does not silently change with it.

**A sale is one transaction.** `SaleDao.commitSale` inserts the sale, its items,
decrements stock, writes stock movements and updates the customer ledger inside a
single `@Transaction`. Half a sale is worse than no sale.

**`CheckoutUseCase` is the only place a `Sale` is constructed.** Stock checks, the
udhaar-needs-a-customer rule and the "change is not revenue" rule all live there,
so no screen can bypass them.

**Stock changes are append-only.** Every movement writes a `StockMovementEntity` with
the resulting quantity. When the shopkeeper says "yeh count ghalat hai", there is a
trail.

**Reports aggregate in SQL.** Summing thousands of sale rows in Kotlin would work
today and stop working in a year. Day bucketing needs the local UTC offset, which is
passed into the query rather than assumed.

**Camera scanning is the only expect/actual UI.** Android uses CameraX + ML Kit, iOS
uses `AVCaptureMetadataOutput`, desktop falls back to a text field because USB barcode
scanners type like a keyboard. Everything above the scanner — lookup, cart, pricing —
is shared.

**Repositories return `Flow`.** Room emits on write, so the dashboard, the POS list and
the khata total all update themselves after a sale with no manual refresh.

## Design system

Not Material defaults. The palette comes from the shop: signboard green, brass, receipt
paper, and a red reserved strictly for money owed. Every number in the app —
price, quantity, balance — renders in a monospaced face so figures line up in columns
and a khata can be scanned down the page without being read.

Tap targets are 48dp minimum. The POS keeps a fixed "counter strip" at the bottom with
the total and a single action, because that is the only thing that matters mid-sale.

## Correctness fixes worth calling out

**Invoice numbers come from a counter table, not `COUNT(*) + 1`.** The old scheme
repeated a number after any deletion and could collide between two concurrent
sales. `counters` is bumped with an SQL upsert inside the same transaction as the
sale.

**Stock is re-checked inside the transaction.** The check on the POS screen is a
courtesy for a fast, friendly error. Between reading stock and committing, another
till can sell the last bag — so `commitSale` reads stock again and throws
`StockConflictException`, rolling the whole sale back.

**No destructive migration fallback.** `DUKAN_MIGRATIONS` is wired up and empty.
On a shop's only copy of its khata, wiping the database because a schema changed is
not an acceptable failure mode; a missing migration should crash loudly in testing.

**Search hits one indexed column.** Every product row stores a lower-cased
`searchText` ("name|urdu name|barcode") written on upsert. Queries prefix-match that
one column and rank prefix hits first, instead of running three infix `LIKE`s per
keystroke.

**Returns are netted off reports.** "Aaj kitna hua" means after returns, so
`SalesSummary` subtracts returned amount and returned profit rather than reporting
gross and hiding the rest.

**Strings are data, not literals.** `Strings` is a data class whose English values
are the defaults and whose Urdu instance overrides by name — a forgotten key falls
back to English instead of crashing, and adding a string is one line. Errors travel
out of ViewModels as `AppError`, and only the screen (which knows the language)
turns them into sentences.

## Backup

The app is offline-only, which means a lost phone is a lost khata unless the
shopkeeper exports. So backup is the **first** section of Settings, not the last.

- **Full backup** is a JSON snapshot of every table, restored inside one
  transaction — a half-applied restore is worse than none.
- **CSV export** writes three separate sheets (products, sales, khata) because
  that is what an accountant actually opens, with a UTF-8 BOM so Excel on Windows
  renders Urdu instead of mojibake.
- File save/open is `expect/actual`: Android uses the Storage Access Framework via
  a two-lambda bridge that MainActivity fills in, desktop uses a Swing chooser, iOS
  uses the share sheet and document picker.

## Testing

- `MoneyTest` — no float drift over 300 additions, formatting, lenient parsing
- `CartTest` — quantity merging, discount floor, profit maths, stock flags
- `CheckoutUseCaseTest` — every checkout rule against fake repositories

Add DAO tests with an in-memory Room database next; those are the highest-value tests
still missing.
