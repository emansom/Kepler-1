package org.alexdev.kepler.game.games.battleball;

import org.alexdev.kepler.game.games.battleball.enums.BattleballPowerType;
import org.alexdev.kepler.game.games.player.GamePlayer;
import org.alexdev.kepler.game.pathfinder.Position;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class BattleballPowerUp {
    private final int id;
    private final AtomicInteger timeToDespawn;
    private final BattleballPowerType powerType;
    private final BattleballTile tile;
    private final Position position;
    private GamePlayer playerHolding;

    public BattleballPowerUp(BattleballGame game, int id, BattleballTile tile) {
        this.id = id;
        this.tile = tile;
        this.position = this.tile.getPosition().copy();
        this.timeToDespawn = new AtomicInteger(ThreadLocalRandom.current().nextInt(10, 20));
        this.powerType = BattleballPowerType.getById(game.getAllowedPowerUps()[ThreadLocalRandom.current().nextInt(0, game.getAllowedPowerUps().length)]);
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
            return this.playerHolding.getPlayer().getRoomUser().getInstanceId();
        }

        return -1;
    }
}
