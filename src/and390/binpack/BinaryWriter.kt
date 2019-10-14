package and390.binpack


//  Use big-endian to support natural order

class BinaryWriter (
        val buffer: ByteArray,
        baseOffset: Int,
        bitCount: Int
) {
    var offset = baseOffset + (bitCount + 7) / 8
    private var bits: Int = 0
    private var bitIndex: Int = Int.SIZE_BITS
    private var bitOffset: Int = baseOffset

    fun finish(): ByteArray {
        val bits = this.bits
        bitOffset =
                if (bitIndex < Byte.SIZE_BITS)  writeInt(bits, buffer, bitOffset)
                else if (bitIndex < 2*Byte.SIZE_BITS)  {
                    val off = writeShort((bits ushr 16).toShort(), buffer, bitOffset)
                    writeByte((bits ushr 8).toByte(), buffer, off)
                }
                else if (bitIndex < 3*Byte.SIZE_BITS)  writeShort((bits ushr 16).toShort(), buffer, bitOffset)
                else if (bitIndex < Int.SIZE_BITS)  writeByte((bits ushr 24).toByte(), buffer, bitOffset)
                else  bitOffset
        bitIndex = 0
        return buffer
    }

    fun writeBit(value: Boolean) {
        if (bitIndex == 0)  {
            // fush
            bitOffset = writeInt(bits, buffer, bitOffset)
            bits = 0
            bitIndex = Int.SIZE_BITS
        }
        bitIndex--
        bits = bits or ((if (value) 1 else 0) shl bitIndex)
    }

    fun writeBits(value: Int, bitCount: Int)  {
        var v = value
        var n = bitCount
        var bitIndex = this.bitIndex
        var bits = this.bits
        if (n > bitIndex) {
            bits = bits or (v ushr (n-bitIndex))
            v = v and ((1 shl (n-bitIndex)) - 1)
            n -= bitIndex
            // flush
            bitOffset = writeInt(bits, buffer, bitOffset)
            bits = 0
            bitIndex = Int.SIZE_BITS
        }
        bitIndex -= n
        bits = bits or (v shl bitIndex)
        this.bitIndex = bitIndex
        this.bits = bits
    }

    fun writeByte(value: Byte)  {  offset = writeByte(value, buffer, offset)  }
    fun writeShort(value: Short)  {  offset = writeShort(value, buffer, offset)  }
    fun writeInt(value: Int)  {  offset = writeInt(value, buffer, offset)  }
    fun writeLong(value: Long)  {  offset = writeLong(value, buffer, offset)  }
    fun writeFloat(value: Float)  {  offset = writeFloat(value, buffer, offset)  }
    fun writeDouble(value: Double)  {  offset = writeDouble(value, buffer, offset)  }

    fun writeVarInt(value: Int)  {  offset = writeVarInt(value, buffer, offset)  }
    fun writeVarIntNeg(value: Int)  {  offset = writeVarIntNeg(value, buffer, offset)  }

    fun writeString(value: String) {
        val bytes = value.toByteArray(charset)
        writeVarInt(bytes.size)
        val off = offset
        offset += bytes.size
        System.arraycopy(bytes, 0, buffer, off, bytes.size)
    }

    inline fun withSubWriter(subBitCount: Int, action: (BinaryWriter) -> Unit) {
        val writer = BinaryWriter(buffer, offset, subBitCount)
        action(writer)
        writer.finish()
        offset = writer.offset
    }

    companion object {
        val charset = Charsets.UTF_8

        fun stringLength(value: String): Int = utf8Length(value).let { it + varIntLength(it) }
    }
}

fun writeVarIntNeg(value: Int, buffer: ByteArray, offset: Int) = writeVarInt(value shl 1 xor (value shr 31), buffer, offset)

