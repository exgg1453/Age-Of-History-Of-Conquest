package com.nx.aohc;

import android.content.Context;

import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.util.Locale;

public class AndroidPlatformBridge implements PlatformBridge {

    private final Context context;

    public AndroidPlatformBridge(Context context) {
        this.context = context;
    }

    @Override
    public FileHandle getModsDirectory() {
        File externalRoot = context.getExternalFilesDir(null);
        if (externalRoot == null) {
            externalRoot = context.getFilesDir();
        }
        File modsDirectory = new File(externalRoot, "mods");
        if (!modsDirectory.exists()) {
            modsDirectory.mkdirs();
        }
        return new FileHandle(modsDirectory);
    }

    @Override
    public FileHandle getSaveDirectory() {
        File externalRoot = context.getExternalFilesDir(null);
        if (externalRoot == null) {
            externalRoot = context.getFilesDir();
        }
        File saveDirectory = new File(externalRoot, "saves");
        if (!saveDirectory.exists()) {
            saveDirectory.mkdirs();
        }
        return new FileHandle(saveDirectory);
    }

    @Override
    public int getDefaultQualityProfile() {
        if ("lite".equals(BuildConfig.FLAVOR)) {
            return 1;
        }
        return 0;
    }

    @Override
    public String getDeviceLanguage() {
        Locale locale = Locale.getDefault();
        return locale.getLanguage().toLowerCase(Locale.ROOT);
    }
}
