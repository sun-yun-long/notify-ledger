package com.example.notifyledger;

import java.util.Locale;

public class ParsedNotification {
    public final long occurredAt;
    public final long amountCents;
    public final String merchant;
    public final String category;
    public final String sourceApp;
    public final String sourcePackage;
    public final String rawText;

    public ParsedNotification(
            long occurredAt,
            long amountCents,
            String merchant,
            String category,
            String sourceApp,
            String sourcePackage,
            String rawText
    ) {
        this.occurredAt = occurredAt;
        this.amountCents = amountCents;
        this.merchant = merchant;
        this.category = category;
        this.sourceApp = sourceApp;
        this.sourcePackage = sourcePackage;
        this.rawText = rawText;
    }

    public String dedupeKey() {
        long minuteBucket = occurredAt / 60000L;
        return String.format(
                Locale.US,
                "%s:%d:%d:%s",
                sourcePackage,
                minuteBucket,
                amountCents,
                rawText.hashCode()
        );
    }
}
