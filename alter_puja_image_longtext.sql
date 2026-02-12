-- Store Puja image as Base64 safely
ALTER TABLE puja
    MODIFY COLUMN image LONGTEXT NULL;
