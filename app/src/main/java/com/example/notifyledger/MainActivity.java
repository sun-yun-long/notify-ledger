package com.example.notifyledger;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int GREEN = 0xFF12B886;
    private static final int GREEN_DARK = 0xFF0CA678;
    private static final int BG = 0xFFF5F7F8;
    private static final int TEXT = 0xFF111816;
    private static final int MUTED = 0xFF8D9592;

    private final String[] tabs = {"首页", "账单", "+", "统计", "我的"};
    private final String[] categories = {"餐饮", "购物", "交通", "娱乐", "日用品", "医疗", "其他"};
    private LedgerDatabase database;
    private LinearLayout root;
    private FrameLayout content;
    private LinearLayout bottomNav;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new LedgerDatabase(this);
        setContentView(createShell());
        requestNotificationRuntimePermission();
        showHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderCurrentTab();
    }

    private View createShell() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1
        ));

        bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setGravity(Gravity.CENTER);
        bottomNav.setPadding(dp(8), dp(6), dp(8), dp(8));
        bottomNav.setBackgroundColor(Color.WHITE);
        root.addView(bottomNav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(72)
        ));
        renderBottomNav();
        return root;
    }

    private void renderCurrentTab() {
        if (currentTab == 0) showHome();
        else if (currentTab == 1) showBills();
        else if (currentTab == 2) showAddBill();
        else if (currentTab == 3) showStats();
        else showMine();
    }

    private void renderBottomNav() {
        bottomNav.removeAllViews();
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            TextView item = new TextView(this);
            item.setGravity(Gravity.CENTER);
            item.setText(tabs[i]);
            item.setTextSize(index == 2 ? 24 : 12);
            item.setTypeface(Typeface.DEFAULT_BOLD);
            item.setTextColor(index == currentTab ? GREEN : 0xFF9AA1A6);
            if (index == 2) {
                item.setTextColor(Color.WHITE);
                item.setBackground(circle(GREEN));
            }
            item.setOnClickListener(v -> {
                currentTab = index;
                renderBottomNav();
                renderCurrentTab();
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, index == 2 ? dp(50) : dp(54), 1);
            params.setMargins(index == 2 ? dp(8) : 0, 0, index == 2 ? dp(8) : 0, 0);
            bottomNav.addView(item, params);
        }
    }

    private void showHome() {
        currentTab = 0;
        renderBottomNav();
        LinearLayout page = page();
        page.addView(topBar("无感记账", "自动记录每一笔消费", "铃", "设置"));
        page.addView(spendHero());

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.addView(metricCard("本月收入(元)", "8,400.00", "较上月  +8.3%", true), weightWithRightMargin());
        metrics.addView(metricCard("本月笔数(笔)", String.valueOf(database.allEntries().size()), "较上月  -4", false), weightNoMargin());
        page.addView(metrics);

        page.addView(sectionHeader("支出分类统计", "更多 >"));
        for (CategoryStat stat : categoryStats()) {
            page.addView(categoryProgress(stat));
        }

        page.addView(sectionHeader("通知记录", "筛选"));
        List<LedgerEntry> entries = database.recentEntries(3);
        if (entries.isEmpty()) {
            page.addView(emptyCard("还没有消费通知，点底部 + 可以先新增一笔账单"));
        } else {
            for (LedgerEntry entry : entries) {
                page.addView(notificationCard(entry));
            }
        }
        setScrollable(page);
    }

    private void showBills() {
        currentTab = 1;
        renderBottomNav();
        LinearLayout page = page();
        page.addView(topBar("账单", null, "筛选", "搜索"));
        page.addView(chip("全部账户"));

        List<LedgerEntry> entries = database.recentEntries(100);
        String lastDate = "";
        if (entries.isEmpty()) {
            page.addView(emptyCard("暂无账单记录"));
        }
        for (LedgerEntry entry : entries) {
            String date = new SimpleDateFormat("M月d日", Locale.CHINA).format(new Date(entry.occurredAt));
            if (!date.equals(lastDate)) {
                page.addView(groupTitle(isToday(entry.occurredAt) ? "今天  " + date : date));
                lastDate = date;
            }
            page.addView(billRow(entry));
        }
        setScrollable(page);
    }

    private void showStats() {
        currentTab = 3;
        renderBottomNav();
        LinearLayout page = page();
        page.addView(topBar("统计", null, null, null));
        page.addView(segmented("支出", "收入"));
        page.addView(monthSwitcher());

        TextView label = text("总支出", 13, MUTED, Typeface.NORMAL);
        label.setPadding(dp(6), dp(18), 0, 0);
        page.addView(label);
        page.addView(text(money(totalCentsThisMonth()), 28, TEXT, Typeface.BOLD));
        page.addView(text("较上月  -12.5%", 13, GREEN_DARK, Typeface.BOLD));

        LinearLayout chartRow = new LinearLayout(this);
        chartRow.setOrientation(LinearLayout.HORIZONTAL);
        DonutChartView donut = new DonutChartView(this, categoryStats());
        chartRow.addView(donut, new LinearLayout.LayoutParams(dp(150), dp(150)));
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.VERTICAL);
        for (CategoryStat stat : categoryStats()) {
            legend.addView(legendRow(stat));
        }
        chartRow.addView(legend, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        page.addView(chartRow);

        page.addView(sectionHeader("支出趋势", "日   周   月"));
        LineChartView line = new LineChartView(this, database.allEntries());
        page.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(160)));
        page.addView(summaryCard());
        setScrollable(page);
    }

    private void showAddBill() {
        currentTab = 2;
        renderBottomNav();
        LinearLayout page = page();
        page.addView(addTopBar());
        page.addView(segmented("支出", "收入"));

        EditText amount = edit("0.00", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setTextSize(30);
        amount.setGravity(Gravity.CENTER);
        amount.setTypeface(Typeface.DEFAULT_BOLD);
        page.addView(amount, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(78)));

        TextView selectedCategory = text("餐饮", 15, TEXT, Typeface.BOLD);
        EditText merchant = edit("请输入商家名称", InputType.TYPE_CLASS_TEXT);
        EditText note = edit("请输入备注", InputType.TYPE_CLASS_TEXT);

        page.addView(formRow("分类", selectedCategory));
        page.addView(formRow("账户", text("支付宝", 15, TEXT, Typeface.NORMAL)));
        page.addView(formRow("时间", text(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date()), 15, TEXT, Typeface.NORMAL)));
        page.addView(formRow("商家", merchant));
        page.addView(formRow("备注", note));

        page.addView(categoryPicker(selectedCategory));
        Button save = greenButton("保存账单");
        save.setOnClickListener(v -> {
            long cents = parseAmount(amount.getText().toString());
            if (cents <= 0) {
                Toast.makeText(this, "请输入金额", Toast.LENGTH_SHORT).show();
                return;
            }
            database.insertManual(cents, merchant.getText().toString(), selectedCategory.getText().toString(), "支付宝", System.currentTimeMillis(), note.getText().toString());
            Toast.makeText(this, "已保存", Toast.LENGTH_SHORT).show();
            showBills();
        });
        page.addView(save, fullButtonParams());
        setScrollable(page);
    }

    private void showMine() {
        currentTab = 4;
        renderBottomNav();
        LinearLayout page = page();
        page.addView(profileCard());
        page.addView(settingsRow("账户管理", ">", v -> showBills()));
        page.addView(settingsRow("预算设置", ">", v -> toastSoon()));
        page.addView(settingsRow("通知设置", isNotificationAccessEnabled() ? "已开启" : "去开启", v -> openNotificationAccessSettings()));
        page.addView(settingsRow("分类管理", ">", v -> showCategoryManager()));
        page.addView(settingsRow("导出数据", ">", v -> toastSoon()));
        page.addView(settingsRow("关于我们", "v1.0.0", v -> toastSoon()));
        setScrollable(page);
    }

    private void showCategoryManager() {
        LinearLayout page = page();
        page.addView(backTopBar("分类管理", "编辑", v -> showMine()));
        page.addView(segmented("支出分类", "收入分类"));
        for (CategoryStat stat : categoryStatsWithDefaults()) {
            page.addView(categoryManagerRow(stat));
        }
        Button add = greenButton("+  添加分类");
        add.setOnClickListener(v -> toastSoon());
        page.addView(add, fullButtonParams());
        setScrollable(page);
    }

    private LinearLayout spendHero() {
        LinearLayout card = card();
        card.setPadding(dp(16), dp(16), dp(16), dp(12));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF0DBE7F, 0xFF16C6A2});
        bg.setCornerRadius(dp(10));
        card.setBackground(bg);
        card.addView(text("本月支出(元)", 14, Color.WHITE, Typeface.BOLD));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView amount = text(money(totalCentsThisMonth()), 30, Color.WHITE, Typeface.BOLD);
        row.addView(amount, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        MiniBarsView bars = new MiniBarsView(this);
        row.addView(bars, new LinearLayout.LayoutParams(dp(110), dp(54)));
        card.addView(row);
        card.addView(text("较上月  -12.5%", 14, 0xDFFFFFFF, Typeface.BOLD));
        return card;
    }

    private LinearLayout metricCard(String title, String value, String delta, boolean positive) {
        LinearLayout card = card();
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.addView(text(title, 12, MUTED, Typeface.NORMAL));
        card.addView(text(value, 20, TEXT, Typeface.BOLD));
        card.addView(text(delta, 12, positive ? GREEN_DARK : 0xFF4A9182, Typeface.BOLD));
        return card;
    }

    private View categoryProgress(CategoryStat stat) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(8), 0, dp(8));

        TextView icon = circleText(stat.category.substring(0, 1), stat.color, 32);
        row.addView(icon);

        LinearLayout middle = new LinearLayout(this);
        middle.setOrientation(LinearLayout.VERTICAL);
        middle.setPadding(dp(10), 0, dp(10), 0);
        middle.addView(text(stat.category, 13, TEXT, Typeface.BOLD));
        ProgressBarLine bar = new ProgressBarLine(this, stat.color, stat.percent);
        middle.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(14)));
        row.addView(middle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);
        right.addView(text(stat.percent + "%", 12, MUTED, Typeface.NORMAL));
        right.addView(text(formatPlain(stat.amountCents), 12, TEXT, Typeface.NORMAL));
        row.addView(right, new LinearLayout.LayoutParams(dp(76), ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View notificationCard(LedgerEntry entry) {
        LinearLayout card = card();
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        card.addView(text(entry.sourceApp + "  " + new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(entry.occurredAt)), 12, MUTED, Typeface.NORMAL));
        card.addView(text("付款成功", 14, TEXT, Typeface.BOLD));
        card.addView(text(entry.formattedAmount(), 22, TEXT, Typeface.BOLD));
        card.addView(text("商家：" + entry.merchant, 12, 0xFF515B56, Typeface.NORMAL));
        card.addView(text("支付方式：" + entry.sourceApp, 12, 0xFF515B56, Typeface.NORMAL));
        return card;
    }

    private View billRow(LedgerEntry entry) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setBackgroundColor(Color.WHITE);
        row.addView(circleText(entry.category.substring(0, 1), categoryColor(entry.category), 34));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), 0, dp(12), 0);
        content.addView(text(entry.sourceApp + "-" + entry.merchant, 15, TEXT, Typeface.BOLD));
        content.addView(text(new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(entry.occurredAt)), 12, MUTED, Typeface.NORMAL));
        row.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text("-" + formatPlain(entry.amountCents), 15, TEXT, Typeface.BOLD));
        return row;
    }

    private View categoryManagerRow(CategoryStat stat) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(14), dp(4), dp(14));
        row.addView(circleText(stat.category.substring(0, 1), stat.color, 34));
        TextView name = text(stat.category, 15, TEXT, Typeface.BOLD);
        name.setPadding(dp(12), 0, 0, 0);
        row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text(stat.percent + "%  >", 13, MUTED, Typeface.NORMAL));
        return row;
    }

    private View legendRow(CategoryStat stat) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(4), 0, dp(4));
        TextView dot = new TextView(this);
        dot.setBackground(circle(stat.color));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotParams.setMargins(0, 0, dp(8), 0);
        row.addView(dot, dotParams);
        row.addView(text(stat.category, 12, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text(stat.percent + "%  " + formatPlain(stat.amountCents), 12, TEXT, Typeface.NORMAL));
        return row;
    }

    private View profileCard() {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(18), dp(16), dp(18));
        card.addView(circleText("无", 0xFFDEC2FF, 56));
        LinearLayout textBox = new LinearLayout(this);
        textBox.setOrientation(LinearLayout.VERTICAL);
        textBox.setPadding(dp(14), 0, 0, 0);
        textBox.addView(text("无感记账", 17, TEXT, Typeface.BOLD));
        textBox.addView(text("记录你的每一笔美好生活", 12, MUTED, Typeface.NORMAL));
        card.addView(textBox, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        card.addView(text(">", 18, MUTED, Typeface.BOLD));
        return card;
    }

    private View settingsRow(String title, String value, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(18), dp(16), dp(18), dp(16));
        row.setBackgroundColor(Color.WHITE);
        row.setOnClickListener(listener);
        row.addView(text(title, 15, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text(value, 13, MUTED, Typeface.NORMAL));
        return row;
    }

    private View summaryCard() {
        LinearLayout card = card();
        card.addView(text("2024年5月账单总结", 15, TEXT, Typeface.BOLD));
        card.addView(text("支出笔数     " + database.allEntries().size(), 14, 0xFF515B56, Typeface.NORMAL));
        List<CategoryStat> stats = categoryStats();
        if (!stats.isEmpty()) {
            card.addView(text("支出最多分类   " + stats.get(0).category + "  " + formatPlain(stats.get(0).amountCents), 14, 0xFF515B56, Typeface.NORMAL));
            card.addView(text("支出最少分类   " + stats.get(stats.size() - 1).category + "  " + formatPlain(stats.get(stats.size() - 1).amountCents), 14, 0xFF515B56, Typeface.NORMAL));
        }
        return card;
    }

    private TextView topBar(String title, String subtitle, String leftAction, String rightAction) {
        TextView bar = text(title + actionText(leftAction, rightAction), 22, TEXT, Typeface.BOLD);
        bar.setPadding(0, dp(22), 0, subtitle == null ? dp(14) : 0);
        if (subtitle != null) {
            bar.setText(title + "\n" + subtitle + actionText(leftAction, rightAction));
            bar.setTextSize(20);
        }
        return bar;
    }

    private View addTopBar() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(22), 0, dp(16));
        TextView back = text("<", 24, TEXT, Typeface.BOLD);
        back.setOnClickListener(v -> showHome());
        row.addView(back, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView title = text("新增账单", 18, TEXT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text("保存", 15, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private View backTopBar(String title, String action, View.OnClickListener backClick) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(22), 0, dp(16));
        TextView back = text("<", 24, TEXT, Typeface.BOLD);
        back.setOnClickListener(backClick);
        row.addView(back, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView titleView = text(title, 18, TEXT, Typeface.BOLD);
        titleView.setGravity(Gravity.CENTER);
        row.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text(action, 14, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(dp(54), ViewGroup.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private String actionText(String leftAction, String rightAction) {
        if (leftAction == null && rightAction == null) return "";
        String left = leftAction == null ? "" : leftAction;
        String right = rightAction == null ? "" : rightAction;
        return "        " + left + "   " + right;
    }

    private View segmented(String selected, String other) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setPadding(0, 0, 0, dp(14));
        box.addView(segment(selected, true), new LinearLayout.LayoutParams(0, dp(34), 1));
        box.addView(segment(other, false), new LinearLayout.LayoutParams(0, dp(34), 1));
        return box;
    }

    private TextView segment(String title, boolean selected) {
        TextView view = text(title, 14, selected ? Color.WHITE : 0xFF56615C, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(round(selected ? GREEN : Color.WHITE, dp(18), selected ? GREEN : 0xFFDDE3E1));
        return view;
    }

    private View monthSwitcher() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, dp(4), 0, dp(8));
        row.addView(text("<", 22, MUTED, Typeface.BOLD));
        TextView month = text("本月", 16, TEXT, Typeface.BOLD);
        month.setGravity(Gravity.CENTER);
        row.addView(month, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text(">", 22, MUTED, Typeface.BOLD));
        return row;
    }

    private View categoryPicker(TextView selectedCategory) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(16), 0, dp(16));
        for (String category : categories) {
            TextView item = circleText(category.substring(0, 1) + "\n" + category, categoryColor(category), 58);
            item.setTextSize(11);
            item.setOnClickListener(v -> selectedCategory.setText(category));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(62), dp(62));
            params.setMargins(0, 0, dp(10), 0);
            row.addView(item, params);
        }
        scroll.addView(row);
        return scroll;
    }

    private View formRow(String label, View value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), dp(11), dp(6), dp(11));
        row.setBackgroundColor(Color.WHITE);
        row.addView(text(label, 14, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(dp(76), ViewGroup.LayoutParams.WRAP_CONTENT));
        row.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private EditText edit(String hint, int inputType) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setTextColor(TEXT);
        edit.setHintTextColor(0xFFB4BAB8);
        edit.setTextSize(15);
        edit.setSingleLine(true);
        edit.setBackgroundColor(Color.TRANSPARENT);
        return edit;
    }

    private View sectionHeader(String title, String action) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(20), 0, dp(8));
        row.addView(text(title, 16, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(text(action, 12, MUTED, Typeface.NORMAL));
        return row;
    }

    private TextView groupTitle(String value) {
        TextView view = text(value, 13, 0xFF66716D, Typeface.BOLD);
        view.setPadding(0, dp(14), 0, dp(8));
        return view;
    }

    private TextView chip(String text) {
        TextView chip = text(text + "⌄", 12, 0xFF66716D, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER);
        chip.setBackground(round(0xFFEFF3F2, dp(18), 0xFFEFF3F2));
        chip.setPadding(dp(12), dp(7), dp(12), dp(7));
        return chip;
    }

    private TextView circleText(String text, int color, int sizeDp) {
        TextView view = text(text, 13, Color.WHITE, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(circle(color));
        view.setIncludeFontPadding(false);
        view.setLines(text.contains("\n") ? 2 : 1);
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return view;
    }

    private Button greenButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setBackground(round(GREEN, dp(10), GREEN));
        return button;
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), 0, dp(18), dp(20));
        return page;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, dp(8), 0, dp(8));
        card.setLayoutParams(params);
        card.setBackground(round(Color.WHITE, dp(10), Color.WHITE));
        card.setElevation(dp(2));
        return card;
    }

    private TextView text(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(true);
        return view;
    }

    private LinearLayout.LayoutParams weightWithRightMargin() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        params.setMargins(0, 0, dp(10), 0);
        return params;
    }

    private LinearLayout.LayoutParams weightNoMargin() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout.LayoutParams fullButtonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        params.setMargins(0, dp(18), 0, dp(16));
        return params;
    }

    private void setScrollable(View page) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(page);
        content.removeAllViews();
        content.addView(scroll);
    }

    private View emptyCard(String message) {
        LinearLayout card = card();
        TextView text = text(message, 14, MUTED, Typeface.NORMAL);
        text.setGravity(Gravity.CENTER);
        card.addView(text, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(90)));
        return card;
    }

    private GradientDrawable round(int color, int radius, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (strokeColor != color) drawable.setStroke(dp(1), strokeColor);
        return drawable;
    }

    private GradientDrawable circle(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private int categoryColor(String category) {
        if ("餐饮".equals(category)) return 0xFFFF6B6B;
        if ("购物".equals(category)) return 0xFFFFA726;
        if ("交通".equals(category)) return 0xFF4AA3FF;
        if ("娱乐".equals(category)) return 0xFF9B7BFF;
        if ("生活".equals(category) || "日用品".equals(category)) return 0xFF7AC943;
        if ("医疗".equals(category)) return 0xFFFF5E64;
        return 0xFFB6BEC3;
    }

    private List<CategoryStat> categoryStatsWithDefaults() {
        List<CategoryStat> stats = categoryStats();
        Map<String, CategoryStat> byName = new LinkedHashMap<>();
        for (CategoryStat stat : stats) byName.put(stat.category, stat);
        for (String category : categories) {
            if (!byName.containsKey(category)) {
                byName.put(category, new CategoryStat(category, 0L, 0, categoryColor(category)));
            }
        }
        return new ArrayList<>(byName.values());
    }

    private List<CategoryStat> categoryStats() {
        Map<String, Long> totals = new LinkedHashMap<>();
        long total = 0L;
        for (LedgerEntry entry : database.allEntries()) {
            String category = entry.category == null ? "其他" : entry.category;
            totals.put(category, totals.containsKey(category) ? totals.get(category) + entry.amountCents : entry.amountCents);
            total += entry.amountCents;
        }
        if (total == 0L) {
            totals.put("餐饮", 124000L);
            totals.put("购物", 98000L);
            totals.put("交通", 52000L);
            totals.put("娱乐", 28000L);
            totals.put("其他", 22600L);
            total = 324600L;
        }
        List<CategoryStat> stats = new ArrayList<>();
        for (Map.Entry<String, Long> entry : totals.entrySet()) {
            int percent = total == 0 ? 0 : Math.round(entry.getValue() * 100f / total);
            stats.add(new CategoryStat(entry.getKey(), entry.getValue(), percent, categoryColor(entry.getKey())));
        }
        Collections.sort(stats, (a, b) -> Long.compare(b.amountCents, a.amountCents));
        return stats;
    }

    private long totalCentsThisMonth() {
        long total = database.totalCentsThisMonth();
        return total > 0 ? total : 324600L;
    }

    private String money(long cents) {
        return NumberFormat.getCurrencyInstance(Locale.CHINA).format(cents / 100.0).replace("￥", "");
    }

    private String formatPlain(long cents) {
        return String.format(Locale.CHINA, "%,.2f", cents / 100.0);
    }

    private long parseAmount(String value) {
        try {
            return Math.round(Double.parseDouble(value.trim()) * 100);
        } catch (Exception ignored) {
            return 0L;
        }
    }

    private boolean isToday(long time) {
        Calendar a = Calendar.getInstance();
        Calendar b = Calendar.getInstance();
        b.setTimeInMillis(time);
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR);
    }

    private void toastSoon() {
        Toast.makeText(this, "下一版接入", Toast.LENGTH_SHORT).show();
    }

    private boolean isNotificationAccessEnabled() {
        String enabledListeners = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabledListeners == null) return false;
        ComponentName componentName = new ComponentName(this, NotificationLedgerService.class);
        return enabledListeners.toLowerCase(Locale.ROOT).contains(componentName.flattenToString().toLowerCase(Locale.ROOT));
    }

    private void openNotificationAccessSettings() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void requestNotificationRuntimePermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class CategoryStat {
        final String category;
        final long amountCents;
        final int percent;
        final int color;

        CategoryStat(String category, long amountCents, int percent, int color) {
            this.category = category;
            this.amountCents = amountCents;
            this.percent = percent;
            this.color = color;
        }
    }

    private class DonutChartView extends View {
        private final List<CategoryStat> stats;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        DonutChartView(Activity context, List<CategoryStat> stats) {
            super(context);
            this.stats = stats;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int size = Math.min(getWidth(), getHeight()) - dp(18);
            RectF rect = new RectF(dp(9), dp(9), dp(9) + size, dp(9) + size);
            float start = -90;
            for (CategoryStat stat : stats) {
                paint.setColor(stat.color);
                float sweep = Math.max(8, stat.percent * 3.6f);
                canvas.drawArc(rect, start, sweep, true, paint);
                start += sweep;
            }
            paint.setColor(Color.WHITE);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, size * 0.28f, paint);
            paint.setColor(TEXT);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(dp(16));
            canvas.drawText(money(totalCentsThisMonth()), getWidth() / 2f, getHeight() / 2f + dp(5), paint);
        }
    }

    private class MiniBarsView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        MiniBarsView(Activity context) {
            super(context);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(0x77FFFFFF);
            int[] bars = {22, 34, 44, 30, 50, 38, 28, 42, 26};
            int w = dp(7);
            for (int i = 0; i < bars.length; i++) {
                float left = dp(4) + i * dp(11);
                canvas.drawRoundRect(left, getHeight() - dp(bars[i]), left + w, getHeight(), dp(3), dp(3), paint);
            }
        }
    }

    private class ProgressBarLine extends View {
        private final int color;
        private final int percent;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        ProgressBarLine(Activity context, int color, int percent) {
            super(context);
            this.color = color;
            this.percent = percent;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setStrokeWidth(dp(4));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(0xFFE9EFED);
            canvas.drawLine(0, getHeight() / 2f, getWidth(), getHeight() / 2f, paint);
            paint.setColor(color);
            canvas.drawLine(0, getHeight() / 2f, getWidth() * percent / 100f, getHeight() / 2f, paint);
        }
    }

    private class LineChartView extends View {
        private final List<LedgerEntry> entries;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        LineChartView(Activity context, List<LedgerEntry> entries) {
            super(context);
            this.entries = entries;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(0xFFE1E8E5);
            paint.setStrokeWidth(dp(1));
            for (int i = 1; i <= 4; i++) {
                float y = getHeight() * i / 5f;
                canvas.drawLine(0, y, getWidth(), y, paint);
            }

            float[] values = dailyValues();
            float max = 1f;
            for (float value : values) max = Math.max(max, value);

            Path path = new Path();
            for (int i = 0; i < values.length; i++) {
                float x = getWidth() * i / (values.length - 1f);
                float y = getHeight() - dp(18) - (getHeight() - dp(36)) * values[i] / max;
                if (i == 0) path.moveTo(x, y);
                else path.lineTo(x, y);
            }
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(3));
            paint.setColor(GREEN);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int i = 0; i < values.length; i++) {
                float x = getWidth() * i / (values.length - 1f);
                float y = getHeight() - dp(18) - (getHeight() - dp(36)) * values[i] / max;
                canvas.drawCircle(x, y, dp(3), paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }

        private float[] dailyValues() {
            float[] values = {80, 420, 650, 500, 160, 380, 640, 410, 620, 360, 180};
            if (entries.isEmpty()) return values;
            float[] real = new float[11];
            for (int i = 0; i < entries.size(); i++) {
                real[i % real.length] += entries.get(i).amountCents / 100f;
            }
            return real;
        }
    }
}
