package org.alexdev.kepler.game.games.battleball;

import org.alexdev.kepler.game.GameScheduler;
import org.alexdev.kepler.game.games.GameObject;
import org.alexdev.kepler.game.games.battleball.enums.BattleballColourType;
import org.alexdev.kepler.game.games.battleball.enums.BattleballPlayerState;
import org.alexdev.kepler.game.games.battleball.enums.BattleballPowerType;
import org.alexdev.kepler.game.games.battleball.events.PinSpawnEvent;
import org.alexdev.kepler.game.games.battleball.events.PlayerUpdateEvent;
import org.alexdev.kepler.game.games.battleball.objects.PinObject;
import org.alexdev.kepler.game.games.battleball.objects.PlayerUpdateObject;
import org.alexdev.kepler.game.games.battleball.objects.PowerObject;
import org.alexdev.kepler.game.games.battleball.powerups.*;
import org.alexdev.kepler.game.games.player.GamePlayer;
import org.alexdev.kepler.game.pathfinder.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BattleballPowerUp {
    private final int id;
    private final PowerObject object;
    private final AtomicInteger timeToDespawn;
    private final BattleballPowerType powerType;
    private final BattleballTile tile;
    private final Position position;
    private final BattleballGame game;
    private GamePlayer playerHolding;

    public BattleballPowerUp(BattleballGame game, int id, BattleballTile tile) {
        this.id = id;
        this.object = new PowerObject(this);
        this.tile = tile;
        this.game = game;
        this.position = this.tile.getPosition().copy();
        this.timeToDespawn = new AtomicInteger(ThreadLocalRandom.current().nextInt(15, 25 + 1));
        this.powerType = BattleballPowerType.getById(game.getAllowedPowerUps()[ThreadLocalRandom.current().nextInt(0, game.getAllowedPowerUps().length)]);
    }

    /**
     * Called when a player decides to use the power they have collected
     *
     * @param gamePlayer the game player that uses it
     * @param position the position that the power up should be used at
     */
    public void usePower(GamePlayer gamePlayer, Position position) {
        if (this.powerType == BattleballPowerType.BOX_OF_PINS) {
            NailBoxHandle.handle(this.game, gamePlayer, game.getRoom());
        }

        if (this.powerType == BattleballPowerType.FLASHLIGHT) {
            TorchHandle.handle(this.game, gamePlayer, game.getRoom());
        }

        if (this.powerType == BattleballPowerType.LIGHT_BLUB) {
            LightbulbHandle.handle(this.game, gamePlayer, game.getRoom());
        }

        if (this.powerType == BattleballPowerType.DRILL) {
            VacuumHandle.handle(this.game, gamePlayer, gamePlayer.getPlayer().getRoomUser().getRoom());
        }

        if (this.powerType == BattleballPowerType.SPRING) {
            SpringHandle.handle(this.game, gamePlayer, gamePlayer.getPlayer().getRoomUser().getRoom());
        }

        if (this.powerType == BattleballPowerType.HARLEQUIN) {
            HarlequinHandle.handle(this.game, gamePlayer, gamePlayer.getPlayer().getRoomUser().getRoom());
        }
    }

    /**
     * Get the game id of this power up
     * @return the game id
     */
    public int getId() {
        return id;
    }

    /**
     * Get the power up type of this instance
     *
     * @return the power up type
     */
    public BattleballPowerType getPowerType() {
        return powerType;
    }

    /**
     * Get the current position of where this power up spawned
     *
     * @return the current position
     */
    public Position getPosition() {
        return this.position;
    }

    /**
     * Get the tile were this power up spawned on
     *
     * @return the tile it spawned on
     */
    public BattleballTile getTile() {
        return tile;
    }

    /**
     * Get the time in seconds before it despawns
     *
     * @return the time before it despawns
     */
    public AtomicInteger getTimeToDespawn() {
        return timeToDespawn;
    }

    /**
     * Set the current player holding this power up
     *
     * @param playerHolding the player holding the power up
     */
    public void setPlayerHolding(GamePlayer playerHolding) {
        this.playerHolding = playerHolding;
    }

    /**
     * Get the current player id holding this power up, -1 if none
     *
     * @return the player id holding this power up
     */
    public Integer getPlayerHolding() {
        if (this.playerHolding != null) {
            return this.playerHolding.getObjectId();
        }

        return -1;
    }

    public GameObject getObject() {
        return object;
    }
}
