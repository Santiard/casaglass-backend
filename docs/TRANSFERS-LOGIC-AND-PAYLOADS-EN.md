# Transfers (Traslados) — Business context & payload guide (for humans + FE agents)

This document gives **context** and **exact backend behaviour** so the frontend (and any coding agent) can build payloads and UIs without guessing. It is aligned with `TrasladoService.aplicarMovimientoLinea` / `revertirMovimientoLinea` in the Casaglass backend.

---

## 1. Sede IDs (single source of truth in code)

| ID | Name (typical) | Role in this doc |
|----|------------------|------------------|
| **1** | Insula / “Sede 1” | Cutting branch; special rules when this sede is **origin** or **destination** for **cut** lines. |
| **2** | Centro | Uses **real** cut inventory + normal product inventory. |
| **3** | Patios | Same as 2. |

All logic below uses these numeric IDs. There is no separate “Haiti” id in this service; **business language may say “Sede 1 / Insula”** — that is **id = 1**.

---

## 2. Two families of transfer (high level)

### Family A — **Only sedes 2 and 3** (Centro ↔ Patios, never 1)

- Both sedes run **day-to-day operations with “real” cut stock**: each **Corte** (cut SKU) has rows in `inventario_cortes` per sede and quantity in that **city/branch** (e.g. qty 1 here, qty 5 there).
- For a **cut line**, the backend behaves like a **normal internal move of that Corte**:
  - **Subtract** from `inventario_cortes` at **origin** (2 or 3).
  - **Add** to `inventario_cortes` at **destination** (3 or 2).
- For a **non-cut** (normal `Producto`), the backend uses **normal** inventory (`inventario` / product table per sede): subtract at origin, add at destination.

This family is **symmetric** and does **not** use `productoInventarioADescontarSede1` (that field is **only** for Insula 1 as origin in 1→2/3; see Family B).

### Family B — **Sede 1 (Insula) is in the transfer** (as origin and/or destination)

- Operationally, **Insula does not work like 2/3** for *every* cut: the company has so many ad-hoc cuts that **tracking every cut in Insula’s cut-inventory the same way as in 2/3** is not how the business runs day to day.  
  The **analogy with sales** (from your process): you may move material from a “full bar” (e.g. 600 cm) and **discount that full profile** in normal (whole-product) stock, or you may treat a line as “not taking a whole bar off stock” and **not** apply that full-bar discount — depending on the scenario.
- The backend encodes the **“discount the whole product at 1”** part with an **optional** field on the line:  
  `productoInventarioADescontarSede1` (whole product, **not** a `Corte` id) when the line itself is a **Corte** and the move is **1 → 2** or **1 → 3** (see validation in code).

**Critical behaviour for cut lines, origin 1 → dest 2 or 3:**

- The system **does not** reduce Insula’s **cut** stock (`inventario_cortes` at sede 1) for that line.  
- It **always** **adds** the cut to the **destination** (2 or 3) in `inventario_cortes` (so the other branch “receives” a real cut unit in **their** cut inventory where it *is* used in real life).
- If `productoInventarioADescontarSede1` is provided: the backend **deducts** the **normal** (whole) product in **Insula (1)** by the line **quantity** — that models “we took it from a full bar / material we do track in normal stock at 1”.
- If that field is **omitted** / null: there is **no** deduction of whole product at 1 for that line; the line still **adds** the cut in 2/3. That models “sending a cut to 2/3 without decrementing a tracked whole bar at 1” (business decision, UI should make this explicit).

**Cut lines, origin 2 or 3 → dest 1**

- The backend **removes** the cut from **origin** 2/3 in `inventario_cortes`.  
- It does **not** add that cut to Insula’s **cut** inventory (no `+` to `inventario_cortes` at 1) — the code path only subtracts at the branch that actually tracks that cut.  
  (So: “returning to 1” is not modeled as a positive cut-bucket at 1 in this service.)

Non-cut lines with sede 1 use **normal** product inventory in both directions (subtract origin, add dest) like any other `Producto`.

---

## 3. Matrix — what the backend does per route (Corte line = `line.product` is a `Corte` id in DB)

| Route | Cut line behaviour (inventory) | `productoInventarioADescontarSede1` (whole product at 1) |
|-------|----------------------------------|-----------------------------------------------------------|
| **2 ↔ 3** (only Centro/Patios) | **Cut stock:** `−` at origin, `+` at dest in `inventario_cortes` (by `Corte` id and quantity). | **Invalid** (400 if sent) — not applicable outside 1→2/3. |
| **1 → 2** or **1 → 3** (cut) | **No** `−` at sede 1 in `inventario_cortes`. **Yes** `+` at 2/3 in `inventario_cortes`. Optional: `−` **normal** product at 1 (whole product) if field set. | Optional; if set must be a **non-cut** `Producto` with enough **normal** stock at 1. |
| **2 → 1** or **3 → 1** (cut) | **Yes** `−` at origin 2/3 in `inventario_cortes`. **No** `+` at 1 in `inventario_cortes` for that line. | N/A (field only for 1→2/3). |
| **Any** (non-cut line) | Normal product: `−` origin, `+` dest in **normal** inventory (`inventario`). | Only validated / used in the 1→2/3 **cut** path in current rules. |

