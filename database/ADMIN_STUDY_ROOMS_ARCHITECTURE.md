# StudyTimer — Admin Panel & Private Rooms Architecture Specification

## Overview
This document specifies the backend schema, API contracts, security guarantees, and admin RPCs for managing Study Rooms (Public & Private) in the StudyTimer Admin Panel.

---

## 1. Core Principles & Privacy Separation
1. **Leaderboard Isolation**:
   - Private rooms are **never exposed** on the public Community Explore tab or in `get_public_study_rooms`.
   - Study time accumulated while in a private room is synced to the public daily leaderboard (`public.daily_leaderboard`) as raw focus seconds (`total_seconds`, `current_subject`), but **no room identifier or room name is ever published** to the leaderboard.
   - Friends can only discover and join a private room if they possess the 6-character room code (and optional passcode).
2. **Minimal Server Overhead**:
   - Room presence is debounced on the client (syncs at most once every 10–20 seconds).
   - Inactive presence older than 5 minutes is clamped to "Resting" on query; older than 48 hours is excluded from active rosters.
   - Partial indexes on PostgreSQL (`WHERE is_private = FALSE AND status = 'ACTIVE'`) ensure public room queries execute in `<2ms`.
3. **Zero-Trust & Moderation**:
   - All admin functions run as `SECURITY DEFINER` and can be invoked either via `service_role` or authorized Admin tokens.

---

## 2. PostgreSQL Tables Reference

### `public.study_rooms`
| Column | Type | Description |
|---|---|---|
| `id` | BIGSERIAL PRIMARY KEY | Internal primary key |
| `code` | VARCHAR(10) UNIQUE NOT NULL | 6-character unique room code (e.g. `K8M2P4`) |
| `name` | VARCHAR(60) NOT NULL | Display name of the room |
| `category` | VARCHAR(30) DEFAULT 'General' | `General`, `Coding`, `Exams`, `Pomodoro`, `Silent` |
| `creator_id` | TEXT NOT NULL | Auth UID or client ID of room creator |
| `creator_name` | VARCHAR(40) NOT NULL | Display name of room creator |
| `member_count` | INT DEFAULT 1 | Count of members active in past 48 hours |
| `max_members` | INT DEFAULT 50 | Maximum allowed simultaneous members |
| `is_private` | BOOLEAN DEFAULT FALSE | `TRUE` = Hidden from Explore, joinable only via code |
| `pass_code` | VARCHAR(32) DEFAULT NULL | Optional passcode required to join private room |
| `status` | VARCHAR(20) DEFAULT 'ACTIVE' | `ACTIVE`, `FLAGGED`, `SUSPENDED`, `ARCHIVED` |
| `admin_notes` | TEXT DEFAULT NULL | Internal notes written by moderators/admins |
| `reports_count` | INT DEFAULT 0 | Count of student moderation reports |
| `last_admin_action_at` | TIMESTAMPTZ | Timestamp of last admin modification |
| `created_at` | TIMESTAMPTZ DEFAULT NOW() | Room creation timestamp |
| `updated_at` | TIMESTAMPTZ DEFAULT NOW() | Last update timestamp |

### `public.study_room_members`
| Column | Type | Description |
|---|---|---|
| `id` | BIGSERIAL PRIMARY KEY | Internal primary key |
| `room_code` | VARCHAR(10) REFERENCES study_rooms(code) ON DELETE CASCADE | Target room code |
| `user_id` | TEXT NOT NULL | Student User ID |
| `user_name` | VARCHAR(40) NOT NULL | Student Display Name |
| `avatar_url` | TEXT | Student Avatar URL or empty |
| `is_studying` | BOOLEAN DEFAULT FALSE | Real-time study status |
| `current_subject` | VARCHAR(50) | Active subject (e.g. "Calculus III") |
| `subject_color` | VARCHAR(16) DEFAULT '#3b82f6' | Subject tag accent color hex |
| `elapsed_today_seconds`| INT DEFAULT 0 | Focus seconds accumulated today |
| `last_active_at` | TIMESTAMPTZ DEFAULT NOW() | Last heartbeat timestamp |
| `joined_at` | TIMESTAMPTZ DEFAULT NOW() | Join timestamp |

---

## 3. Admin Panel RPCs & Endpoints

All endpoints are accessible via Supabase REST RPC: `POST /rest/v1/rpc/{function_name}` with `apikey` and `Authorization: Bearer <service_role_key>` headers.

