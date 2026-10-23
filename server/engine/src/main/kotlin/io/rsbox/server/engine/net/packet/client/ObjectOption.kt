package io.rsbox.server.engine.net.packet.client

import io.rsbox.server.engine.model.coord.Tile
import io.rsbox.server.engine.net.Session
import io.rsbox.server.engine.net.game.ClientPacket
import io.rsbox.server.engine.net.game.Codec
import io.rsbox.server.engine.net.game.Packet
import io.rsbox.server.engine.net.game.PacketType
import io.rsbox.server.util.buffer.ADD
import io.rsbox.server.util.buffer.JagByteBuf
import io.rsbox.server.util.buffer.LITTLE
import io.rsbox.server.util.buffer.NEG
import io.rsbox.server.util.buffer.SUB

abstract class ObjectOption(private val id: Int, private val x: Int, private val y: Int, private val option: Int) : Packet {
    override fun handle(session: Session) {
        session.player.interactions.onObject(id, Tile(x, y, session.player.tile.level), option)
    }
}

@ClientPacket(opcode = 38, type = PacketType.FIXED, length = 7)
class ObjectOption1(id: Int, x: Int, y: Int) : ObjectOption(id, x, y, 1) {
    companion object : Codec<ObjectOption1> {
        override fun decode(session: Session, buf: JagByteBuf): ObjectOption1 {
            val x = buf.readUnsignedShort(endian = LITTLE, transform = ADD)
            val y = buf.readUnsignedShort(transform = ADD)
            val id = buf.readUnsignedShort(transform = ADD)
            buf.readUnsignedByte()
            return ObjectOption1(id, x, y)
        }
    }
}

@ClientPacket(opcode = 39, type = PacketType.FIXED, length = 7)
class ObjectOption2(id: Int, x: Int, y: Int) : ObjectOption(id, x, y, 2) {
    companion object : Codec<ObjectOption2> {
        override fun decode(session: Session, buf: JagByteBuf): ObjectOption2 {
            val id = buf.readUnsignedShort(endian = LITTLE, transform = ADD)
            buf.readUnsignedByte(transform = NEG)
            val x = buf.readUnsignedShort(endian = LITTLE, transform = ADD)
            val y = buf.readUnsignedShort(endian = LITTLE)
            return ObjectOption2(id, x, y)
        }
    }
}

@ClientPacket(opcode = 64, type = PacketType.FIXED, length = 7)
class ObjectOption3(id: Int, x: Int, y: Int) : ObjectOption(id, x, y, 3) {
    companion object : Codec<ObjectOption3> {
        override fun decode(session: Session, buf: JagByteBuf): ObjectOption3 {
            val y = buf.readUnsignedShort(transform = ADD)
            val id = buf.readUnsignedShort()
            val x = buf.readUnsignedShort(endian = LITTLE)
            buf.readUnsignedByte(transform = NEG)
            return ObjectOption3(id, x, y)
        }
    }
}

@ClientPacket(opcode = 79, type = PacketType.FIXED, length = 7)
class ObjectOption4(id: Int, x: Int, y: Int) : ObjectOption(id, x, y, 4) {
    companion object : Codec<ObjectOption4> {
        override fun decode(session: Session, buf: JagByteBuf): ObjectOption4 {
            val y = buf.readUnsignedShort(endian = LITTLE)
            val id = buf.readUnsignedShort(transform = ADD)
            val x = buf.readUnsignedShort(transform = ADD)
            buf.readUnsignedByte(transform = SUB)
            return ObjectOption4(id, x, y)
        }
    }
}

@ClientPacket(opcode = 24, type = PacketType.FIXED, length = 7)
class ObjectOption5(id: Int, x: Int, y: Int) : ObjectOption(id, x, y, 5) {
    companion object : Codec<ObjectOption5> {
        override fun decode(session: Session, buf: JagByteBuf): ObjectOption5 {
            val id = buf.readUnsignedShort()
            val y = buf.readUnsignedShort(endian = LITTLE, transform = ADD)
            val x = buf.readUnsignedShort(endian = LITTLE, transform = ADD)
            buf.readUnsignedByte(transform = NEG)
            return ObjectOption5(id, x, y)
        }
    }
}
