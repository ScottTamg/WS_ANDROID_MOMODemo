package project.android.imageprocessing.entity;

import com.wushuangtech.myvideo.R;

import java.util.ArrayList;

/**
 * Created by tujh on 2018/1/30.
 */

public enum EffectEnum {

    EffectNone("none", R.drawable.ic_delete_all, "none", 1, Effect.EFFECT_TYPE_NONE, ""),

    Effect_fengya_ztt_fu("fengya_ztt_fu", R.drawable.fengya_ztt_fu, "normal/fengya_ztt_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_hudie_lm_fu("hudie_lm_fu", R.drawable.hudie_lm_fu, "normal/hudie_lm_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_touhua_ztt_fu("touhua_ztt_fu", R.drawable.touhua_ztt_fu, "normal/touhua_ztt_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_juanhuzi_lm_fu("juanhuzi_lm_fu", R.drawable.juanhuzi_lm_fu, "normal/juanhuzi_lm_fu.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_mask_hat("mask_hat", R.drawable.mask_hat, "normal/mask_hat.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_yazui("yazui", R.drawable.yazui, "normal/yazui.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),
    Effect_yuguan("yuguan", R.drawable.yuguan, "normal/yuguan.bundle", 4, Effect.EFFECT_TYPE_NORMAL, ""),

    Effect_picasso_e1("picasso_e1", R.drawable.picasso_e1, "portrait_drive/picasso_e1.bundle", 1, Effect.EFFECT_TYPE_PORTRAIT_DRIVE, ""),
    Effect_picasso_e2("picasso_e2", R.drawable.picasso_e2, "portrait_drive/picasso_e2.bundle", 1, Effect.EFFECT_TYPE_PORTRAIT_DRIVE, ""),
    Effect_picasso_e3("picasso_e3", R.drawable.picasso_e3, "portrait_drive/picasso_e3.bundle", 1, Effect.EFFECT_TYPE_PORTRAIT_DRIVE, "");

    private String bundleName;
    private int resId;
    private String path;
    private int maxFace;
    private int effectType;
    private String description;

    EffectEnum(String name, int resId, String path, int maxFace, int effectType, String description) {
        this.bundleName = name;
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

    public Effect effect() {
        return new Effect(bundleName, resId, path, maxFace, effectType, description);
    }

    public static ArrayList<Effect> getEffectsByEffectType(int effectType) {
        ArrayList<Effect> effects = new ArrayList<>();
        effects.add(EffectNone.effect());
        for (EffectEnum e : EffectEnum.values()) {
            if (e.effectType == effectType) {
                effects.add(e.effect());
            }
        }
        return effects;
    }
}
