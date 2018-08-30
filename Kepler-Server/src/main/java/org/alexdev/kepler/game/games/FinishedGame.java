package org.alexdev.kepler.game.games;

import org.alexdev.kepler.game.games.player.GameTeam;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinishedGame {
    private int id;
    private int mapId;
    private String name;
    private String mapCreator;
    private GameType gameType;
    private List<Integer> powerUps;
    private Map<Integer, FinishedGameTeam> teamScores;

    public FinishedGame(Game game) {
        this.id = game.getId();
        this.mapId = game.getMapId();
        this.name = game.getName();
        this.mapCreator = game.getGameCreator().getDetails().getName();
        this.gameType = game.getGameType();
        this.powerUps = game.getPowerUps();
        this.teamScores = new HashMap<>();

        for (int i = 0; i < game.getTeamAmount(); i++) {
            this.teamScores.put(i, new FinishedGameTeam(game.getTeams().get(i)));
        }
    }

    public int getId() {
        return id;
    }

    public int getMapId() {
        return mapId;
    }

    public String getMapCreator() {
        return mapCreator;
    }

    public Map<Integer, FinishedGameTeam> getTeamScores() {
        return teamScores;
    }

    public GameType getGameType() {
        return gameType;
    }

    public String getName() {
        return name;
    }

    public List<Integer> getPowerUps() {
        return powerUps;
    }

    public class FinishedGameTeam {
        private int score;
        private List<Pair<String, Integer>> playerScores;

        private FinishedGameTeam(GameTeam gameTeam) {
            this.score = gameTeam.getScore();
            this.playerScores = new ArrayList<>();

            for (var gamePlayer : gameTeam.getPlayers()) {
                this.playerScores.add(Pair.of(gamePlayer.getPlayer().getDetails().getName(), gamePlayer.getScore()));
            }
        }

        public int getScore() {
            return score;
        }

        public List<Pair<String, Integer>> getPlayerScores() {
            return playerScores;
        }
    }
}
