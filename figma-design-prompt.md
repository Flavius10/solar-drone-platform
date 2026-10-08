# Prompt pentru Figma (First Draft / Figma Make)

Copiază tot ce e mai jos și lipește-l în promptul de generare design din Figma.

---

Design a web application called **Solar Drone Platform** — an enterprise tool for managing drone-based inspections of solar panel farms. It's used by farm operations teams to monitor panel health, review AI-flagged defects, assign repair work, and manage multiple solar farm sites from one place.

**Visual direction:** Enterprise/industrial, serious and data-dense — think Datadog, Grafana, or an industrial SCADA dashboard, not a consumer app. Dark navy/blue header and primary UI chrome, neutral light gray content background, high information density (tables, grids, stat cards), clear status-color coding used consistently everywhere:
- Green = healthy / OK / complete
- Red = defect / critical / failed
- Amber/orange = in progress / processing / warning
- Gray = no data / inactive

Typography: clean sans-serif (Inter or similar), fairly small/compact text sizes appropriate for dense data tables, clear visual hierarchy between headers, labels, and data values.

**Layout pattern (applies to every screen except login):**
- Fixed dark-navy top header bar containing: app logo/name on the left, a farm-switcher dropdown (shows which solar farm site is currently selected), a notification bell icon with unread-count badge, current user's name + role, and a logout link — all on the right side.
- Left sidebar with vertical navigation icons + labels (persistent across all pages), collapsible.
- Main content area to the right of the sidebar, below the header, with generous internal padding.

**User roles that affect what's visible:** ADMIN (sees everything, manages users/farms), TECHNICIAN (handles work orders/repairs), OPERATOR (uploads inspections), VIEWER (read-only, no upload/edit actions). Design should reflect that some nav items and action buttons are role-gated (e.g. only admins see "User Management" and "Farm Management" in the sidebar).

## Screens to design

**1. Login page** — standalone, no sidebar/header. Centered card on a full-bleed dark navy or subtle gradient background, app logo, username + password fields, "Forgot password?" link, primary login button.

**2. Dashboard** — landing page after login. Row of KPI stat cards at top (Total Inspections, Defects Found, Healthy Panels, Total Estimated Repair Cost), each with a large number, label, and small icon. Below that, a "Download PDF Report" button. Leave room for a summary chart or panel status breakdown table.

**3. Farm Map** — the core screen. A grid/lattice visualization representing physical rows and columns of solar panels, where each panel is a colored square/tile (color = health status per the status-color system above). Clicking a panel opens a detail side panel showing: panel ID, last inspection date, status, defect type, confidence %, estimated repair cost, and action links ("Open repair ticket", "View degradation history"). Above the grid: controls to add a new panel (row/column/label inputs) and bulk-import panels via CSV upload. Include a "no farm selected" empty state.

**4. New Inspection (photo upload)** — a form card: dropdown to pick a panel (row/column), toggle/select for inspection type (RGB visual vs. Thermal, with thermal marked as a premium/locked feature for lower subscription tiers), a file picker for the drone photo, and a "Scan for Defects" primary button. After submit, show a non-blocking success state indicating the analysis is running in the background (async — result appears later on the map/reports, not instantly).

**5. Video Inspection** — similar upload form (pick farm, inspection type, video file) but the result is a table below: one row per uploaded video "batch" showing farm, type, a status pill (PENDING/EXTRACTING/PROCESSING/COMPLETE/FAILED, color-coded), a live progress indicator ("6/14 frames — 43%") with a small progress bar, who uploaded it. Completed rows expand (accordion) into a nested sub-table listing each extracted frame: frame #, timestamp, which panel it was matched to, defect status, confidence, estimated cost.

**6. Reports History** — a dense, sortable/filterable data table of all inspections: date, panel, inspection type, status badge, defect type, confidence %, estimated cost, and an "Export to Excel" action.

**7. Work Orders** — ticket-style list/table: ID, farm, panel, title, status badge (OPEN/ASSIGNED/IN_PROGRESS/RESOLVED/CLOSED as a colored pill or stepper), created by, assigned to, with inline controls to assign a technician (username input + button) and change status (dropdown). Above the table, a "New work order" card with a panel picker and title/description fields.

**8. Panel Degradation History** — a single-panel deep-dive page with a custom line/area chart showing status over time (color-coded points per inspection) and a second chart showing cumulative estimated repair cost climbing over time.

**9. Farm Management** (admin only) — list of farm sites with name/location, a "create new farm" card, and per-farm user assignment (list of assigned users + add/remove controls).

**10. User Management** (admin only) — table of all users (username, role, subscription tier, email) plus a "create user" form (username, password, role dropdown, subscription tier).

**11. Audit Log** (admin only) — simple paginated table: timestamp, username, action, details — monospace-ish, log-style dense rows.

**12. Profile** — small card: view username/role, form to update email address.

**13. Notification system (component, appears on every page)** — a bell icon in the header with a red unread-count badge; clicking opens a dropdown panel listing recent notifications (unread ones highlighted/bold), each with a delete "×" and click-to-mark-read. Separately, a toast/snackbar component that slides in from a corner when a new critical-defect alert arrives in real time, auto-dismissing after a few seconds.

## Key reusable components to define in the design system

- Status badge/pill (OK, DEFECT, PENDING, PROCESSING, COMPLETE, FAILED, and work-order statuses) using the green/red/amber/gray palette
- Stat/KPI card
- Data table with sortable columns, row hover state, and empty state
- Progress bar with percentage label
- Form card (title + fields + footer action button)
- Dropdown/select farm-switcher for the header
- Sidebar nav item (default, hover, active states)
- Notification bell + dropdown panel
- Toast/snackbar alert
- Primary/secondary/outline buttons, in default/hover/disabled states
- Panel-grid tile (the colored square used on the Farm Map)

Please produce high-fidelity desktop screens (1440px width) for at minimum: Login, Dashboard, Farm Map, Video Inspection, and Work Orders, plus a small component library page showing the status badges, buttons, and cards defined above.
