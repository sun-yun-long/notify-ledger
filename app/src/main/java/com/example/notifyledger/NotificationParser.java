package com.example.notifyledger;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NotificationParser {
    private static final Pattern AMOUNT_PATTERN = Pattern.compile(
            "(?:(?:¥|￥|RMB|人民币)\\s*([0-9]+(?:\\.[0-9]{1,2})?)|([0-9]+(?:\\.[0-9]{1,2})?)\\s*元)",
            Pattern.CASE_INSENSITIVE
    );

    private static final String[] SPEND_KEYWORDS = {
            "支付", "付款", "消费", "支出", "扣款", "交易成功", "收款方", "商户"
    };

    private static final String[] REFUND_OR_INCOME_KEYWORDS = {
            "退款", "退回", "收入", "收款到账", "转入", "还款成功"
    };

    public ParsedNotification parse(
            String packageName,
            String appName,
            String title,
            String text,
            long postTime
    ) {
        String raw = join(title, text).trim();
        if (raw.length() == 0 || isLikelyIncome(raw) || !looksLikeSpend(raw)) {
            return null;
        }

        Matcher matcher = AMOUNT_PATTERN.matcher(raw);
        long amountCents = 0L;
        while (matcher.find()) {
            String amountText = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            long candidate = toCents(amountText);
            if (candidate > amountCents) {
                amountCents = candidate;
            }
        }

        if (amountCents <= 0L) {
            return null;
        }

        String merchant = guessMerchant(raw, appName);
        String category = guessCategory(raw, merchant);

        return new ParsedNotification(
                postTime > 0L ? postTime : System.currentTimeMillis(),
                amountCents,
                merchant,
                category,
                safe(appName, "未知来源"),
                safe(packageName, "unknown"),
                raw
        );
    }

    private boolean looksLikeSpend(String raw) {
        for (String keyword : SPEND_KEYWORDS) {
            if (raw.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLikelyIncome(String raw) {
        for (String keyword : REFUND_OR_INCOME_KEYWORDS) {
            if (raw.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private long toCents(String value) {
        try {
            return new BigDecimal(value).movePointRight(2).longValue();
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private String guessMerchant(String raw, String fallback) {
        String[] markers = {"向", "在", "商户", "收款方", "付款给"};
        for (String marker : markers) {
            int index = raw.indexOf(marker);
            if (index >= 0 && index + marker.length() < raw.length()) {
                String tail = raw.substring(index + marker.length());
                String merchant = tail.split("[，,。；;\\s]")[0].trim();
                merchant = merchant.replace("：", "").replace(":", "");
                if (merchant.length() >= 2 && !merchant.matches(".*[0-9].*")) {
                    return merchant;
                }
            }
        }
        return safe(fallback, "未识别商户");
    }

    private String guessCategory(String raw, String merchant) {
        String text = (raw + " " + merchant).toLowerCase(Locale.ROOT);
        if (containsAny(text, "美团", "饿了么", "餐", "咖啡", "奶茶", "饭", "kfc", "mcdonald")) {
            return "餐饮";
        }
        if (containsAny(text, "滴滴", "地铁", "公交", "铁路", "高铁", "航旅", "机票", "打车")) {
            return "交通";
        }
        if (containsAny(text, "京东", "淘宝", "天猫", "拼多多", "超市", "便利店", "购物")) {
            return "购物";
        }
        if (containsAny(text, "话费", "电费", "水费", "燃气", "物业")) {
            return "生活";
        }
        if (containsAny(text, "会员", "订阅", "云服务", "视频", "音乐")) {
            return "订阅";
        }
        return "其他";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String join(String title, String text) {
        return safe(title, "") + " " + safe(text, "");
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().length() == 0 ? fallback : value.trim();
    }
}
