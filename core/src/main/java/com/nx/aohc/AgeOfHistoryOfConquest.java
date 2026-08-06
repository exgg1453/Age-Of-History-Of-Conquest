package com.nx.aohc;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

import com.nx.aohc.game.GameAssets;
import com.nx.aohc.localization.Localization;
import com.nx.aohc.mod.ModLoader;
import com.nx.aohc.ui.MainMenuScreen;
import com.nx.aohc.ui.UiSkinFactory;

public class AgeOfHistoryOfConquest extends Game {

    public static final String VERSION = "0.1.0";

    private final PlatformBridge platformBridge;

    private SpriteBatch batch;
    private Skin skin;
    private Localization localization;
    private ModLoader modLoader;
    private GameAssets assets;
    private float uiScale = 1f;

    public AgeOfHistoryOfConquest(PlatformBridge platformBridge) {
        this.platformBridge = platformBridge;
    }

    @Override
    public void create() {
        uiScale = computeUiScale();
        batch = new SpriteBatch();

        localization = new Localization();
        modLoader = new ModLoader();
        modLoader.loadBaseContent(localization);
        modLoader.loadExternalMods(platformBridge.getModsDirectory(), localization);

        String deviceLanguage = platformBridge.getDeviceLanguage();
        if (deviceLanguage != null && localization.hasLanguage(deviceLanguage)) {
            localization.setActiveLanguage(deviceLanguage);
        } else {
            localization.setActiveLanguage("en");
        }

        skin = UiSkinFactory.create(uiScale);

        assets = new GameAssets(
                modLoader.resolveAsset("map/provinces.png"),
                modLoader.resolveAsset("map/provinces.json"),
                modLoader.resolveAsset("map/countries.json"));

        setScreen(new MainMenuScreen(this));
    }

    private float computeUiScale() {
        float density = Gdx.graphics.getDensity();
        float shortestSide = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        float scale = Math.max(density * 0.85f, shortestSide / 720f);
        return Math.max(0.85f, Math.min(scale, 3f));
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public Skin getSkin() {
        return skin;
    }

    public Localization getLocalization() {
        return localization;
    }

    public ModLoader getModLoader() {
        return modLoader;
    }

    public GameAssets getAssets() {
        return assets;
    }

    public PlatformBridge getPlatformBridge() {
        return platformBridge;
    }

    public float getUiScale() {
        return uiScale;
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        if (assets != null) {
            assets.dispose();
        }
        if (skin != null) {
            skin.dispose();
        }
        if (batch != null) {
            batch.dispose();
        }
    }
}
