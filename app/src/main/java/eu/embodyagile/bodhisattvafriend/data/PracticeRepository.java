package eu.embodyagile.bodhisattvafriend.data;

import android.content.Context;
import androidx.annotation.RawRes;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eu.embodyagile.bodhisattvafriend.R;
import eu.embodyagile.bodhisattvafriend.helper.LocaleHelper;
import eu.embodyagile.bodhisattvafriend.logic.InnerCondition;
import eu.embodyagile.bodhisattvafriend.logic.QuickCheckAnswers;
import eu.embodyagile.bodhisattvafriend.logic.TimeAvailable;
import eu.embodyagile.bodhisattvafriend.model.Practice;

public class PracticeRepository {

    private static final String PRACTICES_FILENAME = "practices.json";
    private static PracticeRepository instance;

    private Map<String, Practice> practicesById = new LinkedHashMap<>();
    private Map<String, List<String>> practiceSets = new HashMap<>();

    private String fallbackPracticeId;
    private String manualSelectionSetId;
    private String currentLanguageCode = null;

    private PracticeRepository() {
    }

    public static PracticeRepository getInstance() {
        if (instance == null) {
            instance = new PracticeRepository();
        }
        return instance;
    }

    public static String recommendPracticeId(QuickCheckAnswers answers) {
        List<Practice> recommendedList = recommendPractices(answers);
        PracticeRepository repository = PracticeRepository.getInstance();

        if (!recommendedList.isEmpty()) {
            return recommendedList.get(0).getId();
        }

        return repository.getFallbackPractice().getId();
    }

    public static List<Practice> recommendPractices(QuickCheckAnswers answers) {

        PracticeRepository repository = PracticeRepository.getInstance();

        List<Practice> recommendedList = new ArrayList<Practice>();
        TimeAvailable time = answers.getTimeAvailable();
        InnerCondition inner = answers.getInnerCondition();

        for (Practice p : repository.getAllPractices()) {
            if (p.isAllowedFor(time, inner)) {
                recommendedList.add(p);
            }
        }

        return recommendedList;
    }

