package com.nx.aohc.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
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
import com.nx.aohc.game.Country;
import com.nx.aohc.game.GameState;
import com.nx.aohc.game.Province;
import com.nx.aohc.localization.Localization;
import com.nx.aohc.map.MapCameraController;
import com.nx.aohc.map.MapRenderer;
import com.nx.aohc.map.ProvinceMap;
import com.nx.aohc.scenario.Scenario;
import com.nx.aohc.scenario.ScenarioExporter;

public class MapScreen implements Screen, MapCameraController.ProvinceClickListener {

    private final AgeOfHistoryOfConquest game;
    private final Scenario scenario;
    private final ProvinceMap provinceMap;
    private final MapRenderer mapRenderer;
    private final GameState gameState;
    private final OrthographicCamera camera;
    private final MapCameraController cameraController;
    private final Stage stage;

    private Label headerLabel;
    private Label provinceNameLabel;
    private Label provinceDetailLabel;
    private Table provincePanel;
    private Table editorPanel;
    private com.badlogic.gdx.scenes.scene2d.ui.Cell<Table> editorCell;
    private TextButton editorToggleButton;
    private Label editorTargetLabel;

    private int selectedProvinceId;
    private boolean editorMode;
    private Country editorTargetCountry;

    public MapScreen(AgeOfHistoryOfConquest game, Scenario scenario) {
        this.game = game;
        this.scenario = scenario;
        this.provinceMap = game.getAssets().getProvinceMap();
        this.mapRenderer = new MapRenderer(provinceMap);
        this.gameState = new GameState(provinceMap);
        this.camera = new OrthographicCamera();
        this.stage = new Stage(new ScreenViewport(), game.getBatch());
        this.cameraController = new MapCameraController(camera, provinceMap, this);

        gameState.applyScenario(scenario, game.getAssets().getDefaultCountries(), game.getLocalization().getActiveLanguage());
        gameState.paintAll(mapRenderer);

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(provinceMap.getWidth() * 0.5f, provinceMap.getHeight() * 0.5f, 0f);
        camera.zoom = cameraController.getDefaultZoom();
        cameraController.updateViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        buildInterface();
    }

