package project.android.imageprocessing.entity;

/**
 * 本demo中道具的实体类
 * Created by tujh on 2018/2/7.
 */

public class Effect {
    public static final int EFFECT_TYPE_NONE = 0;
    public static final int EFFECT_TYPE_NORMAL = 1;
    public static final int EFFECT_TYPE_PORTRAIT_DRIVE = 9;

    private String bundleName;
    private int resId;
    private String path;
    private int maxFace;
    private int effectType;
    private String description;

    public Effect(String bundleName, int resId, String path, int maxFace, int effectType, String description) {
        this.bundleName = bundleName;
        this.resId = resId;
        this.path = path;
        this.maxFace = maxFace;
        this.effectType = effectType;
        this.description = description;
    }

    public String bundleName() {
        return bundleName;
    }

    public int resId() {
        return resId;
    }

    public String path() {
        return path;
    }

    public int maxFace() {
        return maxFace;
    }

    public int effectType() {
        return effectType;
    }

    public String description() {
        return description;
    }
}
