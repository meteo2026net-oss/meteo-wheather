# Supabase setup for the Android weather app

The configured project is `vktcvkwqszehgmhqjrkb`.

## Local client configuration

Copy `.env.example` to `.env` and set:

```properties
SUPABASE_URL=https://vktcvkwqszehgmhqjrkb.supabase.co
SUPABASE_PUBLISHABLE_KEY=YOUR_SUPABASE_PUBLISHABLE_KEY
```

The root `.env` is git-ignored. Only the publishable key belongs in the Android client.
Never put the PostgreSQL password, a Supabase secret key, or a service-role key in the app,
BuildConfig, source control, or a distributed APK.

## Database migration

Apply `supabase/migrations/202611060001_station_observations.sql` to the linked project
using the Supabase CLI or SQL Editor. The migration creates station metadata and verified
observations with range constraints and read-only RLS policies for mobile clients.

The app-side `SupabaseStationReadingsDataSource` only fetches rows where
`is_verified = true`. It does not write observations. Ingestion must happen from a
trusted server using a secret stored outside the mobile app.

## CLI

Run each command separately from the repository root:

```bash
supabase login
supabase init
supabase link --project-ref vktcvkwqszehgmhqjrkb
supabase db push
```

Do not run `supabase init` if the repository already has a `supabase/config.toml`.
