package com.nx.aohc;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;

import java.io.File;
import java.util.Locale;

public class DesktopLauncher {

    public static void main(String[] arguments) {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Age Of History Of Conquest");
        configuration.setWindowedMode(1280, 720);
        configuration.useVsync(true);
        configuration.setForegroundFPS(60);

        new Lwjgl3Application(new AgeOfHistoryOfConquest(new DesktopPlatformBridge()), configuration);
    }

    private static class DesktopPlatformBridge implements PlatformBridge {

        private FileHandle ensure(String name) {
            File directory = new File(System.getProperty("user.home"), ".age-of-history-of-conquest/" + name);
            if (!directory.exists()) {
                directory.mkdirs();
            }
            return new FileHandle(directory);
        }

        @Override
        public FileHandle getModsDirectory() {
            return ensure("mods");
        }

        @Override
        public FileHandle getSaveDirectory() {
            return ensure("saves");
        }

        @Override
        public String getDeviceLanguage() {
            return Locale.getDefault().getLanguage().toLowerCase(Locale.ROOT);
        }
    }
}
