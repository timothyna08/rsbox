package io.rsbox.server.engine.net.packet.server

import io.rsbox.server.engine.model.GameObject
import io.rsbox.server.engine.net.Session
import io.rsbox.server.engine.net.game.Codec
import io.rsbox.server.engine.net.game.Packet
import io.rsbox.server.engine.net.game.PacketType
import io.rsbox.server.engine.net.game.ServerPacket
import io.rsbox.server.util.buffer.ADD
import io.rsbox.server.util.buffer.JagByteBuf

@ServerPacket(opcode = 91, type = PacketType.FIXED)
class LocChange(val obj: GameObject, val x: Int, val y: Int) : Packet {
    companion object : Codec<LocChange> {
        override fun encode(session: Session, packet: LocChange, out: JagByteBuf) {
            out.writeByte(31)
            out.writeByte((packet.obj.shape shl 2) or packet.obj.rotation, transform = ADD)
            out.writeByte((packet.x shl 4) or packet.y, transform = ADD)
            out.writeShort(packet.obj.id, transform = ADD)
        }
    }
}
