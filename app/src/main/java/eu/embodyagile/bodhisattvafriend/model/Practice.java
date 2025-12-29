package eu.embodyagile.bodhisattvafriend.model;


import java.util.List;
import java.util.Set;

import eu.embodyagile.bodhisattvafriend.logic.InnerCondition;
import eu.embodyagile.bodhisattvafriend.logic.TimeAvailable;

public class Practice {
    private String id;
    private String name;
    private String shortDescription;
    private String instructionText;
    private List<Integer> defaultDurationsMinutes;
    // New:
    private Set<TimeAvailable> allowedTimeFrames;
    private Set<InnerCondition> allowedInnerStates;
    private AudioConfig audio;
    public Practice(String id,
                    String name,
                    String shortDescription,
                    String instructionText,
                    List<Integer> defaultDurationsMinutes) {
        this.id = id;
        this.name = name;
        this.shortDescription = shortDescription;
        this.instructionText = instructionText;
        this.defaultDurationsMinutes = defaultDurationsMinutes;
    }


    public AudioConfig getAudioConfig() {
        return audio;
    }

    public void setAudio(AudioConfig audio) {
        this.audio = audio;
    }

    public boolean hasAudio() {
        return audio != null && audio.getResourceKey() != null;
    }
    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public String getInstructionText() {
        return instructionText;
    }

    public void setInstructionText(String instructionText) {
        this.instructionText = instructionText;
    }

    public List<Integer> getDefaultDurationsMinutes() {
        return defaultDurationsMinutes;
    }

    public void setDefaultDurationsMinutes(List<Integer> defaultDurationsMinutes) {
        this.defaultDurationsMinutes = defaultDurationsMinutes;
    }

    public Set<TimeAvailable> getAllowedTimeFrames() {
        return allowedTimeFrames;
    }

    public void setAllowedTimeFrames(Set<TimeAvailable> allowedTimeFrames) {
        this.allowedTimeFrames = allowedTimeFrames;
    }

    public Set<InnerCondition> getAllowedInnerStates() {
        return allowedInnerStates;
    }

    public void setAllowedInnerStates(Set<InnerCondition> allowedInnerStates) {
        this.allowedInnerStates = allowedInnerStates;
    }

    // Convenience:
    public boolean isAllowedFor(TimeAvailable time, InnerCondition inner) {
        boolean timeOk = (allowedTimeFrames == null || allowedTimeFrames.isEmpty())
                || allowedTimeFrames.contains(time);
        boolean innerOk = (allowedInnerStates == null || allowedInnerStates.isEmpty())
                || allowedInnerStates.contains(inner);
        return timeOk && innerOk;
    }



    public static class AudioConfig {
        private String type;            // e.g. "GUIDED", "AMBIENT"
        private String resourceKey;     // maps to res/raw
        private String description;     // short description for UI
        private String loopMode;        // "ONCE" or "LOOP"
        private Integer minDurationMinutes; // null if not set

        public boolean isGuided() {
            return "GUIDED".equalsIgnoreCase(type);
        }

        public boolean isLoop() {
            return "LOOP".equalsIgnoreCase(loopMode);
        }

        // getters / setters …

        public String getType() { return type; }
        public String getResourceKey() { return resourceKey; }
        public String getDescription() { return description; }
        public String getLoopMode() { return loopMode; }
        public Integer getMinDurationMinutes() { return minDurationMinutes; }

        public void setType(String type) { this.type = type; }
        public void setResourceKey(String resourceKey) { this.resourceKey = resourceKey; }
        public void setDescription(String description) { this.description = description; }
        public void setLoopMode(String loopMode) { this.loopMode = loopMode; }
        public void setMinDurationMinutes(Integer minDurationMinutes) { this.minDurationMinutes = minDurationMinutes; }
    }

}
