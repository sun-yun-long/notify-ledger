package com.example.notifyledger;

import android.app.Notification;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class NotificationLedgerService extends NotificationListenerService {
    private final NotificationParser parser = new NotificationParser();

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null || sbn.getNotification() == null) {
            return;
        }

        Notification notification = sbn.getNotification();
        CharSequence title = notification.extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = notification.extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigText = notification.extras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        String body = bigText != null ? bigText.toString() : textToString(text);

        ParsedNotification parsed = parser.parse(
                sbn.getPackageName(),
                resolveAppName(sbn.getPackageName()),
                textToString(title),
                body,
                sbn.getPostTime()
        );

        if (parsed != null) {
            new LedgerDatabase(getApplicationContext()).insertIfNew(parsed);
        }
    }

    private String resolveAppName(String packageName) {
        PackageManager packageManager = getPackageManager();
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            CharSequence label = packageManager.getApplicationLabel(info);
            return label == null ? packageName : label.toString();
        } catch (Exception ignored) {
            return packageName;
        }
    }

    private String textToString(CharSequence value) {
        return value == null ? "" : value.toString();
    }
}
