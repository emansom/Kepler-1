package org.alexdev.kepler.game.games.battleball.powerups;

import org.alexdev.kepler.game.entity.Entity;
import org.alexdev.kepler.game.entity.EntityType;
import org.alexdev.kepler.game.games.battleball.BattleballGame;
import org.alexdev.kepler.game.games.battleball.BattleballTile;
import org.alexdev.kepler.game.games.battleball.enums.BattleballPlayerState;
import org.alexdev.kepler.game.games.player.GamePlayer;
import org.alexdev.kepler.game.games.utils.PowerUpUtil;
import org.alexdev.kepler.game.games.utils.TileUtil;
import org.alexdev.kepler.game.pathfinder.Position;
import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.game.room.mapping.RoomTile;

import java.util.ArrayList;
import java.util.List;

public class BombHandle {
    public static void handle(BattleballGame game, GamePlayer gamePlayer, Room room) {
        //PowerUpUtil.stunPlayer(game, gamePlayer, BattleballPlayerState.STUNNED);
        List<GamePlayer> stunnedPlayers = new ArrayList<>();

        for (Position position : gamePlayer.getPlayer().getRoomUser().getPosition().getCircle(5)) {
            RoomTile tile = game.getRoom().getMapping().getTile(position.getX(), position.getY());

            if (tile == null || !RoomTile.isValidTile(gamePlayer.getGame().getRoom(), null, position)) {
                continue;
            }

            BattleballTile battleballTile = (BattleballTile) game.getTile(position.getX(), position.getY());

            if (TileUtil.undoTileAttributes(battleballTile, gamePlayer.getGame())) {
                game.getUpdateTilesQueue().add(battleballTile);
            }

            stunnedPlayers.addAll(battleballTile.getPlayers(gamePlayer));
        }

        for (GamePlayer stunnedPlayer : stunnedPlayers) {
            // TODO: Move player away from blast radius: https://www.youtube.com/watch?v=cP3bvGOx53o&feature=youtu.be&t=242
            PowerUpUtil.stunPlayer(game, stunnedPlayer, BattleballPlayerState.STUNNED);
        }
    }
}
