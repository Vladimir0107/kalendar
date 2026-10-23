package ru.planner.zadachi;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/** Task list for one day: add, tick off, delete; mark the day as a work shift. */
public class DayActivity extends Activity {

    private LocalDate date;
    private List<Store.Task> tasks;
    private LinearLayout list;
    private TextView counter;
    private EditText input;

    private int dp(float v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String ds = getIntent().getStringExtra("date");
        date = ds != null ? LocalDate.parse(ds) : LocalDate.now();
        tasks = Store.getTasks(this, date);

        String title = date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", new Locale("ru")));
        setTitle(title);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(12), dp(16), dp(24));
        sv.addView(root);

        TextView head = new TextView(this);
        head.setText(title);
        head.setTextSize(22);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        head.setTextColor(Color.BLACK);
        root.addView(head);

        CheckBox workBox = new CheckBox(this);
        workBox.setText("Рабочая смена в этот день");
        workBox.setTextColor(MainActivity.C_SHIFT);
        workBox.setChecked(Store.isWorkDay(this, date));
        workBox.setOnCheckedChangeListener((b, checked) -> Store.setWorkOverride(this, date, checked));
        root.addView(workBox);

        counter = new TextView(this);
        counter.setTextSize(16);
        counter.setTextColor(0xFF333333);
        counter.setPadding(0, dp(12), 0, dp(6));
        root.addView(counter);

        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        root.addView(list);

        LinearLayout addRow = new LinearLayout(this);
        addRow.setGravity(Gravity.CENTER_VERTICAL);
        addRow.setPadding(0, dp(12), 0, 0);
        input = new EditText(this);
        input.setHint("Новое дело…");
        input.setSingleLine(true);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        input.setOnEditorActionListener((v, actionId, e) -> {
            addTask();
            return true;
        });
        Button add = new Button(this);
        add.setText("Добавить");
        add.setAllCaps(false);
        add.setOnClickListener(v -> addTask());
        addRow.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        addRow.addView(add);
        root.addView(addRow);

        TextView hint = new TextView(this);
        hint.setText("Долгое нажатие на дело — удалить");
        hint.setTextColor(0xFF888888);
        hint.setPadding(0, dp(8), 0, 0);
        root.addView(hint);

        setContentView(sv);
        renderList();
    }

    private void addTask() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;
        tasks.add(new Store.Task(text, false));
        Store.saveTasks(this, date, tasks);
        input.setText("");
        renderList();
    }

    private void renderList() {
        list.removeAllViews();
        for (int i = 0; i < tasks.size(); i++) {
            final int idx = i;
            Store.Task t = tasks.get(i);
            CheckBox cb = new CheckBox(this);
            cb.setText(t.text);
            cb.setTextSize(18);
            cb.setTextColor(Color.BLACK);
            cb.setChecked(t.done);
            cb.setPadding(dp(4), dp(6), 0, dp(6));
            strike(cb, t.done);
            cb.setOnCheckedChangeListener((b, checked) -> {
                tasks.get(idx).done = checked;
                Store.saveTasks(this, date, tasks);
                strike(cb, checked);
                updateCounter();
            });
            cb.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this)
                        .setMessage("Удалить «" + t.text + "»?")
                        .setPositiveButton("Удалить", (d, w) -> {
                            tasks.remove(idx);
                            Store.saveTasks(this, date, tasks);
                            renderList();
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
                return true;
            });
            list.addView(cb);
        }
        updateCounter();
    }

    private void strike(CheckBox cb, boolean on) {
        if (on) {
            cb.setPaintFlags(cb.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            cb.setTextColor(0xFF888888);
        } else {
            cb.setPaintFlags(cb.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            cb.setTextColor(Color.BLACK);
        }
    }

    private void updateCounter() {
        int done = 0;
        for (Store.Task t : tasks) if (t.done) done++;
        counter.setText(tasks.isEmpty() ? "Дел пока нет" : "Выполнено " + done + " из " + tasks.size());
    }
}
