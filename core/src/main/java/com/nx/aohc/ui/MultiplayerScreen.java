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
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import com.nx.aohc.AgeOfHistoryOfConquest;
import com.nx.aohc.game.Country;
import com.nx.aohc.game.PlayerSlot;
import com.nx.aohc.localization.Localization;
import com.nx.aohc.net.ClientSession;
import com.nx.aohc.net.GameCommand;
import com.nx.aohc.net.HostSession;
import com.nx.aohc.net.LanDiscovery;
import com.nx.aohc.net.LobbyPlayer;
import com.nx.aohc.net.LocalSession;
import com.nx.aohc.net.NetworkSession;
import com.nx.aohc.scenario.Scenario;

public class MultiplayerScreen implements Screen, NetworkSession.Listener {

    private static final int STAGE_MODE = 0;
    private static final int STAGE_HOTSEAT = 1;
    private static final int STAGE_BROWSE = 2;
    private static final int STAGE_LOBBY = 3;

    private final AgeOfHistoryOfConquest game;
    private final Stage stage;
    private final LanDiscovery discovery = new LanDiscovery();
    private final Array<String> hotseatNames = new Array<String>();
    private final Array<PlayerSlot> resolvedSlots = new Array<PlayerSlot>();

    private int currentStage = STAGE_MODE;
    private NetworkSession session;
    private Scenario selectedScenario;
    private String statusText = "";
    private TextField nameField;

