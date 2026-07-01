# BeeTalk Project — AGENTS.md

## Project Structure

```
F:\RexT Studio\BeeTalk\BeeTalk Rebornv1\
├── BeetalkRebornFix\       ← Android App (Java, Retrofit, Room)
└── beetalk-panel\          ← Backend API + Admin Panel (CodeIgniter 4)
```

## Backend (`beetalk-panel/`)

- **Framework:** CodeIgniter 4 (PHP 8.1+)
- **Database:** MySQL (`beelite`), 24+ tabel
- **Auth:** JWT HS256 (stateless), admin pakai session
- **Push:** Firebase Cloud Messaging
- **Email:** PHPMailer SMTP
- **Base URL:** `https://beetalk.xuaxin.org/api/`
- **Admin Panel:** `/admin/login` — Tabler CSS (custom Pixinvent theme)

### Database (key tables)

| Table | Purpose |
|-------|---------|
| `users` | Auth, is_bot flag, super_like_quota |
| `user_profiles` | Name, gender, birth, bio, hobbies, dreams, city, lat/lng |
| `user_preferences` | min_age, max_age, max_distance, interested_in |
| `swipes` | Like/pass + is_super_like |
| `matches` | Mutual like → match |
| `messages` | Chat text |
| `pending_bot_messages` | Bot auto-reply queue (2-8s delay) |
| `bot_message_templates` | welcome, reply_keyword, reply_default |
| `gallery_posts` | Feed foto, status: pending/approved/rejected |
| `admins` | Admin panel accounts |
| `token_blacklist` | JWT logout blacklist |
| `app_settings` | Key-value store (ads, maintenance, etc) |
| `scheduled_notifications` | Push broadcast queue |

### API Endpoints

```
POST /api/auth/register          (rate: 5/60s)
POST /api/auth/login             (rate: 10/60s)
GET  /api/auth/me                (auth)
POST /api/auth/logout            (auth) — blacklist token
POST /api/auth/refresh           (auth) — blacklist old + issue new
GET  /api/swipe/potential        (auth) — Haversine distance filter
POST /api/swipe                  (auth) — like/pass
POST /api/swipe/super-like       (auth)
GET  /api/matches                (auth)
GET  /api/chat/conversations     (auth)
POST /api/chat/:id/messages      (auth) — REST-based, not WebSocket
GET  /api/gallery/feed           (auth)
POST /api/gallery                (auth)
GET  /api/explore/nearby         (auth)
POST /api/block                  (auth)
POST /api/report                 (auth)
GET  /api/ad-config              (public) — remote ad config
```

### Bot System

1. **Auto-match on register:** `BotMatchHelper::createBotMatchesForUser()` → 3-5 random bots match + welcome message
2. **Auto-reply:** User chat bot → keyword detection in `bot_message_templates` → reply queued 2-8s delay via `pending_bot_messages`
3. **Generate via admin:** `/admin/bots/generate` — random Indo names, random cities (Jakarta/Bandung/Surabaya/Jogja), random photos from `public/uploads/bot_photos/{male,female}/`

### Admin Panel Routes

```
/admin/login          (rate: 5/300s)
/admin/dashboard      (stats + chart.js)
/admin/users           (CRUD + ban/unban/delete)
/admin/bots            (generate/edit/delete/bulk photo update)
/admin/templates       (bot message templates CRUD)
/admin/gallery         (approve/reject posts)
/admin/reports         (moderate user reports)
/admin/notifications   (push broadcast: all/gender/city/user_ids)
/admin/settings        (app name, maintenance mode)
/admin/settings/ads    (12 placement toggles, unit IDs, presets, kill switch)
```

---

## Android App (`BeetalkRebornFix/`)

- **Package:** `com.beeflirt.beetalk`
- **Min SDK:** API 23+
- **Auth:** JWT stored in SharedPreferences, AuthInterceptor auto-injects Bearer token
- **DB:** Room (SQLite) — offline cache for profiles, matches, chat, photos
- **Network:** Retrofit2 + OkHttp, Volley for ad config
- **Ads:** AppLovin MAX + AdMob (dual network, waterfall)

### Key Classes

