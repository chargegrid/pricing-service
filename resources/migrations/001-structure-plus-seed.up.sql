CREATE TABLE IF NOT EXISTS policies (
  id        UUID,
  name      TEXT,
  tenant_id UUID NOT NULL,
  PRIMARY KEY (id)
);
--;;
CREATE TYPE unit AS ENUM ('kwh', 'min', 'transaction');
CREATE TYPE weekday AS ENUM ('mon', 'tue', 'wed', 'thu', 'fri', 'sat', 'sun');
--;;
CREATE TABLE IF NOT EXISTS policy_rules (
  id        UUID,
  policy_id UUID REFERENCES policies    NOT NULL,
  start_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  end_at    TIMESTAMP WITHOUT TIME ZONE NOT NULL,
  CHECK (end_at > start_at),
  time_from TIME WITHOUT TIME ZONE      NOT NULL,
  time_to   TIME WITHOUT TIME ZONE      NOT NULL,
  days      weekday []                  NOT NULL,
  "unit"    unit                        NOT NULL,
  step_size INTEGER                     NOT NULL,
  price     NUMERIC(12, 2)              NOT NULL,
  PRIMARY KEY (id)
);
--;;
CREATE TABLE IF NOT EXISTS policies_evses (
  policy_id UUID REFERENCES policies,
  evse_id   VARCHAR(50),
  PRIMARY KEY (policy_id, evse_id)
);
--;