    public MultiplayerScreen(AgeOfHistoryOfConquest game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getBatch());
        hotseatNames.add("Player 1");
        hotseatNames.add("Player 2");
        buildInterface();
    }

    private void buildInterface() {
        stage.clear();
        Table root = new Table();
        root.setFillParent(true);
        root.pad(18f * game.getUiScale());
        root.top();
        stage.addActor(root);

        Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        Label title = new Label(localization.get("multiplayer.title"), game.getSkin(), "title");
        root.add(title).left().padBottom(10f * scale).row();

        if (!statusText.isEmpty()) {
            Label status = new Label(statusText, game.getSkin(), "small");
            status.setWrap(true);
            root.add(status).growX().left().padBottom(8f * scale).row();
        }

        switch (currentStage) {
            case STAGE_HOTSEAT:
                buildHotseatStage(root);
                break;
            case STAGE_BROWSE:
                buildBrowseStage(root);
                break;
            case STAGE_LOBBY:
                buildLobbyStage(root);
                break;
            default:
                buildModeStage(root);
                break;
        }

        TextButton backButton = new TextButton(localization.get("common.back"), game.getSkin());
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentStage == STAGE_MODE) {
                    closeSession();
                    game.setScreen(new MainMenuScreen(game));
                    dispose();
                } else {
                    closeSession();
                    currentStage = STAGE_MODE;
                    statusText = "";
                    buildInterface();
                }
            }
        });
        root.add(backButton).left().padTop(12f * scale).row();
    }

    private void buildModeStage(Table root) {
        Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        Label hint = new Label(localization.get("multiplayer.modeHintOnline"), game.getSkin(), "small");
        hint.setWrap(true);
        root.add(hint).growX().left().padBottom(12f * scale).row();

        Table buttons = new Table();
        buttons.defaults().growX().padBottom(8f * scale);

        TextButton hotseatButton = new TextButton(localization.get("multiplayer.hotseat"), game.getSkin(), "accent");
        hotseatButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentStage = STAGE_HOTSEAT;
                buildInterface();
            }
        });

        TextButton hostButton = new TextButton(localization.get("multiplayer.host"), game.getSkin());
        hostButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startHosting();
            }
        });

        TextButton joinButton = new TextButton(localization.get("multiplayer.join"), game.getSkin());
        joinButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                currentStage = STAGE_BROWSE;
                discovery.scan();
                buildInterface();
            }
        });

        TextButton onlineButton = new TextButton(localization.get("online.title"), game.getSkin(), "accent");
        onlineButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                closeSession();
                game.setScreen(new OnlineScreen(game));
                dispose();
            }
        });

        buttons.add(onlineButton).row();
        buttons.add(hotseatButton).row();
        buttons.add(hostButton).row();
        buttons.add(joinButton).row();
        root.add(buttons).growX().row();
        root.add().grow().row();
    }

    private void buildHotseatStage(Table root) {
        final Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        Label hint = new Label(localization.get("multiplayer.hotseatHint"), game.getSkin(), "small");
        hint.setWrap(true);
        root.add(hint).growX().left().padBottom(10f * scale).row();

        Table listTable = new Table();
        listTable.defaults().growX().padBottom(6f * scale);

        for (int index = 0; index < hotseatNames.size; index++) {
            final int slotIndex = index;
            Table row = new Table();
            row.setBackground(game.getSkin().getDrawable("panel-light"));
            row.pad(10f * scale);

            final TextField field = new TextField(hotseatNames.get(index), buildTextFieldStyle());
            field.setMessageText(localization.get("multiplayer.playerName"));
            field.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener() {
                @Override
                public void changed(ChangeEvent event, com.badlogic.gdx.scenes.scene2d.Actor actor) {
                    hotseatNames.set(slotIndex, field.getText());
                }
            });

            row.add(field).growX();

            if (index >= 2) {
                TextButton removeButton = new TextButton("X", game.getSkin());
                removeButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        hotseatNames.removeIndex(slotIndex);
                        buildInterface();
                    }
                });
                row.add(removeButton).padLeft(8f * scale);
            }

            listTable.add(row).row();
        }

        ScrollPane scrollPane = new ScrollPane(listTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        root.add(scrollPane).grow().row();

        Table actions = new Table();
        actions.defaults().padTop(10f * scale).padRight(8f * scale);

        TextButton addButton = new TextButton(localization.get("multiplayer.addPlayer"), game.getSkin());
        addButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (hotseatNames.size < 8) {
                    hotseatNames.add("Player " + (hotseatNames.size + 1));
                    buildInterface();
                }
            }
        });

        TextButton continueButton = new TextButton(localization.get("multiplayer.chooseScenario"), game.getSkin(), "accent");
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                session = new LocalSession(NetworkSession.MODE_HOTSEAT, hotseatNames);
                session.setListener(MultiplayerScreen.this);
                currentStage = STAGE_LOBBY;
                buildInterface();
            }
        });

        actions.add(addButton).left();
        actions.add(continueButton).left();
        root.add(actions).left().row();
    }

    private TextField.TextFieldStyle buildTextFieldStyle() {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = game.getSkin().getFont("default");
        style.fontColor = UiSkinFactory.TEXT;
        style.messageFontColor = UiSkinFactory.TEXT_DIM;
        style.background = game.getSkin().getDrawable("button-up");
        style.cursor = game.getSkin().getDrawable("button-accent");
        style.selection = game.getSkin().getDrawable("selection");
        return style;
    }

    private void buildBrowseStage(Table root) {
        Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        Label hint = new Label(discovery.isScanning()
                ? localization.get("multiplayer.scanning")
                : localization.get("multiplayer.browseHint"), game.getSkin(), "small");
        hint.setWrap(true);
        root.add(hint).growX().left().padBottom(10f * scale).row();

        Table listTable = new Table();
        listTable.defaults().growX().padBottom(6f * scale);

        Array<LanDiscovery.DiscoveredHost> hosts = discovery.getHosts();
        if (hosts.size == 0 && !discovery.isScanning()) {
            listTable.add(new Label(localization.get("multiplayer.noHosts"), game.getSkin(), "small")).left().row();
        }

        for (int index = 0; index < hosts.size; index++) {
            final LanDiscovery.DiscoveredHost host = hosts.get(index);
            TextButton button = new TextButton(host.name + "   " + host.address
                    + "   " + localization.format("multiplayer.playerCount", host.playerCount), game.getSkin());
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    joinHost(host.address);
                }
            });
            listTable.add(button).row();
        }

        ScrollPane scrollPane = new ScrollPane(listTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        root.add(scrollPane).grow().row();

        Table manual = new Table();
        nameField = new TextField("", buildTextFieldStyle());
        nameField.setMessageText(localization.get("multiplayer.manualAddress"));

        TextButton connectButton = new TextButton(localization.get("multiplayer.connect"), game.getSkin());
        connectButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                String address = nameField.getText().trim();
                if (!address.isEmpty()) {
                    joinHost(address);
                }
            }
        });

        TextButton rescanButton = new TextButton(localization.get("multiplayer.rescan"), game.getSkin());
        rescanButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                discovery.scan();
                buildInterface();
            }
        });

        manual.add(nameField).growX().padRight(8f * scale);
        manual.add(connectButton).padRight(8f * scale);
        manual.add(rescanButton);
        root.add(manual).growX().padTop(10f * scale).row();
    }

    private void startHosting() {
        session = new HostSession(deviceName(), deviceName());
        session.setListener(this);
        currentStage = STAGE_LOBBY;
        statusText = game.getLocalization().get("multiplayer.hostingHint");
        buildInterface();
    }

    private void joinHost(String address) {
        session = new ClientSession(address, deviceName());
        session.setListener(this);
        currentStage = STAGE_LOBBY;
        statusText = game.getLocalization().format("multiplayer.connecting", address);
        buildInterface();
    }

    private String deviceName() {
        String model = System.getProperty("os.name", "Player");
        return model.length() > 16 ? model.substring(0, 16) : model;
    }

    private void buildLobbyStage(Table root) {
        final Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        Table playerTable = new Table();
        playerTable.defaults().growX().padBottom(6f * scale);

        Array<LobbyPlayer> players = session.getPlayers();
        for (int index = 0; index < players.size; index++) {
            final LobbyPlayer player = players.get(index);
            Table row = new Table();
            row.setBackground(game.getSkin().getDrawable("panel-light"));
            row.pad(10f * scale);
            row.left();

            String countryName = localization.get("multiplayer.noCountry");
            if (player.countryId != null) {
                countryName = player.countryId;
            }

            Label label = new Label(player.name + (player.host ? "  *" : ""), game.getSkin(), "bold");
            Label country = new Label(countryName, game.getSkin(), "small");

            Table column = new Table();
            column.left();
            column.add(label).left().growX().row();
            column.add(country).left().growX().row();

            row.add(column).growX();

            boolean canEdit = player.local
                    || (session.getMode() == NetworkSession.MODE_HOTSEAT);
            if (canEdit) {
                TextButton pickButton = new TextButton(localization.get("multiplayer.pickCountry"), game.getSkin());
                pickButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        showCountryPicker(player);
                    }
                });
                row.add(pickButton).padLeft(8f * scale);
            }

            playerTable.add(row).row();
        }

        ScrollPane scrollPane = new ScrollPane(playerTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        root.add(scrollPane).grow().row();

        if (session.isAuthoritative()) {
            Table scenarioRow = new Table();
            scenarioRow.defaults().padTop(10f * scale).padRight(8f * scale);

            String scenarioName = selectedScenario != null
                    ? selectedScenario.getDisplayName(localization.getActiveLanguage())
                    : localization.get("multiplayer.noScenario");

            TextButton scenarioButton = new TextButton(
                    localization.get("multiplayer.scenario") + ": " + scenarioName, game.getSkin());
            scenarioButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    showScenarioPicker();
                }
            });

            TextButton startButton = new TextButton(localization.get("multiplayer.start"), game.getSkin(), "accent");
            boolean canStart = selectedScenario != null && everyPlayerHasCountry();
            startButton.setDisabled(!canStart);
            startButton.setColor(canStart ? Color.WHITE : new Color(1f, 1f, 1f, 0.4f));
            startButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (selectedScenario != null && everyPlayerHasCountry()) {
                        session.startGame(selectedScenario.id);
                    }
                }
            });

            scenarioRow.add(scenarioButton).left();
            scenarioRow.add(startButton).left();
            root.add(scenarioRow).left().row();
        } else {
            root.add(new Label(localization.get("multiplayer.waitingForHost"), game.getSkin(), "small"))
                    .left().padTop(10f * scale).row();
        }
    }

    private boolean everyPlayerHasCountry() {
        Array<LobbyPlayer> players = session.getPlayers();
        if (players.size == 0) {
            return false;
        }
        for (int index = 0; index < players.size; index++) {
            if (players.get(index).countryId == null) {
                return false;
            }
        }
        return true;
    }

    private void showScenarioPicker() {
        final Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        final Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(game.getSkin().getDrawable("panel"));
        overlay.pad(16f * scale);
        overlay.top();

        overlay.add(new Label(localization.get("multiplayer.scenario"), game.getSkin(), "bold")).left()
                .padBottom(10f * scale).row();

        Table listTable = new Table();
        listTable.defaults().growX().padBottom(4f * scale);

        Array<Scenario> scenarios = game.getModLoader().getScenarios();
        for (int index = 0; index < scenarios.size; index++) {
            final Scenario scenario = scenarios.get(index);
            TextButton button = new TextButton(scenario.getDisplayName(localization.getActiveLanguage()),
                    game.getSkin());
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    selectedScenario = scenario;
                    overlay.remove();
                    buildInterface();
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

    private void showCountryPicker(final LobbyPlayer player) {
        final Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        if (selectedScenario == null && session.isAuthoritative()) {
            statusText = localization.get("multiplayer.pickScenarioFirst");
            buildInterface();
            return;
        }

        final Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(game.getSkin().getDrawable("panel"));
        overlay.pad(16f * scale);
        overlay.top();

        overlay.add(new Label(localization.get("multiplayer.pickCountry"), game.getSkin(), "bold")).left()
                .padBottom(10f * scale).row();

        Table listTable = new Table();
        listTable.defaults().growX().padBottom(4f * scale);

        Array<Country> countries = new Array<Country>();
        for (com.badlogic.gdx.utils.ObjectMap.Entry<String, Country> entry
                : game.getAssets().getDefaultCountries()) {
            countries.add(entry.value);
        }
        countries.sort(new java.util.Comparator<Country>() {
            @Override
            public int compare(Country first, Country second) {
                return first.name.compareToIgnoreCase(second.name);
            }
        });

        for (int index = 0; index < countries.size; index++) {
            final Country country = countries.get(index);
            TextButton button = new TextButton(country.name, game.getSkin());
            button.getLabel().setColor(country.color);
            button.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    session.claimCountry(player.id, country.id);
                    overlay.remove();
                    buildInterface();
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

    private void closeSession() {
        if (session != null) {
            session.setListener(null);
            session.close();
            session = null;
        }
    }

    @Override
    public void onLobbyChanged() {
        buildInterface();
    }

    @Override
    public void onGameStarted(String scenarioId) {
        Scenario scenario = game.getModLoader().findScenario(scenarioId);
        if (scenario == null) {
            statusText = "Scenario not found: " + scenarioId;
            buildInterface();
            return;
        }

        resolvedSlots.clear();
        Array<LobbyPlayer> players = session.getPlayers();
        boolean hotseat = session.getMode() == NetworkSession.MODE_HOTSEAT;
        for (int index = 0; index < players.size; index++) {
            LobbyPlayer player = players.get(index);
            boolean local = hotseat || player.id.equals(session.getLocalPlayerId());
            resolvedSlots.add(new PlayerSlot(player.id, player.name, player.countryId, local));
        }

        NetworkSession activeSession = session;
        session = null;
        activeSession.setListener(null);

        MapScreen mapScreen = new MapScreen(game, scenario, activeSession, resolvedSlots);
        game.setScreen(mapScreen);
        dispose();
    }

    @Override
    public void onCommandReceived(GameCommand command) {
    }

    @Override
    public void onSnapshotReceived(String payload) {
    }

    @Override
    public void onTurnChanged(int activePlayerIndex) {
    }

    @Override
    public void onChatReceived(String playerName, String message) {
    }

    @Override
    public void onDisconnected(String reason) {
        statusText = game.getLocalization().format("multiplayer.disconnected", reason);
        closeSession();
        currentStage = STAGE_MODE;
        buildInterface();
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        if (session != null) {
            session.poll();
        }
        if (currentStage == STAGE_BROWSE) {
            int previous = discovery.getHosts().size;
            discovery.poll();
            if (discovery.getHosts().size != previous) {
                buildInterface();
            }
        }

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
