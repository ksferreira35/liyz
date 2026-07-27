ALTER TABLE task
    ADD COLUMN IF NOT EXISTS due_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS notification_status VARCHAR(30) NOT NULL DEFAULT 'NONE',
    ADD COLUMN IF NOT EXISTS notification_attempts INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS notification_queued_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS notified_at TIMESTAMP(6),
    ADD COLUMN IF NOT EXISTS last_notification_error VARCHAR(1000);

CREATE INDEX IF NOT EXISTS idx_task_notification_due
    ON task (notification_status, due_at)
    WHERE due_at IS NOT NULL;
