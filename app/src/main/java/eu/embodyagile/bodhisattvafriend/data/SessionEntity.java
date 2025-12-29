package eu.embodyagile.bodhisattvafriend.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "sessions")
public class SessionEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long timestamp;          // epoch millis
    public String practiceId;

    public long plannedMinutes;
    public long actualDurationMs;

    public String innerBefore;
    public String timeBefore;
    public String innerAfter;
}
