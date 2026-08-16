package com.nx.aohc.net;

public class GameCommand {

    public static final int TYPE_MOVE = 1;
    public static final int TYPE_RECRUIT = 2;
    public static final int TYPE_DECLARE_WAR = 3;
    public static final int TYPE_PEACE = 4;
    public static final int TYPE_ALLIANCE = 5;
    public static final int TYPE_FORM = 6;
    public static final int TYPE_END_TURN = 7;

    private static final String SEPARATOR = "\u001f";

    public int type;
    public String actorCountry = "";
    public String targetCountry = "";
    public String textValue = "";
    public int originProvince;
    public int targetProvince;
    public int resultCode;
    public int attackerLosses;
    public int defenderLosses;
    public int survivingTroops;
    public boolean accepted;

    public static GameCommand move(String actorCountry, int originProvince, int targetProvince) {
        GameCommand command = new GameCommand();
        command.type = TYPE_MOVE;
        command.actorCountry = actorCountry;
        command.originProvince = originProvince;
        command.targetProvince = targetProvince;
        return command;
    }

    public static GameCommand recruit(String actorCountry, int provinceId) {
        GameCommand command = new GameCommand();
        command.type = TYPE_RECRUIT;
        command.actorCountry = actorCountry;
        command.targetProvince = provinceId;
        return command;
    }

    public static GameCommand declareWar(String actorCountry, String targetCountry) {
        GameCommand command = new GameCommand();
        command.type = TYPE_DECLARE_WAR;
        command.actorCountry = actorCountry;
        command.targetCountry = targetCountry;
        return command;
    }

    public static GameCommand peace(String actorCountry, String targetCountry) {
        GameCommand command = new GameCommand();
        command.type = TYPE_PEACE;
        command.actorCountry = actorCountry;
        command.targetCountry = targetCountry;
        return command;
    }

    public static GameCommand alliance(String actorCountry, String targetCountry) {
        GameCommand command = new GameCommand();
        command.type = TYPE_ALLIANCE;
        command.actorCountry = actorCountry;
        command.targetCountry = targetCountry;
        return command;
    }

    public static GameCommand form(String actorCountry, String formableId) {
        GameCommand command = new GameCommand();
        command.type = TYPE_FORM;
        command.actorCountry = actorCountry;
        command.textValue = formableId;
        return command;
    }

    public static GameCommand endTurn(String actorCountry) {
        GameCommand command = new GameCommand();
        command.type = TYPE_END_TURN;
        command.actorCountry = actorCountry;
        return command;
    }

    public String encode() {
        StringBuilder builder = new StringBuilder();
        builder.append(type).append(SEPARATOR);
        builder.append(actorCountry == null ? "" : actorCountry).append(SEPARATOR);
        builder.append(targetCountry == null ? "" : targetCountry).append(SEPARATOR);
        builder.append(textValue == null ? "" : textValue).append(SEPARATOR);
        builder.append(originProvince).append(SEPARATOR);
        builder.append(targetProvince).append(SEPARATOR);
        builder.append(resultCode).append(SEPARATOR);
        builder.append(attackerLosses).append(SEPARATOR);
        builder.append(defenderLosses).append(SEPARATOR);
        builder.append(survivingTroops).append(SEPARATOR);
        builder.append(accepted ? 1 : 0);
        return builder.toString();
    }

    public static GameCommand decode(String payload) {
        String[] parts = payload.split(SEPARATOR, -1);
        if (parts.length < 11) {
            return null;
        }
        GameCommand command = new GameCommand();
        command.type = Integer.parseInt(parts[0]);
        command.actorCountry = parts[1];
        command.targetCountry = parts[2];
        command.textValue = parts[3];
        command.originProvince = Integer.parseInt(parts[4]);
        command.targetProvince = Integer.parseInt(parts[5]);
        command.resultCode = Integer.parseInt(parts[6]);
        command.attackerLosses = Integer.parseInt(parts[7]);
        command.defenderLosses = Integer.parseInt(parts[8]);
        command.survivingTroops = Integer.parseInt(parts[9]);
        command.accepted = "1".equals(parts[10]);
        return command;
    }
}
