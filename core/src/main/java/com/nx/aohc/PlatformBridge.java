package com.nx.aohc;

import com.badlogic.gdx.files.FileHandle;

public interface PlatformBridge {

    FileHandle getModsDirectory();

    FileHandle getSaveDirectory();

    String getDeviceLanguage();
}
