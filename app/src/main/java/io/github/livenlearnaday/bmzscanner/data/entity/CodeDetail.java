package io.github.livenlearnaday.bmzscanner.data.entity;


import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(
        tableName = "codes"
)
public class CodeDetail {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String codeString;

    private boolean isScreenCapped;

    private String detectionMethod;

    private String codeLength;


    public CodeDetail(String codeString, String codeLength) {
        this.codeString = codeString;
        this.codeLength = codeLength;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getCodeString() {
        return codeString;
    }

    public void setCodeString(String codeString) {
        this.codeString = codeString;
    }

    public boolean isScreenCapped() {
        return isScreenCapped;
    }

    public void setScreenCapped(boolean screenCapped) {
        isScreenCapped = screenCapped;
    }

    public String getDetectionMethod() {
        return detectionMethod;
    }

    public void setDetectionMethod(String detectionMethod) {
        this.detectionMethod = detectionMethod;
    }

    public String getCodeLength() {
        return codeLength;
    }

    public void setCodeLength(String codeLength) {
        this.codeLength = codeLength;
    }
}