### 3.1 Overview Statistics: `admin_get_rooms_overview_stats`
Returns high-level platform health metrics for the admin dashboard cards.

- **Request**: `{}`
- **Response**:
```json
{
  "total_rooms": 142,
  "active_rooms": 128,
  "private_rooms": 45,
  "public_rooms": 97,
  "active_students_now": 312,
  "room_hours_today": 845.5
}
```

---

### 3.2 List All Rooms: `admin_list_all_rooms`
Lists rooms with multi-parameter filtering, search, and pagination.

- **Parameters**:
  - `p_status` (string, optional, default `"ALL"`): `"ALL"`, `"ACTIVE"`, `"FLAGGED"`, `"SUSPENDED"`, `"ARCHIVED"`
  - `p_is_private` (boolean, optional, default `null`): `true` for private only, `false` for public only, `null` for all
  - `p_search` (string, optional, default `""`): Search query matched against room code, name, or creator name
  - `p_limit` (integer, optional, default `50`): Max records (1–100)
  - `p_offset` (integer, optional, default `0`): Pagination offset
- **Response**: Array of room objects with all moderation attributes.

---

### 3.3 Get Full Room Details & Live Roster: `admin_get_room_full_details`
Returns room metadata plus the list of members currently inside.

- **Parameters**:
  - `p_room_code` (string, required): e.g. `"PRIV88"`
- **Response**:
```json
{
  "success": true,
  "room": {
    "id": 84,
    "code": "PRIV88",
    "name": "Private Study Circle",
    "category": "General",
    "creator_id": "usr_9921",
    "creator_name": "Maya",
    "member_count": 4,
    "max_members": 50,
    "is_private": true,
    "pass_code": "1234",
    "status": "ACTIVE",
    "admin_notes": null,
    "reports_count": 0,
    "created_at": "2026-10-15T15:20:00Z",
    "updated_at": "2026-10-15T15:30:00Z"
  },
  "members": [
    {
      "user_id": "usr_9921",
      "user_name": "Maya",
      "avatar_url": "",
      "is_studying": true,
      "current_subject": "Math",
      "elapsed_today_seconds": 3600,
      "last_active_at": "2026-10-15T15:32:00Z",
      "joined_at": "2026-10-15T15:20:00Z"
    }
  ]
}
```

---

### 3.4 Create / Edit Room: `admin_upsert_room`
Allows administrators to create official sponsored rooms or update any existing room's privacy, category, name, or passcode.

- **Parameters**:
  - `p_code` (string, required): 3–10 character code (e.g. `"MED2026"`)
  - `p_name` (string, required): Room display name
  - `p_category` (string, required): Category tag
  - `p_creator_id` (string, optional, default `"admin"`)
  - `p_creator_name` (string, optional, default `"Admin"`)
  - `p_is_private` (boolean, optional, default `false`)
  - `p_pass_code` (string, optional, default `null`)
  - `p_status` (string, optional, default `"ACTIVE"`)
  - `p_max_members` (integer, optional, default `100`)
- **Response**:
```json
{
  "success": true,
  "code": "MED2026",
  "name": "Medical School Exam Sprint",
  "is_private": false,
  "status": "ACTIVE"
}
```

---

### 3.5 Update Moderation Status: `admin_update_room_moderation`
Changes status to flag or suspend a room.

- **Parameters**:
  - `p_room_code` (string, required)
  - `p_new_status` (string, required): `"ACTIVE"`, `"FLAGGED"`, `"SUSPENDED"`, or `"ARCHIVED"`
  - `p_admin_notes` (string, optional): Reason or moderator remarks
- **Response**:
```json
{
  "success": true,
  "code": "EXAM99",
  "status": "SUSPENDED"
}
```

---

### 3.6 Kick Member: `admin_kick_member`
Ejects a troll or disruptive member from any study room.

- **Parameters**:
  - `p_room_code` (string, required)
  - `p_user_id` (string, required)
- **Response**:
```json
{
  "success": true,
  "message": "Member removed successfully"
}
```

---

### 3.7 Force Delete Room: `admin_force_delete_room`
Permanently deletes a room and automatically cascades to remove all associated member rows.

- **Parameters**:
  - `p_room_code` (string, required)
- **Response**:
```json
{
  "success": true,
  "message": "Room deleted permanently"
}
```
