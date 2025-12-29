package eu.embodyagile.bodhisattvafriend.logic;

public class QuickCheckAnswers {


    private final InnerCondition innerCondition;
    private final TimeAvailable timeAvailable;

    public QuickCheckAnswers(InnerCondition innerCondition,
                             TimeAvailable timeAvailable) {
        this.innerCondition = innerCondition;
        this.timeAvailable = timeAvailable;
    }

    public InnerCondition getInnerCondition() {
        return innerCondition;
    }

    public TimeAvailable getTimeAvailable() {
        return timeAvailable;
    }
}
