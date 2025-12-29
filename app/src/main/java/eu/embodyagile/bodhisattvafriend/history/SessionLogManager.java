package eu.embodyagile.bodhisattvafriend.history;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import eu.embodyagile.bodhisattvafriend.data.AppDatabase;
import eu.embodyagile.bodhisattvafriend.data.SessionDao;
import eu.embodyagile.bodhisattvafriend.data.SessionEntity;
import eu.embodyagile.bodhisattvafriend.logic.InnerCondition;
import eu.embodyagile.bodhisattvafriend.logic.TimeAvailable;

public class SessionLogManager {

    // Single-thread executor keeps inserts ordered and avoids concurrency headaches
    private static final ExecutorService DB_EXECUTOR = Executors.newSingleThreadExecutor();

    private static SessionDao dao(Context context) {
        return AppDatabase.getInstance(context.getApplicationContext()).sessionDao();
    }

    /**
     * Insert session asynchronously (non-blocking).
     */
    public static void addSession(Context context, SessionLogEntry entry) {
        if (context == null || entry == null) return;

        Context appCtx = context.getApplicationContext();

        DB_EXECUTOR.execute(() -> {
            try {
                SessionEntity e = toEntity(entry);
                dao(appCtx).insert(e);

                // Notify on main thread (safe for UI observers)
                new Handler(Looper.getMainLooper()).post(() -> {
                    try {
                        MeditationInsightsRepository.getInstance(appCtx).notifySessionsChanged();
                    } catch (Exception ignored) {
                        // Keep logging robust even if insights repo has issues
                    }
                });

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    /**
     * Load sessions synchronously (returns a List like before).
     *
     * Important:
     * - This method blocks until DB is read.
     * - It runs the actual Room query on DB_EXECUTOR so it won't crash with
     *   "Cannot access database on the main thread".
     */
    public static List<SessionLogEntry> getSessions(Context context) {
        List<SessionLogEntry> result = new ArrayList<>();
        if (context == null) return result;

        Context appCtx = context.getApplicationContext();

        try {
            Future<List<SessionEntity>> f = DB_EXECUTOR.submit(new Callable<List<SessionEntity>>() {
                @Override
                public List<SessionEntity> call() {
                    return dao(appCtx).getAllDesc();
                }
            });

            List<SessionEntity> entities = f.get(); // blocks
            if (entities == null) return result;

            for (SessionEntity e : entities) {
                result.add(toEntry(e));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return result;
    }

    /**
     * Exports current DB history to CSV (same behavior as before).
     */
    public static File exportHistoryToCsv(Context context) {
        if (context == null) return null;

        try {
            List<SessionLogEntry> sessions = getSessions(context);
            if (sessions.isEmpty()) return null;

            File dir = context.getExternalFilesDir(null);
            if (dir == null) return null;

            SimpleDateFormat sdfName = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            String fileName = "sessions_" + sdfName.format(new Date()) + ".csv";
            File csvFile = new File(dir, fileName);

            FileWriter writer = new FileWriter(csvFile);

            // Header
            writer.append("date,time,practiceId,practiceName,plannedMinutes,actualMinutes,innerBefore,timeBefore,innerAfter\n");

            SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());

            for (SessionLogEntry e : sessions) {
                Date d = new Date(e.timestamp);
                String dateStr = sdfDate.format(d);
                String timeStr = sdfTime.format(d);
                long minutes = e.actualMillis > 0 ? Math.round(e.actualMillis / 60000f) : 0;

                String innerBefore = e.innerBefore != null ? e.innerBefore.name() : "";
                String timeBefore = e.timeBefore != null ? e.timeBefore.name() : "";
                String innerAfter  = e.innerAfter  != null ? e.innerAfter.name()  : "";

                writer.append(escapeCsv(dateStr)).append(',')
                        .append(escapeCsv(timeStr)).append(',')
                        .append(escapeCsv(e.practiceId)).append(',')
                        .append(escapeCsv(e.getPracticeName())).append(',')
                        .append(String.valueOf(e.plannedMinutes)).append(',')
                        .append(String.valueOf(minutes)).append(',')
                        .append(escapeCsv(innerBefore)).append(',')
                        .append(escapeCsv(timeBefore)).append(',')
                        .append(escapeCsv(innerAfter)).append('\n');
            }

            writer.flush();
            writer.close();

            return csvFile;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // --- Mapping helpers ---

    private static SessionEntity toEntity(SessionLogEntry entry) {
        SessionEntity e = new SessionEntity();

        e.timestamp = entry.timestamp;
        e.practiceId = entry.practiceId;
        //e. = entry.practiceName;        // ensure your entity has this column
        e.plannedMinutes = entry.plannedMinutes;
        e.actualDurationMs = entry.actualMillis;

        e.innerBefore = (entry.innerBefore != null) ? entry.innerBefore.name() : null;
        e.timeBefore  = (entry.timeBefore  != null) ? entry.timeBefore.name()  : null;
        e.innerAfter  = (entry.innerAfter  != null) ? entry.innerAfter.name()  : null;

        return e;
    }

    private static SessionLogEntry toEntry(SessionEntity e) {
        SessionLogEntry entry = new SessionLogEntry();

        entry.timestamp = e.timestamp;
        entry.practiceId = e.practiceId;
      //  entry.practiceName =    e.practiceName;
        entry.plannedMinutes = (int) e.plannedMinutes;
        entry.actualMillis = e.actualDurationMs;

        if (e.innerBefore != null && !e.innerBefore.isEmpty()) {
            try { entry.innerBefore = InnerCondition.valueOf(e.innerBefore); } catch (Exception ignored) {}
        }
        if (e.timeBefore != null && !e.timeBefore.isEmpty()) {
            try { entry.timeBefore = TimeAvailable.valueOf(e.timeBefore); } catch (Exception ignored) {}
        }
        if (e.innerAfter != null && !e.innerAfter.isEmpty()) {
            try { entry.innerAfter = InnerCondition.valueOf(e.innerAfter); } catch (Exception ignored) {}
        }

        return entry;
    }

    private static String escapeCsv(String value) {
        if (value == null) return "";
        String v = value.replace("\"", "\"\"");
        return "\"" + v + "\"";
    }
}
