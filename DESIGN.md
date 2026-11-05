---
version: 1.0
name: MedCare-Enterprise-Design-System
description: A clinical-grade SaaS design system synthesizing Cal.com's high-contrast scheduling clarity, Stripe's tabular financial precision, and Linear's effortless navigation ergonomics. Built on a crisp cool-alabaster canvas (#f6f9fc) with pure white elevated surfaces (#ffffff), deep navy ink (#0d253d), high-affordance 40px+ navigation controls, nav-pill-group switchers, and a deep carbon footer (#101010).

colors:
  primary: "#0f172a"
  primary-active: "#1e293b"
  primary-disabled: "#e2e8f0"
  brand-accent: "#0284c7"
  brand-accent-deep: "#0369a1"
  brand-indigo: "#4f46e5"
  ink: "#0d253d"
  body: "#334155"
  muted: "#64748b"
  muted-soft: "#94a3b8"
  hairline: "#e2e8f0"
  hairline-strong: "#cbd5e1"
  canvas: "#f6f9fc"
  surface: "#ffffff"
  surface-soft: "#f8fafc"
  surface-card: "#f1f5f9"
  surface-dark: "#101010"
  surface-dark-elevated: "#18181b"
  on-primary: "#ffffff"
  on-dark: "#ffffff"
  on-dark-soft: "#a1a1aa"
  success: "#059669"
  warning: "#d97706"
  error: "#e11d48"

typography:
  display-lg:
    fontFamily: "'Plus Jakarta Sans', 'Inter', -apple-system, BlinkMacSystemFont, sans-serif"
    fontSize: 32px
    fontWeight: 700
    lineHeight: 1.15
    letterSpacing: -0.8px
  display-md:
    fontFamily: "'Plus Jakarta Sans', 'Inter', -apple-system, BlinkMacSystemFont, sans-serif"
    fontSize: 24px
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: -0.5px
  title-lg:
    fontFamily: "'Plus Jakarta Sans', 'Inter', sans-serif"
    fontSize: 18px
    fontWeight: 600
    lineHeight: 1.35
    letterSpacing: -0.25px
  title-md:
    fontFamily: "'Inter', sans-serif"
    fontSize: 15px
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: -0.1px
  body-md:
    fontFamily: "'Inter', sans-serif"
    fontSize: 14px
    fontWeight: 400
    lineHeight: 1.5
    letterSpacing: 0
  body-tabular:
    fontFamily: "'Inter', sans-serif"
    fontSize: 13px
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: -0.1px
    fontFeature: tnum
  button-md:
    fontFamily: "'Inter', sans-serif"
    fontSize: 13px
    fontWeight: 600
    lineHeight: 1.0
    letterSpacing: 0
  code:
    fontFamily: "'JetBrains Mono', ui-monospace, monospace"
    fontSize: 12px
    fontWeight: 600
    lineHeight: 1.4
    letterSpacing: 0

rounded:
  xs: 4px
  sm: 6px
  md: 8px
  lg: 12px
  xl: 16px
  pill: 9999px
  full: 9999px

spacing:
  xxs: 4px
  xs: 8px
  sm: 12px
  md: 16px
  lg: 24px
  xl: 32px
  xxl: 48px

components:
  button-back:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    typography: "{typography.button-md}"
    rounded: "{rounded.lg}"
    padding: 10px 16px
    minHeight: 40px
  button-next:
    backgroundColor: "{colors.brand-accent}"
    textColor: "{colors.on-primary}"
    typography: "{typography.button-md}"
    rounded: "{rounded.lg}"
    padding: 10px 18px
    minHeight: 40px
  nav-pill-group:
    backgroundColor: "{colors.surface-card}"
    textColor: "{colors.ink}"
    rounded: "{rounded.pill}"
    padding: 5px
  workflow-stepper-bar:
    backgroundColor: "{colors.surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.xl}"
    padding: 12px 16px
---

