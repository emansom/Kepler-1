package org.alexdev.kepler.messages.outgoing.rooms.games;

import org.alexdev.kepler.messages.types.MessageComposer;
import org.alexdev.kepler.server.netty.streams.NettyResponse;

public class OPENGAMEBOARD extends MessageComposer {
    private final int gameId;
    private final String type;

    public OPENGAMEBOARD(String type) {
        this.gameId = 1;
        this.type = type;
    }

    @Override
    public void compose(NettyResponse response) {
        response.writeDelimeter(this.gameId, (char)9);
        response.writeDelimeter(this.type, (char)9);
        //response.writeDelimeter("xo", (char)9);
    }

    @Override
    public short getHeader() {
        return 145; // "BQ"
    }
}
