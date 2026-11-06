-- Verified Syrian station metadata and observations.
-- The mobile app is intentionally read-only. Trusted ingestion must use a server-side
-- secret in a protected environment, never the Android publishable key.

create table if not exists public.weather_stations (
    station_id text primary key,
    name_ar text not null,
    governorate text not null,
    latitude double precision not null check (latitude between -90 and 90),
    longitude double precision not null check (longitude between -180 and 180),
    is_active boolean not null default true,
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now()
);

create table if not exists public.station_readings (
    id uuid primary key default gen_random_uuid(),
    station_id text not null references public.weather_stations(station_id) on delete restrict,
    observed_at timestamptz not null,
    temperature_c double precision check (temperature_c between -100 and 70),
    relative_humidity double precision check (relative_humidity between 0 and 100),
    pressure_hpa double precision check (pressure_hpa between 800 and 1100),
    wind_speed_kmh double precision check (wind_speed_kmh between 0 and 300),
    wind_direction_deg double precision check (wind_direction_deg between 0 and 360),
    precipitation_mm double precision check (precipitation_mm >= 0),
    source_name text not null,
    source_reference text,
    is_verified boolean not null default false,
    received_at timestamptz not null default now(),
    constraint station_readings_has_measurement check (
        temperature_c is not null
        or relative_humidity is not null
        or pressure_hpa is not null
        or wind_speed_kmh is not null
        or wind_direction_deg is not null
        or precipitation_mm is not null
    ),
    constraint station_readings_source_unique unique (station_id, observed_at, source_name)
);

create index if not exists station_readings_verified_latest_idx
    on public.station_readings (observed_at desc)
    where is_verified = true;

create index if not exists station_readings_station_latest_idx
    on public.station_readings (station_id, observed_at desc)
    where is_verified = true;

alter table public.weather_stations enable row level security;
alter table public.station_readings enable row level security;

drop policy if exists "Public can read active weather stations" on public.weather_stations;
create policy "Public can read active weather stations"
    on public.weather_stations for select
    to anon, authenticated
    using (is_active = true);

drop policy if exists "Public can read verified station readings" on public.station_readings;
create policy "Public can read verified station readings"
    on public.station_readings for select
    to anon, authenticated
    using (is_verified = true);

grant select on public.weather_stations to anon, authenticated;
grant select on public.station_readings to anon, authenticated;

-- No INSERT, UPDATE, or DELETE policies are granted to mobile clients.