# MedCare Enterprise — DESIGN.md
*(Built on the [VoltAgent Awesome DESIGN.md](https://github.com/voltagent/awesome-design-md) & Google Stitch specification)*

## 1. Visual Theme & Atmosphere
MedCare Enterprise combines three benchmark design systems from `awesome-design-md`:
- **Cal.com**: Clean scheduling matrices, generous touch targets (`>= 40px`), `nav-pill-group` sub-navigation switchers, and a high-contrast dark footer (`#101010`).
- **Stripe**: Deep navy ink (`#0d253d`), cool-alabaster canvas (`#f6f9fc`), crisp hairline borders (`#e2e8f0`), and `tabular-nums` alignment for all shift counts, multipliers (`1.0x`, `1.5x`), and VND currency columns.
- **Linear**: Zero-friction navigation with persistent left-docked Back/Forward controls, full synchronization with browser native Back/Forward (`history.pushState` + `popstate`), and contextual Next-Step workflow bars.

## 2. Color Palette & Roles
- **Canvas (`#f6f9fc`)**: Default background surface that gives white cards immediate visual elevation without heavy drop-shadows.
- **Surface (`#ffffff`)**: Primary card, table, and modal surface.
- **Ink (`#0d253d`)**: High-contrast headings, staff names, and total remuneration figures.
- **Primary Action (`#0f172a` / `#0284c7`)**: Primary CTAs and Next-Step navigation buttons.
- **Semantic Accents**:
  - Emerald (`#059669`): Completed schedules, valid timesheets, paid slips.
  - Amber (`#d97706`): Incomplete shift slots, pending complaints, outside-registration dispatch warnings.
  - Rose (`#e11d48`): Missing card swipes, rejected complaints, destructive actions.

## 3. Typography Rules
- **Display & Headings**: `Plus Jakarta Sans` (600–700 weight, `-0.3px` to `-0.8px` tracking) for geometric SaaS authority.
- **UI & Body**: `Inter` (400–600 weight) for high legibility across dense clinical schedules.
- **Numeric & Code (`tnum`)**: `JetBrains Mono` and `font-variant-numeric: tabular-nums` on all IDs (`BS001`, `PL-2026-001`), timestamps (`07:00–11:30`), and monetary values (`8.250.000 đ`).

## 4. Component Stylings (Navigation & Ergonomics Focus)
- **Left-Docked Back Button (`.ds-btn-back`)**:
  - Placed prominently at the far-left of the sticky workspace bar AND at the top-left of every child screen header.
  - Minimum height `40px`, padding `10px 16px`, border `1.5px solid #cbd5e1`, background `#ffffff`, bold icon + label (`← Quay lại`).
  - Hover state shifts background to `#f1f5f9`, border to `#94a3b8`, and translates icon `-2px` left.
- **Forward / Next-Step Button (`.ds-btn-next`)**:
  - Placed on the right side of the sticky workspace bar and inside table rows.
  - Minimum height `40px` (or `36px` in compact table rows), high-contrast fill (`#0284c7` or `#0f172a`), white text, right arrow icon (`Tiếp theo →`).
- **Segmented Pill Navigation (`.ds-nav-pill-group`)**:
  - Pill-in-pill container (`border-radius: 9999px`, `background: #f1f5f9`, `padding: 4px`) holding role screen tabs with `36px+` tap height.

## 5. Layout Principles
- **Sticky Dual-Layer Chrome**: Top enterprise identity bar + sticky Secondary Workspace Navigation Bar containing the Left Back/Forward cluster, Breadcrumb, Segmented Role Tabs, and Next-Step CTA.
- **Bottom Workflow Pagination Bar**: Every multi-step screen includes a bottom navigation bar with large `← Quay lại bước trước` and `Chuyển sang bước tiếp theo →` buttons so users never have to scroll back to the top after inspecting a long table.

## 6. Depth & Elevation
- **Level 0 (Canvas)**: `#f6f9fc`
- **Level 1 (Cards & Tables)**: `#ffffff` with `1px solid #e2e8f0` and `0 1px 3px rgba(13, 37, 61, 0.04)`
- **Level 2 (Interactive Buttons & Sticky Bars)**: `0 2px 6px rgba(13, 37, 61, 0.07)`
- **Level 3 (Modals & Floating Docks)**: `0 20px 25px -5px rgba(13, 37, 61, 0.15)`

## 7. Do's and Don'ts
- **Do** synchronize every screen transition with `window.history.pushState` and handle `window.onpopstate` so the browser's native Back/Forward buttons work identically to in-app navigation buttons.
- **Do** make all Back, Next, and table action buttons at least `36px–42px` tall with explicit border and fill contrast.
- **Don't** use tiny text-only links (`11px` unboxed links) for primary screen-to-screen navigation.
- **Don't** hide the Back button on mobile or tablet viewports.

## 8. Responsive Behavior & Touch Targets
- All navigation buttons (`.ds-btn-back`, `.ds-btn-next`, `.ds-pill-tab`) maintain a `40px` minimum touch target height across desktop, tablet, and mobile.
- Keyboard shortcuts: `Alt + ←` (or Browser Back) triggers `goBackFromCurrentScreen()`, `Alt + →` (or Browser Forward) triggers `goForwardFromCurrentScreen()`.

## 9. Agent Prompt Guide
- Always reference `.ds-btn-back` for backward navigation, `.ds-btn-next` for forward progression, and `.ds-table-action` for row-level drill-down actions.

