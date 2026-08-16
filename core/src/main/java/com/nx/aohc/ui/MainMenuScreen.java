package com.nx.aohc.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.nx.aohc.AgeOfHistoryOfConquest;
import com.nx.aohc.game.GameSettings;
import com.nx.aohc.graphics.QualitySettings;
import com.nx.aohc.localization.Localization;
import com.nx.aohc.scenario.Scenario;

public class MainMenuScreen implements Screen {

    private final AgeOfHistoryOfConquest game;
    private final Stage stage;
    private Table rootTable;

    public MainMenuScreen(AgeOfHistoryOfConquest game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getBatch());
        buildInterface();
    }

    private void buildInterface() {
        stage.clear();
        rootTable = new Table();
        rootTable.setFillParent(true);
        rootTable.pad(20f * game.getUiScale());
        stage.addActor(rootTable);

        Localization localization = game.getLocalization();

        Label title = new Label(localization.get("game.title"), game.getSkin(), "title");
        Label subtitle = new Label(localization.get("menu.subtitle"), game.getSkin(), "small");

        rootTable.add(title).left().padBottom(4f * game.getUiScale()).row();
        rootTable.add(subtitle).left().padBottom(16f * game.getUiScale()).row();

        Table scenarioTable = new Table();
        scenarioTable.top().left();
        scenarioTable.defaults().growX().padBottom(8f * game.getUiScale());

        Array<Scenario> scenarios = game.getModLoader().getScenarios();
        scenarios.sort(new java.util.Comparator<Scenario>() {
            @Override
            public int compare(Scenario first, Scenario second) {
                return first.startYear - second.startYear;
            }
        });

        for (int index = 0; index < scenarios.size; index++) {
            scenarioTable.add(buildScenarioRow(scenarios.get(index))).row();
        }

        ScrollPane scrollPane = new ScrollPane(scenarioTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        rootTable.add(scrollPane).grow().row();

        Table bottomBar = new Table();
        bottomBar.defaults().padTop(12f * game.getUiScale()).padRight(8f * game.getUiScale());

        final TextButton languageButton = new TextButton(
                localization.get("menu.language") + ": " + localization.getActiveLanguage().toUpperCase(),
                game.getSkin());
        languageButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                cycleLanguage();
            }
        });

        Label versionLabel = new Label("v" + AgeOfHistoryOfConquest.VERSION
                + "  ·  " + localization.format("menu.mods.count", game.getModLoader().getMods().size),
                game.getSkin(), "small");

        final QualitySettings qualitySettings = game.getQualitySettings();
        TextButton qualityButton = new TextButton(
                localization.get("menu.quality") + ": " + localization.get(qualitySettings.getSelectedProfileKey()),
                game.getSkin());
        qualityButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                qualitySettings.cycleProfile();
                buildInterface();
            }
        });

        TextButton achievementsButton = new TextButton(
                localization.format("menu.achievements",
                        game.getAchievementManager().getUnlockedCount(),
                        game.getAchievementManager().getAchievements().size),
                game.getSkin());
        achievementsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new AchievementsScreen(game));
                dispose();
            }
        });

        TextButton multiplayerButton = new TextButton(localization.get("multiplayer.title"), game.getSkin(), "accent");
        multiplayerButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MultiplayerScreen(game));
                dispose();
            }
        });

        bottomBar.add(multiplayerButton).left();
        bottomBar.add(languageButton).left();
        bottomBar.add(qualityButton).left();
        bottomBar.add(achievementsButton).left();
        bottomBar.add(versionLabel).right().expandX();
        rootTable.add(bottomBar).growX().row();

        final GameSettings gameSettings = game.getGameSettings();

        Table gameplayBar = new Table();
        gameplayBar.defaults().padTop(8f * game.getUiScale()).padRight(8f * game.getUiScale());

        TextButton difficultyButton = new TextButton(
                localization.get("menu.difficulty") + ": " + localization.get(gameSettings.getDifficultyKey()),
                game.getSkin());
        difficultyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameSettings.cycleDifficulty();
                buildInterface();
            }
        });

        TextButton aggressionDownButton = new TextButton("-10", game.getSkin());
        aggressionDownButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameSettings.adjustAiAggression(-10);
                buildInterface();
            }
        });

        TextButton aggressionUpButton = new TextButton("+10", game.getSkin());
        aggressionUpButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                gameSettings.adjustAiAggression(10);
                buildInterface();
            }
        });

        Label aggressionLabel = new Label(localization.format("menu.aggression",
                gameSettings.getAiAggression()), game.getSkin(), "small");

        gameplayBar.add(difficultyButton).left();
        gameplayBar.add(aggressionDownButton).left();
        gameplayBar.add(aggressionLabel).left();
        gameplayBar.add(aggressionUpButton).left().expandX();
        rootTable.add(gameplayBar).growX().row();
    }

    private Table buildScenarioRow(final Scenario scenario) {
        Localization localization = game.getLocalization();
        String language = localization.getActiveLanguage();

        Table row = new Table();
        row.setBackground(game.getSkin().getDrawable("panel-light"));
        row.pad(12f * game.getUiScale());
        row.left();

        Label name = new Label(scenario.getDisplayName(language), game.getSkin(), "bold");
        name.setWrap(true);

        String yearText = scenario.startYear > 0
                ? String.valueOf(scenario.startYear)
                : localization.get("scenario.year.unknown");
        Label meta = new Label(yearText + "  ·  " + scenario.sourceMod, game.getSkin(), "small");

        Label description = new Label(scenario.getDescription(language), game.getSkin(), "small");
        description.setWrap(true);

        Table textColumn = new Table();
        textColumn.left();
        textColumn.add(name).growX().left().row();
        textColumn.add(meta).growX().left().padTop(2f * game.getUiScale()).row();
        textColumn.add(description).growX().left().padTop(4f * game.getUiScale()).row();

        TextButton playButton = new TextButton(localization.get("menu.play"), game.getSkin(), "accent");
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MapScreen(game, scenario));
                dispose();
            }
        });

        row.add(textColumn).growX().left();
        row.add(playButton).right().padLeft(12f * game.getUiScale()).width(120f * game.getUiScale());
        return row;
    }

    private void cycleLanguage() {
        Array<String> languages = game.getLocalization().getAvailableLanguages();
        if (languages.size == 0) {
            return;
        }
        int currentIndex = languages.indexOf(game.getLocalization().getActiveLanguage(), false);
        int nextIndex = (currentIndex + 1) % languages.size;
        game.getLocalization().setActiveLanguage(languages.get(nextIndex));
        buildInterface();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(UiSkinFactory.BACKGROUND.r, UiSkinFactory.BACKGROUND.g, UiSkinFactory.BACKGROUND.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
