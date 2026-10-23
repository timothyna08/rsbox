package io.rsbox.server.engine.net.packet.client

import io.rsbox.server.engine.net.Session
import io.rsbox.server.engine.net.game.ClientPacket
import io.rsbox.server.engine.net.game.Codec
import io.rsbox.server.engine.net.game.Packet
import io.rsbox.server.engine.net.game.PacketType
import io.rsbox.server.util.buffer.ADD
import io.rsbox.server.util.buffer.JagByteBuf
import io.rsbox.server.util.buffer.LITTLE
import io.rsbox.server.util.buffer.NEG

abstract class NpcOption(private val index: Int, private val option: Int) : Packet {
    override fun handle(session: Session) {
        session.player.interactions.onNpc(index, option)
    }
}

@ClientPacket(opcode = 70, type = PacketType.FIXED, length = 3)
class NpcOption1(index: Int) : NpcOption(index, 1) {
    companion object : Codec<NpcOption1> {
        override fun decode(session: Session, buf: JagByteBuf): NpcOption1 {
            val index = buf.readUnsignedShort(endian = LITTLE, transform = ADD)
            buf.readUnsignedByte()
            return NpcOption1(index)
        }
    }
}

@ClientPacket(opcode = 11, type = PacketType.FIXED, length = 3)
class NpcOption2(index: Int) : NpcOption(index, 2) {
    companion object : Codec<NpcOption2> {
        override fun decode(session: Session, buf: JagByteBuf): NpcOption2 {
            buf.readUnsignedByte(transform = ADD)
            return NpcOption2(buf.readUnsignedShort(endian = LITTLE, transform = ADD))
        }
    }
}

@ClientPacket(opcode = 47, type = PacketType.FIXED, length = 3)
class NpcOption3(index: Int) : NpcOption(index, 3) {
    companion object : Codec<NpcOption3> {
        override fun decode(session: Session, buf: JagByteBuf): NpcOption3 {
            buf.readUnsignedByte(transform = ADD)
            return NpcOption3(buf.readUnsignedShort(transform = ADD))
        }
    }
}

@ClientPacket(opcode = 35, type = PacketType.FIXED, length = 3)
class NpcOption4(index: Int) : NpcOption(index, 4) {
    companion object : Codec<NpcOption4> {
        override fun decode(session: Session, buf: JagByteBuf): NpcOption4 {
            buf.readUnsignedByte(transform = NEG)
            return NpcOption4(buf.readUnsignedShort())
        }
    }
}

@ClientPacket(opcode = 85, type = PacketType.FIXED, length = 3)
class NpcOption5(index: Int) : NpcOption(index, 5) {
    companion object : Codec<NpcOption5> {
        override fun decode(session: Session, buf: JagByteBuf): NpcOption5 {
            buf.readUnsignedByte()
            return NpcOption5(buf.readUnsignedShort(endian = LITTLE))
        }
    }
}
