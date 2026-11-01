# SmartBoard Pro — On-Canvas Math Nodes, Drawing Engine Audit (Zoom/Pan & Custom Settings) & Real-Time Online Classroom Plan

This plan covers three major upgrades to **SmartBoard Pro**:
1. **On-Canvas Interactive Compose Math Overlay** supporting both live editable Compose math nodes and one-tap flattening onto the canvas.
2. **10-Point Drawing Engine Technical Audit, Canvas Zoom & Pan (`0.5x–4.0x`), and User-Customizable Toolbar Settings**.
3. **Real-Time Online Classroom System** powered by **Firebase Authentication (Google Sign-In)** and **Cloud Firestore** working seamlessly alongside the offline Room whiteboard.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> Your confirmed preferences from Phase 1 govern the implementation:

- **Confirmed Decision 1 — Dual-Mode On-Canvas Math Overlay ("দুটোই চাই")**:
  - Math formulas (fractions $\frac{a}{b}$, square/cube roots $\sqrt{x}$, integrals $\int_a^b$, summations $\sum$, powers $x^n$, subscripts $x_n$, and equations) render as **interactive Jetpack Compose overlay nodes** directly on the canvas with drag positioning, pinch/button scaling, inline tap-to-edit, AND a one-tap **"Stamp to Canvas"** action that bakes the formula into the underlying native canvas vector layer.
- **Confirmed Decision 2 — Dual Zoom & Pan + Customizable Tool Settings ("দুটোই চাই তবে সেটিংস থেকে আমার মন মতো টুলস চালু করার ব্যবস্থা")**:
  - Supports **both** 2-finger pinch-to-zoom/pan during drawing **and** a dedicated **Hand Pan / Zoom tool**, plus a floating `100% Reset` zoom indicator pill.
  - Adds a **Toolbar & Gesture Settings Dialog** (`Settings` gear in the top bar) where you can customize which tools appear on your bottom dock (Brushes, Eraser, Shapes, Geometry, Math, Insert, Backgrounds, Hand Pan, Quick Strip) and toggle 2-finger pinch-to-zoom on or off at any time, saved locally via DataStore Preferences.
- **Confirmed Decision 3 — Hybrid Offline + Real-Time Online Classroom ("দুটোই চাই")**:
  - Works 100% offline out-of-the-box with local Room auto-save, while also providing a **Live Classroom** button in the top bar (and optional sign-in gate) using **Firebase Auth (Google Sign-In via Credential Manager)** and **Cloud Firestore** for real-time Teacher/Student whiteboard synchronization.

---

## 1. On-Canvas Interactive Compose Math Overlay

- **Custom Compose Mathematical Nodes (`OnCanvasMathOverlay.kt`)**:
  - Renders true typographic Compose layout trees above the canvas transformed by the current canvas zoom/pan matrix:
    - **Fraction Node**: Vertically stacked numerator `Text`, horizontal `HorizontalDivider` vinculum bar sized dynamically to the wider expression, and denominator `Text`.
    - **Radical Node ($\sqrt{x}$ / $\sqrt[3]{x}$)**: Custom Compose `Canvas` radical checkmark + overbar wrapping the radicand composable.
    - **Integral ($\int_a^b$) & Summation ($\sum_{i=1}^n$) Nodes**: Oversized serif operator symbol flanked by upper/lower limit nodes and integrand expression.
    - **Power ($x^n$) & Subscript ($x_n$) Nodes**: Baseline-aligned base text with offset superscript/subscript nodes.
  - **Interactive Floating Controls on Selected Math Node**:
    - Drag handle to reposition anywhere on the board.
    - **Inline Edit (`Edit`)**: Re-opens the formula fields in-place to modify numerator, denominator, bounds, or symbols live.
    - **Stamp / Flatten (`DoneAll`)**: Converts the live Compose node into a permanent native canvas vector object so it becomes part of the baked page drawing.

---

## 2. Complete Technical Audit of the Drawing Engine (10-Point Verification & Fixes)

| # | Audit Area | Technical Finding & Engineered Enhancement |
|---|---|---|
| 1 | **Smooth Finger & Stylus Handwriting** | Replace raw point segments with velocity-aware midpoint quadratic Bezier curves (`quadTo`) and historical motion event batching (`change.historical`) so fast curves never polygonize. |
| 2 | **Fast & Slow Drawing** | Reduce minimum point distance threshold to `1.5f` in world coordinates while coalescing micro-jitter during slow precision writing. |
| 3 | **Continuous Multi-Hundred-Point Strokes** | Maintain a pre-built `android.graphics.Path` during active pointer drag instead of rebuilding the path from list allocations on every frame, keeping 1,000+ point continuous strokes locked at 60 FPS on budget phones. |
| 4 | **Pen Colors (24 Swatches + HSV)** | Verify ARGB bit-masking `(colorInt.toLong() and 0xFFFFFFFFL)` across all 6 brush engines and math/text objects. |
| 5 | **Stroke Widths (`2px–56px`)** | Scale stroke width accurately with canvas zoom and ensure dot-taps (`pts.size == 1`) render a crisp filled circle of radius `strokeWidth / 2f`. |
| 6 | **Eraser Accuracy (Stroke & Pixel)** | Transform touch coordinates through the inverse canvas Zoom/Pan matrix `(screenPt - panOffset) / zoomScale` so both **Stroke Eraser** segment-distance hit testing (point-to-segment projection, not just endpoints) and **Pixel Eraser** (`PorterDuff.Mode.CLEAR` on offscreen layer) are 100% accurate at any zoom level. |
| 7 | **Undo & Redo** | Deep-copy stroke and object lists on each snapshot (`MAX_UNDO = 40`) so mutating or moving an object never corrupts prior undo states. |
| 8 | **Canvas Zoom (`0.5x–4.0x`) & Pan** | Add `zoomScale` and `panOffsetX/Y` state to the canvas engine. Support 2-finger pinch-to-zoom & pan (when enabled in Settings) + dedicated `HAND_PAN` tool + a floating `100%` zoom pill that resets to `1.0x` on tap. |
| 9 | **Drawing Persistence After Restart** | Persist active project ID and custom tool preferences in local storage + Room `WhiteboardProjectEntity` so reopening the app restores the exact active board, page, strokes, and math nodes. |
| 10 | **Portrait & Landscape Rotation** | Preserve all state in `WhiteboardViewModel` across orientation changes and normalize canvas coordinates so drawings never clip when rotating between portrait and landscape. |

