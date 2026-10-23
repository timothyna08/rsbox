package io.rsbox.server.engine.net.packet.server

import io.rsbox.server.engine.net.Session
import io.rsbox.server.engine.net.game.Codec
import io.rsbox.server.engine.net.game.Packet
import io.rsbox.server.engine.net.game.PacketType
import io.rsbox.server.engine.net.game.ServerPacket
import io.rsbox.server.util.buffer.JagByteBuf

@ServerPacket(opcode = 11, type = PacketType.FIXED)
class UpdateZonePartialFollows(val x: Int, val y: Int) : Packet {
    companion object : Codec<UpdateZonePartialFollows> {
        override fun encode(session: Session, packet: UpdateZonePartialFollows, out: JagByteBuf) {
            out.writeByte(packet.x)
            out.writeByte(packet.y)
        }
    }
}