fun writeVarInt(value: Int, buffer: ByteArray, offset: Int): Int {
    if (value.ushr(7) == 0) {
        buffer[offset] = value.toByte()
        return offset+1
    }
    else if (value.ushr(14) == 0) {
        buffer[offset] = (value.ushr(7) or 0x80).toByte()
        buffer[offset+1] = (value and 0x7F).toByte()
        return offset+2
    }
    else if (value.ushr(21) == 0) {
        buffer[offset] = (value.ushr(14) or 0x80).toByte()
        buffer[offset+1] = (value.ushr(7) or 0x80).toByte()
        buffer[offset+2] = (value and 0x7F).toByte()
        return offset+3
    }
    else if (value.ushr(28) == 0) {
        buffer[offset] = (value.ushr(21) or 0x80).toByte()
        buffer[offset+1] = (value.ushr(14) or 0x80).toByte()
        buffer[offset+2] = (value.ushr(7) or 0x80).toByte()
        buffer[offset+3] = (value and 0x7F).toByte()
        return offset+4
    }
    else  {
        buffer[offset] = (value.ushr(28) or 0x80).toByte()
        buffer[offset+1] = (value.ushr(21) or 0x80).toByte()
        buffer[offset+2] = (value.ushr(14) or 0x80).toByte()
        buffer[offset+3] = (value.ushr(7) or 0x80).toByte()
        buffer[offset+4] = (value and 0x7F).toByte()
        return offset+5
    }
}

fun varIntLength(value: Int) =
        if (value.ushr(7) == 0)  1
        else if (value.ushr(14) == 0)  2
        else if (value.ushr(21) == 0)  3
        else if (value.ushr(28) == 0)  4
        else  5

fun varIntNegLength(value: Int) = (value xor (value shr 31)).let {
        if (it.ushr(6) == 0)  1
        else if (it.ushr(13) == 0)  2
        else if (it.ushr(20) == 0)  3
        else if (it.ushr(27) == 0)  4
        else  5
    }

fun writeLong(value: Long, buffer: ByteArray, offset: Int = 0): Int {
    var i = offset
    buffer[i++] = ((value ushr 56) and 0xFF).toByte()
    buffer[i++] = ((value ushr 48) and 0xFF).toByte()
    buffer[i++] = ((value ushr 40) and 0xFF).toByte()
    buffer[i++] = ((value ushr 32) and 0xFF).toByte()
    buffer[i++] = ((value ushr 24) and 0xFF).toByte()
    buffer[i++] = ((value ushr 16) and 0xFF).toByte()
    buffer[i++] = ((value ushr 8) and 0xFF).toByte()
    buffer[i++] = (value and 0xFF).toByte()
    return i
}

fun writeInt(value: Int, buffer: ByteArray, offset: Int = 0): Int {
    var i = offset
    buffer[i++] = ((value ushr 24) and 0xFF).toByte()
    buffer[i++] = ((value ushr 16) and 0xFF).toByte()
    buffer[i++] = ((value ushr 8) and 0xFF).toByte()
    buffer[i++] = (value and 0xFF).toByte()
    return i
}

fun writeShort(value: Short, buffer: ByteArray, offset: Int = 0): Int {
    @Suppress("name_shadowing")  val value = value.toInt()
    var i = offset
    buffer[i++] = ((value ushr 8) and 0xFF).toByte()
    buffer[i++] = (value and 0xFF).toByte()
    return i
}

fun writeByte(value: Byte, buffer: ByteArray, offset: Int = 0): Int {
    buffer[offset] = value
    return offset+1
}

fun writeFloat(value: Float, buffer: ByteArray, offset: Int = 0): Int = writeInt(value.toRawBits(), buffer, offset)

fun writeDouble(value: Double, buffer: ByteArray, offset: Int = 0): Int = writeLong(value.toRawBits(), buffer, offset)


fun utf8Length(sequence: CharSequence): Int {
    var count = 0
    var i = 0
    val len = sequence.length
    while (i < len) {
        val ch = sequence[i]
        if (ch.toInt() <= 0x7F) {
            count++
        } else if (ch.toInt() <= 0x7FF) {
            count += 2
        } else if (Character.isHighSurrogate(ch)) {
            count += 4
            ++i
        } else {
            count += 3
        }
        i++
    }
    return count
}