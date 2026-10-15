-- =========================================================================
-- StudyTimer Study Rooms & Collaborative Focus Schema (Supabase SQL)
-- High Performance, Zero-Bloat, Free-Tier Optimized Architecture
-- Includes Full Admin Panel Suite & Private Rooms Isolation
-- =========================================================================

-- 1. Study Rooms Table
CREATE TABLE IF NOT EXISTS public.study_rooms (
    id BIGSERIAL PRIMARY KEY,
    code TEXT NOT NULL UNIQUE,
    name VARCHAR(60) NOT NULL,
    description VARCHAR(200) DEFAULT '',
    category VARCHAR(30) NOT NULL DEFAULT 'General',
    creator_id TEXT NOT NULL,
    creator_name VARCHAR(40) NOT NULL DEFAULT 'Student',
    member_count INT NOT NULL DEFAULT 1,
    max_members INT NOT NULL DEFAULT 50,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    pass_code VARCHAR(32) DEFAULT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',       -- 'ACTIVE', 'FLAGGED', 'SUSPENDED', 'ARCHIVED'
    admin_notes TEXT DEFAULT NULL,
    reports_count INT NOT NULL DEFAULT 0,
    last_admin_action_at TIMESTAMPTZ DEFAULT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_room_code_len CHECK (char_length(code) >= 3 AND char_length(code) <= 12),
    CONSTRAINT chk_room_name_len CHECK (char_length(name) >= 2)
);

-- Upgrade existing schema if columns are missing
ALTER TABLE public.study_rooms ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE public.study_rooms ADD COLUMN IF NOT EXISTS admin_notes TEXT DEFAULT NULL;
ALTER TABLE public.study_rooms ADD COLUMN IF NOT EXISTS reports_count INT NOT NULL DEFAULT 0;
ALTER TABLE public.study_rooms ADD COLUMN IF NOT EXISTS last_admin_action_at TIMESTAMPTZ DEFAULT NULL;

-- Targeted Composite Indexes (Reduces index maintenance & memory footprint)
CREATE INDEX IF NOT EXISTS idx_study_rooms_public_listing 
    ON public.study_rooms(is_private, updated_at DESC) 
    WHERE is_private = FALSE AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_study_rooms_category 
    ON public.study_rooms(category, is_private, updated_at DESC) 
    WHERE is_private = FALSE AND status = 'ACTIVE';

CREATE INDEX IF NOT EXISTS idx_study_rooms_code 
    ON public.study_rooms(code);

CREATE INDEX IF NOT EXISTS idx_study_rooms_admin_status 
    ON public.study_rooms(status, is_private, created_at DESC);

-- 2. Study Room Members Table
CREATE TABLE IF NOT EXISTS public.study_room_members (
    id BIGSERIAL PRIMARY KEY,
    room_code TEXT NOT NULL REFERENCES public.study_rooms(code) ON DELETE CASCADE,
    user_id TEXT NOT NULL,
    user_name VARCHAR(40) NOT NULL DEFAULT 'Student',
    avatar_url TEXT DEFAULT '',
    is_studying BOOLEAN NOT NULL DEFAULT FALSE,
    current_subject VARCHAR(50) DEFAULT '',
    subject_color VARCHAR(16) DEFAULT '#3b82f6',
    elapsed_today_seconds INT NOT NULL DEFAULT 0,
    last_active_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT unique_room_member UNIQUE (room_code, user_id),
    CONSTRAINT chk_elapsed_today CHECK (elapsed_today_seconds >= 0 AND elapsed_today_seconds <= 86400)
);

-- Fast Index for live roster lookups within a room
CREATE INDEX IF NOT EXISTS idx_room_members_roster 
    ON public.study_room_members(room_code, last_active_at DESC);

CREATE INDEX IF NOT EXISTS idx_room_members_user 
    ON public.study_room_members(user_id);

-- 3. Row Level Security (RLS)
ALTER TABLE public.study_rooms ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.study_room_members ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Public read access for study rooms" ON public.study_rooms;
CREATE POLICY "Public read access for study rooms" ON public.study_rooms
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Public read access for study room members" ON public.study_room_members;
CREATE POLICY "Public read access for study room members" ON public.study_room_members
    FOR SELECT USING (true);

