package org.alexdev.kepler.messages.incoming.games;

import org.alexdev.kepler.game.games.Game;
import org.alexdev.kepler.game.games.GameManager;
import org.alexdev.kepler.game.games.player.GamePlayer;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.messages.outgoing.games.PLAYERREJOINED;
import org.alexdev.kepler.messages.types.MessageEvent;
import org.alexdev.kepler.server.netty.streams.NettyRequest;

public class GAMERESTART implements MessageEvent {
    @Override
    public void handle(Player player, NettyRequest reader) throws Exception {
        if (player.getRoomUser().getRoom() == null) {
            return;
        }

        GamePlayer gamePlayer = player.getRoomUser().getGamePlayer();

        if (gamePlayer == null) {
            return;
        }

        Game game = GameManager.getInstance().getGameById(gamePlayer.getGameId());

        if (game == null || !game.isGameFinished()) {
            return;
        }

        // Only allow restart once everyone has clicked they'd like to restart
        gamePlayer.setClickedRestart(true);
        game.send(new PLAYERREJOINED(player.getRoomUser().getInstanceId()));

/*        for (GameTeam gameTeam : game.getTeams().values()) {
            for (GamePlayer p : gameTeam.getActivePlayers()) {
                if (!p.isClickedRestart()) {
                    return;
                }
            }

        }*/
        /*Game restartGame = new Game(game.getId(), game.getMapId(), game.getGameType(), game.getName(), game.getTeamAmount(), game.getGameCreator().getDetails().getId());

        List<GamePlayer> restartedPlayers = new ArrayList<>();

        for (var gameUser : newPlayers) {
            var gp = new GamePlayer(gameUser.getPlayer());
            gp.setGameId(game.getId());
            gp.setTeamId(gameUser.getTeamId());
            gp.setInGame(true);

            gameUser.getPlayer().getRoomUser().setGamePlayer(gp);

            game.leaveGame(gameUser);
            restartGame.movePlayer(gp, -1, gp.getTeamId());

            restartedPlayers.add(gp);
        }

        restartGame.assignSpawnPoints();*/

    }
}
