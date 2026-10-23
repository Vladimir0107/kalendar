package ru.planner.zadachi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/** Monthly salary summary: hours, earnings, percent of max salary; pay settings. */
public class SalaryActivity extends Activity {

    private static final String[] MONTHS = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};

    private YearMonth shown = YearMonth.now();
    private TextView monthTitle, summary, percentTv, earnedTv, settingsInfo;
    private ProgressBar bar;
    private LinearLayout dayList;

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Зарплата");

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14), dp(10), dp(14), dp(24));
        sv.addView(root);

        // Month switcher
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button prev = new Button(this);
        prev.setText("‹");
        prev.setTextSize(22);
        prev.setOnClickListener(v -> { shown = shown.minusMonths(1); render(); });
        Button next = new Button(this);
        next.setText("›");
        next.setTextSize(22);
        next.setOnClickListener(v -> { shown = shown.plusMonths(1); render(); });
        monthTitle = new TextView(this);
        monthTitle.setTextSize(22);
        monthTitle.setTypeface(Typeface.DEFAULT_BOLD);
        monthTitle.setTextColor(Color.BLACK);
        monthTitle.setGravity(Gravity.CENTER);
        header.addView(prev, new LinearLayout.LayoutParams(dp(56), dp(56)));
        header.addView(monthTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(next, new LinearLayout.LayoutParams(dp(56), dp(56)));
        root.addView(header);

        // Summary card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFFF1F8E9);
        bg.setCornerRadius(dp(12));
        bg.setStroke(dp(1), 0xFFC5E1A5);
        card.setBackground(bg);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.setMargins(0, dp(8), 0, dp(8));
        root.addView(card, clp);

        earnedTv = new TextView(this);
        earnedTv.setTextSize(30);
        earnedTv.setTypeface(Typeface.DEFAULT_BOLD);
        earnedTv.setTextColor(0xFF2E7D32);
        card.addView(earnedTv);

        percentTv = new TextView(this);
        percentTv.setTextSize(16);
        percentTv.setTextColor(0xFF333333);
        card.addView(percentTv);

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(16));
        blp.setMargins(0, dp(8), 0, dp(8));
        card.addView(bar, blp);

        summary = new TextView(this);
        summary.setTextSize(15);
        summary.setTextColor(0xFF333333);
        card.addView(summary);

        settingsInfo = new TextView(this);
        settingsInfo.setTextColor(0xFF666666);
        settingsInfo.setPadding(dp(4), dp(4), 0, 0);
        root.addView(settingsInfo);

        Button settings = new Button(this);
        settings.setText("Настройки оплаты");
        settings.setAllCaps(false);
        settings.setOnClickListener(v -> showSettings());
        root.addView(settings);

        TextView listHead = new TextView(this);
        listHead.setText("Отработанные дни");
        listHead.setTextSize(18);
        listHead.setTypeface(Typeface.DEFAULT_BOLD);
        listHead.setTextColor(Color.BLACK);
        listHead.setPadding(0, dp(16), 0, dp(4));
        root.addView(listHead);

        dayList = new LinearLayout(this);
        dayList.setOrientation(LinearLayout.VERTICAL);
        root.addView(dayList);

        TextView hint = new TextView(this);
        hint.setText("Часы вводятся в календаре: нажми на день → «Работа и зарплата».");
        hint.setTextColor(0xFF888888);
        hint.setPadding(0, dp(12), 0, 0);
        root.addView(hint);

        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        monthTitle.setText(MONTHS[shown.getMonthValue() - 1] + " " + shown.getYear());
        boolean piece = Store.isPiecePay(this);
        DateTimeFormatter f = DateTimeFormatter.ofPattern("EE, d MMM", Store.RU);

        double hours = 0, pieces = 0, earned = 0;
        int worked = 0, shifts = 0;
        dayList.removeAllViews();
        for (int i = 1; i <= shown.lengthOfMonth(); i++) {
            LocalDate d = shown.atDay(i);
            if (Store.isWorkDay(this, d)) shifts++;
            Store.Work w = Store.getWork(this, d);
            if (w.isEmpty()) continue;
            worked++;
            hours += w.hours;
            pieces += w.pieces;
            double pay = Store.dayPay(this, w);
            earned += pay;

            StringBuilder sb = new StringBuilder(d.format(f));
            if (w.in != null || w.out != null) {
                sb.append("  ").append(w.in == null ? "?" : w.in).append("–").append(w.out == null ? "?" : w.out);
            }
            if (w.hours > 0) sb.append("  ").append(Store.num(w.hours)).append(" ч");
            if (piece && w.pieces > 0) sb.append("  ").append(Store.num(w.pieces)).append(" шт");
            sb.append("  →  ").append(Store.money(pay));

            TextView row = new TextView(this);
            row.setText(sb);
            row.setTextSize(15);
            row.setTextColor(Color.BLACK);
            row.setPadding(dp(4), dp(10), dp(4), dp(10));
            row.setOnClickListener(v -> startActivity(
                    new Intent(this, DayActivity.class).putExtra("date", d.toString())));
            dayList.addView(row);
        }
        if (worked == 0) {
            TextView empty = new TextView(this);
            empty.setText("В этом месяце часы ещё не отмечены.");
            empty.setTextColor(0xFF888888);
            dayList.addView(empty);
        }

        double max = Store.maxForMonth(this, shifts);
        earnedTv.setText(Store.money(earned));
        if (max > 0) {
            double pct = earned / max * 100;
            percentTv.setText(String.format(Store.RU, "%.1f %% от максимума (%s)", pct, Store.money(max)));
            bar.setProgress((int) Math.min(1000, Math.round(pct * 10)));
            int color = pct >= 90 ? 0xFF43A047 : pct >= 50 ? 0xFFFBC02D : 0xFFE53935;
            bar.setProgressTintList(ColorStateList.valueOf(color));
        } else {
            percentTv.setText("Максимум не задан — открой «Настройки оплаты»");
            bar.setProgress(0);
        }

        StringBuilder s = new StringBuilder();
        s.append("Отработано: ").append(Store.num(hours)).append(" ч, дней: ").append(worked);
        if (piece) s.append("\nСделано: ").append(Store.num(pieces)).append(" шт");
        s.append("\nСмен по графику: ").append(shifts);
        if (max > 0 && earned < max) s.append("\nДо максимума: ").append(Store.money(max - earned));
        summary.setText(s);

        settingsInfo.setText(piece
                ? "Сдельная: " + Store.money(Store.getD(this, Store.RATE_PIECE)) + " за штуку"
                : "Почасовая: " + Store.money(Store.getD(this, Store.RATE_HOUR)) + " в час");
    }

    // ---------- settings dialog ----------

    private EditText field(LinearLayout box, String label, double value) {
        TextView l = new TextView(this);
        l.setText(label);
        l.setTextColor(0xFF555555);
        l.setPadding(0, dp(10), 0, 0);
        box.addView(l);
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setSingleLine(true);
        if (value > 0) e.setText(Store.num(value));
        box.addView(e);
        return e;
    }

    private void showSettings() {
        ScrollView sv = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), dp(8));
        sv.addView(box);

        RadioGroup type = new RadioGroup(this);
        RadioButton hourly = new RadioButton(this);
        hourly.setText("Почасовая");
        hourly.setId(1);
        RadioButton piece = new RadioButton(this);
        piece.setText("Сдельная (за штуки)");
        piece.setId(2);
        type.addView(hourly);
        type.addView(piece);
        type.check(Store.isPiecePay(this) ? 2 : 1);
        box.addView(type);

        EditText rateHour = field(box, "Ставка за час, ₽", Store.getD(this, Store.RATE_HOUR));
        EditText planHours = field(box, "Часов в смене по плану", Store.getD(this, Store.PLAN_HOURS));
        EditText ratePiece = field(box, "Цена за штуку, ₽ (сдельная)", Store.getD(this, Store.RATE_PIECE));
        EditText planPieces = field(box, "Штук за смену по плану (сдельная)", Store.getD(this, Store.PLAN_PIECES));
        EditText breakMin = field(box, "Неоплачиваемый перерыв, минут", Store.getD(this, Store.BREAK_MIN));
        EditText maxMonth = field(box,
                "Максимальная ЗП за месяц, ₽\n(пусто — посчитать: смены по графику × план × ставка)",
                Store.getD(this, Store.MAX_MONTH));

        new AlertDialog.Builder(this)
                .setTitle("Настройки оплаты")
                .setView(sv)
                .setPositiveButton("Сохранить", (d, w) -> {
                    Store.setPiecePay(this, type.getCheckedRadioButtonId() == 2);
                    Store.setD(this, Store.RATE_HOUR, Store.parseNum(rateHour.getText().toString()));
                    Store.setD(this, Store.PLAN_HOURS, Store.parseNum(planHours.getText().toString()));
                    Store.setD(this, Store.RATE_PIECE, Store.parseNum(ratePiece.getText().toString()));
                    Store.setD(this, Store.PLAN_PIECES, Store.parseNum(planPieces.getText().toString()));
                    Store.setD(this, Store.BREAK_MIN, Store.parseNum(breakMin.getText().toString()));
                    Store.setD(this, Store.MAX_MONTH, Store.parseNum(maxMonth.getText().toString()));
                    render();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
