package org.tensorflow.lite.examples.detection;

public class DtItem {
    String dateName;
    String timeName;

    public DtItem(String dateName, String timeName) {
        this.dateName = dateName;
        this.timeName = timeName;
    }

    public String getDateName() {
        return dateName;
    }

    public void setDateName(String dataName) {
        this.dateName = dataName;
    }

    public String getTimeName() {
        return timeName;
    }

    public void setTimeName(String timeName) {
        this.timeName = timeName;
    }
}
