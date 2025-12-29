package eu.embodyagile.bodhisattvafriend.history;

import eu.embodyagile.bodhisattvafriend.data.PracticeRepository;
import eu.embodyagile.bodhisattvafriend.logic.InnerCondition;
import eu.embodyagile.bodhisattvafriend.logic.TimeAvailable;

public class SessionLogEntry {
    public long timestamp;
    public String practiceId;
    private String practiceName;
    public int plannedMinutes;
    public long actualMillis;

    // vorher
    public InnerCondition innerBefore;
    public TimeAvailable timeBefore;

    // nachher
    public InnerCondition innerAfter;

    public String getPracticeName() {
        return PracticeRepository.getInstance().getPracticeById(practiceId).getName();
    }
}
