ALTER SYSTEM SET max_connections = 1000;

-- Creation of status table
CREATE TABLE IF NOT EXISTS status
(
    "uuid"        uuid          NOT NULL,
    "vin"         character(17) NOT NULL,
    "requested"   timestamp,
    "triggered"   timestamp,
    "fetched"     timestamp,
    "unfetchable" timestamp,
    "created"     timestamp,
    "rolled_out"  timestamp,
    "last_update" timestamp,
    PRIMARY KEY ("uuid")
);

CREATE INDEX IF NOT EXISTS requested_idx ON status (requested);
CREATE INDEX IF NOT EXISTS triggered_idx ON status (triggered);
CREATE INDEX IF NOT EXISTS rolled_out_idx ON status (rolled_out);
