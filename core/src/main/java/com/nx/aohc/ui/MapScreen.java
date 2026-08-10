package com.nx.aohc.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.nx.aohc.AgeOfHistoryOfConquest;
import com.nx.aohc.game.Country;
import com.nx.aohc.game.CountryAI;
import com.nx.aohc.game.Diplomacy;
import com.nx.aohc.game.GameState;
import com.nx.aohc.game.Province;
import com.nx.aohc.game.TurnManager;
import com.nx.aohc.formable.Formable;
import com.nx.aohc.formable.FormableManager;
import com.nx.aohc.graphics.QualitySettings;
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
    private final TurnManager turnManager;
    private final Diplomacy diplomacy;
    private final CountryAI countryAI;
    private final FormableManager formableManager;
    private final OrthographicCamera camera;
    private final MapCameraController cameraController;
    private final Stage stage;

    private final IntArray highlightedProvinces = new IntArray();
    private final Color selectionColor = new Color(1f, 1f, 1f, 1f);
    private final Color targetColor = new Color(0.95f, 0.85f, 0.45f, 1f);

    private Label headerLabel;
    private Label resourceLabel;
    private Label provinceNameLabel;
    private Label provinceDetailLabel;
    private Label messageLabel;
    private Table actionBar;
    private Cell<Table> editorCell;
    private Table editorPanel;
    private Label editorTargetLabel;
    private TextButton recruitButton;
    private TextButton endTurnButton;
    private TextButton formNationButton;
    private TextButton declareWarButton;
    private TextButton offerPeaceButton;
    private Label diplomacyLabel;

    private int selectedProvinceId;
    private boolean editorMode;
    private boolean countrySelectionPending = true;
    private Country editorTargetCountry;

    public MapScreen(AgeOfHistoryOfConquest game, Scenario scenario) {
        this.game = game;
        this.scenario = scenario;
        this.provinceMap = game.getAssets().getProvinceMap();
        this.mapRenderer = new MapRenderer(provinceMap, game.getQualitySettings());
        this.gameState = new GameState(provinceMap);
        this.diplomacy = new Diplomacy();
        this.turnManager = new TurnManager(gameState, diplomacy);
        this.countryAI = new CountryAI(gameState, turnManager, diplomacy);
        this.turnManager.setCountryAI(countryAI);
        this.formableManager = new FormableManager(gameState, game.getModLoader().getFormables());
        this.camera = new OrthographicCamera();
        this.stage = new Stage(new ScreenViewport(), game.getBatch());
        this.cameraController = new MapCameraController(camera, provinceMap, this);

        gameState.applyScenario(scenario, game.getAssets().getDefaultCountries(), game.getLocalization().getActiveLanguage());
        turnManager.initialiseCountries();
        gameState.paintAll(mapRenderer);

        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(provinceMap.getWidth() * 0.5f, provinceMap.getHeight() * 0.5f, 0f);
        camera.zoom = cameraController.getDefaultZoom();
        cameraController.updateViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        mapRenderer.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

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
        resourceLabel = new Label("", game.getSkin(), "small");

        Table headerColumn = new Table();
        headerColumn.left();
        headerColumn.add(headerLabel).left().growX().row();
        headerColumn.add(resourceLabel).left().growX().padTop(2f * scale).row();

        TextButton backButton = new TextButton(localization.get("common.back"), game.getSkin());
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
                dispose();
            }
        });

        TextButton editorToggleButton = new TextButton(localization.get("editor.toggle"), game.getSkin());
        editorToggleButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                setEditorMode(!editorMode);
            }
        });

        topBar.add(headerColumn).left().growX();
        topBar.add(editorToggleButton).right().padLeft(8f * scale);
        topBar.add(backButton).right().padLeft(8f * scale);

        root.add(topBar).growX().row();
        root.add().grow().row();

        messageLabel = new Label("", game.getSkin(), "small");
        messageLabel.setWrap(true);
        root.add(messageLabel).growX().padBottom(6f * scale).row();

        Table provincePanel = new Table();
        provincePanel.setBackground(game.getSkin().getDrawable("panel"));
        provincePanel.pad(12f * scale);
        provincePanel.left();

        provinceNameLabel = new Label(localization.get("map.noSelection"), game.getSkin(), "bold");
        provinceDetailLabel = new Label("", game.getSkin(), "small");
        provinceDetailLabel.setWrap(true);
        diplomacyLabel = new Label("", game.getSkin(), "small");

        Table infoColumn = new Table();
        infoColumn.left();
        infoColumn.add(provinceNameLabel).left().growX().row();
        infoColumn.add(provinceDetailLabel).left().growX().padTop(4f * scale).row();
        infoColumn.add(diplomacyLabel).left().growX().padTop(2f * scale).row();

        actionBar = new Table();
        actionBar.right();

        recruitButton = new TextButton(localization.get("action.recruit"), game.getSkin());
        recruitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                performRecruit();
            }
        });

        endTurnButton = new TextButton(localization.get("action.endTurn"), game.getSkin(), "accent");
        endTurnButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                performEndTurn();
            }
        });

        declareWarButton = new TextButton(localization.get("diplomacy.declareWar"), game.getSkin());
        declareWarButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                performDeclareWar();
            }
        });

        offerPeaceButton = new TextButton(localization.get("diplomacy.offerPeace"), game.getSkin());
        offerPeaceButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                performOfferPeace();
            }
        });

        formNationButton = new TextButton(localization.get("formable.button"), game.getSkin());
        formNationButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                showFormableList();
            }
        });

        actionBar.add(formNationButton).padRight(8f * scale);
        actionBar.add(declareWarButton).padRight(8f * scale);
        actionBar.add(offerPeaceButton).padRight(8f * scale);
        actionBar.add(recruitButton).padRight(8f * scale);
        actionBar.add(endTurnButton);

        provincePanel.add(infoColumn).growX().left();
        provincePanel.add(actionBar).right().padLeft(12f * scale);

        root.add(provincePanel).growX().row();

        buildEditorPanel();
        editorCell = root.add((Table) null).growX().padTop(8f * scale);
        editorCell.getTable().row();

        updateHeader();
        updateActionAvailability();

        if (countrySelectionPending) {
            showCountryChooser();
        }
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
        clearHighlights();
        updateHeader();
        updateActionAvailability();
    }

    private void showCountryChooser() {
        Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        final Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(game.getSkin().getDrawable("panel"));
        overlay.pad(16f * scale);
        overlay.top();

        Label title = new Label(localization.get("start.chooseCountry"), game.getSkin(), "bold");
        Label hint = new Label(localization.get("start.chooseHint"), game.getSkin(), "small");
        hint.setWrap(true);

        overlay.add(title).left().row();
        overlay.add(hint).left().growX().padBottom(10f * scale).row();

        Table listTable = new Table();
        listTable.top().left();
        listTable.defaults().growX().padBottom(4f * scale);

        Array<Country> countries = new Array<Country>(gameState.getCountryList());
        countries.sort(new java.util.Comparator<Country>() {
            @Override
            public int compare(Country first, Country second) {
                return second.ownedProvinces.size - first.ownedProvinces.size;
            }
        });

        for (int index = 0; index < countries.size; index++) {
            final Country country = countries.get(index);
            TextButton button = new TextButton(country.name + "   " + country.ownedProvinces.size, game.getSkin());
            button.getLabel().setColor(country.color);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    startAsCountry(country);
                    overlay.remove();
                }
            });
            listTable.add(button).row();
        }

        ScrollPane scrollPane = new ScrollPane(listTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        overlay.add(scrollPane).grow().row();

        stage.addActor(overlay);
    }

    private void startAsCountry(Country country) {
        gameState.setPlayerCountry(country);
        countrySelectionPending = false;

        if (country.ownedProvinces.size > 0) {
            Province capital = provinceMap.getProvince(country.capitalProvince > 0
                    ? country.capitalProvince
                    : country.ownedProvinces.get(0));
            if (capital != null) {
                cameraController.focusOn(capital.centroidX, provinceMap.getHeight() - capital.centroidY);
                camera.zoom = Math.max(0.25f, cameraController.getDefaultZoom() * 0.25f);
                cameraController.clampCamera();
            }
        }

        updateHeader();
        updateActionAvailability();
        setMessage(game.getLocalization().format("start.playingAs", country.name));
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
            setMessage(game.getLocalization().format("editor.saved", scenarioId));
        } catch (Exception exception) {
            Gdx.app.error("MapScreen", "Scenario export failed", exception);
            setMessage(game.getLocalization().get("editor.saveFailed"));
        }
    }

    private void performRecruit() {
        Country player = gameState.getPlayerCountry();
        Province province = provinceMap.getProvince(selectedProvinceId);
        if (player == null || province == null) {
            return;
        }
        if (turnManager.recruit(player, province)) {
            setMessage(game.getLocalization().format("action.recruited", TurnManager.RECRUIT_BATCH, province.name));
        } else {
            setMessage(game.getLocalization().get("action.cannotRecruit"));
        }
        refreshProvincePanel();
        updateHeader();
        updateActionAvailability();
    }

    private void showFormableList() {
        final Localization localization = game.getLocalization();
        final Country player = gameState.getPlayerCountry();
        if (player == null) {
            return;
        }
        float scale = game.getUiScale();

        final Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(game.getSkin().getDrawable("panel"));
        overlay.pad(16f * scale);
        overlay.top();

        Label title = new Label(localization.get("formable.title"), game.getSkin(), "bold");
        Label hint = new Label(localization.get("formable.hint"), game.getSkin(), "small");
        hint.setWrap(true);

        overlay.add(title).left().row();
        overlay.add(hint).left().growX().padBottom(10f * scale).row();

        Table listTable = new Table();
        listTable.top().left();
        listTable.defaults().growX().padBottom(6f * scale);

        Array<Formable> candidates = formableManager.getCandidates(player);
        if (candidates.size == 0) {
            listTable.add(new Label(localization.get("formable.none"), game.getSkin(), "small")).left().row();
        }

        for (int index = 0; index < candidates.size; index++) {
            final Formable formable = candidates.get(index);
            boolean satisfied = formableManager.isSatisfied(player, formable);
            int owned = formableManager.countOwnedRequirements(player, formable);

            Table row = new Table();
            row.setBackground(game.getSkin().getDrawable("panel-light"));
            row.pad(10f * scale);
            row.left();

            Label name = new Label(formable.getDisplayName(localization.getActiveLanguage()), game.getSkin(), "bold");
            name.setColor(formable.red, formable.green, formable.blue, 1f);
            Label progress = new Label(localization.format("formable.progress",
                    owned, formable.requiredProvinces.size), game.getSkin(), "small");

            Table textColumn = new Table();
            textColumn.left();
            textColumn.add(name).left().growX().row();
            textColumn.add(progress).left().growX().padTop(2f * scale).row();

            TextButton proclaimButton = new TextButton(localization.get("formable.proclaim"),
                    game.getSkin(), satisfied ? "accent" : "default");
            proclaimButton.setDisabled(!satisfied);
            proclaimButton.setColor(satisfied ? Color.WHITE : new Color(1f, 1f, 1f, 0.4f));
            proclaimButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (formableManager.form(player, formable, localization.getActiveLanguage())) {
                        gameState.paintAll(mapRenderer);
                        setMessage(localization.format("formable.formed",
                                formable.getDisplayName(localization.getActiveLanguage())));
                        updateHeader();
                        refreshProvincePanel();
                        updateActionAvailability();
                        overlay.remove();
                    }
                }
            });

            row.add(textColumn).growX().left();
            row.add(proclaimButton).right().padLeft(10f * scale).width(140f * scale);
            listTable.add(row).row();
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

    private void performDeclareWar() {
        Country player = gameState.getPlayerCountry();
        Country target = gameState.getCountryOfProvince(selectedProvinceId);
        if (player == null || target == null || target == player) {
            return;
        }
        if (!diplomacy.canDeclareWar(player.id, target.id)) {
            setMessage(game.getLocalization().get("diplomacy.cannotDeclare"));
            return;
        }
        diplomacy.declareWar(player.id, target.id);
        setMessage(game.getLocalization().format("diplomacy.warDeclared", target.name));
        refreshProvincePanel();
        updateActionAvailability();
    }

    private void performOfferPeace() {
        Country player = gameState.getPlayerCountry();
        Country target = gameState.getCountryOfProvince(selectedProvinceId);
        if (player == null || target == null || target == player) {
            return;
        }
        if (!diplomacy.isAtWar(player.id, target.id)) {
            return;
        }
        if (countryAI.considerPlayerPeaceOffer(target, player)) {
            setMessage(game.getLocalization().format("diplomacy.peaceAccepted", target.name));
        } else {
            setMessage(game.getLocalization().format("diplomacy.peaceRejected", target.name));
        }
        refreshProvincePanel();
        updateActionAvailability();
    }

    private void performEndTurn() {
        CountryAI.TurnReport report = turnManager.endTurn();
        clearHighlights();
        selectedProvinceId = 0;
        gameState.paintAll(mapRenderer);
        refreshProvincePanel();
        updateHeader();
        updateActionAvailability();
        setMessage(buildTurnReport(report));
    }

    private String buildTurnReport(CountryAI.TurnReport report) {
        Localization localization = game.getLocalization();
        StringBuilder builder = new StringBuilder();
        builder.append(localization.format("action.turnAdvanced", gameState.getTurnNumber()));

        if (report.warsDeclaredOnPlayer > 0) {
            builder.append("   ");
            StringBuilder names = new StringBuilder();
            for (int index = 0; index < report.attackersOnPlayer.size; index++) {
                if (index > 0) {
                    names.append(", ");
                }
                names.append(report.attackersOnPlayer.get(index));
            }
            builder.append(localization.format("report.warsDeclared", names.toString()));
        }
        if (report.provincesTakenFromPlayer > 0) {
            builder.append("   ");
            builder.append(localization.format("report.provincesLost", report.provincesTakenFromPlayer));
        }
        if (report.peaceOffersAcceptedByPlayer > 0) {
            builder.append("   ");
            builder.append(localization.get("report.peaceMade"));
        }
        return builder.toString();
    }

    private void updateHeader() {
        Localization localization = game.getLocalization();
        QualitySettings qualitySettings = game.getQualitySettings();

        String modeText = editorMode ? "  ·  " + localization.get("editor.active") : "";
        headerLabel.setText(scenario.getDisplayName(localization.getActiveLanguage())
                + "  ·  " + localization.format("map.year", gameState.getCurrentYear())
                + "  ·  " + localization.format("map.turn", gameState.getTurnNumber())
                + modeText);

        Country player = gameState.getPlayerCountry();
        if (player == null) {
            resourceLabel.setText(localization.get(qualitySettings.getEffectiveProfileKey()));
        } else {
            resourceLabel.setText(localization.format("map.resources",
                    player.name, player.gold, player.incomePerTurn, player.manpower, player.ownedProvinces.size));
        }
    }

    private void updateActionAvailability() {
        Country player = gameState.getPlayerCountry();
        Province province = provinceMap.getProvince(selectedProvinceId);
        Country owner = gameState.getCountryOfProvince(selectedProvinceId);

        boolean canRecruit = !editorMode && turnManager.canRecruit(player, province);
        setButtonEnabled(recruitButton, canRecruit);

        boolean playing = !editorMode && player != null;
        setButtonEnabled(endTurnButton, playing);
        setButtonEnabled(formNationButton, playing && formableManager.getCandidates(player).size > 0);

        boolean foreignSelected = playing && owner != null && owner != player;
        setButtonEnabled(declareWarButton, foreignSelected && diplomacy.canDeclareWar(player.id, owner.id));
        setButtonEnabled(offerPeaceButton, foreignSelected && diplomacy.isAtWar(player.id, owner.id));
    }

    private void setButtonEnabled(TextButton button, boolean enabled) {
        button.setDisabled(!enabled);
        button.setColor(enabled ? Color.WHITE : new Color(1f, 1f, 1f, 0.4f));
    }

    private void setMessage(String text) {
        messageLabel.setText(text);
    }

    private void clearHighlights() {
        for (int index = 0; index < highlightedProvinces.size; index++) {
            gameState.paintProvince(mapRenderer, highlightedProvinces.get(index));
        }
        highlightedProvinces.clear();
    }

    private void applyHighlights(Province origin) {
        clearHighlights();
        if (origin == null) {
            return;
        }

        mapRenderer.setProvinceColor(origin.id, selectionColor);
        highlightedProvinces.add(origin.id);

        Country player = gameState.getPlayerCountry();
        if (player == null || !player.id.equals(origin.owner) || origin.hasActedThisTurn || origin.army <= 0) {
            return;
        }

        for (int index = 0; index < origin.neighbours.length; index++) {
            int neighbourId = origin.neighbours[index];
            mapRenderer.setProvinceColor(neighbourId, targetColor);
            highlightedProvinces.add(neighbourId);
        }
    }

    private void refreshProvincePanel() {
        Localization localization = game.getLocalization();
        Province province = provinceMap.getProvince(selectedProvinceId);

        if (province == null) {
            provinceNameLabel.setText(localization.get("map.noSelection"));
            provinceDetailLabel.setText("");
            return;
        }

        Country owner = gameState.getCountryOfProvince(province.id);
        provinceNameLabel.setText(province.name);
        provinceDetailLabel.setText(localization.format("map.provinceInfo",
                owner != null ? owner.name : localization.get("map.unclaimed"),
                province.army,
                province.population,
                province.economy));

        Country player = gameState.getPlayerCountry();
        if (player == null || owner == null || owner == player) {
            diplomacyLabel.setText("");
        } else {
            int state = diplomacy.getState(player.id, owner.id);
            diplomacyLabel.setText(localization.format("diplomacy.status",
                    owner.name, localization.get(diplomacy.stateKey(state))));
        }
    }

    @Override
    public void onProvinceClicked(int provinceId, float worldX, float worldY) {
        Localization localization = game.getLocalization();

        if (provinceId <= 0) {
            clearHighlights();
            selectedProvinceId = 0;
            provinceNameLabel.setText(localization.get("map.sea"));
            provinceDetailLabel.setText("");
            updateActionAvailability();
            return;
        }

        Province province = provinceMap.getProvince(provinceId);
        if (province == null) {
            return;
        }

        if (editorMode) {
            if (editorTargetCountry != null) {
                gameState.transferProvince(provinceId, editorTargetCountry.id);
                gameState.paintProvince(mapRenderer, provinceId);
                gameState.recomputeCountryStatistics();
            }
            selectedProvinceId = provinceId;
            refreshProvincePanel();
            return;
        }

        Country player = gameState.getPlayerCountry();
        Province origin = provinceMap.getProvince(selectedProvinceId);

        boolean originIsOwnActionable = player != null
                && origin != null
                && player.id.equals(origin.owner)
                && !origin.hasActedThisTurn
                && origin.army > 0;

        if (originIsOwnActionable && origin.id != provinceId && turnManager.areAdjacent(origin, province)) {
            TurnManager.ActionResult result = turnManager.performAction(player, origin, province);
            handleActionResult(result, origin, province);
            return;
        }

        selectedProvinceId = provinceId;
        applyHighlights(province);
        refreshProvincePanel();
        updateActionAvailability();
    }

    private void handleActionResult(TurnManager.ActionResult result, Province origin, Province target) {
        Localization localization = game.getLocalization();

        switch (result.type) {
            case TurnManager.ACTION_REINFORCED:
                setMessage(localization.format("action.reinforced", target.name, target.army));
                break;
            case TurnManager.ACTION_CAPTURED:
                setMessage(localization.format("action.captured", target.name, result.attackerLosses, result.defenderLosses));
                gameState.removeDeadCountries();
                break;
            case TurnManager.ACTION_REPELLED:
                setMessage(localization.format("action.repelled", target.name, result.attackerLosses, result.defenderLosses));
                break;
            default:
                setMessage(localization.get(result.messageKey));
                break;
        }

        clearHighlights();
        gameState.paintProvince(mapRenderer, origin.id);
        gameState.paintProvince(mapRenderer, target.id);
        gameState.recomputeCountryStatistics();

        selectedProvinceId = target.id;
        refreshProvincePanel();
        updateHeader();
        updateActionAvailability();
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
        mapRenderer.render(camera.combined, game.getBatch());

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        cameraController.updateViewport(width, height);
        mapRenderer.resize(width, height);
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
