package com.example.notifyledger;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LedgerEntry {
    public long id;
    public long occurredAt;
    public long amountCents;
    public String merchant;
    public String category;
    public String sourceApp;
    public String sourcePackage;
    public String rawText;

    public LedgerEntry(
            long id,
            long occurredAt,
            long amountCents,
            String merchant,
            String category,
            String sourceApp,
            String sourcePackage,
            String rawText
    ) {
        this.id = id;
        this.occurredAt = occurredAt;
        this.amountCents = amountCents;
        this.merchant = merchant;
        this.category = category;
        this.sourceApp = sourceApp;
        this.sourcePackage = sourcePackage;
        this.rawText = rawText;
    }

    public String formattedAmount() {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.CHINA);
        return format.format(amountCents / 100.0);
    }

    public String formattedDate() {
        SimpleDateFormat format = new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA);
        return format.format(new Date(occurredAt));
    }
}
