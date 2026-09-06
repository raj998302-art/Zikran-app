# ZIKRAN — Daily Islamic Remembrance 🌙

<div align="center">
  <img src="icon.png" width="120" alt="ZIKRAN icon"/>
</div>

> **ZIKRAN v5.0** — a complete offline-first Islamic azkar companion: daily adhkar with authentic references, an intelligent tasbeeh counter, offline prayer times, reliable salat alarms that work even when the app is closed, Qibla compass, duas, hadith, and a reward (hasanat) tracker that keeps you consistent.

![Build APK](https://github.com/raj998302-art/Zikran-app/actions/workflows/build.yml/badge.svg)

---

## ✨ What's New in v5.0 (Founder Edition)

### Fixed (all v4 bugs)
| Bug | Fix |
|---|---|
| App icon was an empty rectangle | Real adaptive icon — golden crescent + tasbih, all densities + Android 13 themed icon |
| Emojis used everywhere as icons | 45+ crisp gold SVG icons — consistent on every device |
| Location unreliable in WebView | Native Android location (GPS + Network) via JS bridge + manual city picker with 60+ cities + custom lat/lng |
| Prayer times needed internet | **100% offline** astronomical engine (verified against Aladhan API, ±2 min) |
| Alarms died when app closed | Native AlarmManager exact alarms + auto re-schedule + survive reboot |
| Reset button did nothing | `confirm()` never worked in WebView — replaced with custom modal |
| Streak never progressed | Daily scope fixed to the 73 daily azkar (weekly/situational tracked separately) |
| Keep Screen On toggle dead | Now wired to native window flag |
| Broken vibration bridge | Fixed native pattern vibration |
| Build could fail randomly | Proper Gradle 8.9 wrapper + AGP 8.7.2 + stable signing keystore |

### New
- 🏠 **Home Dashboard** — greeting, next-prayer card with live countdown, daily progress chips, 7-day activity dots
- 📿 **Sawab (Hasanat) Tracker** — estimated rewards for every dhikr (based on sahih hadith weights), lifetime counter
- 🔥 **Streaks & Badges** — daily completion streak with best-streak record and milestone celebrations (3/7/21/40/100 days)
- 🌙 **Ramadan & Fasting section** — niyyat, suhoor blessing, iftar dua, 3 ashra duas, Laylatul Qadr
- 🧭 **Better Qibla compass** — live sensor fusion with graceful fallback to bearing-only mode
- ⚙️ **Prayer settings** — 5 calculation methods (Karachi default, MWL, ISNA, Egypt, Umm al-Qura), Hanafi Asr option, Hijri date offset adjust
- 🌍 **60+ city picker** (India focus + global) for when GPS is off

## 📥 Download & Install

1. Go to the **[Actions](../../actions)** tab → latest **Build ZIKRAN APK** run
2. Download the **ZIKRAN-v5.0-APK** artifact
3. On your phone: allow "Install from unknown sources" and install
4. Every future update installs over the old one — **no uninstall needed** (stable signing key)

## 🛠 Building Yourself

**GitHub Actions (automatic):** every push builds a signed APK — no setup needed.

**Termux:**
```bash
pkg install openjdk-17 gradle
cd Zikran-app
gradle assembleRelease --no-daemon
# APK: app/build/outputs/apk/release/app-release.apk
```

**PC:** Android Studio → Open → Run, or `./gradlew assembleRelease`

## 🔑 Signing

`keystore/zikran-release.jks` ( passwords in `keystore/keystore.properties` ) — keep both files safe; they keep your app signature stable so updates install cleanly. For a personal/sideloaded app this is intentional and convenient.

## 📱 Tech

- Android WebView shell (minSdk 21 → Android 5.0+, targetSdk 35)
- Single-file offline-first app (`assets/index.html`) with native JS bridge
- Native: LocationManager, AlarmManager exact alarms, boot receiver, notification channel with alarm sound, pattern vibration
- Prayer engine: solar-position astronomy (verified vs Aladhan API across 10 global cities)

## 🤲 Dua

May Allah accept this effort, make it a source of **sadaqah jariyah** for everyone involved, and grant every user consistency in dhikr. آمین

---
v5.0 — rebuilt with care by the ZIKRAN team.
