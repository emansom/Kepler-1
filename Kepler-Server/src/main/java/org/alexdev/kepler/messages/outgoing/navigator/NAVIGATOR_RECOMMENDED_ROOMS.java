package org.alexdev.kepler.messages.outgoing.navigator;

import org.alexdev.kepler.game.player.Player;
import org.alexdev.kepler.game.room.Room;
import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

import java.util.List;

public class NAVIGATOR_RECOMMENDED_ROOMS extends MessageComposer {
    private final Player player;
    private final List<Room> roomList;

    public NAVIGATOR_RECOMMENDED_ROOMS(Player player, List<Room> roomList) {
        this.player = player;
        this.roomList = roomList;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeInt(this.roomList.size());

        for (Room room : this.roomList) {
            response.writeInt(room.getId());
            response.writeString(room.getData().getName());

            if (room.isOwner(this.player.getEntityId()) || room.getData().showName() || this.player.hasFuse("fuse_see_all_roomowners")) {
                response.writeString(room.getData().getOwnerName());
            } else {
                response.writeString("-");
            }

            response.writeString(room.getData().getAccessType());
            response.writeInt(room.getData().getVisitorsNow());
            response.writeInt(room.getData().getVisitorsMax());
            response.writeString(room.getData().getDescription());
        }
    }

    @Override
    public short getHeader() {
        return 351; // "E_"
    }
}
