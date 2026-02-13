package eu.embodyagile.bodhisattvafriend;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.embodyagile.bodhisattvafriend.data.AppDatabase;
import eu.embodyagile.bodhisattvafriend.data.SessionDao;
import eu.embodyagile.bodhisattvafriend.data.SessionEntity;

public class SessionListActivity extends BaseActivity implements SessionListAdapter.Callbacks {

    // --- Tune this for Mudita / e-ink ---
    private static final int PAGE_SIZE = 10;         // adjust to 15–20 as needed
    private static final int DEFAULT_START_HOUR = 6; // 06:00
    private static final int DEFAULT_START_MIN = 0;
    private static final int BREAK_MINUTES = 5;      // between sessions

    // Pattern: "10 x 25" or "2 x 40 ZAZEN" (practiceId optional)
    private static final Pattern LINE_PATTERN =
            Pattern.compile("^\\s*(\\d+)\\s*[xX]\\s*(\\d+)\\s*(?:\\s+(.+))?\\s*$");

    private RecyclerView recyclerView;
    private TextView tvPage;
    private Button btnPrev, btnNext, btnAddBatch, btnClose;

    private SessionListAdapter adapter;

    private SessionDao dao;

    private int totalCount = 0;
    private int currentPage = 0; // 0-based

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_list);

        recyclerView = findViewById(R.id.sessionRecyclerView);
        tvPage = findViewById(R.id.textPage);
        btnPrev = findViewById(R.id.buttonPrev);
        btnNext = findViewById(R.id.buttonNext);
        btnAddBatch = findViewById(R.id.buttonAddBatch);
        btnClose = findViewById(R.id.buttonClose);

        adapter = new SessionListAdapter(this);

        // LayoutManager that never scrolls vertically (extra safety)
        LinearLayoutManager lm = new LinearLayoutManager(this) {
            @Override
            public boolean canScrollVertically() {
                return false; // enforce no scrolling
            }
        };
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);
        recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Also cancel MOVE events (prevents fling on some devices)
        recyclerView.setOnTouchListener((v, e) -> e.getAction() == MotionEvent.ACTION_MOVE);

        // Room
        // ⚠️ If your DB singleton differs, adjust this:
        dao = AppDatabase.getInstance(getApplicationContext()).sessionDao();

        btnPrev.setOnClickListener(v -> {
            if (currentPage > 0) {
                currentPage--;
                loadPage();
            }
        });

        btnNext.setOnClickListener(v -> {
            int maxPage = Math.max(0, (totalCount - 1) / PAGE_SIZE);
            if (currentPage < maxPage) {
                currentPage++;
                loadPage();
            }
        });

        btnAddBatch.setOnClickListener(v -> showBatchAddDialog());

        btnClose.setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCountAndLoad();
    }

    private void refreshCountAndLoad() {
        new Thread(() -> {
            totalCount = dao.countSessions();
            // clamp page
            int maxPage = Math.max(0, (totalCount - 1) / PAGE_SIZE);
            if (currentPage > maxPage) currentPage = maxPage;

            runOnUiThread(this::loadPage);
        }).start();
    }

    private void loadPage() {
        btnPrev.setEnabled(currentPage > 0);

        int maxPage = Math.max(0, (totalCount - 1) / PAGE_SIZE);
        btnNext.setEnabled(currentPage < maxPage);

        tvPage.setText(String.format(Locale.getDefault(), "%d/%d",
                (totalCount == 0 ? 0 : (currentPage + 1)),
                (totalCount == 0 ? 0 : (maxPage + 1))));

        if (totalCount == 0) {
            adapter.setItems(new ArrayList<>());
            return;
        }

        final int offset = currentPage * PAGE_SIZE;
        new Thread(() -> {
            List<SessionEntity> page = dao.getPageDesc(PAGE_SIZE, offset);
            runOnUiThread(() -> adapter.setItems(page));
        }).start();
    }

    // --- Delete ---

    @Override
    public void onDeleteClicked(SessionEntity session) {
        if (session == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Delete session?")
                .setMessage("This will remove the session from your stats.")
                .setPositiveButton("Delete", (d, w) -> deleteSessionNow(session))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteSessionNow(SessionEntity session) {
        new Thread(() -> {
            int rows = dao.deleteById(session.id); // requires SessionEntity.id
            if (rows > 0) totalCount = Math.max(0, totalCount - 1);

            runOnUiThread(() -> {
                Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show();
                // reload current page safely
                int maxPage = Math.max(0, (totalCount - 1) / PAGE_SIZE);
                if (currentPage > maxPage) currentPage = maxPage;
                loadPage();

                // trigger insights refresh if you have such a hook
                // MeditationInsightsRepository.getInstance(this).notifySessionsChanged();
            });
        }).start();
    }

    // --- Batch add ---

    private void showBatchAddDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_batch_add_sessions, null);

        EditText editStartDate = v.findViewById(R.id.editStartDate);
        EditText editEndDate = v.findViewById(R.id.editEndDate);
        EditText editStartTime = v.findViewById(R.id.editStartTime);
        EditText editPattern = v.findViewById(R.id.editPattern);

        // Defaults
        editStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", DEFAULT_START_HOUR, DEFAULT_START_MIN));

        new AlertDialog.Builder(this)
                .setTitle("Batch add sessions")
                .setView(v)
                .setPositiveButton("Add", (dlg, which) -> {
                    String sd = text(editStartDate);
                    String ed = text(editEndDate);
                    String st = text(editStartTime);
                    String patternText = text(editPattern);

                    if (TextUtils.isEmpty(sd)) {
                        Toast.makeText(this, "Start date required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (TextUtils.isEmpty(ed)) ed = sd; // single day
                    if (TextUtils.isEmpty(st)) st = "06:00";
                    if (TextUtils.isEmpty(patternText)) {
                        Toast.makeText(this, "Pattern required", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    BatchInput input = BatchInput.parse(sd, ed, st, patternText);
                    if (input == null) {
                        Toast.makeText(this, "Invalid input. Use yyyy-mm-dd and HH:mm.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    batchInsert(input);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static String text(EditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }

    private void batchInsert(BatchInput input) {
        new Thread(() -> {
            List<SessionEntity> toInsert = generateSessions(input);

            if (toInsert.isEmpty()) {
                runOnUiThread(() -> Toast.makeText(this, "Nothing to add (pattern empty?)", Toast.LENGTH_SHORT).show());
                return;
            }

            dao.insertAll(toInsert);

            totalCount = dao.countSessions();
            currentPage = 0; // jump to newest page
            runOnUiThread(() -> {
                Toast.makeText(this, "Added " + toInsert.size() + " sessions", Toast.LENGTH_SHORT).show();
                loadPage();

                // MeditationInsightsRepository.getInstance(this).notifySessionsChanged();
            });
        }).start();
    }

    private List<SessionEntity> generateSessions(BatchInput input) {
        List<SessionEntity> out = new ArrayList<>();

        // Use local timezone day boundaries
        TimeZone tz = TimeZone.getDefault();

        Calendar day = Calendar.getInstance(tz);
        day.set(Calendar.YEAR, input.startYear);
        day.set(Calendar.MONTH, input.startMonth - 1);
        day.set(Calendar.DAY_OF_MONTH, input.startDay);
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);

        Calendar end = Calendar.getInstance(tz);
        end.set(Calendar.YEAR, input.endYear);
        end.set(Calendar.MONTH, input.endMonth - 1);
        end.set(Calendar.DAY_OF_MONTH, input.endDay);
        end.set(Calendar.HOUR_OF_DAY, 0);
        end.set(Calendar.MINUTE, 0);
        end.set(Calendar.SECOND, 0);
        end.set(Calendar.MILLISECOND, 0);

        while (!day.after(end)) {
            // Start time for the day
            Calendar t = (Calendar) day.clone();
            t.set(Calendar.HOUR_OF_DAY, input.startHour);
            t.set(Calendar.MINUTE, input.startMinute);
            t.set(Calendar.SECOND, 0);
            t.set(Calendar.MILLISECOND, 0);

            for (PatternLine pl : input.lines) {
                for (int i = 0; i < pl.count; i++) {
                    SessionEntity e = new SessionEntity();

                    e.timestamp = t.getTimeInMillis();

                    // Practice resolution:
                    // If not set or unknown -> default practiceId
                    String pid = resolvePracticeId(pl.practiceId);
                    e.practiceId = pid;

                    e.plannedMinutes = pl.minutes;
                    e.actualDurationMs = pl.minutes * 60_000L; // treat as actual too for stats consistency
                    // Set other fields to defaults if you have them:
                    // e.innerBefore = "";
                    // e.timeBefore = "";
                    // e.innerAfter = "";

                    out.add(e);

                    // advance time: session duration + break
                    t.add(Calendar.MINUTE, pl.minutes);
                    t.add(Calendar.MINUTE, BREAK_MINUTES);
                }
            }

            day.add(Calendar.DAY_OF_MONTH, 1);
        }

        return out;
    }

    private String resolvePracticeId(String requested) {
        // If you have PracticeRepository: validate here.
        // For now: fallback to a safe default if null/empty
        if (requested == null || requested.trim().isEmpty()) {
            return getDefaultPracticeId();
        }

        // TODO (optional): validate existence and fallback if unknown.
        // Example (if you have PracticeRepository):
        // PracticeRepository pr = PracticeRepository.getInstance(getApplicationContext());
        // if (pr.getPracticeById(requested) == null) return getDefaultPracticeId();

        return requested.trim();
    }

    private String getDefaultPracticeId() {
        // TODO: replace with your actual fallback practice id strategy.
        // If you have PracticeRepository.getFallbackPractice(): return that id.
        // For now, keep a conservative default:
        return "MICROPAUSE";
    }

    // --- parsing helpers ---

    private static class BatchInput {
        int startYear, startMonth, startDay;
        int endYear, endMonth, endDay;
        int startHour, startMinute;
        List<PatternLine> lines;

        static BatchInput parse(String startDate, String endDate, String startTime, String patternText) {
            int[] sd = parseDate(startDate);
            int[] ed = parseDate(endDate);
            int[] st = parseTime(startTime);
            if (sd == null || ed == null || st == null) return null;

            List<PatternLine> lines = parsePattern(patternText);
            if (lines.isEmpty()) return null;

            BatchInput bi = new BatchInput();
            bi.startYear = sd[0]; bi.startMonth = sd[1]; bi.startDay = sd[2];
            bi.endYear = ed[0]; bi.endMonth = ed[1]; bi.endDay = ed[2];
            bi.startHour = st[0]; bi.startMinute = st[1];
            bi.lines = lines;
            return bi;
        }
    }

    private static class PatternLine {
        int count;
        int minutes;
        String practiceId; // optional
    }

    private static List<PatternLine> parsePattern(String patternText) {
        List<PatternLine> out = new ArrayList<>();
        String[] lines = patternText.split("\\r?\\n");

        for (String raw : lines) {
            String s = raw.trim();
            if (s.isEmpty()) continue;

            Matcher m = LINE_PATTERN.matcher(s);
            if (!m.matches()) continue;

            int count = safeInt(m.group(1));
            int minutes = safeInt(m.group(2));
            String pid = m.group(3); // optional (may be null)

            if (count <= 0 || minutes <= 0) continue;

            PatternLine pl = new PatternLine();
            pl.count = count;
            pl.minutes = minutes;
            pl.practiceId = (pid == null ? "" : pid.trim());
            out.add(pl);
        }

        return out;
    }

    private static int[] parseDate(String s) {
        // yyyy-mm-dd
        try {
            String[] parts = s.trim().split("-");
            if (parts.length != 3) return null;
            int y = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            int d = Integer.parseInt(parts[2]);
            if (m < 1 || m > 12) return null;
            if (d < 1 || d > 31) return null;
            return new int[]{y, m, d};
        } catch (Exception e) {
            return null;
        }
    }

    private static int[] parseTime(String s) {
        // HH:mm
        try {
            String[] parts = s.trim().split(":");
            if (parts.length != 2) return null;
            int h = Integer.parseInt(parts[0]);
            int m = Integer.parseInt(parts[1]);
            if (h < 0 || h > 23) return null;
            if (m < 0 || m > 59) return null;
            return new int[]{h, m};
        } catch (Exception e) {
            return null;
        }
    }

    private static int safeInt(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }
}