---

## 3. Real-Time Online Classroom Architecture (Firebase vs. Custom Backend Analysis)

### Architectural Analysis: Firebase Capabilities vs. Separate Backend

```
┌───────────────────────────────────────────────────────────────────────────────┐
│                SMARTBOARD PRO — HYBRID CLASSROOM ARCHITECTURE                 │
│                                                                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐  │
│  │ TEACHER / STUDENT ANDROID CLIENT (Jetpack Compose + Native Canvas)      │  │
│  │  • Local Room DB (Offline Boards)  • Live Classroom Engine (Online)     │  │
│  └──────────────────┬───────────────────────────────────┬──────────────────┘  │
│                     │ Google Sign-In (CredentialMgr)    │ Real-Time Snapshots │
│                     ▼                                   ▼                     │
│  ┌──────────────────────────────────┐ ┌────────────────────────────────────┐  │
│  │ FIREBASE AUTHENTICATION          │ │ CLOUD FIRESTORE (Enterprise DB)    │  │
│  │ • Verified Teacher & Student UID │ │ • /classrooms/{roomCode}           │  │
│  │ • Zero-trust identity token      │ │   - teacherUid, title, activePage  │  │
│  │                                  │ │   - allowStudentDrawing, status    │  │
│  │                                  │ │   - pagesJson (live synced board)  │  │
│  │                                  │ │ • /classrooms/{roomCode}/members/  │  │
│  │                                  │ │   - {uid}: name, role, canDraw     │  │
│  └──────────────────────────────────┘ └────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────────────────────────┘
```

1. **Features Powered Directly by Firebase (Implemented in SmartBoard Pro)**:
   - **Secure Classroom Access & Identity**: **Firebase Authentication** with Google Sign-In (`CredentialManager` + `GetSignInWithGoogleOption`) provides verified `uid`, `displayName`, and email for Teachers and Students without custom auth servers.
   - **Classroom Creation & Unique 6-Character Classroom ID**: Teacher creates a document in `/classrooms/{roomCode}` (e.g., `SB-4829`) containing `teacherUid`, `title`, `allowStudentDrawing`, `currentPageIndex`, and serialized `pagesJson`.
   - **Real-Time Stroke & Board Synchronization**: Students and Teachers subscribe to `/classrooms/{roomCode}` via Firestore's first-party `.snapshots()` Kotlin `Flow`. Whenever the Teacher (or an authorized Student) finishes a stroke, erases, adds a math formula, or changes pages, the updated board state is pushed to Firestore and rendered on all connected student screens in real time (`~100–250ms`).
   - **Teacher Permission Controls & Student View-Only Mode**:
     - Teacher can toggle **Class-Wide View-Only Mode** (`allowStudentDrawing = false`) or grant/revoke **Individual Student Drawing Permission** (`canDraw = true/false`) in `/classrooms/{roomCode}/members/{studentUid}`.
     - Enforced both in the Android UI (locking the student's canvas into View/Zoom-Only mode when `canDraw == false`) **and** in `firestore.rules` on the backend so unauthorized students cannot write strokes.
   - **Participant Roster & Connection Status**: Live `.snapshots()` listener on `/classrooms/{roomCode}/members` plus Firestore `hasPendingWrites` / snapshot metadata displays real-time `LIVE • SYNCED` vs `OFFLINE / SYNCING` connection badges and the live participant list.

2. **Features That Would Require a Separate Dedicated Backend (Outside Firestore)**:
   - **Live Two-Way Voice/Video Streaming or Screen Broadcast**: Requires a WebRTC SFU media server (e.g., LiveKit, Agora, or custom WebRTC TURN/STUN servers).
   - **Sub-16ms Ephemeral Laser Pointer Cursor Streaming at 120Hz for 500+ Concurrent Viewers**: While Firestore handles stroke completion and collaborative board state for classroom groups smoothly, ultra-high-frequency ephemeral cursor telemetry at scale would use a dedicated WebSocket/UDP relay.

### Security Rules & Verification Strategy (`firestore.rules`)

- **Zero-Trust Rules**:
  - Only authenticated Google users (`request.auth != null`) can read or join a classroom.
  - Only the classroom's `teacherUid` can change `allowStudentDrawing`, remove participants, or delete the classroom.
  - Board updates (`pagesJson`) are permitted only if `request.auth.uid == resource.data.teacherUid` OR `resource.data.allowStudentDrawing == true` (or the student's member doc has `canDraw == true`).
- **Two-Tier Local Emulator Verification**:
  - Validate `firestore.rules` with `firestore.test.js` (`node --test`) and `ClassroomRepositoryRuleTest.kt` (Robolectric against local Firestore/Auth emulators) before deploying rules via `DeployRules`.