| Class | Purpose |
|-------|---------|
| `SplashActivity` | Remote config fetch, GDPR consent, app update check, kill switch |
| `MainSelectActivity` | Welcome screen + banner/interstitial ads |
| `AppActivity` | Login/Register entry + banner ads |
| `MainActivity` | Tab host: Swipe, Explore, Messages, Matches, Profile |
| `Ads.java` | Banner/interstitial/rewarded placement-aware facade |
| `SmallBanner.java` | Banner: AppLovin, AdMob, waterfall (primary+backup) |
| `Interstitial.java` | Interstitial load/show with fallback |
| `Rewarded.java` | Rewarded video with AppLovin reward callback |
| `GDPR.java` | UMP consent for AdMob |
| `SettingAds.java` | Placement matrix, unit IDs, waterfall priority |
| `BotMatchHelper.java` | Auto-match + auto-reply bot logic |

### Ad Architecture

```
SplashActivity.loadUrlData()
  → GET /api/ad-config
  → Parse JSON {Ads: [{placements: {...}, waterfall_priority, unit_ids, ...}]}
  → Update SettingAds.PLACEMENTS (per-placement toggles)
  → Sync SELECT_MAIN_ADS from interstitial_main_select placement
  → Sync MAIN_ADS_INTERTITIAL/REWARDS to correct network ID
  → Check kill switch / app update

Banner flow (PER-PLACEMENT):
  Ads.ShowBanner(activity, layAds, "banner_main_select")
    → isPlacementEnabled("banner_main_select", "applovin/admob")
    → AppLovin/AdMob/waterfall based on toggle

Interstitial flow (GLOBAL — all placements share one config):
  Ads.LoadInterstitialAds(activity, "interstitial_main_select")
    → shouldShow(placement) checks matrix
    → Interstitial.loadAds() → switch SELECT_MAIN_ADS
    → LoadApplovinMax/LoadAdmob with MAIN_ADS_INTERTITIAL

Rewarded flow (GLOBAL):
  Same pattern as Interstitial — uses SELECT_MAIN_ADS + MAIN_ADS_REWARDS
```

---

## Ad Placement Matrix (12 placements)

| Key | Type | Risk |
|-----|------|------|
| `banner_language` | banner | medium |
| `banner_main_select` | banner | low |
| `banner_app_activity` | banner | low |
| `banner_swipe` | banner | high |
| `banner_explore` | banner | low |
| `banner_messages` | banner | high |
| `banner_matches` | banner | medium |
| `banner_profile` | banner | low |
| `interstitial_main_select` | interstitial | low |
| `interstitial_geo` | interstitial | high |
| `interstitial_swipe` | interstitial | medium |
| `rewarded_super_like` | rewarded | low |

**Presets:** Safe / Balanced / Aggressive / AppLovin-only / AdMob-only

**Banner = per-placement** (masing-masing bisa beda network).  
**Interstitial & Rewarded = global** (semua placement share 1 `SELECT_MAIN_ADS` + 1 ad instance). Keterbatasan arsitektur — perlu refactor `Interstitial.java` untuk full per-placement.

---

## Security Status (fixes applied 2026-07-01)

| Fix | File | Status |
|-----|------|--------|
| `.env` with `jwt.secret` (64-char hex) | `.env` | ✅ Done |
| `token_blacklist` migration | `2026-07-01-000001_CreateTokenBlacklistTable.php` | ✅ Done |
| Refresh invalidates old token | `AuthController.php::refresh()` | ✅ Done |
| Admin login rate limit (5/300s) | `Routes.php` | ✅ Done |
| CSRF on admin authenticated routes | `Filters.php`, `Security.php`, layouts | ✅ Done |
| CORS restricted to whitelist | `Cors.php` | ✅ Done |
| `.gitignore` (excludes `.env`) | `.gitignore` | ✅ Done |

**CSRF notes:** Admin login excluded (protected by rate limit). API routes excluded (use JWT). All admin authenticated forms auto-inject `csrf_test_name` via JS in `main.php` layout.

**CORS notes:** Android apps bypass CORS entirely (not enforced by OkHttp). Whitelist: `beetalk.xuaxin.org`, `localhost`.

---

## Design System

Admin panel uses custom **Pixinvent Light** theme (no Bootstrap/Tabler dependency):
- **Font:** Outfit (Google Fonts), 300 body / 600 headings
- **Primary:** `#6E66ED` (indigo)
- **Background:** `#F9F9FB` / **Cards:** `#FFFFFF`
- **CSS:** `public/admin-assets/pixinvent.css` (~23KB)
- **Spec:** `design.md` (root)
