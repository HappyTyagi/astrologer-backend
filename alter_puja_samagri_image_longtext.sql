ALTER TABLE puja_samagri_master
    MODIFY COLUMN image_url LONGTEXT NULL;

ALTER TABLE puja_samagri_master_images
    MODIFY COLUMN image_url LONGTEXT NOT NULL;
