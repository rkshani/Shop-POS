# DukanPOS

Offline-first POS, stock, purchase and udhaar-khata app for a Pakistani kiryana shop.
Kotlin Multiplatform + Compose Multiplatform: **one codebase → Android, iOS, Desktop.**

Everything runs locally. No account, no server, no internet needed to make a sale.

## What it does

| Screen | Purpose |
|---|---|
| **Dukan** (dashboard) | Today's net sale, 7-day trend, udhaar outstanding, stock value, low-stock count |
| **Sell** (POS) | Search or scan → cart → discount → cash / udhaar / Easypaisa → sale saved |
| **Stock** | Products with cost, sale price, unit, barcode, stock and low-stock alert level |
| **Purchase** | Maal aane ki entry: supplier, bill no, lines, optional cost-price update |
| **Sales** | Every invoice, with a per-item **return** flow (refund cash or off the khata) |
| **Khata** | Customers, running balances, receive payments, ledger, **WhatsApp reminder** |
| **Reports** | Today / 7-day / 30-day sale, profit, cash vs udhaar, returns, top sellers |
| **Settings** | **Backup & restore**, CSV export, shop details, currency, English/Urdu, theme |

Fully translated: English and Urdu (RTL), switchable at runtime.

## Running it

**Android** — open in Android Studio (Ladybug or newer), run `androidApp`.

**Desktop** — `./gradlew :desktopApp:run`

**Tests** — `./gradlew :shared:jvmTest`

**iOS** — see [`iosApp/README.md`](iosApp/README.md).

> The Gradle wrapper is not committed. Run `gradle wrapper --gradle-version 8.11.1`
> once, or let Android Studio generate it on first sync.

## Layout

```
shared/                 all logic and all UI
├── core/               Money, AppResult, i18n, design system, scanner + platform expect/actual
├── domain/             models, repository interfaces, use cases   (zero framework imports)
├── data/               Room entities, DAOs, mappers, repository implementations
├── di/                 Koin modules
└── presentation/       ViewModels + Compose screens, one package per feature
androidApp/             Activity + Application + SAF file bridge
desktopApp/             main() window
iosApp/                 SwiftUI shell
```

Dependencies point one way only: `presentation → domain ← data`. The domain layer
imports nothing from Room, Compose or Koin, which is why the checkout, return and
purchase rules are testable without a device.

See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for the decisions behind this.

## Honest status

This has **not been compiled yet** — it was written without access to the Gradle
plugin and Maven repositories. Expect to fix version resolution and a handful of
compile errors on first sync.

Still to do before a real launch:
- **Room DAO tests against an in-memory database.** The transactional paths
  (`commitSale`, `commitReturn`, `commitPurchase`) are the highest-risk code in the
  app and are currently only covered indirectly through fakes.
- Launcher icon assets (`@mipmap/ic_launcher`).
- Thermal-printer receipts (ESC/POS over Bluetooth) — the obvious next feature.
- Scheduled/automatic backup reminders. Right now backup is manual and one tap;
  it should nag.
- Product search uses an indexed normalised column, not FTS. Fine into the low
  thousands of items; move to FTS4 if a shop outgrows it.