DROP POLICY IF EXISTS "Admin full access for study rooms" ON public.study_rooms;
CREATE POLICY "Admin full access for study rooms" ON public.study_rooms
    FOR ALL TO service_role USING (true) WITH CHECK (true);

DROP POLICY IF EXISTS "Admin full access for study room members" ON public.study_room_members;
CREATE POLICY "Admin full access for study room members" ON public.study_room_members
    FOR ALL TO service_role USING (true) WITH CHECK (true);

-- =========================================================================
-- 4. RPC: Create Study Room (Input-Sanitized, Rate-Clamped)
-- =========================================================================
CREATE OR REPLACE FUNCTION public.create_study_room(
    p_code TEXT,
    p_name TEXT,
    p_category TEXT,
    p_creator_id TEXT,
    p_creator_name TEXT,
    p_is_private BOOLEAN DEFAULT FALSE,
    p_pass_code TEXT DEFAULT NULL,
    p_max_members INT DEFAULT 50
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_clean_code TEXT;
    v_clean_name TEXT;
    v_clean_cat TEXT;
    v_clean_creator TEXT;
    v_clean_pass TEXT;
    v_clean_max INT;
    v_room_id BIGINT;
BEGIN
    -- Input sanitization
    v_clean_code := UPPER(SUBSTRING(REGEXP_REPLACE(COALESCE(p_code, ''), '[^a-zA-Z0-9]', '', 'g'), 1, 10));
    v_clean_name := SUBSTRING(TRIM(COALESCE(p_name, '')), 1, 60);
    v_clean_cat := SUBSTRING(TRIM(COALESCE(p_category, 'General')), 1, 30);
    v_clean_creator := SUBSTRING(TRIM(COALESCE(p_creator_name, 'Student')), 1, 40);
    v_clean_pass := NULLIF(SUBSTRING(TRIM(COALESCE(p_pass_code, '')), 1, 32), '');
    v_clean_max := LEAST(GREATEST(COALESCE(p_max_members, 50), 2), 100);

    IF char_length(v_clean_code) < 3 THEN
        RETURN jsonb_build_object('success', false, 'error', 'Room code must be at least 3 alphanumeric characters');
    END IF;

    IF char_length(v_clean_name) < 2 THEN
        RETURN jsonb_build_object('success', false, 'error', 'Room name must be at least 2 characters');
    END IF;

    IF p_creator_id IS NULL OR TRIM(p_creator_id) = '' THEN
        RETURN jsonb_build_object('success', false, 'error', 'Valid creator ID required');
    END IF;

    -- Insert Room
    INSERT INTO public.study_rooms (
        code, name, category, creator_id, creator_name,
        member_count, max_members, is_private, pass_code,
        status, created_at, updated_at
    )
    VALUES (
        v_clean_code, v_clean_name, v_clean_cat, p_creator_id, v_clean_creator,
        1, v_clean_max, COALESCE(p_is_private, false), v_clean_pass,
        'ACTIVE', NOW(), NOW()
    )
    RETURNING id INTO v_room_id;

    -- Add Creator as initial member
    INSERT INTO public.study_room_members (
        room_code, user_id, user_name, is_studying, last_active_at, joined_at
    )
    VALUES (
        v_clean_code, p_creator_id, v_clean_creator, false, NOW(), NOW()
    )
    ON CONFLICT (room_code, user_id) DO UPDATE SET
        user_name = EXCLUDED.user_name,
        last_active_at = NOW();

    RETURN jsonb_build_object(
        'success', true,
        'code', v_clean_code,
        'name', v_clean_name,
        'category', v_clean_cat,
        'creator_name', v_clean_creator,
        'member_count', 1,
        'is_private', COALESCE(p_is_private, false)
    );
EXCEPTION WHEN unique_violation THEN
    RETURN jsonb_build_object('success', false, 'error', 'Room code already in use. Please pick another.');
END;
$$;

-- =========================================================================
-- 5. RPC: Join Study Room (Passcode Verified for Private Rooms)
-- =========================================================================
CREATE OR REPLACE FUNCTION public.join_study_room(
    p_room_code TEXT,
    p_user_id TEXT,
    p_user_name TEXT,
    p_avatar_url TEXT DEFAULT '',
    p_pass_code TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_code TEXT;
    v_room RECORD;
    v_clean_name TEXT;
    v_clean_pass TEXT;
BEGIN
    v_code := UPPER(SUBSTRING(REGEXP_REPLACE(COALESCE(p_room_code, ''), '[^a-zA-Z0-9]', '', 'g'), 1, 10));
    v_clean_name := SUBSTRING(TRIM(COALESCE(p_user_name, 'Student')), 1, 40);
    v_clean_pass := SUBSTRING(TRIM(COALESCE(p_pass_code, '')), 1, 32);

    IF p_user_id IS NULL OR TRIM(p_user_id) = '' THEN
        RETURN jsonb_build_object('success', false, 'error', 'Valid user ID required');
    END IF;

    -- Fetch room
    SELECT * INTO v_room FROM public.study_rooms WHERE code = v_code;
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Study room not found');
    END IF;

    -- Check if suspended/banned by admin
    IF v_room.status <> 'ACTIVE' THEN
        RETURN jsonb_build_object('success', false, 'error', 'This study room is currently closed or suspended.');
    END IF;

    -- Verify passcode if private
    IF v_room.is_private AND v_room.pass_code IS NOT NULL AND v_room.pass_code <> '' THEN
        IF v_clean_pass IS NULL OR v_clean_pass <> v_room.pass_code THEN
            RETURN jsonb_build_object('success', false, 'error', 'Incorrect room passcode');
        END IF;
    END IF;

    -- Upsert membership
    INSERT INTO public.study_room_members (
        room_code, user_id, user_name, avatar_url, is_studying, last_active_at, joined_at
    )
    VALUES (
        v_code, p_user_id, v_clean_name, COALESCE(p_avatar_url, ''), false, NOW(), NOW()
    )
    ON CONFLICT (room_code, user_id) DO UPDATE SET
        user_name = EXCLUDED.user_name,
        avatar_url = CASE WHEN EXCLUDED.avatar_url <> '' THEN EXCLUDED.avatar_url ELSE study_room_members.avatar_url END,
        last_active_at = NOW();

    -- Recount active members in last 48 hours for lightweight server caching
    UPDATE public.study_rooms
    SET member_count = (
        SELECT COUNT(*) FROM public.study_room_members
        WHERE room_code = v_code AND last_active_at >= NOW() - INTERVAL '48 hours'
    ),
    updated_at = NOW()
    WHERE code = v_code;

    RETURN jsonb_build_object(
        'success', true,
        'code', v_room.code,
        'name', v_room.name,
        'category', v_room.category,
        'creator_name', v_room.creator_name,
        'member_count', v_room.member_count,
        'is_private', v_room.is_private
    );
END;
$$;

-- =========================================================================
-- 6. RPC: Leave Study Room
-- =========================================================================
CREATE OR REPLACE FUNCTION public.leave_study_room(
    p_room_code TEXT,
    p_user_id TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_code TEXT;
    v_remaining INT;
BEGIN
    v_code := UPPER(SUBSTRING(REGEXP_REPLACE(COALESCE(p_room_code, ''), '[^a-zA-Z0-9]', '', 'g'), 1, 10));

    DELETE FROM public.study_room_members
    WHERE room_code = v_code AND user_id = p_user_id;

    SELECT COUNT(*) INTO v_remaining
    FROM public.study_room_members
    WHERE room_code = v_code;

    -- If user-created room is empty after 24 hours, prune to minimize server table bloat
    IF v_remaining = 0 THEN
        DELETE FROM public.study_rooms
        WHERE code = v_code AND created_at < NOW() - INTERVAL '24 hours';
    ELSE
        UPDATE public.study_rooms
        SET member_count = v_remaining, updated_at = NOW()
        WHERE code = v_code;
    END IF;

    RETURN jsonb_build_object('success', true);
END;
$$;

-- =========================================================================
-- 7. RPC: Update Room Presence (Heartbeat with 10s Server Throttling)
-- =========================================================================
CREATE OR REPLACE FUNCTION public.update_room_presence(
    p_room_code TEXT,
    p_user_id TEXT,
    p_user_name TEXT,
    p_avatar_url TEXT DEFAULT '',
    p_is_studying BOOLEAN DEFAULT FALSE,
    p_current_subject TEXT DEFAULT '',
    p_subject_color TEXT DEFAULT '#3b82f6',
    p_elapsed_seconds INT DEFAULT 0
)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_code TEXT;
    v_last_active TIMESTAMPTZ;
    v_clean_name TEXT;
    v_clean_subject TEXT;
    v_clamped_seconds INT;
BEGIN
    IF p_user_id IS NULL OR TRIM(p_user_id) = '' THEN
        RETURN;
    END IF;

    v_code := UPPER(SUBSTRING(REGEXP_REPLACE(COALESCE(p_room_code, ''), '[^a-zA-Z0-9]', '', 'g'), 1, 10));
    v_clean_name := SUBSTRING(TRIM(COALESCE(p_user_name, 'Student')), 1, 40);
    v_clean_subject := SUBSTRING(TRIM(COALESCE(p_current_subject, '')), 1, 50);
    v_clamped_seconds := LEAST(GREATEST(COALESCE(p_elapsed_seconds, 0), 0), 86400);

    -- Check last active timestamp to throttle writes to max once per 10 seconds per user
    SELECT last_active_at INTO v_last_active
    FROM public.study_room_members
    WHERE room_code = v_code AND user_id = p_user_id;

    IF v_last_active IS NOT NULL AND NOW() - v_last_active < INTERVAL '10 seconds' THEN
        RETURN; -- Throttled: saves DB write load
    END IF;

    INSERT INTO public.study_room_members (
        room_code, user_id, user_name, avatar_url,
        is_studying, current_subject, subject_color,
        elapsed_today_seconds, last_active_at
    )
    VALUES (
        v_code, p_user_id, v_clean_name, COALESCE(p_avatar_url, ''),
        COALESCE(p_is_studying, false), v_clean_subject, COALESCE(p_subject_color, '#3b82f6'),
        v_clamped_seconds, NOW()
    )
    ON CONFLICT (room_code, user_id) DO UPDATE SET
        user_name = CASE WHEN EXCLUDED.user_name <> 'Student' THEN EXCLUDED.user_name ELSE study_room_members.user_name END,
        avatar_url = CASE WHEN EXCLUDED.avatar_url <> '' THEN EXCLUDED.avatar_url ELSE study_room_members.avatar_url END,
        is_studying = EXCLUDED.is_studying,
        current_subject = EXCLUDED.current_subject,
        subject_color = EXCLUDED.subject_color,
        elapsed_today_seconds = EXCLUDED.elapsed_today_seconds,
        last_active_at = NOW();
END;
$$;

-- =========================================================================
-- 8. RPC: Get Public Study Rooms (Cached, Lightweight Payload)
-- Strictly excludes private rooms, non-active rooms, and rooms inactive > 7 days
-- =========================================================================
CREATE OR REPLACE FUNCTION public.get_public_study_rooms(
    p_category TEXT DEFAULT 'All',
    p_search TEXT DEFAULT '',
    p_limit INT DEFAULT 20
)
RETURNS TABLE (
    code TEXT,
    name VARCHAR(60),
    category VARCHAR(30),
    creator_name VARCHAR(40),
    member_count INT,
    is_private BOOLEAN,
    created_at TIMESTAMPTZ
)
LANGUAGE sql
STABLE
AS $$
    SELECT 
        r.code,
        r.name,
        r.category,
        r.creator_name,
        r.member_count,
        r.is_private,
        r.created_at
    FROM public.study_rooms r
    WHERE r.is_private = FALSE
      AND r.status = 'ACTIVE'
      AND r.updated_at >= NOW() - INTERVAL '7 days'
      AND (p_category = 'All' OR p_category IS NULL OR r.category = p_category)
      AND (p_search = '' OR p_search IS NULL OR r.name ILIKE ('%' || p_search || '%') OR r.code ILIKE (p_search || '%'))
    ORDER BY r.member_count DESC, r.updated_at DESC
    LIMIT LEAST(GREATEST(COALESCE(p_limit, 20), 1), 50);
$$;

-- =========================================================================
-- 8.1 Auto-Prune Inactive Study Rooms (No Activity for 7 Days)
-- =========================================================================
CREATE OR REPLACE FUNCTION public.prune_inactive_study_rooms(
    p_inactive_days INT DEFAULT 7
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_deleted_count INT;
BEGIN
    WITH inactive_targets AS (
        SELECT r.code
        FROM public.study_rooms r
        WHERE r.updated_at < NOW() - (p_inactive_days || ' days')::INTERVAL
          AND NOT EXISTS (
              SELECT 1 FROM public.study_room_members m
              WHERE m.room_code = r.code
                AND m.last_active_at >= NOW() - (p_inactive_days || ' days')::INTERVAL
          )
    ),
    deleted AS (
        DELETE FROM public.study_rooms
        WHERE code IN (SELECT code FROM inactive_targets)
        RETURNING code
    )
    SELECT COUNT(*) INTO v_deleted_count FROM deleted;

    RETURN jsonb_build_object(
        'success', true,
        'deleted_rooms_count', v_deleted_count,
        'cutoff_days', p_inactive_days
    );
END;
$$;

-- =========================================================================
-- 9. RPC: Get Room Members (Auto Stale Presence Clamp)
-- =========================================================================
CREATE OR REPLACE FUNCTION public.get_room_members(
    p_room_code TEXT
)
RETURNS TABLE (
    user_id TEXT,
    user_name VARCHAR(40),
    avatar_url TEXT,
    is_studying BOOLEAN,
    current_subject VARCHAR(50),
    subject_color VARCHAR(16),
    elapsed_today_seconds INT,
    last_active_at TIMESTAMPTZ
)
LANGUAGE sql
STABLE
AS $$
    SELECT 
        m.user_id,
        m.user_name,
        m.avatar_url,
        CASE 
            WHEN m.is_studying AND m.last_active_at >= NOW() - INTERVAL '5 minutes' THEN TRUE 
            ELSE FALSE 
        END AS is_studying,
        m.current_subject,
        m.subject_color,
        m.elapsed_today_seconds,
        m.last_active_at
    FROM public.study_room_members m
    WHERE m.room_code = UPPER(p_room_code)
      AND m.last_active_at >= NOW() - INTERVAL '48 hours'
    ORDER BY 
        (m.is_studying AND m.last_active_at >= NOW() - INTERVAL '5 minutes') DESC,
        m.elapsed_today_seconds DESC,
        m.last_active_at DESC
    LIMIT 50;
$$;

-- =========================================================================
-- 10. ADMIN PANEL SUITE (Ready for Web / Admin Dashboard)
-- =========================================================================

-- 10.1 Admin List All Rooms (Public & Private with Status Filtering)
CREATE OR REPLACE FUNCTION public.admin_list_all_rooms(
    p_status TEXT DEFAULT 'ALL',
    p_is_private BOOLEAN DEFAULT NULL,
    p_search TEXT DEFAULT '',
    p_limit INT DEFAULT 50,
    p_offset INT DEFAULT 0
)
RETURNS TABLE (
    id BIGINT,
    code TEXT,
    name VARCHAR(60),
    category VARCHAR(30),
    creator_id TEXT,
    creator_name VARCHAR(40),
    member_count INT,
    max_members INT,
    is_private BOOLEAN,
    pass_code VARCHAR(32),
    status VARCHAR(20),
    admin_notes TEXT,
    reports_count INT,
    created_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ
)
LANGUAGE sql
SECURITY DEFINER
AS $$
    SELECT 
        r.id,
        r.code,
        r.name,
        r.category,
        r.creator_id,
        r.creator_name,
        r.member_count,
        r.max_members,
        r.is_private,
        r.pass_code,
        r.status,
        r.admin_notes,
        r.reports_count,
        r.created_at,
        r.updated_at
    FROM public.study_rooms r
    WHERE (p_status = 'ALL' OR p_status IS NULL OR r.status = p_status)
      AND (p_is_private IS NULL OR r.is_private = p_is_private)
      AND (p_search = '' OR p_search IS NULL OR r.name ILIKE ('%' || p_search || '%') OR r.code ILIKE (p_search || '%') OR r.creator_name ILIKE ('%' || p_search || '%'))
    ORDER BY r.created_at DESC
    LIMIT LEAST(GREATEST(COALESCE(p_limit, 50), 1), 100)
    OFFSET GREATEST(COALESCE(p_offset, 0), 0);
$$;

-- 10.2 Admin Get Room Full Details & Live Roster
CREATE OR REPLACE FUNCTION public.admin_get_room_full_details(
    p_room_code TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_room RECORD;
    v_members JSONB;
BEGIN
    SELECT * INTO v_room FROM public.study_rooms WHERE code = UPPER(TRIM(p_room_code));
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Room not found');
    END IF;

    SELECT COALESCE(jsonb_agg(jsonb_build_object(
        'user_id', m.user_id,
        'user_name', m.user_name,
        'avatar_url', m.avatar_url,
        'is_studying', m.is_studying,
        'current_subject', m.current_subject,
        'elapsed_today_seconds', m.elapsed_today_seconds,
        'last_active_at', m.last_active_at,
        'joined_at', m.joined_at
    )), '[]'::jsonb)
    INTO v_members
    FROM public.study_room_members m
    WHERE m.room_code = v_room.code;

    RETURN jsonb_build_object(
        'success', true,
        'room', jsonb_build_object(
            'id', v_room.id,
            'code', v_room.code,
            'name', v_room.name,
            'category', v_room.category,
            'creator_id', v_room.creator_id,
            'creator_name', v_room.creator_name,
            'member_count', v_room.member_count,
            'max_members', v_room.max_members,
            'is_private', v_room.is_private,
            'pass_code', v_room.pass_code,
            'status', v_room.status,
            'admin_notes', v_room.admin_notes,
            'reports_count', v_room.reports_count,
            'created_at', v_room.created_at,
            'updated_at', v_room.updated_at
        ),
        'members', v_members
    );
END;
$$;

-- 10.3 Admin Update Room Moderation Status
CREATE OR REPLACE FUNCTION public.admin_update_room_moderation(
    p_room_code TEXT,
    p_new_status TEXT,
    p_admin_notes TEXT DEFAULT NULL
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_clean_status TEXT;
BEGIN
    v_clean_status := UPPER(TRIM(p_new_status));
    IF v_clean_status NOT IN ('ACTIVE', 'FLAGGED', 'SUSPENDED', 'ARCHIVED') THEN
        RETURN jsonb_build_object('success', false, 'error', 'Invalid status. Use ACTIVE, FLAGGED, SUSPENDED, or ARCHIVED.');
    END IF;

    UPDATE public.study_rooms
    SET status = v_clean_status,
        admin_notes = COALESCE(p_admin_notes, admin_notes),
        last_admin_action_at = NOW(),
        updated_at = NOW()
    WHERE code = UPPER(TRIM(p_room_code));

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Room not found');
    END IF;

    RETURN jsonb_build_object('success', true, 'code', UPPER(TRIM(p_room_code)), 'status', v_clean_status);
END;
$$;

-- 10.4 Admin Force Delete Room
CREATE OR REPLACE FUNCTION public.admin_force_delete_room(
    p_room_code TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
BEGIN
    DELETE FROM public.study_rooms WHERE code = UPPER(TRIM(p_room_code));
    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Room not found');
    END IF;
    RETURN jsonb_build_object('success', true, 'message', 'Room deleted permanently');
END;
$$;

-- 10.5 Admin Overview Analytics
CREATE OR REPLACE FUNCTION public.admin_get_rooms_overview_stats()
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_total_rooms INT;
    v_active_rooms INT;
    v_private_rooms INT;
    v_public_rooms INT;
    v_active_students INT;
    v_total_hours_today NUMERIC;
BEGIN
    SELECT COUNT(*) INTO v_total_rooms FROM public.study_rooms;
    SELECT COUNT(*) INTO v_active_rooms FROM public.study_rooms WHERE status = 'ACTIVE';
    SELECT COUNT(*) INTO v_private_rooms FROM public.study_rooms WHERE is_private = TRUE;
    SELECT COUNT(*) INTO v_public_rooms FROM public.study_rooms WHERE is_private = FALSE;

    SELECT COUNT(*) INTO v_active_students 
    FROM public.study_room_members 
    WHERE is_studying = TRUE AND last_active_at >= NOW() - INTERVAL '5 minutes';

    SELECT COALESCE(ROUND((SUM(elapsed_today_seconds)::NUMERIC / 3600), 1), 0)
    INTO v_total_hours_today
    FROM public.study_room_members
    WHERE last_active_at >= NOW() - INTERVAL '24 hours';

    RETURN jsonb_build_object(
        'total_rooms', v_total_rooms,
        'active_rooms', v_active_rooms,
        'private_rooms', v_private_rooms,
        'public_rooms', v_public_rooms,
        'active_students_now', v_active_students,
        'room_hours_today', v_total_hours_today
    );
END;
$$;

-- 10.6 Admin Upsert / Edit Room (Create or Edit Any Room from Admin Dashboard)
CREATE OR REPLACE FUNCTION public.admin_upsert_room(
    p_code TEXT,
    p_name TEXT,
    p_category TEXT,
    p_creator_id TEXT DEFAULT 'admin',
    p_creator_name TEXT DEFAULT 'Admin',
    p_is_private BOOLEAN DEFAULT FALSE,
    p_pass_code TEXT DEFAULT NULL,
    p_status TEXT DEFAULT 'ACTIVE',
    p_max_members INT DEFAULT 100
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_clean_code TEXT;
    v_clean_name TEXT;
    v_clean_cat TEXT;
    v_clean_pass TEXT;
    v_clean_status TEXT;
BEGIN
    v_clean_code := UPPER(SUBSTRING(REGEXP_REPLACE(COALESCE(p_code, ''), '[^a-zA-Z0-9]', '', 'g'), 1, 10));
    v_clean_name := SUBSTRING(TRIM(COALESCE(p_name, '')), 1, 60);
    v_clean_cat := SUBSTRING(TRIM(COALESCE(p_category, 'General')), 1, 30);
    v_clean_pass := NULLIF(SUBSTRING(TRIM(COALESCE(p_pass_code, '')), 1, 32), '');
    v_clean_status := UPPER(SUBSTRING(TRIM(COALESCE(p_status, 'ACTIVE')), 1, 20));

    IF char_length(v_clean_code) < 3 THEN
        RETURN jsonb_build_object('success', false, 'error', 'Room code must be at least 3 alphanumeric characters');
    END IF;

    IF char_length(v_clean_name) < 2 THEN
        RETURN jsonb_build_object('success', false, 'error', 'Room name must be at least 2 characters');
    END IF;

    INSERT INTO public.study_rooms (
        code, name, category, creator_id, creator_name, is_private, pass_code, status, max_members, updated_at
    ) VALUES (
        v_clean_code, v_clean_name, v_clean_cat, COALESCE(p_creator_id, 'admin'), COALESCE(p_creator_name, 'Admin'),
        COALESCE(p_is_private, FALSE), v_clean_pass, v_clean_status, LEAST(GREATEST(COALESCE(p_max_members, 100), 2), 500), NOW()
    )
    ON CONFLICT (code) DO UPDATE SET
        name = EXCLUDED.name,
        category = EXCLUDED.category,
        is_private = EXCLUDED.is_private,
        pass_code = EXCLUDED.pass_code,
        status = EXCLUDED.status,
        max_members = EXCLUDED.max_members,
        updated_at = NOW();

    RETURN jsonb_build_object(
        'success', true,
        'code', v_clean_code,
        'name', v_clean_name,
        'is_private', COALESCE(p_is_private, FALSE),
        'status', v_clean_status
    );
END;
$$;

-- 10.7 Admin Kick / Remove Member from Room
CREATE OR REPLACE FUNCTION public.admin_kick_member(
    p_room_code TEXT,
    p_user_id TEXT
)
RETURNS JSONB
LANGUAGE plpgsql
SECURITY DEFINER
AS $$
DECLARE
    v_code TEXT;
BEGIN
    v_code := UPPER(SUBSTRING(REGEXP_REPLACE(COALESCE(p_room_code, ''), '[^a-zA-Z0-9]', '', 'g'), 1, 10));

    DELETE FROM public.study_room_members
    WHERE room_code = v_code AND user_id = p_user_id;

    IF NOT FOUND THEN
        RETURN jsonb_build_object('success', false, 'error', 'Member not found in room');
    END IF;

    -- Recount active members
    UPDATE public.study_rooms
    SET member_count = (
        SELECT COUNT(*) FROM public.study_room_members
        WHERE room_code = v_code AND last_active_at >= NOW() - INTERVAL '48 hours'
    ),
    updated_at = NOW()
    WHERE code = v_code;

    RETURN jsonb_build_object('success', true, 'message', 'Member removed successfully');
END;
$$;

-- Reload PostgREST Schema Cache
NOTIFY pgrst, 'reload schema';

