CREATE TABLE IF NOT EXISTS vehicles (
 id UUID PRIMARY KEY,
 vin VARCHAR(17) NOT NULL UNIQUE,
 brand VARCHAR(40) NOT NULL,
 model VARCHAR(60) NOT NULL,
 price NUMERIC(15,2) NOT NULL CHECK (price > 0),
 status VARCHAR(16) NOT NULL CHECK (status IN ('AVAILABLE','RESERVED','SOLD')),
 version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_vehicle_brand_id ON vehicles(brand,id);
CREATE TABLE IF NOT EXISTS reservation_results (
 reference UUID PRIMARY KEY,
 vehicle_id UUID NOT NULL,
 buyer_alias VARCHAR(40) NOT NULL,
 processed_at TIMESTAMP WITH TIME ZONE NOT NULL,
 outcome VARCHAR(16) NOT NULL CHECK(outcome IN ('ACCEPTED','REJECTED')),
 reason VARCHAR(40),
 applied_price NUMERIC(15,2),
 CHECK ((outcome='ACCEPTED' AND applied_price IS NOT NULL AND reason IS NULL)
     OR (outcome='REJECTED' AND applied_price IS NULL AND reason IS NOT NULL))
);
-- No FK on vehicle_id: nonexistent units must also retain their rejected result.
