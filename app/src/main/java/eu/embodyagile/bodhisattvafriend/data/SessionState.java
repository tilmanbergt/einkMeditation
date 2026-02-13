package eu.embodyagile.bodhisattvafriend.data;

import android.os.SystemClock;

public final class SessionState {



    public long remainingMillisNow() {
        return remainingMillis(SystemClock.elapsedRealtime());
    }

    enum Phase { IDLE, PRETIMER, RUNNING, PAUSED, FINISHED, CANCELED }
    enum FinishReason { NATURAL, EARLY_END }

    private Phase phase = Phase.IDLE;

    private int plannedMinutes = 0;

    // monotonic time (elapsedRealtime)
    private long startedAtMs = 0;
    private long pausedAtMs = 0;
    private long pausedTotalMs = 0;
    private long finishedAtMs = 0;

    private FinishReason finishReason = null;

    // ---- derived ----
    public  long plannedMillis() { return plannedMinutes * 60_000L; }

    public long elapsedActiveMillis(long nowMs) {
        if (phase == Phase.IDLE || phase == Phase.PRETIMER) return 0;

        long endRef = nowMs;
        if (phase == Phase.PAUSED) endRef = pausedAtMs;
        if (phase == Phase.FINISHED || phase == Phase.CANCELED) endRef = finishedAtMs;

        long active = (endRef - startedAtMs) - pausedTotalMs;
        return Math.max(0, active);
    }

    public long remainingMillis(long nowMs) {
        long rem = plannedMillis() - elapsedActiveMillis(nowMs);
        return Math.max(0, rem);
    }

    public boolean isRunning() { return phase == Phase.RUNNING; }
    public boolean isPaused()  { return phase == Phase.PAUSED; }
    public boolean isFinished(){ return phase == Phase.FINISHED; }
    public Phase phase() { return phase; }
    public FinishReason finishReason() { return finishReason; }
    public  int plannedMinutes() { return plannedMinutes; }
    public  long finishedAtMs() { return finishedAtMs; }

    // ---- transitions ----
    public void start() {
        long now = SystemClock.elapsedRealtime();
        startedAtMs = now;
        pausedAtMs = 0;
        pausedTotalMs = 0;
        finishedAtMs = 0;
        finishReason = null;
        phase = Phase.RUNNING;
    }

    public void pause() {
        if (phase != Phase.RUNNING) return;
        pausedAtMs = SystemClock.elapsedRealtime();
        phase = Phase.PAUSED;
    }

    public void resume() {
        if (phase != Phase.PAUSED) return;
        long now = SystemClock.elapsedRealtime();
        pausedTotalMs += (now - pausedAtMs);
        pausedAtMs = 0;
        phase = Phase.RUNNING;
    }

    public void finishNatural() {
        if (phase != Phase.RUNNING && phase != Phase.PAUSED) return;
        finishedAtMs = SystemClock.elapsedRealtime();
        finishReason = FinishReason.NATURAL;
        phase = Phase.FINISHED;
    }

    public void finishEarly() {
        if (phase != Phase.RUNNING && phase != Phase.PAUSED) return;
        finishedAtMs = SystemClock.elapsedRealtime();
        finishReason = FinishReason.EARLY_END;
        phase = Phase.FINISHED;
    }

    public void cancel() {
        finishedAtMs = SystemClock.elapsedRealtime();
        phase = Phase.CANCELED;
    }

    public long overtimeMillis(long nowMs) {
        if (phase != Phase.FINISHED) return 0;
        return Math.max(0, nowMs - finishedAtMs);
    }

    public void setPlannedMinutes(int minutes) {
        plannedMinutes = Math.max(1, minutes);    }

    // ----)



    public long elapsedActiveMillisNow() { return elapsedActiveMillis(SystemClock.elapsedRealtime()); }

    public long elapsedActiveMillisAtFinish() {
        if (phase != Phase.FINISHED && phase != Phase.CANCELED) return 0;
        return elapsedActiveMillis(finishedAtMs);
    }

    public boolean canOvertime() {
        return phase == Phase.FINISHED && finishReason == FinishReason.NATURAL;
    }

    public long overtimeMillisNow() {
        return overtimeMillis(SystemClock.elapsedRealtime());
    }

    /** Für Speicherung: NATURAL => plannedMillis, EARLY_END => elapsed (geclamped). */
    public long durationToStoreMillis() {
        if (phase != Phase.FINISHED) return 0;
        long planned = plannedMillis();
        if (finishReason == FinishReason.NATURAL) return planned;
        long elapsed = elapsedActiveMillisAtFinish();
        if (elapsed < 0) elapsed = 0;
        if (elapsed > planned) elapsed = planned;
        return elapsed;
    }

    public boolean isActive() { return phase == Phase.RUNNING || phase == Phase.PAUSED; }
    public boolean isFinishedNaturally() { return finishReason == FinishReason.NATURAL; }


    /** Basisdauer für Summary (ohne Overtime): natural => planned, early => elapsedAtFinish (clamped). */
    public long baseDurationMillis() {
        if (phase != Phase.FINISHED) return 0;
        long planned = plannedMillis();
        if (finishReason == FinishReason.NATURAL) return planned;

        long elapsed = elapsedActiveMillisAtFinish();
        if (elapsed < 0) elapsed = 0;
        if (elapsed > planned) elapsed = planned;
        return elapsed;
    }

    /** Overtime nur, wenn erlaubt (natürlich beendet). */
    public long overtimeMillisNowIfAllowed() {
        return canOvertime() ? overtimeMillisNow() : 0;
    }

    /** Finaler Speicherwert inkl. optionaler Overtime-Option (einziger “Policy”-Schalter aus UI). */
    public long durationToStoreMillis(boolean includeOvertime) {
        long base = baseDurationMillis();
        if (!includeOvertime) return base;
        return base + overtimeMillisNowIfAllowed();
    }
    // --- seconds helpers (UI-friendly) ---
    public long baseDurationSeconds() {
        return baseDurationMillis() / 1000L;
    }

    public long overtimeSecondsNowIfAllowed() {
        return overtimeMillisNowIfAllowed() / 1000L;
    }

    public long remainingSecondsNow() {
        return remainingMillisNow() / 1000L;
    }
    public long durationToStoreSeconds(boolean includeOvertime) {
        return durationToStoreMillis(includeOvertime) / 1000L;
    }
}
