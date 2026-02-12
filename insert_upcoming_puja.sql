-- Insert upcoming pujas (safe: only when name does not exist)
INSERT INTO puja
(
    name, description, price, duration_minutes, category, image, benefits, rituals,
    status, view_count, is_featured, feature_expiry,
    popup_enabled, popup_start_date, popup_end_date, popup_priority, popup_label,
    created_at, updated_at, is_active
)
SELECT * FROM (
    SELECT
        'Sarva Karya Siddhi Puja' AS name,
        'Remove obstacles and invite success in work and life' AS description,
        1100 AS price,
        90 AS duration_minutes,
        'Success' AS category,
        'https://images.unsplash.com/photo-1614676471928-2ed0ad1061a4?q=80&w=1200&auto=format&fit=crop' AS image,
        'Career growth, business success, obstacle removal' AS benefits,
        'Ganesh sthapana, sankalp, mantra jaap, aarti' AS rituals,
        'ACTIVE' AS status,
        0 AS view_count,
        1 AS is_featured,
        DATE_ADD(NOW(), INTERVAL 30 DAY) AS feature_expiry,
        1 AS popup_enabled,
        DATE_SUB(CURDATE(), INTERVAL 1 DAY) AS popup_start_date,
        DATE_ADD(CURDATE(), INTERVAL 21 DAY) AS popup_end_date,
        10 AS popup_priority,
        'Upcoming' AS popup_label,
        NOW() AS created_at,
        NOW() AS updated_at,
        1 AS is_active
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM puja p WHERE LOWER(p.name) = LOWER(tmp.name)
);

INSERT INTO puja
(
    name, description, price, duration_minutes, category, image, benefits, rituals,
    status, view_count, is_featured, feature_expiry,
    popup_enabled, popup_start_date, popup_end_date, popup_priority, popup_label,
    created_at, updated_at, is_active
)
SELECT * FROM (
    SELECT
        'Maha Mrityunjaya Jaap' AS name,
        'For health protection, healing and long life blessings' AS description,
        2100 AS price,
        120 AS duration_minutes,
        'Health' AS category,
        'https://images.unsplash.com/photo-1609766857041-ed402ea8069a?q=80&w=1200&auto=format&fit=crop' AS image,
        'Health stability, peace, recovery support' AS benefits,
        'Rudrabhishek, mantra jaap, havan, prasad' AS rituals,
        'ACTIVE' AS status,
        0 AS view_count,
        1 AS is_featured,
        DATE_ADD(NOW(), INTERVAL 30 DAY) AS feature_expiry,
        1 AS popup_enabled,
        CURDATE() AS popup_start_date,
        DATE_ADD(CURDATE(), INTERVAL 30 DAY) AS popup_end_date,
        9 AS popup_priority,
        'Upcoming' AS popup_label,
        NOW() AS created_at,
        NOW() AS updated_at,
        1 AS is_active
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM puja p WHERE LOWER(p.name) = LOWER(tmp.name)
);

INSERT INTO puja
(
    name, description, price, duration_minutes, category, image, benefits, rituals,
    status, view_count, is_featured, feature_expiry,
    popup_enabled, popup_start_date, popup_end_date, popup_priority, popup_label,
    created_at, updated_at, is_active
)
SELECT * FROM (
    SELECT
        'Lakshmi Kuber Puja' AS name,
        'Attract prosperity, wealth flow and financial balance' AS description,
        1800 AS price,
        80 AS duration_minutes,
        'Wealth' AS category,
        'https://images.unsplash.com/photo-1627894483216-2138af692e32?q=80&w=1200&auto=format&fit=crop' AS image,
        'Money stability, savings growth, business support' AS benefits,
        'Kalash sthapana, Lakshmi pujan, Kuber mantra' AS rituals,
        'ACTIVE' AS status,
        0 AS view_count,
        1 AS is_featured,
        DATE_ADD(NOW(), INTERVAL 30 DAY) AS feature_expiry,
        1 AS popup_enabled,
        DATE_ADD(CURDATE(), INTERVAL 1 DAY) AS popup_start_date,
        DATE_ADD(CURDATE(), INTERVAL 25 DAY) AS popup_end_date,
        8 AS popup_priority,
        'Upcoming' AS popup_label,
        NOW() AS created_at,
        NOW() AS updated_at,
        1 AS is_active
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM puja p WHERE LOWER(p.name) = LOWER(tmp.name)
);

INSERT INTO puja
(
    name, description, price, duration_minutes, category, image, benefits, rituals,
    status, view_count, is_featured, feature_expiry,
    popup_enabled, popup_start_date, popup_end_date, popup_priority, popup_label,
    created_at, updated_at, is_active
)
SELECT * FROM (
    SELECT
        'Navgrah Shanti Puja' AS name,
        'Balance planetary effects for smoother life events' AS description,
        2500 AS price,
        140 AS duration_minutes,
        'Planetary' AS category,
        'https://images.unsplash.com/photo-1518568814500-bf0f8d125f46?q=80&w=1200&auto=format&fit=crop' AS image,
        'Planetary peace, reduced dosha effects, clarity' AS benefits,
        'Navgrah mantra, offerings, shanti havan' AS rituals,
        'ACTIVE' AS status,
        0 AS view_count,
        1 AS is_featured,
        DATE_ADD(NOW(), INTERVAL 30 DAY) AS feature_expiry,
        1 AS popup_enabled,
        DATE_ADD(CURDATE(), INTERVAL 2 DAY) AS popup_start_date,
        DATE_ADD(CURDATE(), INTERVAL 35 DAY) AS popup_end_date,
        7 AS popup_priority,
        'Upcoming' AS popup_label,
        NOW() AS created_at,
        NOW() AS updated_at,
        1 AS is_active
) AS tmp
WHERE NOT EXISTS (
    SELECT 1 FROM puja p WHERE LOWER(p.name) = LOWER(tmp.name)
);
