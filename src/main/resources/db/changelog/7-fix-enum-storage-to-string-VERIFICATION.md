# Verification runbook: 7-fix-enum-storage-to-string

Companion to `7-fix-enum-storage-to-string.yaml`. Run the pre-deploy queries before
this changelog applies; run the post-deploy queries immediately after.

## Pre-deploy (read-only)

Confirm every existing row only holds the expected raw ordinal values before the
backfill runs. Any non-zero count below is a stop-ship condition -- it means a row
holds something the migration's `CASE ... ELSE` will silently pass through unchanged,
which would then fail at read time under `@Enumerated(EnumType.STRING)`.

```sql
SELECT count(*) FROM accounts WHERE currency::varchar NOT IN ('0','1','2');
SELECT count(*) FROM categories WHERE type::varchar NOT IN ('0','1','2');
SELECT count(*) FROM transactions WHERE transactiontype NOT IN ('0','1','2');
```

## Post-deploy (read-only, run within 5 minutes of deploy)

Confirm every row now holds a valid enum name. Any non-zero count below means a row
slipped through un-converted and will throw `IllegalArgumentException: No enum constant ...`
the next time it's read.

```sql
SELECT count(*) FROM accounts WHERE currency NOT IN ('ARS','USD','EUR');
SELECT count(*) FROM categories WHERE type NOT IN ('INCOME','EXPENSE','TRANSFER');
SELECT count(*) FROM transactions WHERE transactiontype NOT IN ('INCOME','EXPENSE','TRANSFER');
```

Also watch application logs for `IllegalArgumentException: No enum constant` for the
first 24h post-deploy.

## Rollback

Each changeset in `7-fix-enum-storage-to-string.yaml` carries a `rollback:` block that
reverses it individually. Liquibase's `rollback-count`/`rollback-to-tag` runs them in
reverse order (7-6 down to 7-1), which correctly restores each column's values before
narrowing its type back. No data is lost on rollback unless new rows were written by
the new (string-enum) application code after cutover and before rollback -- those still
map cleanly through the reverse `CASE` as long as no new enum constants were added
since this migration ran.
