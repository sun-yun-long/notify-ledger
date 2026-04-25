package com.example.notifyledger;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
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
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int GREEN = 0xFF13B886;
    private static final int GREEN_DARK = 0xFF0C9F73;
    private static final int BG = 0xFFF4F6F7;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF101715;
    private static final int MUTED = 0xFF8A9490;
    private static final int LINE = 0xFFE9EEEC;

    private final String[] tabs = {"首页", "账单", "+", "统计", "我的"};
    private final String[] tabIcons = {"⌂", "▤", "+", "▥", "●"};
    private final String[] defaultCategories = {"餐饮", "购物", "交通", "娱乐", "日用品", "医疗", "其他"};

    private LedgerDatabase database;
    private SharedPreferences prefs;
    private FrameLayout content;
    private LinearLayout nav;
    private int currentTab = 0;
    private String billFilter = "全部";
    private String billSearch = "";
    private String addCategory = "餐饮";
    private boolean addExpense = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = new LedgerDatabase(this);
        prefs = getSharedPreferences("notify_ledger", MODE_PRIVATE);
        setContentView(shell());
        requestNotificationRuntimePermission();
        seedDemoDataIfEmpty();
        showHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        renderCurrent();
    }

    private View shell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));

        nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(6), dp(8), dp(8));
        nav.setBackgroundColor(CARD);
        root.addView(nav, new LinearLayout.LayoutParams(-1, dp(76)));
        renderNav();
        return root;
    }

    private void renderCurrent() {
        if (currentTab == 0) showHome();
        else if (currentTab == 1) showBills();
        else if (currentTab == 2) showAddBill();
        else if (currentTab == 3) showStats();
        else showMine();
    }

    private void renderNav() {
        nav.removeAllViews();
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setOnClickListener(v -> {
                currentTab = index;
                renderCurrent();
            });

            TextView icon = tv(tabIcons[i], index == 2 ? 24 : 20, index == currentTab ? GREEN : 0xFF9BA2A6, Typeface.BOLD);
            icon.setGravity(Gravity.CENTER);
            if (index == 2) {
                icon.setTextColor(Color.WHITE);
                icon.setBackground(oval(GREEN));
                item.addView(icon, new LinearLayout.LayoutParams(dp(48), dp(48)));
            } else {
                item.addView(icon);
                TextView label = tv(tabs[i], 11, index == currentTab ? GREEN : 0xFF9BA2A6, Typeface.BOLD);
                label.setGravity(Gravity.CENTER);
                item.addView(label);
            }
            nav.addView(item, new LinearLayout.LayoutParams(0, -1, 1));
        }
    }

    private void showHome() {
        currentTab = 0;
        renderNav();
        LinearLayout page = page();
        page.addView(header("无感记账", "自动记录每一笔消费", "通知", v -> openNotificationAccessSettings(), "设置", v -> showMine()));
        page.addView(heroCard());

        LinearLayout metrics = new LinearLayout(this);
        metrics.setOrientation(LinearLayout.HORIZONTAL);
        metrics.addView(metric("本月收入(元)", money(getIncomeBudgetCents()), "预算收入", GREEN_DARK), half(true));
        metrics.addView(metric("本月笔数(笔)", String.valueOf(database.allEntries().size()), "自动+手动", 0xFF4B8D80), half(false));
        page.addView(metrics);

        page.addView(section("支出分类统计", "更多", v -> showStats()));
        for (CategoryStat stat : categoryStats()) page.addView(categoryProgress(stat));

        page.addView(section("通知记录", "设置", v -> openNotificationAccessSettings()));
        List<LedgerEntry> entries = database.recentEntries(3);
        if (entries.isEmpty()) page.addView(empty("暂无通知账单"));
        for (LedgerEntry entry : entries) page.addView(notificationCard(entry));
        setPage(page);
    }

    private void showBills() {
        currentTab = 1;
        renderNav();
        LinearLayout page = page();
        page.addView(header("账单", null, "筛选", v -> chooseBillFilter(), "搜索", v -> showSearchDialog()));
        page.addView(filterChip());
        List<LedgerEntry> entries = filteredEntries();
        if (entries.isEmpty()) page.addView(empty("没有匹配的账单"));

        String last = "";
        for (LedgerEntry entry : entries) {
            String day = new SimpleDateFormat("M月d日", Locale.CHINA).format(new Date(entry.occurredAt));
            if (!day.equals(last)) {
                page.addView(groupTitle(isToday(entry.occurredAt) ? "今天  " + day : day));
                last = day;
            }
            page.addView(billRow(entry));
        }
        setPage(page);
    }

    private void showAddBill() {
        currentTab = 2;
        renderNav();
        LinearLayout page = page();
        page.addView(backHeader("新增账单", "保存", v -> saveManualFromPage(page)));
        page.addView(segment(addExpense ? "支出" : "收入", addExpense ? "收入" : "支出", v -> {
            addExpense = !addExpense;
            showAddBill();
        }));

        EditText amount = edit("0.00", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        amount.setTag("amount");
        amount.setTextSize(34);
        amount.setTypeface(Typeface.DEFAULT_BOLD);
        amount.setGravity(Gravity.CENTER);
        page.addView(amount, new LinearLayout.LayoutParams(-1, dp(88)));

        TextView categoryValue = tv(addCategory, 15, TEXT, Typeface.BOLD);
        categoryValue.setTag("category");
        page.addView(formRow("分类", categoryValue));
        page.addView(formRow("账户", tv("支付宝", 15, TEXT, Typeface.NORMAL)));
        page.addView(formRow("时间", tv(new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA).format(new Date()), 15, TEXT, Typeface.NORMAL)));
        EditText merchant = edit("请输入商家名称", InputType.TYPE_CLASS_TEXT);
        merchant.setTag("merchant");
        page.addView(formRow("商家", merchant));
        EditText note = edit("请输入备注", InputType.TYPE_CLASS_TEXT);
        note.setTag("note");
        page.addView(formRow("备注", note));
        page.addView(categoryPicker(categoryValue));

        Button save = greenButton("保存账单");
        save.setOnClickListener(v -> saveManualFromPage(page));
        page.addView(save, buttonParams());
        setPage(page);
        amount.requestFocus();
    }

    private void showStats() {
        currentTab = 3;
        renderNav();
        LinearLayout page = page();
        page.addView(header("统计", null, null, null, null, null));
        page.addView(segment("支出", "收入", v -> toast("当前版本先统计支出")));
        page.addView(monthSwitcher());
        page.addView(label("总支出"));
        page.addView(tv(money(totalCents()), 30, TEXT, Typeface.BOLD));
        page.addView(tv("较上月  -12.5%", 13, GREEN_DARK, Typeface.BOLD));

        LinearLayout chartRow = new LinearLayout(this);
        chartRow.setGravity(Gravity.CENTER_VERTICAL);
        chartRow.setPadding(0, dp(14), 0, dp(16));
        chartRow.addView(new DonutChartView(this, categoryStats()), new LinearLayout.LayoutParams(dp(158), dp(158)));
        LinearLayout legend = new LinearLayout(this);
        legend.setOrientation(LinearLayout.VERTICAL);
        for (CategoryStat stat : categoryStats()) legend.addView(legendRow(stat));
        chartRow.addView(legend, new LinearLayout.LayoutParams(0, -2, 1));
        page.addView(chartRow);

        page.addView(section("支出趋势", "日  周  月", v -> toast("趋势维度切换会在下一版细化")));
        page.addView(new LineChartView(this, database.allEntries()), new LinearLayout.LayoutParams(-1, dp(160)));
        page.addView(summaryCard());
        setPage(page);
    }

    private void showMine() {
        currentTab = 4;
        renderNav();
        LinearLayout page = page();
        page.addView(profileCard());
        page.addView(settings("账户管理", "支付宝 / 微信 / 银行卡", v -> accountDialog()));
        page.addView(settings("预算设置", money(getBudgetCents()), v -> budgetDialog()));
        page.addView(settings("通知设置", isNotificationAccessEnabled() ? "已开启" : "去开启", v -> openNotificationAccessSettings()));
        page.addView(settings("分类管理", "管理支出分类", v -> showCategoryManager()));
        page.addView(settings("导出数据", "CSV", v -> exportCsv()));
        page.addView(settings("关于我们", "v1.0.0", v -> aboutDialog()));
        setPage(page);
    }

    private void showCategoryManager() {
        LinearLayout page = page();
        page.addView(backHeader("分类管理", "编辑", v -> toast("长按分类可删除自定义项")));
        page.addView(segment("支出分类", "收入分类", v -> toast("收入分类会在收入账单完善后启用")));
        for (CategoryStat stat : categoryStatsWithDefaults()) {
            View row = categoryManagerRow(stat);
            row.setOnLongClickListener(v -> {
                deleteCategory(stat.category);
                return true;
            });
            page.addView(row);
        }
        Button add = greenButton("+  添加分类");
        add.setOnClickListener(v -> addCategoryDialog());
        page.addView(add, buttonParams());
        setPage(page);
    }

    private View heroCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(14));
        card.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF12BF88, 0xFF16C7A6}));
        card.getBackground().setAlpha(255);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(136));
        params.setMargins(0, dp(8), 0, dp(10));
        card.setLayoutParams(params);
        card.setClipToOutline(false);
        card.addView(tv("本月支出(元)", 14, Color.WHITE, Typeface.BOLD));
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.addView(tv(money(totalCents()), 30, Color.WHITE, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(new MiniBarsView(this), new LinearLayout.LayoutParams(dp(112), dp(58)));
        card.addView(row, new LinearLayout.LayoutParams(-1, 0, 1));
        card.addView(tv("较上月  -12.5%", 13, 0xDDFFFFFF, Typeface.BOLD));
        card.setBackground(roundGradient());
        return card;
    }

    private GradientDrawable roundGradient() {
        GradientDrawable drawable = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF10B981, 0xFF16C6A3});
        drawable.setCornerRadius(dp(10));
        return drawable;
    }

    private View metric(String title, String value, String desc, int accent) {
        LinearLayout card = card();
        card.addView(tv(title, 12, MUTED, Typeface.NORMAL));
        card.addView(tv(value, 20, TEXT, Typeface.BOLD));
        card.addView(tv(desc, 12, accent, Typeface.BOLD));
        return card;
    }

    private View notificationCard(LedgerEntry entry) {
        LinearLayout card = card();
        card.addView(tv(entry.sourceApp + "  " + new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(entry.occurredAt)), 12, MUTED, Typeface.NORMAL));
        card.addView(tv("付款成功", 14, TEXT, Typeface.BOLD));
        card.addView(tv(entry.formattedAmount(), 23, TEXT, Typeface.BOLD));
        card.addView(tv("商家：" + entry.merchant, 12, 0xFF4E5A55, Typeface.NORMAL));
        card.addView(tv("分类：" + entry.category, 12, 0xFF4E5A55, Typeface.NORMAL));
        return card;
    }

    private View billRow(LedgerEntry entry) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(12), dp(16), dp(12));
        row.setBackgroundColor(CARD);
        row.addView(circleText(categoryInitial(entry.category), categoryColor(entry.category), 34));

        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(dp(12), 0, dp(12), 0);
        mid.addView(tv(entry.sourceApp + "-" + entry.merchant, 15, TEXT, Typeface.BOLD));
        mid.addView(tv(new SimpleDateFormat("HH:mm", Locale.CHINA).format(new Date(entry.occurredAt)), 12, MUTED, Typeface.NORMAL));
        row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(tv("-" + plain(entry.amountCents), 15, TEXT, Typeface.BOLD));
        return row;
    }

    private View categoryProgress(CategoryStat stat) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(7), 0, dp(7));
        row.addView(circleText(categoryInitial(stat.category), stat.color, 32));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(dp(10), 0, dp(10), 0);
        mid.addView(tv(stat.category, 13, TEXT, Typeface.BOLD));
        mid.addView(new ProgressLine(this, stat.color, stat.percent), new LinearLayout.LayoutParams(-1, dp(16)));
        row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.RIGHT);
        right.addView(tv(stat.percent + "%", 12, MUTED, Typeface.NORMAL));
        right.addView(tv(plain(stat.amountCents), 12, TEXT, Typeface.NORMAL));
        row.addView(right, new LinearLayout.LayoutParams(dp(76), -2));
        return row;
    }

    private View legendRow(CategoryStat stat) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView dot = new TextView(this);
        dot.setBackground(oval(stat.color));
        LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(8), dp(8));
        dotParams.setMargins(0, 0, dp(8), 0);
        row.addView(dot, dotParams);
        row.addView(tv(stat.category, 12, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(tv(stat.percent + "%  " + plain(stat.amountCents), 12, TEXT, Typeface.NORMAL));
        return row;
    }

    private View summaryCard() {
        LinearLayout card = card();
        card.addView(tv("本月账单总结", 16, TEXT, Typeface.BOLD));
        card.addView(divider());
        card.addView(summaryLine("支出笔数", String.valueOf(database.allEntries().size())));
        List<CategoryStat> stats = categoryStats();
        if (!stats.isEmpty()) {
            card.addView(summaryLine("支出最多分类", stats.get(0).category + "  " + plain(stats.get(0).amountCents)));
            card.addView(summaryLine("支出最少分类", stats.get(stats.size() - 1).category + "  " + plain(stats.get(stats.size() - 1).amountCents)));
        }
        return card;
    }

    private View profileCard() {
        LinearLayout card = card();
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(18), dp(16), dp(18));
        card.addView(circleText("记", 0xFFB79CFF, 56));
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setPadding(dp(14), 0, 0, 0);
        mid.addView(tv("无感记账", 17, TEXT, Typeface.BOLD));
        mid.addView(tv("记录你的每一笔美好生活", 12, MUTED, Typeface.NORMAL));
        card.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(tv(">", 18, MUTED, Typeface.BOLD));
        return card;
    }

    private View categoryManagerRow(CategoryStat stat) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(8), dp(15), dp(4), dp(15));
        row.addView(circleText(categoryInitial(stat.category), stat.color, 34));
        TextView name = tv(stat.category, 15, TEXT, Typeface.BOLD);
        name.setPadding(dp(12), 0, 0, 0);
        row.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(tv(stat.percent + "%  >", 13, MUTED, Typeface.NORMAL));
        return row;
    }

    private View settings(String title, String value, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(15), dp(16), dp(15));
        row.setBackgroundColor(CARD);
        row.setOnClickListener(listener);
        row.addView(tv(title, 15, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(tv(value, 12, MUTED, Typeface.NORMAL));
        return row;
    }

    private View formRow(String label, View value) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), dp(11), dp(6), dp(11));
        row.setBackgroundColor(CARD);
        row.addView(tv(label, 14, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(dp(76), -2));
        row.addView(value, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private View categoryPicker(TextView selected) {
        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(16), 0, dp(16));
        for (String category : categories()) {
            TextView item = circleText(categoryInitial(category) + "\n" + category, categoryColor(category), 62);
            item.setTextSize(11);
            item.setOnClickListener(v -> {
                addCategory = category;
                selected.setText(category);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(66), dp(66));
            params.setMargins(0, 0, dp(10), 0);
            row.addView(item, params);
        }
        scroll.addView(row);
        return scroll;
    }

    private View filterChip() {
        TextView chip = tv((billFilter.equals("全部") ? "全部账户" : billFilter) + "  " + (billSearch.isEmpty() ? "" : "搜索: " + billSearch), 12, 0xFF66716D, Typeface.BOLD);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        chip.setPadding(dp(12), 0, dp(12), 0);
        chip.setBackground(round(0xFFEFF3F2, dp(18), 0xFFEFF3F2));
        chip.setOnClickListener(v -> chooseBillFilter());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(34));
        params.setMargins(0, 0, 0, dp(8));
        chip.setLayoutParams(params);
        return chip;
    }

    private View segment(String selected, String other, View.OnClickListener otherClick) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(dp(18), 0, dp(18), dp(14));
        TextView a = segmentButton(selected, true);
        TextView b = segmentButton(other, false);
        b.setOnClickListener(otherClick);
        row.addView(a, new LinearLayout.LayoutParams(0, dp(34), 1));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(34), 1);
        params.setMargins(dp(8), 0, 0, 0);
        row.addView(b, params);
        return row;
    }

    private TextView segmentButton(String text, boolean selected) {
        TextView view = tv(text, 14, selected ? Color.WHITE : 0xFF56615C, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setBackground(round(selected ? GREEN : CARD, dp(18), selected ? GREEN : 0xFFDDE4E1));
        return view;
    }

    private View monthSwitcher() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        row.setPadding(0, 0, 0, dp(8));
        row.addView(tv("<", 22, MUTED, Typeface.BOLD));
        TextView month = tv("本月", 16, TEXT, Typeface.BOLD);
        month.setGravity(Gravity.CENTER);
        row.addView(month, new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(tv(">", 22, MUTED, Typeface.BOLD));
        return row;
    }

    private View section(String title, String action, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(18), 0, dp(8));
        row.addView(tv(title, 16, TEXT, Typeface.BOLD), new LinearLayout.LayoutParams(0, -2, 1));
        TextView right = tv(action, 12, MUTED, Typeface.NORMAL);
        right.setOnClickListener(listener);
        row.addView(right);
        return row;
    }

    private View header(String title, String subtitle, String left, View.OnClickListener leftClick, String right, View.OnClickListener rightClick) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(22), 0, dp(14));
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.addView(tv(title, 22, TEXT, Typeface.BOLD));
        if (subtitle != null) titles.addView(tv(subtitle, 12, MUTED, Typeface.NORMAL));
        row.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        if (left != null) row.addView(action(left, leftClick));
        if (right != null) row.addView(action(right, rightClick));
        return row;
    }

    private View backHeader(String title, String action, View.OnClickListener actionClick) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(22), 0, dp(14));
        TextView back = tv("<", 24, TEXT, Typeface.BOLD);
        back.setOnClickListener(v -> showMine());
        row.addView(back, new LinearLayout.LayoutParams(dp(44), -2));
        TextView center = tv(title, 18, TEXT, Typeface.BOLD);
        center.setGravity(Gravity.CENTER);
        row.addView(center, new LinearLayout.LayoutParams(0, -2, 1));
        TextView act = tv(action, 14, TEXT, Typeface.BOLD);
        act.setGravity(Gravity.RIGHT);
        act.setOnClickListener(actionClick);
        row.addView(act, new LinearLayout.LayoutParams(dp(58), -2));
        return row;
    }

    private TextView action(String value, View.OnClickListener click) {
        TextView view = tv(value, 13, TEXT, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setOnClickListener(click);
        view.setBackground(round(CARD, dp(17), LINE));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(52), dp(34));
        params.setMargins(dp(8), 0, 0, 0);
        view.setLayoutParams(params);
        return view;
    }

    private TextView groupTitle(String value) {
        TextView title = tv(value, 13, 0xFF66716D, Typeface.BOLD);
        title.setPadding(0, dp(14), 0, dp(8));
        return title;
    }

    private TextView label(String value) {
        TextView view = tv(value, 13, MUTED, Typeface.NORMAL);
        view.setPadding(dp(4), dp(8), 0, dp(2));
        return view;
    }

    private View summaryLine(String left, String right) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, dp(9), 0, dp(9));
        row.addView(tv(left, 14, 0xFF58635F, Typeface.NORMAL), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(tv(right, 14, TEXT, Typeface.BOLD));
        return row;
    }

    private View divider() {
        View view = new View(this);
        view.setBackgroundColor(LINE);
        view.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        return view;
    }

    private LinearLayout page() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(18), 0, dp(18), dp(22));
        return page;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(round(CARD, dp(10), CARD));
        card.setElevation(dp(2));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(7), 0, dp(7));
        card.setLayoutParams(params);
        return card;
    }

    private View empty(String message) {
        LinearLayout card = card();
        TextView text = tv(message, 14, MUTED, Typeface.NORMAL);
        text.setGravity(Gravity.CENTER);
        card.addView(text, new LinearLayout.LayoutParams(-1, dp(88)));
        return card;
    }

    private Button greenButton(String value) {
        Button button = new Button(this);
        button.setText(value);
        button.setTextColor(Color.WHITE);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT_BOLD);
        button.setAllCaps(false);
        button.setBackground(round(GREEN, dp(10), GREEN));
        return button;
    }

    private EditText edit(String hint, int inputType) {
        EditText edit = new EditText(this);
        edit.setHint(hint);
        edit.setInputType(inputType);
        edit.setSingleLine(true);
        edit.setTextSize(15);
        edit.setTextColor(TEXT);
        edit.setHintTextColor(0xFFB4BAB8);
        edit.setBackgroundColor(Color.TRANSPARENT);
        return edit;
    }

    private TextView tv(String value, int sp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sp);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        view.setIncludeFontPadding(true);
        return view;
    }

    private TextView circleText(String value, int color, int sizeDp) {
        TextView view = tv(value, 13, Color.WHITE, Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setIncludeFontPadding(false);
        view.setLines(value.contains("\n") ? 2 : 1);
        view.setBackground(oval(color));
        view.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return view;
    }

    private LinearLayout.LayoutParams half(boolean rightMargin) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, -2, 1);
        if (rightMargin) params.setMargins(0, 0, dp(10), 0);
        return params;
    }

    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.setMargins(0, dp(18), 0, dp(20));
        return params;
    }

    private void setPage(View view) {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(false);
        scroll.addView(view);
        content.removeAllViews();
        content.addView(scroll);
    }

    private GradientDrawable round(int color, int radius, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        if (stroke != color) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private GradientDrawable oval(int color) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(color);
        return drawable;
    }

    private void saveManualFromPage(View root) {
        EditText amount = findTagged(root, "amount");
        EditText merchant = findTagged(root, "merchant");
        EditText note = findTagged(root, "note");
        long cents = parseCents(amount == null ? "" : amount.getText().toString());
        if (cents <= 0) {
            toast("请输入金额");
            return;
        }
        database.insertManual(cents, merchant == null ? "" : merchant.getText().toString(), addCategory, addExpense ? "支付宝" : "收入账户", System.currentTimeMillis(), note == null ? "" : note.getText().toString());
        hideKeyboard(amount);
        toast("账单已保存");
        showBills();
    }

    @SuppressWarnings("unchecked")
    private <T extends View> T findTagged(View root, String tag) {
        if (root == null) return null;
        Object value = root.getTag();
        if (tag.equals(value)) return (T) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = findTagged(group.getChildAt(i), tag);
                if (found != null) return found;
            }
        }
        return null;
    }

    private void chooseBillFilter() {
        List<String> options = new ArrayList<>();
        options.add("全部");
        options.addAll(categories());
        new AlertDialog.Builder(this)
                .setTitle("筛选分类")
                .setItems(options.toArray(new String[0]), (dialog, which) -> {
                    billFilter = options.get(which);
                    showBills();
                })
                .show();
    }

    private void showSearchDialog() {
        EditText input = edit("输入商家、分类或来源", InputType.TYPE_CLASS_TEXT);
        input.setText(billSearch);
        new AlertDialog.Builder(this)
                .setTitle("搜索账单")
                .setView(input)
                .setNegativeButton("清空", (d, w) -> {
                    billSearch = "";
                    showBills();
                })
                .setPositiveButton("搜索", (d, w) -> {
                    billSearch = input.getText().toString().trim();
                    showBills();
                })
                .show();
    }

    private void budgetDialog() {
        EditText input = edit("输入月预算", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        input.setText(String.format(Locale.CHINA, "%.2f", getBudgetCents() / 100.0));
        new AlertDialog.Builder(this)
                .setTitle("预算设置")
                .setView(input)
                .setPositiveButton("保存", (d, w) -> {
                    prefs.edit().putLong("budget_cents", parseCents(input.getText().toString())).apply();
                    showMine();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void accountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("账户管理")
                .setMessage("当前支持：支付宝、微信支付、银行卡、手动记录。\n后续会把账户做成可新增、可隐藏。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private void aboutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("无感记账")
                .setMessage("通知监听 + 手动补录的本地记账应用。\n交易数据保存在手机本地。")
                .setPositiveButton("好的", null)
                .show();
    }

    private void addCategoryDialog() {
        EditText input = edit("输入分类名称", InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle("添加分类")
                .setView(input)
                .setPositiveButton("添加", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.length() == 0) return;
                    List<String> categories = categories();
                    if (!categories.contains(name)) {
                        categories.add(name);
                        saveCategories(categories);
                    }
                    showCategoryManager();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteCategory(String name) {
        for (String item : defaultCategories) {
            if (item.equals(name)) {
                toast("默认分类不能删除");
                return;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle("删除分类")
                .setMessage("确定删除“" + name + "”？已有账单不会被删除。")
                .setPositiveButton("删除", (d, w) -> {
                    List<String> categories = categories();
                    categories.remove(name);
                    saveCategories(categories);
                    showCategoryManager();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void exportCsv() {
        try {
            File dir = getExternalFilesDir(null);
            if (dir == null) dir = getFilesDir();
            File file = new File(dir, "notify-ledger-export.csv");
            FileWriter writer = new FileWriter(file);
            writer.write("time,amount,category,merchant,source,raw\n");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA);
            for (LedgerEntry entry : database.allEntries()) {
                writer.write(csv(format.format(new Date(entry.occurredAt))) + ",");
                writer.write(csv(plain(entry.amountCents)) + ",");
                writer.write(csv(entry.category) + ",");
                writer.write(csv(entry.merchant) + ",");
                writer.write(csv(entry.sourceApp) + ",");
                writer.write(csv(entry.rawText) + "\n");
            }
            writer.close();
            toast("已导出：" + file.getAbsolutePath());
        } catch (Exception e) {
            toast("导出失败：" + e.getMessage());
        }
    }

    private String csv(String value) {
        if (value == null) value = "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private List<LedgerEntry> filteredEntries() {
        List<LedgerEntry> result = new ArrayList<>();
        for (LedgerEntry entry : database.recentEntries(300)) {
            if (!"全部".equals(billFilter) && !billFilter.equals(entry.category)) continue;
            if (!billSearch.isEmpty()) {
                String haystack = (entry.merchant + " " + entry.category + " " + entry.sourceApp + " " + entry.rawText).toLowerCase(Locale.ROOT);
                if (!haystack.contains(billSearch.toLowerCase(Locale.ROOT))) continue;
            }
            result.add(entry);
        }
        return result;
    }

    private List<CategoryStat> categoryStatsWithDefaults() {
        Map<String, CategoryStat> map = new LinkedHashMap<>();
        for (CategoryStat stat : categoryStats()) map.put(stat.category, stat);
        for (String category : categories()) {
            if (!map.containsKey(category)) map.put(category, new CategoryStat(category, 0, 0, categoryColor(category)));
        }
        return new ArrayList<>(map.values());
    }

    private List<CategoryStat> categoryStats() {
        Map<String, Long> totals = new LinkedHashMap<>();
        long total = 0;
        for (LedgerEntry entry : database.allEntries()) {
            String category = entry.category == null ? "其他" : entry.category;
            totals.put(category, totals.containsKey(category) ? totals.get(category) + entry.amountCents : entry.amountCents);
            total += entry.amountCents;
        }
        if (total == 0) {
            totals.put("餐饮", 124000L);
            totals.put("购物", 98000L);
            totals.put("交通", 52000L);
            totals.put("娱乐", 28000L);
            totals.put("其他", 22600L);
            total = 324600L;
        }
        List<CategoryStat> stats = new ArrayList<>();
        for (Map.Entry<String, Long> entry : totals.entrySet()) {
            int percent = Math.round(entry.getValue() * 100f / total);
            stats.add(new CategoryStat(entry.getKey(), entry.getValue(), percent, categoryColor(entry.getKey())));
        }
        Collections.sort(stats, (a, b) -> Long.compare(b.amountCents, a.amountCents));
        return stats;
    }

    private List<String> categories() {
        String saved = prefs.getString("categories", "");
        List<String> result = new ArrayList<>();
        if (saved.trim().isEmpty()) {
            Collections.addAll(result, defaultCategories);
            return result;
        }
        for (String item : saved.split("\\|")) if (!item.trim().isEmpty()) result.add(item.trim());
        return result;
    }

    private void saveCategories(List<String> categories) {
        StringBuilder builder = new StringBuilder();
        for (String category : categories) {
            if (builder.length() > 0) builder.append("|");
            builder.append(category);
        }
        prefs.edit().putString("categories", builder.toString()).apply();
    }

    private void seedDemoDataIfEmpty() {
        if (database.allEntries().size() > 0 || prefs.getBoolean("seeded", false)) return;
        database.insertManual(3250, "餐饮", "餐饮", "支付宝", System.currentTimeMillis() - 3600_000L, "支付成功");
        database.insertManual(4580, "超市", "购物", "微信支付", System.currentTimeMillis() - 7200_000L, "付款成功");
        database.insertManual(19900, "招商银行(1234)", "其他", "招商银行", System.currentTimeMillis() - 10800_000L, "消费提醒");
        database.insertManual(1860, "打车", "交通", "微信支付", System.currentTimeMillis() - 26 * 3600_000L, "付款成功");
        prefs.edit().putBoolean("seeded", true).apply();
    }

    private int categoryColor(String category) {
        if ("餐饮".equals(category)) return 0xFFFF6B6B;
        if ("购物".equals(category)) return 0xFFFFA726;
        if ("交通".equals(category)) return 0xFF4AA3FF;
        if ("娱乐".equals(category)) return 0xFF9B7BFF;
        if ("日用品".equals(category) || "生活".equals(category)) return 0xFF7AC943;
        if ("医疗".equals(category)) return 0xFFFF5E64;
        return 0xFFB7BEC3;
    }

    private String categoryInitial(String category) {
        if (category == null || category.length() == 0) return "其";
        return category.substring(0, 1);
    }

    private long totalCents() {
        long total = database.totalCentsThisMonth();
        return total > 0 ? total : 324600;
    }

    private long getBudgetCents() {
        return prefs.getLong("budget_cents", 500000);
    }

    private long getIncomeBudgetCents() {
        return prefs.getLong("income_cents", 840000);
    }

    private String money(long cents) {
        return NumberFormat.getCurrencyInstance(Locale.CHINA).format(cents / 100.0).replace("￥", "");
    }

    private String plain(long cents) {
        return String.format(Locale.CHINA, "%,.2f", cents / 100.0);
    }

    private long parseCents(String value) {
        try {
            return Math.round(Double.parseDouble(value.trim()) * 100);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean isToday(long time) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(time);
        return now.get(Calendar.YEAR) == then.get(Calendar.YEAR) && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR);
    }

    private boolean isNotificationAccessEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (enabled == null) return false;
        ComponentName component = new ComponentName(this, NotificationLedgerService.class);
        return enabled.toLowerCase(Locale.ROOT).contains(component.flattenToString().toLowerCase(Locale.ROOT));
    }

    private void openNotificationAccessSettings() {
        startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
    }

    private void requestNotificationRuntimePermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
        }
    }

    private void hideKeyboard(View view) {
        if (view == null) return;
        InputMethodManager manager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_SHORT).show();
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
            int size = Math.min(getWidth(), getHeight()) - dp(18);
            RectF rect = new RectF(dp(9), dp(9), dp(9) + size, dp(9) + size);
            float start = -90;
            for (CategoryStat stat : stats) {
                paint.setColor(stat.color);
                float sweep = Math.max(6, stat.percent * 3.6f);
                canvas.drawArc(rect, start, sweep, true, paint);
                start += sweep;
            }
            paint.setColor(CARD);
            canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, size * 0.30f, paint);
            paint.setColor(TEXT);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextSize(dp(16));
            canvas.drawText(money(totalCents()), getWidth() / 2f, getHeight() / 2f + dp(5), paint);
        }
    }

    private class MiniBarsView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        MiniBarsView(Activity context) { super(context); }
        @Override
        protected void onDraw(Canvas canvas) {
            paint.setColor(0x77FFFFFF);
            int[] bars = {20, 32, 46, 38, 52, 41, 30, 44, 28};
            for (int i = 0; i < bars.length; i++) {
                float left = dp(4) + i * dp(11);
                canvas.drawRoundRect(left, getHeight() - dp(bars[i]), left + dp(7), getHeight(), dp(3), dp(3), paint);
            }
        }
    }

    private class ProgressLine extends View {
        private final int color;
        private final int percent;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ProgressLine(Activity context, int color, int percent) {
            super(context);
            this.color = color;
            this.percent = percent;
        }
        @Override
        protected void onDraw(Canvas canvas) {
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
            paint.setColor(0xFFE1E8E5);
            paint.setStrokeWidth(dp(1));
            for (int i = 1; i <= 4; i++) canvas.drawLine(0, getHeight() * i / 5f, getWidth(), getHeight() * i / 5f, paint);
            float[] values = values();
            float max = 1;
            for (float v : values) max = Math.max(max, v);
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
        private float[] values() {
            if (entries.isEmpty()) return new float[]{80, 420, 650, 500, 160, 380, 640, 410, 620, 360, 180};
            float[] result = new float[11];
            for (int i = 0; i < entries.size(); i++) result[i % result.length] += entries.get(i).amountCents / 100f;
            return result;
        }
    }
}
