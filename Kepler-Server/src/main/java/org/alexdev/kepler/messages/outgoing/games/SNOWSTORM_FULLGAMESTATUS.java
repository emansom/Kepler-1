package org.alexdev.kepler.messages.outgoing.games;

import org.alexdev.kepler.game.games.Game;
import org.alexdev.kepler.game.games.GameManager;
import org.alexdev.kepler.game.games.GameObject;
import org.alexdev.kepler.game.games.player.GamePlayer;
import org.alexdev.kepler.game.games.player.GameTeam;
import org.alexdev.kepler.game.games.snowstorm.SnowStormGame;
import org.alexdev.kepler.game.games.snowstorm.object.SnowStormPlayerObject;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.ArrayList;
import java.util.List;

public class SNOWSTORM_FULLGAMESTATUS extends MessageComposer {
    private final SnowStormGame game;
    private final GamePlayer gamePlayer;

    public SNOWSTORM_FULLGAMESTATUS(SnowStormGame game, GamePlayer gamePlayer) {
        this.game = game;
        this.gamePlayer = gamePlayer;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.game.getGameState().getStateId());
        response.writeInt(this.game.getPreparingGameSecondsLeft().get());
        response.writeInt(GameManager.getInstance().getPreparingSeconds(game.getGameType()));

        List<GameObject> objects = new ArrayList<>();
        List<GameObject> events = new ArrayList<>();

        for (var gamePlayer : this.game.getGameObjects()) {
            objects.add(gamePlayer);
            //objects.add(new SnowStormSpawnPlayerObject(gamePlayer));
        }

        response.writeInt(objects.size());

        for (var obj : objects) {
            obj.serialiseObject(response);
        }

        response.writeBool(false);
        response.writeInt(4);//this.game.getTeamAmount());

        this.gamePlayer.getTurnContainer().iterateTurn();
        this.gamePlayer.getTurnContainer().calculateChecksum(objects);

        new SNOWSTORM_GAMESTATUS((SnowStormGame) this.game, events, this.gamePlayer).compose(response);
    }

    @Override
    public short getHeader() {
        return 243; // "Cs"
    }
}
