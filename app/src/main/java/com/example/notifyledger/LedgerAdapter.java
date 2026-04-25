package com.example.notifyledger;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class LedgerAdapter extends BaseAdapter {
    private final Context context;
    private List<LedgerEntry> entries = new ArrayList<>();

    public LedgerAdapter(Context context) {
        this.context = context;
    }

    public void setEntries(List<LedgerEntry> entries) {
        this.entries = entries;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return entries.size();
    }

    @Override
    public Object getItem(int position) {
        return entries.get(position);
    }

    @Override
    public long getItemId(int position) {
        return entries.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            holder = createViewHolder();
            convertView = holder.root;
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        LedgerEntry entry = entries.get(position);
        holder.icon.setText(shortName(entry.category));
        holder.icon.setBackground(circle(categoryColor(entry.category)));
        holder.merchant.setText(entry.merchant);
        holder.amount.setText("-" + entry.formattedAmount().replace("￥", ""));
        holder.meta.setText(entry.sourceApp + "  " + entry.formattedDate());
        holder.raw.setText(entry.rawText);
        return convertView;
    }

    private ViewHolder createViewHolder() {
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.HORIZONTAL);
        root.setGravity(Gravity.CENTER_VERTICAL);
        root.setPadding(dp(18), dp(12), dp(18), dp(12));
        root.setMinimumHeight(dp(82));
        root.setBackgroundColor(Color.WHITE);

        TextView icon = new TextView(context);
        icon.setGravity(Gravity.CENTER);
        icon.setTextColor(Color.WHITE);
        icon.setTextSize(14);
        icon.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(34), dp(34));
        iconParams.setMargins(0, 0, dp(12), 0);
        root.addView(icon, iconParams);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        TextView merchant = new TextView(context);
        merchant.setTextColor(0xFF14211D);
        merchant.setTextSize(15);
        merchant.setTypeface(Typeface.DEFAULT_BOLD);
        content.addView(merchant);

        TextView meta = new TextView(context);
        meta.setTextColor(0xFF8D9592);
        meta.setTextSize(12);
        meta.setPadding(0, dp(4), 0, 0);
        content.addView(meta);

        TextView raw = new TextView(context);
        raw.setTextColor(0xFF7A8581);
        raw.setTextSize(11);
        raw.setMaxLines(1);
        raw.setPadding(0, dp(4), 0, 0);
        content.addView(raw);

        TextView amount = new TextView(context);
        amount.setTextColor(0xFF141B18);
        amount.setTextSize(15);
        amount.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(amount);

        return new ViewHolder(root, icon, merchant, amount, meta, raw);
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

    private String shortName(String category) {
        if (category == null || category.length() == 0) {
            return "其";
        }
        return category.substring(0, 1);
    }

    private int dp(int value) {
        return (int) (value * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static class ViewHolder {
        final LinearLayout root;
        final TextView icon;
        final TextView merchant;
        final TextView amount;
        final TextView meta;
        final TextView raw;

        ViewHolder(LinearLayout root, TextView icon, TextView merchant, TextView amount, TextView meta, TextView raw) {
            this.root = root;
            this.icon = icon;
            this.merchant = merchant;
            this.amount = amount;
            this.meta = meta;
            this.raw = raw;
        }
    }
}
