package ru.planner.zadachi;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/** Month calendar screen. */
public class MainActivity extends Activity {

    private static final String[] MONTHS = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
    private static final String[] WEEKDAYS = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};

    static final int C_FREE = 0xFFFFFFFF;
    static final int C_GREEN = 0xFF81C784;
    static final int C_YELLOW = 0xFFFFEB3B;
    static final int C_RED = 0xFFEF5350;
    static final int C_SHIFT = 0xFF1565C0;

    private YearMonth shown = YearMonth.now();
    private LinearLayout grid;
    private TextView monthTitle;
    private TextView shiftInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Notifier.createChannel(this);
        Notifier.scheduleAll(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
        setContentView(buildUi());
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    static int colorFor(int count) {
        if (count == 0) return C_FREE;
        if (count <= 3) return C_GREEN;
        if (count <= 6) return C_YELLOW;
        return C_RED;
    }

    private View buildUi() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(10), dp(10), dp(24));
        sv.addView(root);

        // Header: < Month Year >
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
        monthTitle.setOnClickListener(v -> { shown = YearMonth.now(); render(); });
        header.addView(prev, new LinearLayout.LayoutParams(dp(56), dp(56)));
        header.addView(monthTitle, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(next, new LinearLayout.LayoutParams(dp(56), dp(56)));
        root.addView(header);

        Button salary = button("💰 Зарплата", v -> startActivity(new Intent(this, SalaryActivity.class)));
        salary.setTextSize(17);
        root.addView(salary);

        // Weekday labels
        LinearLayout wd = new LinearLayout(this);
        for (int i = 0; i < 7; i++) {
            TextView t = new TextView(this);
            t.setText(WEEKDAYS[i]);
            t.setGravity(Gravity.CENTER);
            t.setTextColor(i >= 5 ? 0xFFD32F2F : 0xFF666666);
            t.setPadding(0, dp(6), 0, dp(4));
            wd.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        root.addView(wd);

        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        root.addView(grid);

        // Legend
        root.addView(legendRow(C_FREE, "нет дел"));
        root.addView(legendRow(C_GREEN, "1–3 дела"));
        root.addView(legendRow(C_YELLOW, "4–6 дел"));
        root.addView(legendRow(C_RED, "больше 6 дел"));
        TextView sl = new TextView(this);
        sl.setText("синяя рамка и «смена» — рабочий день");
        sl.setTextColor(C_SHIFT);
        sl.setPadding(dp(4), dp(4), 0, dp(8));
        root.addView(sl);

        shiftInfo = new TextView(this);
        shiftInfo.setTextColor(0xFF444444);
        shiftInfo.setPadding(dp(4), dp(4), 0, dp(4));
        root.addView(shiftInfo);

        root.addView(button("График работы (2/2 и др.)", v -> showShiftDialog()));
        root.addView(button("Открыть сегодня", v -> openDay(LocalDate.now())));
        root.addView(button("Проверить уведомление", v -> {
            Notifier.showNoon(this);
            Toast.makeText(this, "Отправлено. Если не видно — разреши уведомления в настройках",
                    Toast.LENGTH_LONG).show();
        }));
        return sv;
    }

    private Button button(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        return b;
    }

    private View legendRow(int color, String text) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(4), dp(3), 0, dp(3));
        View sq = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(4));
        g.setStroke(dp(1), 0xFFBBBBBB);
        sq.setBackground(g);
        row.addView(sq, new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFF333333);
        t.setPadding(dp(8), 0, 0, 0);
        row.addView(t);
        return row;
    }

    private void render() {
        if (grid == null) return;
        monthTitle.setText(MONTHS[shown.getMonthValue() - 1] + " " + shown.getYear());
        shiftInfo.setText("График: " + Store.shiftDescription(this));
        grid.removeAllViews();

        int offset = shown.atDay(1).getDayOfWeek().getValue() - 1; // Monday = 0
        int days = shown.lengthOfMonth();
        int cells = ((offset + days + 6) / 7) * 7;
        LinearLayout row = null;
        for (int i = 0; i < cells; i++) {
            if (i % 7 == 0) {
                row = new LinearLayout(this);
                grid.addView(row);
            }
            int dayNum = i - offset + 1;
            View cell = (dayNum < 1 || dayNum > days) ? new View(this) : dayCell(shown.atDay(dayNum));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(66), 1f);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            row.addView(cell, lp);
        }
    }

    private View dayCell(LocalDate d) {
        List<Store.Task> tasks = Store.getTasks(this, d);
        int n = tasks.size();
        int done = 0;
        for (Store.Task t : tasks) if (t.done) done++;
        boolean work = Store.isWorkDay(this, d);
        boolean today = d.equals(LocalDate.now());

        SpannableStringBuilder sb = new SpannableStringBuilder(String.valueOf(d.getDayOfMonth()));
        sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sb.setSpan(new RelativeSizeSpan(1.25f), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Store.Work w = Store.getWork(this, d);
        if (w.hours > 0) {
            int s = sb.length();
            sb.append("\n").append(Store.num(w.hours)).append("ч");
            sb.setSpan(new ForegroundColorSpan(C_SHIFT), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new StyleSpan(Typeface.BOLD), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new RelativeSizeSpan(0.75f), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        } else if (work) {
            int s = sb.length();
            sb.append("\nсмена");
            sb.setSpan(new ForegroundColorSpan(C_SHIFT), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            sb.setSpan(new RelativeSizeSpan(0.7f), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (n > 0) {
            int s = sb.length();
            sb.append("\n").append(String.valueOf(done)).append("/").append(String.valueOf(n));
            sb.setSpan(new RelativeSizeSpan(0.75f), s, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        TextView tv = new TextView(this);
        tv.setGravity(Gravity.CENTER);
        tv.setTextColor(Color.BLACK);
        tv.setText(sb);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(colorFor(n));
        if (today) bg.setStroke(dp(3), Color.BLACK);
        else if (work) bg.setStroke(dp(2), C_SHIFT);
        else bg.setStroke(dp(1), 0xFFCCCCCC);
        tv.setBackground(bg);
        tv.setOnClickListener(v -> openDay(d));
        return tv;
    }

    private void openDay(LocalDate d) {
        startActivity(new Intent(this, DayActivity.class).putExtra("date", d.toString()));
    }

    // ---------- shift schedule ----------

    private void showShiftDialog() {
        String[] items = {"2/2 — два через два", "1/3 — сутки через трое", "3/3", "5/2 — пятидневка",
                "Свой вариант…", "Убрать график"};
        new AlertDialog.Builder(this)
                .setTitle("График работы")
                .setItems(items, (dlg, which) -> {
                    switch (which) {
                        case 0: pickStart(2, 2); break;
                        case 1: pickStart(1, 3); break;
                        case 2: pickStart(3, 3); break;
                        case 3: pickStart(5, 2); break;
                        case 4: customShift(); break;
                        default:
                            Store.clearShift(this);
                            render();
                            Toast.makeText(this, "График убран", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void pickStart(int work, int off) {
        Toast.makeText(this, "Выбери первый рабочий день", Toast.LENGTH_LONG).show();
        LocalDate t = LocalDate.now();
        DatePickerDialog dlg = new DatePickerDialog(this, (view, y, m, dd) -> {
            Store.setShift(this, work, off, LocalDate.of(y, m + 1, dd));
            render();
            Toast.makeText(this, "График " + work + "/" + off + " построен", Toast.LENGTH_SHORT).show();
        }, t.getYear(), t.getMonthValue() - 1, t.getDayOfMonth());
        dlg.setTitle("Первый рабочий день");
        dlg.show();
    }

    private void customShift() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        EditText w = new EditText(this);
        w.setHint("Рабочих дней подряд");
        w.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText o = new EditText(this);
        o.setHint("Выходных дней подряд");
        o.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(w);
        box.addView(o);
        new AlertDialog.Builder(this)
                .setTitle("Свой график")
                .setView(box)
                .setPositiveButton("Далее", (dlg, which) -> {
                    try {
                        int work = Integer.parseInt(w.getText().toString().trim());
                        int off = Integer.parseInt(o.getText().toString().trim());
                        if (work < 1 || off < 0 || work > 60 || off > 60) throw new NumberFormatException();
                        pickStart(work, off);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Введи нормальные числа", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Отмена", null)
                .show();
    }
}