> **Re-read for FE:** For **1 → 2/3** with a **cut**, you must **not** expect Insula to lose a “cut” unit from `inventario_cortes`. You *do* expect **2/3** to **gain** that cut. What may decrease at 1 is **only** the optional **whole product** in **normal** stock, if you send `productoInventarioADescontarSede1`.

---

## 4. Payload building — what the UI must send (avoid 400/409)

### Every line (all routes)

- `producto: { "id": <id> }`  
  - For a **cut** line, this must be the **`Corte` row id** (same `id` in `productos` / `cortes` as used elsewhere: sales, `inventario_cortes`).  
  - Sending the **parent “whole” product id** (not a `Corte`) makes the line behave as a **non-cut** in inventory logic and will show the **wrong** name/behaviour.
- `cantidad` — quantity to move (same unit the rest of the app uses for that line; backend uses it in stock math).

### Only for **1 → 2** or **1 → 3** and **line is a cut**

Optionally (business choice):

- **`productoInventarioADescontarSede1: { "id": <id> }`**  
  - Meaning: at Insula, decrement **this whole product** (bar/profile, **not** a `Corte`) in **normal** inventory, by the same `cantidad` as the line.  
  - Backend validates: whole product exists, is **not** a cut id, and has enough **normal** stock at **1**; otherwise 400/409.
- In **batch** DTO, the same is the **flat** id: `productoInventarioADescontarSede1Id` on create/update; see the Spanish `API-TRASLADOS-FRONTEND.md` for exact JSON names.

If the user chooses **not** to decrement a full bar at 1: **omit** that field; only destination **cut** stock increases (and whole product at 1 is untouched).

### Never

- Do not send `productoInventarioADescontarSede1` for routes other than **1 → 2** or **1 → 3** (or for non-cut lines) — the API responds with **400** when rules are broken.

---

## 5. How this ties to “names” and mapping cuts

- A **Corte** is a **subclass of `Producto`**: one `id` in the DB, with `largoCm`, `nombre`, etc.
- **Display** on read usually comes from **`producto.nombre`** (or DTOs that map it). The backend can **align** the stored `Corte` name to `[base whole product] Corte de {medida} CMS` on **1 → 2/3** when `productoInventarioADescontarSede1` is present (see `TrasladoService.actualizarNombreCorteTrasladoInsulaADestino` and the same pattern in `OrdenService` when creating/naming cuts).
- The frontend should:
  1. Pick the **Corte** `id` from the same source as **inventario de cortes** (or the catalog the app already uses for that sede for cut list).
  2. When 1→2/3 and the user wants to **discount the full profile at 1**, select the **whole product** id that the business treats as the “bar” (same as sales logic when you decide 600 cm vs 300-from-400 — that product-level rule is product/business, not a second cut id).

**Orders vs transfers:** `OrdenItem` can store a separate **`nombre`** string on the line; `TrasladoDetalle` does **not** have that column — the canonical text for a cut is the **`Corte`’s** `nombre` in DB. So the FE must be consistent on **ids**; optional backend rename on transfer helps 2/3 see a proper label when the “whole” product is provided.

---

## 6. Related API reference (field names, GET shapes, errors)

See **`docs/API-TRASLADOS-FRONTEND.md`** (Spanish) for: exact JSON property names, `GET` response differences (`TrasladoResponseDTO` vs raw `Traslado` list), batch vs entity body, and **409** `INVENTARIO_INSUFICIENTE` body.

OpenAPI: controllers under `/api/traslados` and `/api/traslados-movimientos` are annotated for Swagger in the same codebase.

---

## 7. One diagram (Corte line, 1 → 2 or 3)

```mermaid
flowchart LR
  subgraph s1 [Sede 1 Insula]
    W[Normal inventory whole product - optional -]
    C1[Cut inventory at 1 for this line - NOT used in this path]
  end
  subgraph s2 [Sede 2 or 3]
    IC2[inventario_cortes + quantity]
  end
  W -->|if productoInventarioADescontarSede1 set| Wminus[-qty]
  C1 -.-x[no change]
  line[Corte id + qty] --> IC2
```

---

## 8. Code pointer (for audits / agents)

- **Core:** `com.casaglass.casaglass_backend.service.TrasladoService`  
  - `aplicarMovimientoLinea` / `revertirMovimientoLinea`  
  - `validarReglasProductoInventarioADescontarSede1`  
- **Cut stock:** `InventarioCorteService` (`incrementarStock` / `decrementarStock` per corte + sede).  
- **Naming helper (orders):** `OrdenService` (search for `Corte de` and `largoCm` in cut creation).

*Last updated to match backend traslado logic and explicit Insula 1 / Centro 2 / Patios 3 rules.*
