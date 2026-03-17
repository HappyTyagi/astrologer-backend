-- Ensure users.role enum supports Pandit login/accounts.
ALTER TABLE users
    MODIFY COLUMN role ENUM ('ADMIN', 'ASTROLOGER', 'USER', 'PANDIT') NULL;
