## Key Updates & Modernization

### 🏗️ Architectural Migration
- **Applet to Swing:** Completely removed `java.applet.Applet` and `AudioClip`. Replaced with `javax.swing.JFrame` and `javax.sound.sampled.Clip`.
- **Thread Safety:** Implemented `SwingUtilities.invokeLater` for Event Dispatch Thread (EDT) compliance, ensuring stable UI updates.
- **Robustness:** Fixed potential crashes in `RankingData.java` when high-score lists are empty.

### 🎨 Visual & UI Overhaul
- **Neon-Dark Theme:** Replaced legacy background images with a pure Java2D-rendered "Midnight Navy & Neon Cyan" design.
- **Modern UI Elements:**
    - Implemented **Glow Effects** (8px semi-transparent layers) for a tech-inspired aesthetic.
    - Added **Rounded Card UI** for side panels and **Dot-Grid backgrounds**.
    - Pure code-based rendering (eliminating dependency on `playing.jpg` and `standby.jpg`).

### ⚙️ Development Improvements
- **JDK 21 Verified:** Fully tested and compiled using JDK 21.
- **Improved UX:** Replaced basic help text with modern `JDialog` components.