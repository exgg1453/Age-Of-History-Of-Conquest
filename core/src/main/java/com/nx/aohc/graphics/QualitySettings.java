package com.nx.aohc.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.GL20;

import java.nio.IntBuffer;

import com.badlogic.gdx.utils.BufferUtils;

public class QualitySettings {

    public static final int PROFILE_AUTOMATIC = 0;
    public static final int PROFILE_LOW = 1;
    public static final int PROFILE_MEDIUM = 2;
    public static final int PROFILE_HIGH = 3;

    public static final int BORDER_MODE_NONE = 0;
    public static final int BORDER_MODE_COUNTRY = 1;
    public static final int BORDER_MODE_FULL = 2;

    private static final String PREFERENCES_NAME = "aohc-graphics";
    private static final String KEY_PROFILE = "profile";

    private final Preferences preferences;
    private final int defaultProfile;

    private int selectedProfile;
    private int effectiveProfile;
    private int maximumTextureSize;

    private float renderScale;
    private int borderMode;
    private boolean linearMapFiltering;
    private boolean continuousRendering;

    public QualitySettings(int defaultProfile) {
        this.defaultProfile = defaultProfile;
        this.preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        this.selectedProfile = preferences.getInteger(KEY_PROFILE, PROFILE_AUTOMATIC);
        detectDevice();
        apply();
    }

    private void detectDevice() {
        IntBuffer buffer = BufferUtils.newIntBuffer(16);
        Gdx.gl.glGetIntegerv(GL20.GL_MAX_TEXTURE_SIZE, buffer);
        maximumTextureSize = buffer.get(0);
        if (maximumTextureSize <= 0) {
            maximumTextureSize = 2048;
        }
    }

    private int resolveAutomaticProfile() {
        if (defaultProfile != PROFILE_AUTOMATIC) {
            return defaultProfile;
        }

        int processors = Runtime.getRuntime().availableProcessors();
        long maximumMemory = Runtime.getRuntime().maxMemory() / (1024L * 1024L);
        int screenPixels = Gdx.graphics.getWidth() * Gdx.graphics.getHeight();

        if (maximumTextureSize < 4096 || processors <= 4 || maximumMemory < 192) {
            return PROFILE_LOW;
        }
        if (processors <= 6 || maximumMemory < 384 || screenPixels > 2600000) {
            return PROFILE_MEDIUM;
        }
        return PROFILE_HIGH;
    }

    public void apply() {
        effectiveProfile = selectedProfile == PROFILE_AUTOMATIC ? resolveAutomaticProfile() : selectedProfile;

        switch (effectiveProfile) {
            case PROFILE_LOW:
                renderScale = 0.55f;
                borderMode = BORDER_MODE_COUNTRY;
                linearMapFiltering = true;
                continuousRendering = true;
                break;
            case PROFILE_MEDIUM:
                renderScale = 0.75f;
                borderMode = BORDER_MODE_FULL;
                linearMapFiltering = true;
                continuousRendering = true;
                break;
            default:
                renderScale = 1f;
                borderMode = BORDER_MODE_FULL;
                linearMapFiltering = true;
                continuousRendering = true;
                break;
        }
    }

    public void setSelectedProfile(int profile) {
        selectedProfile = profile;
        preferences.putInteger(KEY_PROFILE, profile);
        preferences.flush();
        apply();
    }

    public int cycleProfile() {
        int next = selectedProfile + 1;
        if (next > PROFILE_HIGH) {
            next = PROFILE_AUTOMATIC;
        }
        setSelectedProfile(next);
        return next;
    }

    public int getSelectedProfile() {
        return selectedProfile;
    }

    public int getEffectiveProfile() {
        return effectiveProfile;
    }

    public String getSelectedProfileKey() {
        return profileKey(selectedProfile);
    }

    public String getEffectiveProfileKey() {
        return profileKey(effectiveProfile);
    }

    public static String profileKey(int profile) {
        switch (profile) {
            case PROFILE_LOW:
                return "quality.low";
            case PROFILE_MEDIUM:
                return "quality.medium";
            case PROFILE_HIGH:
                return "quality.high";
            default:
                return "quality.auto";
        }
    }

    public float getRenderScale() {
        return renderScale;
    }

    public int getBorderMode() {
        return borderMode;
    }

    public boolean isLinearMapFiltering() {
        return linearMapFiltering;
    }

    public boolean isContinuousRendering() {
        return continuousRendering;
    }

    public int getMaximumTextureSize() {
        return maximumTextureSize;
    }
}
