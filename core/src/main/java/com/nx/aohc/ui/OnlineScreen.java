package com.nx.aohc.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
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
import com.nx.aohc.game.GameSettings;
import com.nx.aohc.game.PlayerSlot;
import com.nx.aohc.localization.Localization;
import com.nx.aohc.net.GameCommand;
import com.nx.aohc.net.LobbyPlayer;
import com.nx.aohc.net.NetworkSession;
import com.nx.aohc.net.OnlineSession;
import com.nx.aohc.net.HttpRelayClient;
import com.nx.aohc.net.Relay;
import com.nx.aohc.net.RelayClient;
import com.nx.aohc.scenario.Scenario;

public class OnlineScreen implements Screen, Relay.Listener, NetworkSession.Listener,
        OnlineSession.RoomListener {

    private static final int STAGE_BROWSER = 0;
    private static final int STAGE_CREATE = 1;
    private static final int STAGE_ROOM = 2;

    private static final String PREFERENCES_NAME = "aohc-online";
    private static final String KEY_SERVER = "server";
    private static final String KEY_NAME = "playerName";
    public static final String DEFAULT_SERVER = "https://aohc-lobby.netlify.app";

    private final AgeOfHistoryOfConquest game;
    private final Stage stage;
    private final Preferences preferences;
    private final Array<RelayClient.RoomInfo> rooms = new Array<RelayClient.RoomInfo>();
    private final Array<PlayerSlot> resolvedSlots = new Array<PlayerSlot>();

    private Relay relay;
    private OnlineSession session;
    private int currentStage = STAGE_BROWSER;
    private String statusText = "";
    private String searchQuery = "";
    private String pendingJoinRoomId = "";

    private TextField serverField;
    private TextField nameField;
    private TextField searchField;
    private TextField roomNameField;
    private TextField roomPasswordField;
    private TextField joinPasswordField;

    private Scenario selectedScenario;
    private int maxPlayers = 4;

    public OnlineScreen(AgeOfHistoryOfConquest game) {
        this.game = game;
        this.stage = new Stage(new ScreenViewport(), game.getBatch());
        this.preferences = Gdx.app.getPreferences(PREFERENCES_NAME);
        buildInterface();
    }

    private String getServerUrl() {
        return preferences.getString(KEY_SERVER, DEFAULT_SERVER);
    }

    private String getPlayerName() {
        return preferences.getString(KEY_NAME, "Player");
    }

    private void ensureConnected() {
        if (relay != null && relay.isOpen()) {
            return;
        }
        if (relay != null) {
            relay.close();
        }
        String url = getServerUrl();
        if (url.startsWith("http://") || url.startsWith("https://")) {
            relay = new HttpRelayClient(url, getPlayerName());
        } else {
            relay = new RelayClient(url, getPlayerName());
        }
        relay.setListener(this);
        relay.connect();
        statusText = game.getLocalization().format("online.connecting", getServerUrl());
    }

    private TextField.TextFieldStyle textFieldStyle() {
        TextField.TextFieldStyle style = new TextField.TextFieldStyle();
        style.font = game.getSkin().getFont("default");
        style.fontColor = UiSkinFactory.TEXT;
        style.messageFontColor = UiSkinFactory.TEXT_DIM;
        style.background = game.getSkin().getDrawable("button-up");
        style.cursor = game.getSkin().getDrawable("button-accent");
        style.selection = game.getSkin().getDrawable("selection");
        return style;
    }

    private void buildInterface() {
        stage.clear();
        Table root = new Table();
        root.setFillParent(true);
        root.pad(16f * game.getUiScale());
        root.top();
        stage.addActor(root);

        Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        root.add(new Label(localization.get("online.title"), game.getSkin(), "title")).left()
                .padBottom(8f * scale).row();

        if (!statusText.isEmpty()) {
            Label status = new Label(statusText, game.getSkin(), "small");
            status.setWrap(true);
            root.add(status).growX().left().padBottom(8f * scale).row();
        }

        switch (currentStage) {
            case STAGE_CREATE:
                buildCreateStage(root);
                break;
            case STAGE_ROOM:
                buildRoomStage(root);
                break;
            default:
                buildBrowserStage(root);
                break;
        }

        TextButton backButton = new TextButton(localization.get("common.back"), game.getSkin());
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (currentStage == STAGE_BROWSER) {
                    shutdown();
                    game.setScreen(new MultiplayerScreen(game));
                    dispose();
                } else {
                    if (session != null) {
                        session.close();
                        session = null;
                        relay = null;
                    }
                    currentStage = STAGE_BROWSER;
                    statusText = "";
                    buildInterface();
                }
            }
        });
        root.add(backButton).left().padTop(10f * scale).row();
    }

    private void buildBrowserStage(Table root) {
        final Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        Table settingsRow = new Table();
        serverField = new TextField(getServerUrl(), textFieldStyle());
        serverField.setMessageText(localization.get("online.serverAddress"));
        nameField = new TextField(getPlayerName(), textFieldStyle());
        nameField.setMessageText(localization.get("online.playerName"));

        TextButton saveButton = new TextButton(localization.get("online.saveSettings"), game.getSkin());
        saveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                preferences.putString(KEY_SERVER, serverField.getText().trim());
                preferences.putString(KEY_NAME, nameField.getText().trim());
                preferences.flush();
                if (relay != null) {
                    relay.close();
                    relay = null;
                }
                statusText = localization.get("online.settingsSaved");
                buildInterface();
            }
        });

        settingsRow.add(serverField).growX().padRight(6f * scale);
        settingsRow.add(nameField).width(180f * scale).padRight(6f * scale);
        settingsRow.add(saveButton);
        root.add(settingsRow).growX().padBottom(8f * scale).row();

        Table searchRow = new Table();
        searchField = new TextField(searchQuery, textFieldStyle());
        searchField.setMessageText(localization.get("online.searchHint"));

        TextButton searchButton = new TextButton(localization.get("online.refresh"), game.getSkin());
        searchButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                searchQuery = searchField.getText().trim();
                ensureConnected();
                if (relay != null && relay.isOpen()) {
                    relay.requestRoomList(searchQuery);
                }
                buildInterface();
            }
        });

        TextButton createButton = new TextButton(localization.get("online.createRoom"), game.getSkin(), "accent");
        createButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ensureConnected();
                currentStage = STAGE_CREATE;
                buildInterface();
            }
        });

        searchRow.add(searchField).growX().padRight(6f * scale);
        searchRow.add(searchButton).padRight(6f * scale);
        searchRow.add(createButton);
        root.add(searchRow).growX().padBottom(8f * scale).row();

        Table listTable = new Table();
        listTable.top();
        listTable.defaults().growX().padBottom(6f * scale);

        if (rooms.size == 0) {
            listTable.add(new Label(localization.get("online.noRooms"), game.getSkin(), "small")).left().row();
        }

        for (int index = 0; index < rooms.size; index++) {
            final RelayClient.RoomInfo room = rooms.get(index);
            Table row = new Table();
            row.setBackground(game.getSkin().getDrawable("panel-light"));
            row.pad(10f * scale);
            row.left();

            Label name = new Label(room.name + (room.locked ? "   *" : ""), game.getSkin(), "bold");
            String scenarioName = room.scenarioName.isEmpty()
                    ? localization.get("multiplayer.noScenario") : room.scenarioName;
            Label detail = new Label(localization.format("online.roomDetail",
                    room.id, room.hostName, room.players, room.maxPlayers, scenarioName), game.getSkin(), "small");
            detail.setWrap(true);
            Label rules = new Label(localization.format("online.roomRules",
                    localization.get(difficultyKey(room.difficulty)), room.aggression), game.getSkin(), "small");

            Table column = new Table();
            column.left();
            column.add(name).left().growX().row();
            column.add(detail).left().growX().padTop(2f * scale).row();
            column.add(rules).left().growX().row();

            TextButton joinButton = new TextButton(localization.get("online.join"), game.getSkin());
            joinButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (room.locked) {
                        askPassword(room);
                    } else {
                        joinRoom(room.id, "");
                    }
                }
            });

            row.add(column).growX().left();
            row.add(joinButton).right().padLeft(10f * scale);
            listTable.add(row).row();
        }

        ScrollPane scrollPane = new ScrollPane(listTable, game.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        root.add(scrollPane).grow().row();
    }

    private String difficultyKey(int difficulty) {
        switch (difficulty) {
            case GameSettings.DIFFICULTY_EASY:
                return "difficulty.easy";
            case GameSettings.DIFFICULTY_HARD:
                return "difficulty.hard";
            case GameSettings.DIFFICULTY_IMPOSSIBLE:
                return "difficulty.impossible";
            default:
                return "difficulty.normal";
        }
    }

    private void askPassword(final RelayClient.RoomInfo room) {
        final Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        final Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(game.getSkin().getDrawable("panel"));
        overlay.pad(20f * scale);
        overlay.top();

        overlay.add(new Label(localization.format("online.passwordFor", room.name),
                game.getSkin(), "bold")).left().padBottom(10f * scale).row();

        joinPasswordField = new TextField("", textFieldStyle());
        joinPasswordField.setPasswordMode(true);
        joinPasswordField.setPasswordCharacter('*');
        joinPasswordField.setMessageText(localization.get("online.password"));
        overlay.add(joinPasswordField).growX().padBottom(10f * scale).row();

        Table buttons = new Table();
        TextButton confirmButton = new TextButton(localization.get("online.join"), game.getSkin(), "accent");
        confirmButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                joinRoom(room.id, joinPasswordField.getText());
                overlay.remove();
            }
        });
        TextButton cancelButton = new TextButton(localization.get("common.close"), game.getSkin());
        cancelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                overlay.remove();
            }
        });

        buttons.add(confirmButton).padRight(8f * scale);
        buttons.add(cancelButton);
        overlay.add(buttons).left().row();
        overlay.add().grow().row();

        stage.addActor(overlay);
    }

    private void buildCreateStage(Table root) {
        final Localization localization = game.getLocalization();
        final GameSettings settings = game.getGameSettings();
        float scale = game.getUiScale();

        roomNameField = new TextField(getPlayerName() + " " + localization.get("online.room"), textFieldStyle());
        roomNameField.setMessageText(localization.get("online.roomName"));
        roomPasswordField = new TextField("", textFieldStyle());
        roomPasswordField.setMessageText(localization.get("online.passwordOptional"));

        root.add(roomNameField).growX().padBottom(6f * scale).row();
        root.add(roomPasswordField).growX().padBottom(6f * scale).row();

        Table optionsTable = new Table();
        optionsTable.defaults().padRight(8f * scale).padBottom(6f * scale);

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

        TextButton difficultyButton = new TextButton(
                localization.get("menu.difficulty") + ": " + localization.get(settings.getDifficultyKey()),
                game.getSkin());
        difficultyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settings.cycleDifficulty();
                buildInterface();
            }
        });

        TextButton aggressionDown = new TextButton("-10", game.getSkin());
        aggressionDown.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settings.adjustAiAggression(-10);
                buildInterface();
            }
        });

        TextButton aggressionUp = new TextButton("+10", game.getSkin());
        aggressionUp.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                settings.adjustAiAggression(10);
                buildInterface();
            }
        });

        TextButton playersButton = new TextButton(
                localization.format("online.maxPlayers", maxPlayers), game.getSkin());
        playersButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                maxPlayers = maxPlayers >= 8 ? 2 : maxPlayers + 1;
                buildInterface();
            }
        });

        optionsTable.add(scenarioButton).left().row();
        optionsTable.add(difficultyButton).left().row();

        Table aggressionRow = new Table();
        aggressionRow.add(aggressionDown).padRight(6f * scale);
        aggressionRow.add(new Label(localization.format("menu.aggression", settings.getAiAggression()),
                game.getSkin(), "small")).padRight(6f * scale);
        aggressionRow.add(aggressionUp);
        optionsTable.add(aggressionRow).left().row();
        optionsTable.add(playersButton).left().row();

        root.add(optionsTable).left().row();
        root.add().grow().row();

        TextButton createButton = new TextButton(localization.get("online.createAndOpen"), game.getSkin(), "accent");
        boolean ready = selectedScenario != null && relay != null && relay.isOpen();
        createButton.setDisabled(!ready);
        createButton.setColor(ready ? Color.WHITE : new Color(1f, 1f, 1f, 0.4f));
        createButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (selectedScenario == null || relay == null || !relay.isOpen()) {
                    return;
                }
                session = new OnlineSession(relay, true);
                session.setRoomListener(OnlineScreen.this);
                session.setListener(OnlineScreen.this);
                relay.createRoom(roomNameField.getText().trim(),
                        roomPasswordField.getText(),
                        selectedScenario.id,
                        selectedScenario.getDisplayName(localization.getActiveLanguage()),
                        maxPlayers,
                        settings.getDifficulty(),
                        settings.getAiAggression());
                currentStage = STAGE_ROOM;
                statusText = localization.get("online.creating");
                buildInterface();
            }
        });
        root.add(createButton).left().row();
    }

    private void joinRoom(String roomId, String password) {
        ensureConnected();
        pendingJoinRoomId = roomId;
        session = new OnlineSession(relay, false);
        session.setRoomListener(this);
        session.setListener(this);
        relay.joinRoom(roomId, password);
        currentStage = STAGE_ROOM;
        statusText = game.getLocalization().format("online.joining", roomId);
        buildInterface();
    }

    private void showScenarioPicker() {
        final Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        final Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(game.getSkin().getDrawable("panel"));
        overlay.pad(16f * scale);
        overlay.top();

        overlay.add(new Label(localization.get("multiplayer.scenario"), game.getSkin(), "bold"))
                .left().padBottom(10f * scale).row();

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

    private void buildRoomStage(Table root) {
        final Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        if (session == null) {
            root.add(new Label(localization.get("online.notInRoom"), game.getSkin(), "small")).left().row();
            root.add().grow().row();
            return;
        }

        RelayClient.RoomInfo room = session.getRoom();
        if (room != null) {
            root.add(new Label(room.name + "   " + localization.format("online.roomCode", room.id),
                    game.getSkin(), "bold")).left().row();
            String scenarioName = room.scenarioName.isEmpty()
                    ? localization.get("multiplayer.noScenario") : room.scenarioName;
            root.add(new Label(localization.format("online.roomRulesFull",
                    scenarioName, localization.get(difficultyKey(room.difficulty)), room.aggression),
                    game.getSkin(), "small")).left().padBottom(8f * scale).row();
        }

        Table playerTable = new Table();
        playerTable.defaults().growX().padBottom(6f * scale);

        Array<LobbyPlayer> players = session.getPlayers();
        for (int index = 0; index < players.size; index++) {
            final LobbyPlayer player = players.get(index);
            Table row = new Table();
            row.setBackground(game.getSkin().getDrawable("panel-light"));
            row.pad(10f * scale);
            row.left();

            Country country = player.countryId != null
                    ? game.getAssets().getDefaultCountries().get(player.countryId) : null;
            String countryName = country != null ? country.name
                    : (player.countryId != null ? player.countryId : localization.get("multiplayer.noCountry"));

            Label name = new Label(player.name + (player.host ? "  *" : ""), game.getSkin(), "bold");
            Label countryLabel = new Label(countryName, game.getSkin(), "small");
            if (country != null) {
                countryLabel.setColor(country.color);
            }

            Table column = new Table();
            column.left();
            column.add(name).left().growX().row();
            column.add(countryLabel).left().growX().row();
            row.add(column).growX();

            if (player.local) {
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
            boolean canStart = everyPlayerHasCountry();
            TextButton startButton = new TextButton(localization.get("multiplayer.start"), game.getSkin(), "accent");
            startButton.setDisabled(!canStart);
            startButton.setColor(canStart ? Color.WHITE : new Color(1f, 1f, 1f, 0.4f));
            startButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    if (!everyPlayerHasCountry() || selectedScenario == null) {
                        return;
                    }
                    relay.updateRoom(selectedScenario.id,
                            selectedScenario.getDisplayName(localization.getActiveLanguage()),
                            game.getGameSettings().getDifficulty(),
                            game.getGameSettings().getAiAggression(),
                            true);
                    session.startGame(selectedScenario.id);
                }
            });
            root.add(startButton).left().padTop(8f * scale).row();
        } else {
            root.add(new Label(localization.get("multiplayer.waitingForHost"), game.getSkin(), "small"))
                    .left().padTop(8f * scale).row();
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

    private void showCountryPicker(final LobbyPlayer player) {
        final Localization localization = game.getLocalization();
        float scale = game.getUiScale();

        final Table overlay = new Table();
        overlay.setFillParent(true);
        overlay.setBackground(game.getSkin().getDrawable("panel"));
        overlay.pad(16f * scale);
        overlay.top();

        overlay.add(new Label(localization.get("multiplayer.pickCountry"), game.getSkin(), "bold"))
                .left().padBottom(10f * scale).row();

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
                    session.claimCountry(session.getLocalPlayerId(), country.id);
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

    private void shutdown() {
        if (session != null) {
            session.close();
            session = null;
            relay = null;
        }
        if (relay != null) {
            relay.close();
            relay = null;
        }
    }

    @Override
    public void onConnected(String peerId) {
        statusText = game.getLocalization().get("online.connected");
        relay.requestRoomList(searchQuery);
        buildInterface();
    }

    @Override
    public void onRoomList(Array<RelayClient.RoomInfo> list) {
        rooms.clear();
        rooms.addAll(list);
        statusText = game.getLocalization().format("online.roomsFound", rooms.size);
        buildInterface();
    }

    @Override
    public void onRoomEntered(RelayClient.RoomInfo room, Array<RelayClient.PeerInfo> members, boolean asHost) {
    }

    @Override
    public void onRoomUpdated(RelayClient.RoomInfo room, Array<RelayClient.PeerInfo> members) {
    }

    @Override
    public void onPeerJoined(RelayClient.PeerInfo peer, Array<RelayClient.PeerInfo> members) {
    }

    @Override
    public void onPeerLeft(String peerId, Array<RelayClient.PeerInfo> members) {
    }

    @Override
    public void onRelay(String fromPeerId, String data) {
    }

    @Override
    public void onServerError(String code) {
        statusText = game.getLocalization().get("online.error." + code);
        if ("wrong_password".equals(code) || "room_not_found".equals(code)
                || "room_full".equals(code) || "already_started".equals(code)) {
            session = null;
            currentStage = STAGE_BROWSER;
            if (relay != null && relay.isOpen()) {
                relay.requestRoomList(searchQuery);
            }
        }
        buildInterface();
    }

    @Override
    public void onClosed(String reason) {
        statusText = game.getLocalization().format("multiplayer.disconnected", reason);
        session = null;
        relay = null;
        currentStage = STAGE_BROWSER;
        buildInterface();
    }

    @Override
    public void onRoomStateChanged(RelayClient.RoomInfo room) {
        if (room != null && selectedScenario == null) {
            selectedScenario = game.getModLoader().findScenario(room.scenario);
        }
        if (room != null) {
            game.getGameSettings().setDifficulty(room.difficulty);
            game.getGameSettings().setAiAggression(room.aggression);
        }
        statusText = "";
        buildInterface();
    }

    @Override
    public void onLobbyChanged() {
        buildInterface();
    }

    @Override
    public void onGameStarted(String scenarioId) {
        Scenario scenario = game.getModLoader().findScenario(scenarioId);
        if (scenario == null) {
            statusText = game.getLocalization().format("online.missingScenario", scenarioId);
            buildInterface();
            return;
        }

        resolvedSlots.clear();
        Array<LobbyPlayer> players = session.getPlayers();
        for (int index = 0; index < players.size; index++) {
            LobbyPlayer player = players.get(index);
            resolvedSlots.add(new PlayerSlot(player.id, player.name, player.countryId,
                    player.id.equals(session.getLocalPlayerId())));
        }

        OnlineSession activeSession = session;
        session = null;
        activeSession.setRoomListener(null);
        activeSession.setListener(null);

        game.setScreen(new MapScreen(game, scenario, activeSession, resolvedSlots));
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
        onClosed(reason);
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        ensureConnected();
    }

    @Override
    public void render(float delta) {
        if (session != null) {
            session.poll();
        } else if (relay != null) {
            relay.poll();
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