    private void buildInterface() {
        stage.clear();
        Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        Table root = new Table();
        root.setFillParent(true);
        root.pad(10f * scale);
        root.top();
        stage.addActor(root);

        Table topBar = new Table();
        topBar.setBackground(game.getSkin().getDrawable("panel"));
        topBar.pad(10f * scale);

        headerLabel = new Label("", game.getSkin(), "bold");
        updateHeader();

        TextButton backButton = new TextButton(localization.get("common.back"), game.getSkin());
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        editorToggleButton = new TextButton(localization.get("editor.toggle"), game.getSkin());
        editorToggleButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setEditorMode(!editorMode);
            }
        });

        topBar.add(headerLabel).left().growX();
        topBar.add(editorToggleButton).right().padLeft(8f * scale);
        topBar.add(backButton).right().padLeft(8f * scale);

        root.add(topBar).growX().row();
        root.add().grow().row();

        provincePanel = new Table();
        provincePanel.setBackground(game.getSkin().getDrawable("panel"));
        provincePanel.pad(12f * scale);
        provincePanel.left();

        provinceNameLabel = new Label(localization.get("map.noSelection"), game.getSkin(), "bold");
        provinceDetailLabel = new Label("", game.getSkin(), "small");
        provinceDetailLabel.setWrap(true);

        Table infoColumn = new Table();
        infoColumn.left();
        infoColumn.add(provinceNameLabel).left().growX().row();
        infoColumn.add(provinceDetailLabel).left().growX().padTop(4f * scale).row();
        provincePanel.add(infoColumn).growX();

        root.add(provincePanel).growX().row();

        buildEditorPanel();
        editorCell = root.add((Table) null).growX().padTop(8f * scale);
        editorCell.getTable().row();
    }

    private void buildEditorPanel() {
        Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        editorPanel = new Table();
        editorPanel.setBackground(game.getSkin().getDrawable("panel"));
        editorPanel.pad(10f * scale);

        editorTargetLabel = new Label(localization.get("editor.noTarget"), game.getSkin(), "small");

        TextButton pickCountryButton = new TextButton(localization.get("editor.pickCountry"), game.getSkin());
        pickCountryButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showCountryPicker();
            }
        });

        TextButton saveButton = new TextButton(localization.get("editor.save"), game.getSkin(), "accent");
        saveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                exportScenario();
            }
        });

        editorPanel.add(editorTargetLabel).left().growX();
        editorPanel.add(pickCountryButton).right().padLeft(8f * scale);
        editorPanel.add(saveButton).right().padLeft(8f * scale);
    }

    private void setEditorMode(boolean enabled) {
        editorMode = enabled;
        editorCell.setActor(enabled ? editorPanel : null);
        editorCell.getTable().invalidateHierarchy();
        updateHeader();
    }

    private void showCountryPicker() {
        Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        final Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(game.getSkin().getDrawable("panel"));
        overlay.pad(16f * scale);
        overlay.top();

        Label title = new Label(localization.get("editor.pickCountry"), game.getSkin(), "bold");
        overlay.add(title).left().padBottom(10f * scale).row();

        Table listTable = new Table();
        listTable.top().left();
        listTable.defaults().growX().padBottom(4f * scale);

        Array<Country> countries = new Array<Country>(gameState.getCountryList());
        countries.sort(new java.util.Comparator<Country>() {
            @Override
            public int compare(Country first, Country second) {
                return first.name.compareToIgnoreCase(second.name);
            }
        });

        for (int index = 0; index < countries.size; index++) {
            final Country country = countries.get(index);
            TextButton button = new TextButton(country.name + "  (" + country.ownedProvinces.size + ")", game.getSkin());
            button.getLabel().setColor(country.color);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    editorTargetCountry = country;
                    editorTargetLabel.setText(game.getLocalization().format("editor.target", country.name));
                    overlay.remove();
                }
            });
            listTable.add(button).row();
        }

        ScrollPane scrollPane = new ScrollPane(listTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        overlay.add(scrollPane).grow().row();

        TextButton closeButton = new TextButton(localization.get("common.close"), game.getSkin());
        closeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                overlay.remove();
            }
        });
        overlay.add(closeButton).right().padTop(10f * scale).row();

        stage.addActor(overlay);
    }

    private void exportScenario() {
        try {
            String scenarioId = scenario.id + "_edited";
            ScenarioExporter.export(gameState, scenarioId, scenario.startYear,
                    game.getPlatformBridge().getModsDirectory());
            provinceDetailLabel.setText(game.getLocalization().format("editor.saved", scenarioId));
        } catch (Exception exception) {
            Gdx.app.error("MapScreen", "Scenario export failed", exception);
            provinceDetailLabel.setText(game.getLocalization().get("editor.saveFailed"));
        }
    }

    private void updateHeader() {
        Localization localization = game.getLocalization();
        String modeText = editorMode ? "  ·  " + localization.get("editor.active") : "";
        headerLabel.setText(scenario.getDisplayName(localization.getActiveLanguage())
                + "  ·  " + localization.format("map.year", gameState.getCurrentYear())
                + modeText);
    }

    @Override
    public void onProvinceClicked(int provinceId, float worldX, float worldY) {
        Localization localization = game.getLocalization();

        if (provinceId <= 0) {
            selectedProvinceId = 0;
            provinceNameLabel.setText(localization.get("map.sea"));
            provinceDetailLabel.setText("");
            return;
        }

        Province province = provinceMap.getProvince(provinceId);
        if (province == null) {
            return;
        }

        if (editorMode && editorTargetCountry != null) {
            gameState.transferProvince(provinceId, editorTargetCountry.id);
            gameState.paintProvince(mapRenderer, provinceId);
            gameState.recomputeCountryStatistics();
        }

        selectedProvinceId = provinceId;
        Country owner = gameState.getCountryOfProvince(provinceId);
        provinceNameLabel.setText(province.name);
        provinceDetailLabel.setText(localization.format("map.provinceInfo",
                owner != null ? owner.name : localization.get("map.unclaimed"),
                province.population,
                province.economy,
                province.neighbours.length));
    }

    @Override
    public void show() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(cameraController);
        Gdx.input.setInputProcessor(multiplexer);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(mapRenderer.getSeaColor().r, mapRenderer.getSeaColor().g, mapRenderer.getSeaColor().b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        mapRenderer.render(camera.combined);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        cameraController.updateViewport(width, height);
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
        mapRenderer.dispose();
    }
}
