# 🚀 Ever Dialer - Release v6.0.0

## 🐛 Bug Fix: Accidental Contact Open on Swipe
* **Swipe Navigation Fix:** Increased the swipe detection threshold from 450px to 700px and the minimum gesture time from 80ms to 150ms across all home screens (Favorites, Recents, Contacts, Notes).
* **Stricter Directionality:** Raised the horizontal-to-vertical ratio requirement from 4.5× to 5.5× so that only deliberate horizontal swipes trigger tab navigation — tapping or vertically scrolling contacts no longer accidentally opens random contacts.
* **Notes Screen:** Also added the missing elapsed-time guard to the Notes screen swipe handler, bringing it in line with all other screens.

## 🔢 Version Bump
* App version updated to v6.0.0.

---

# 🚀 Ever Dialer - Release v3.0.0

## 🎨 Pill Style Navigation Bar
* **Floating Pill Nav:** Added a new pill-style floating navigation bar as the default navigation experience.
* **Settings Toggle:** Added "Pill Style Navigation" toggle under Settings > User Interface > UI Elements to switch between the pill bar and the classic bottom navigation.
* The pill bar dynamically expands the active tab label and highlights with the primary container color.

## 👋 First Launch Welcome Dialog
* Added a welcome popup shown on first install explaining how to allow restricted settings on Android 14+ for setting Ever Dialer as the default dialer.
* Includes pill-style "App Info" and "Continue" action buttons.

## 🔢 Version Bump
* App version updated to v3.0.0.

---

# 🚀 Pdialer Optimized - Release v1.0.3

## 💎 Material 3 Expressive UI (Android 16 Style)
* **New Containment Logic:** Redesigned the Settings screen with a "Grouped Card" layout, providing better visual separation and a premium feel.
* **Expressive Shapes:** Increased corner radii and adjusted padding to match the latest Material Design 3 "Expressive" guidelines.
* **Enhanced Visual Hierarchy:** High-contrast headers and refined supporting text for easier navigation.

## 📞 Call Experience & Interaction
* **Haptic Feedback Toggle:** Added a new system setting to enable or disable tactile vibration for dialer interactions and gestures.


## 🛠️ Bug Fixes & System Stability
* **Fixed About Page Crash:** Resolved a navigation conflict by refactoring `AboutAppScreen` and cleaning up generated destination routes.


---
### 👨‍💻 Contributor
* **Lead Developer:** Hama (@MoHamed-B-M)
* **Status:** beta Build
