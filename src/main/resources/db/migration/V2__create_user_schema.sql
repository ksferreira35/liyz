CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL
);

ALTER TABLE task
    ADD COLUMN IF NOT EXISTS user_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_task_users'
    ) THEN
        ALTER TABLE task
            ADD CONSTRAINT fk_task_users
            FOREIGN KEY (user_id)
            REFERENCES users(id);
    END IF;
END $$;
