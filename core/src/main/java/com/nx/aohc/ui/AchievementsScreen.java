package com.nx.aohc.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
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
import com.nx.aohc.achievement.Achievement;
import com.nx.aohc.achievement.AchievementManager;
import com.nx.aohc.localization.Localization;

public class AchievementsScreen implements Screen {

    private final AgeOfHistoryOfConquest game;
    private final Stage stage;

    public AchievementsScreen(AgeOfHistoryOfConquest game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getBatch());
        buildInterface();
    }

    private void buildInterface() {
        stage.clear();
        Localization localization = game.getLocalization();
        AchievementManager manager = game.getAchievementManager();
        String language = localization.getActiveLanguage();
        float scale = game.getUiScale();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(20f * scale);
        root.top();
        stage.addActor(root);

        Label title = new Label(localization.get("achievements.title"), game.getSkin(), "title");
        Label progress = new Label(localization.format("achievements.progress",
                manager.getUnlockedCount(), manager.getAchievements().size), game.getSkin(), "small");

        root.add(title).left().row();
        root.add(progress).left().padBottom(14f * scale).row();

        Table listTable = new Table();
        listTable.top().left();
        listTable.defaults().growX().padBottom(6f * scale);

        Array<Achievement> achievements = manager.getAchievements();
        for (int index = 0; index < achievements.size; index++) {
            Achievement achievement = achievements.get(index);
            boolean unlocked = manager.isUnlocked(achievement.id);

            Table row = new Table();
            row.setBackground(game.getSkin().getDrawable("panel-light"));
            row.pad(12f * scale);
            row.left();

            Label name = new Label(achievement.getDisplayName(language), game.getSkin(), "bold");
            Label description = new Label(achievement.getDescription(language), game.getSkin(), "small");
            description.setWrap(true);

            if (!unlocked) {
                name.setColor(new Color(1f, 1f, 1f, 0.45f));
                description.setColor(new Color(1f, 1f, 1f, 0.35f));
            } else {
                name.setColor(UiSkinFactory.ACCENT);
            }

            Table textColumn = new Table();
            textColumn.left();
            textColumn.add(name).left().growX().row();
            textColumn.add(description).left().growX().padTop(2f * scale).row();

            Label status = new Label(localization.get(unlocked ? "achievements.unlocked" : "achievements.locked"),
                    game.getSkin(), "small");

            row.add(textColumn).growX().left();
            row.add(status).right().padLeft(12f * scale);
            listTable.add(row).row();
        }

        ScrollPane scrollPane = new ScrollPane(listTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        root.add(scrollPane).grow().row();

        TextButton backButton = new TextButton(localization.get("common.back"), game.getSkin());
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        root.add(backButton).left().padTop(12f * scale).row();
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
