# iOS build troubleshooting

## 1. The key does **not** fix “won’t build”

Putting the Supabase key in `AppConfig.swift` only affects **runtime** (talking to the server).  
If Xcode **fails to compile**, the cause is almost always something else (packages, signing, a red error line in code).

**Do this:** In Xcode, open the **Report navigator** (⌘9) → select the latest **Build** → scroll to the **first red error** → read that line. That message is what we need.

---

## 2. Use the **same** key as Android (`eyJ…`)

- Open **`local.properties`** in the **Android project root** (same folder as `build.gradle.kts`).
- Find **`SUPABASE_ANON_KEY=`** and copy **everything after the `=`** (one long line, starts with **`eyJ`**).
- Paste it into **`AppConfig.swift`** → `supabaseAnonKey = "…"`.

If you used a **publishable** key (`sb_publishable_…`) from the dashboard, it may not match what your Android app uses. For parity, prefer the **same JWT** as `local.properties`.

---

## 3. Common fixes (try in order)

1. **Resolve Swift packages**  
   **File → Packages → Resolve Package Versions**  
   (If that fails: **File → Packages → Reset Package Caches**, then resolve again.)

2. **Clean build**  
   **Product → Clean Build Folder** (⇧⌘K), then **Product → Build** (⌘B).

3. **Signing**  
   Select the project → **Signing & Capabilities** → enable **Automatically manage signing** and pick your **Team**.

4. **Command Line Tools**  
   If you use Terminal `xcodebuild`, run:  
   `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`  
   (Point at your real **Xcode.app**, not “Command Line Tools”.)

---

## 4. Paste the error here

Copy the **first** compiler error (and 2–3 lines around it) or attach a screenshot of the Xcode issue. Examples:

- *Unable to find module dependency: 'Supabase'*
- *Cannot find 'SupabaseClient' in scope*
- *Signing for "Club360fit" requires a development team*

Each has a different fix.