    public void init(Context context) {

        String lang = LocaleHelper.getCurrentLanguage(context);
        if (lang == null || lang.isEmpty()) {
            lang = "de";
        }

        if (lang.equals(currentLanguageCode) && !practicesById.isEmpty()) {
            return;
        }

        currentLanguageCode = lang;

        practicesById.clear();
        practiceSets.clear();

        File internalPracticesFile = new File(context.getFilesDir(), PRACTICES_FILENAME);
        if (!internalPracticesFile.exists()) {
            try {
                copyAssetToInternalStorage(context, PRACTICES_FILENAME);
            } catch (IOException e) {
                e.printStackTrace();
                return; 
            }
        }

        try {
            String json = readInternalFile(context, PRACTICES_FILENAME);
            JSONObject root = new JSONObject(json);
            if (root.has("meta")) {
                JSONObject meta = root.getJSONObject("meta");
                fallbackPracticeId = meta.optString("fallbackPracticeId", null);
                manualSelectionSetId = meta.optString("manualSelectionSetId", null);
            }

            if (root.has("practiceSets")) {
                JSONArray setsArray = root.getJSONArray("practiceSets");
                for (int i = 0; i < setsArray.length(); i++) {
                    JSONObject setObj = setsArray.getJSONObject(i);
                    String setId = setObj.getString("id");

                    JSONArray idsArray = setObj.getJSONArray("practiceIds");
                    List<String> ids = new ArrayList<>();
                    for (int j = 0; j < idsArray.length(); j++) {
                        ids.add(idsArray.getString(j));
                    }
                    practiceSets.put(setId, ids);
                }
            }

            JSONArray practices = root.getJSONArray("practices");
            for (int i = 0; i < practices.length(); i++) {
                JSONObject obj = practices.getJSONObject(i);
                String id = obj.getString("id");
                String name = getMaybeLocalizedString(obj,"name",currentLanguageCode);
                String shortDescription = getMaybeLocalizedString(obj,"shortDescription",currentLanguageCode);
                String instructionText = getMaybeLocalizedString(obj,"instructionText",currentLanguageCode);
                JSONArray defaultsArray = obj.getJSONArray("defaultDurationsMinutes");
                List<Integer> defaults = new ArrayList<>();
                for (int j = 0; j < defaultsArray.length(); j++) {
                    defaults.add(defaultsArray.getInt(j));
                }
                Practice practice = new Practice(
                        id,
                        name,
                        shortDescription,
                        instructionText,
                        defaults
                );
                if (obj.has("allowedTimeFrames")) {
                    JSONArray timeArray = obj.getJSONArray("allowedTimeFrames");
                    EnumSet<TimeAvailable> times = EnumSet.noneOf(TimeAvailable.class);
                    for (int j = 0; j < timeArray.length(); j++) {
                        String time = timeArray.getString(j);
                        times.add(TimeAvailable.valueOf(time));
                    }
                    practice.setAllowedTimeFrames(times);
                } else {
                    practice.setAllowedTimeFrames(EnumSet.allOf(TimeAvailable.class));
                }

                if (obj.has("allowedInnerStates")) {
                    JSONArray innerArray = obj.getJSONArray("allowedInnerStates");
                    EnumSet<InnerCondition> states = EnumSet.noneOf(InnerCondition.class);
                    for (int j = 0; j < innerArray.length(); j++) {
                        String inner = innerArray.getString(j);
                        states.add(InnerCondition.valueOf(inner));
                    }
                    practice.setAllowedInnerStates(states);
                } else {
                    practice.setAllowedInnerStates(EnumSet.allOf(InnerCondition.class));
                }

                if (obj.has("audio")) {
                    JSONObject audioJson = obj.getJSONObject("audio");
                    Practice.AudioConfig audio = new Practice.AudioConfig();

                    audio.setType(audioJson.optString("type", null));
                    audio.setResourceKey(audioJson.optString("resourceKey", null));
                  
                    audio.setDescription(getMaybeLocalizedString(audioJson,"description",currentLanguageCode));
                    audio.setLoopMode(audioJson.optString("loopMode", "ONCE"));

                    if (audioJson.has("minDurationMinutes")) {
                        int mins = audioJson.optInt("minDurationMinutes", 0);
                        if (mins > 0) {
                            audio.setMinDurationMinutes(mins);
                        }
                    }

                    practice.setAudio(audio);
                }
                practicesById.put(id, practice);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addPractice(Practice practice, Context context) {
        practicesById.put(practice.getId(), practice);

        if (manualSelectionSetId != null && practiceSets.containsKey(manualSelectionSetId)) {
            List<String> manualSelectionIds = practiceSets.get(manualSelectionSetId);
            if (manualSelectionIds != null && !manualSelectionIds.contains(practice.getId())) {
                manualSelectionIds.add(practice.getId());
            }
        }

        savePracticesToFile(context);
    }

    public void updatePractice(Practice practice, Context context) {
        practicesById.put(practice.getId(), practice);
        savePracticesToFile(context);
    }

    public void deletePractice(Practice practice, Context context) {
        practicesById.remove(practice.getId());

        if (manualSelectionSetId != null && practiceSets.containsKey(manualSelectionSetId)) {
            List<String> manualSelectionIds = practiceSets.get(manualSelectionSetId);
            if (manualSelectionIds != null) {
                manualSelectionIds.remove(practice.getId());
            }
        }

        savePracticesToFile(context);
    }
    public void importPractices(Context context, List<Practice> importedPractices, boolean overwriteDuplicates) {
        for (Practice importedPractice : importedPractices) {
            boolean exists = practicesById.containsKey(importedPractice.getId());
            if (!exists) {
                practicesById.put(importedPractice.getId(), importedPractice);
                if (manualSelectionSetId != null && practiceSets.containsKey(manualSelectionSetId)) {
                    List<String> manualSelectionIds = practiceSets.get(manualSelectionSetId);
                    if (manualSelectionIds != null && !manualSelectionIds.contains(importedPractice.getId())) {
                         manualSelectionIds.add(importedPractice.getId());
                    }
                }
            } else if (overwriteDuplicates) {
                practicesById.put(importedPractice.getId(), importedPractice);
            }
        }
        savePracticesToFile(context);
        init(context); // Reload data
    }

    private void savePracticesToFile(Context context) {
        try {
            // Read the existing file to preserve all language data
            String jsonString = readInternalFile(context, PRACTICES_FILENAME);
            JSONObject root = new JSONObject(jsonString);
            JSONArray practicesArray = root.getJSONArray("practices");
            Map<String, JSONObject> jsonPracticesMap = new HashMap<>();
            for (int i = 0; i < practicesArray.length(); i++) {
                JSONObject practiceJson = practicesArray.getJSONObject(i);
                jsonPracticesMap.put(practiceJson.getString("id"), practiceJson);
            }

            // Update or add practices from in-memory list
            for (Practice practice : practicesById.values()) {
                JSONObject practiceJson = jsonPracticesMap.get(practice.getId());
                boolean isNew = practiceJson == null;
                if (isNew) {
                    practiceJson = new JSONObject();
                    practiceJson.put("id", practice.getId());
                }

                // Update name, description, etc. for the current language
                updateLocalizedField(practiceJson, "name", practice.getName());
                updateLocalizedField(practiceJson, "shortDescription", practice.getShortDescription());
                updateLocalizedField(practiceJson, "instructionText", practice.getInstructionText());

                practiceJson.put("defaultDurationsMinutes", new JSONArray(practice.getDefaultDurationsMinutes()));

                if (isNew) {
                    practicesArray.put(practiceJson);
                }
            }
            
            // Handle deletions
            for (int i = practicesArray.length() - 1; i >= 0; i--) {
                JSONObject practiceJson = practicesArray.getJSONObject(i);
                if (!practicesById.containsKey(practiceJson.getString("id"))) {
                    practicesArray.remove(i);
                }
            }

            // Save the updated practice sets
            root.put("practiceSets", new JSONArray()); // Re-create from scratch
            JSONArray newSetsArray = root.getJSONArray("practiceSets");
            for (Map.Entry<String, List<String>> entry : practiceSets.entrySet()) {
                JSONObject setObj = new JSONObject();
                setObj.put("id", entry.getKey());
                setObj.put("practiceIds", new JSONArray(entry.getValue()));
                newSetsArray.put(setObj);
            }

            FileOutputStream fos = context.openFileOutput(PRACTICES_FILENAME, Context.MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(fos);
            writer.write(root.toString(4));
            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateLocalizedField(JSONObject practiceJson, String key, String value) throws JSONException {
        JSONObject localizedNode = practiceJson.optJSONObject(key);
        if (localizedNode == null) {
            localizedNode = new JSONObject();
        }
        localizedNode.put(currentLanguageCode, value);
        practiceJson.put(key, localizedNode);
    }

    public void resetToDefault(Context context) {
        File internalPracticesFile = new File(context.getFilesDir(), PRACTICES_FILENAME);
        if (internalPracticesFile.exists()) {
            internalPracticesFile.delete();
        }
        clearData();
        init(context);
    }

    public void clearData() {
        practicesById.clear();
        practiceSets.clear();
        currentLanguageCode = null;
    }

    private void copyAssetToInternalStorage(Context context, String filename) throws IOException {
        InputStream is = context.getAssets().open(filename);
        FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) > 0) {
            fos.write(buffer, 0, length);
        }
        fos.close();
        is.close();
    }

    private String readInternalFile(Context context, String fileName) throws IOException {
        FileInputStream fis = context.openFileInput(fileName);
        InputStreamReader inputStreamReader = new InputStreamReader(fis);
        BufferedReader reader = new BufferedReader(inputStreamReader);
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        reader.close();
        inputStreamReader.close();
        fis.close();
        return sb.toString();
    }

    public Practice getPracticeById(String id) {
        return practicesById.get(id);
    }

    public List<Practice> getAllPractices() {
        return new ArrayList<>(practicesById.values());
    }

    public Practice getFallbackPractice() {
        Practice p = practicesById.get(fallbackPracticeId);
        if (p != null) return p;
        
        if (practicesById.isEmpty()) return null;
        return practicesById.values().iterator().next();
    }

    public List<Practice> getPracticesForSet(String setId) {
        List<String> ids = practiceSets.get(setId);
        if (ids == null) {
            return new ArrayList<>(practicesById.values());
        }
        List<Practice> result = new ArrayList<>();
        for (String id : ids) {
            Practice p = practicesById.get(id);
            if (p != null) {
                result.add(p);
            }
        }
        return result;
    }

    public List<Practice> getPracticesForManualSelection() {
        if (manualSelectionSetId != null) {
            return getPracticesForSet(manualSelectionSetId);
        } else {
            return new ArrayList<>(practicesById.values());
        }
    }

    public @RawRes int resolveAudioResId(Practice.AudioConfig audio) {
        if (audio == null || audio.getResourceKey() == null) return 0;

        switch (audio.getResourceKey()) {
//            case "gm_tara_brach_sacred_pause":
//                return R.raw.gm_tara_brach_sacred_pause;
//            case "gm_tara_brach_smile":
//                return R.raw.gm_tara_brach_smile;
//            case "am_birds":
//                return R.raw.am_birds;
            default:
                return 0;
        }
    }
    private String resolveLocalizedText(JSONObject node, String desiredLang) {
        if (node == null) return "";

        if (desiredLang != null && node.has(desiredLang)) {
            String v = node.optString(desiredLang, "").trim();
            if (!v.isEmpty()) return v;
        }
        if (node.has("en")) {
            String v = node.optString("en", "").trim();
            if (!v.isEmpty()) return v;
        }
        if (node.has("de")) {
            String v = node.optString("de", "").trim();
            if (!v.isEmpty()) return v;
        }
        Iterator<String> keys = node.keys();
        while (keys.hasNext()) {
            String k = keys.next();
            String v = node.optString(k, "").trim();
            if (!v.isEmpty()) return v;
        }
        return "";
    }
    private String getMaybeLocalizedString(JSONObject practice, String key, String currentLanguage) {
        Object value = practice.opt(key);
        if (value instanceof JSONObject) {
            return resolveLocalizedText((JSONObject) value, currentLanguage);
        } else if (value instanceof String) {
            return ((String) value).trim();
        } else {
            return "";
        }
    }

}