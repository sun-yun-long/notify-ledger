package com.example.notifyledger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class LedgerDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notify-ledger.db";
    private static final int DATABASE_VERSION = 1;

    public LedgerDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE ledger_entries (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "occurred_at INTEGER NOT NULL, " +
                        "amount_cents INTEGER NOT NULL, " +
                        "merchant TEXT NOT NULL, " +
                        "category TEXT NOT NULL, " +
                        "source_app TEXT NOT NULL, " +
                        "source_package TEXT NOT NULL, " +
                        "raw_text TEXT NOT NULL, " +
                        "dedupe_key TEXT NOT NULL UNIQUE" +
                        ")"
        );
        db.execSQL("CREATE INDEX idx_ledger_entries_occurred_at ON ledger_entries(occurred_at DESC)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS ledger_entries");
        onCreate(db);
    }

    public boolean insertIfNew(ParsedNotification parsed) {
        ContentValues values = new ContentValues();
        values.put("occurred_at", parsed.occurredAt);
        values.put("amount_cents", parsed.amountCents);
        values.put("merchant", parsed.merchant);
        values.put("category", parsed.category);
        values.put("source_app", parsed.sourceApp);
        values.put("source_package", parsed.sourcePackage);
        values.put("raw_text", parsed.rawText);
        values.put("dedupe_key", parsed.dedupeKey());

        try {
            return getWritableDatabase().insertOrThrow("ledger_entries", null, values) > 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    public List<LedgerEntry> recentEntries(int limit) {
        List<LedgerEntry> entries = new ArrayList<>();
        Cursor cursor = getReadableDatabase().query(
                "ledger_entries",
                null,
                null,
                null,
                null,
                null,
                "occurred_at DESC",
                String.valueOf(limit)
        );

        try {
            while (cursor.moveToNext()) {
                entries.add(new LedgerEntry(
                        cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("occurred_at")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("amount_cents")),
                        cursor.getString(cursor.getColumnIndexOrThrow("merchant")),
                        cursor.getString(cursor.getColumnIndexOrThrow("category")),
                        cursor.getString(cursor.getColumnIndexOrThrow("source_app")),
                        cursor.getString(cursor.getColumnIndexOrThrow("source_package")),
                        cursor.getString(cursor.getColumnIndexOrThrow("raw_text"))
                ));
            }
        } finally {
            cursor.close();
        }

        return entries;
    }

    public List<LedgerEntry> allEntries() {
        return recentEntries(1000);
    }

    public long totalCentsThisMonth() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 1);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);

        Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COALESCE(SUM(amount_cents), 0) FROM ledger_entries WHERE occurred_at >= ?",
                new String[]{String.valueOf(calendar.getTimeInMillis())}
        );

        try {
            if (cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
            return 0L;
        } finally {
            cursor.close();
        }
    }

    public boolean insertManual(long amountCents, String merchant, String category, String account, long occurredAt, String note) {
        ParsedNotification parsed = new ParsedNotification(
                occurredAt,
                amountCents,
                merchant == null || merchant.trim().isEmpty() ? category : merchant.trim(),
                category,
                account == null || account.trim().isEmpty() ? "手动记录" : account.trim(),
                "manual.entry",
                note == null || note.trim().isEmpty() ? "手动新增账单" : note.trim()
        );
        return insertIfNew(parsed);
    }
}
