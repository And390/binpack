package and390.binpack


//  Use big-endian to support natural order

class BinaryReader (
        val buffer: ByteArray,
        baseOffset: Int,
        bitCount: Int
) {
    var offset = baseOffset + (bitCount + 7) / 8
    private var bits: Int = 0
    private var bitIndex: Int = 0
    private var bitOffset: Int = baseOffset

    fun readBit(): Boolean {
        if (bitIndex == 0)  {
            bits = buffer[bitOffset++].toInt()
            bitIndex = Byte.SIZE_BITS
        }
        bitIndex--
        return ((bits ushr bitIndex) and 1) != 0
    }

    fun readBits(bitCount: Int): Int {
        var n = bitCount
        var v = 0
        var bits = this.bits
        var bitIndex = this.bitIndex
        while (n > bitIndex) {
            v = (v shl bitIndex) or (bits.and(1.shl(bitIndex)-1))
            n -= bitIndex
            // read next
            bits = buffer[bitOffset++].toInt()
            bitIndex = Byte.SIZE_BITS
        }
        bitIndex -= n
        v = (v shl n) or (bits ushr bitIndex).and(1.shl(n)-1)
        this.bitIndex = bitIndex
        this.bits = bits
        return v
    }

    fun readByte(): Byte {
        val off = offset
        offset = off + Byte.SIZE_BYTES
        return readByte(buffer, off)
    }

    fun readShort(): Short {
        val off = offset
        offset = off + Short.SIZE_BYTES
        return readShort(buffer, off)
    }

    fun readInt(): Int {
        val off = offset
        offset = off + Int.SIZE_BYTES
        return readInt(buffer, off)
    }

    fun readLong(): Long {
        val off = offset
        offset = off + Long.SIZE_BYTES
        return readLong(buffer, off)
    }

    fun readFloat(): Float {
        val off = offset
        offset = off + 4
        return readFloat(buffer, off)
    }

    fun readDouble(): Double {
        val off = offset
        offset = off + 8
        return readDouble(buffer, off)
    }

    fun readVarIntNeg() = readVarInt().let { it.ushr(1) xor -(it and 1) }

    fun readVarInt(): Int {
        var p = offset
        var b = buffer[p++].toInt()
        var result = b and 0x7F
        if (b and 0x80 != 0) {
            b = buffer[p++].toInt()
            result = (result shl 7) or (b and 0x7F)
            if (b and 0x80 != 0) {
                b = buffer[p++].toInt()
                result = (result shl 7) or (b and 0x7F)
                if (b and 0x80 != 0) {
                    b = buffer[p++].toInt()
                    result = (result shl 7) or (b and 0x7F)
                    if (b and 0x80 != 0) {
                        b = buffer[p++].toInt()
                        result = (result shl 7) or (b and 0x7F)
                    }
                }
            }
        }
        offset = p
        return result
    }

    fun readString(): String {
        val n = readVarInt()
        val off = offset
        offset += n
        return String(buffer, off, n, BinaryWriter.charset)
    }

    inline fun <T> withSubReader(bitCount: Int, action: (BinaryReader) -> T): T {
        val reader = BinaryReader(buffer, offset, bitCount)
        val result = action(reader)
        offset = reader.offset
        return result
    }
}


fun readLong(buffer: ByteArray, offset: Int = 0) : Long {
    return ((buffer[offset].toLong() and 0xFF) shl 56) +
            ((buffer[offset+1].toLong() and 0xFF) shl 48) +
            ((buffer[offset+2].toLong() and 0xFF) shl 40) +
            ((buffer[offset+3].toLong() and 0xFF) shl 32) +
            ((buffer[offset+4].toLong() and 0xFF) shl 24) +
            ((buffer[offset+5].toLong() and 0xFF) shl 16) +
            ((buffer[offset+6].toLong() and 0xFF) shl 8) +
            (buffer[offset+7].toLong() and 0xFF)
}

fun readInt(buffer: ByteArray, offset: Int = 0) : Int {
    return ((buffer[offset].toInt() and 0xFF) shl 24) +
            ((buffer[offset+1].toInt() and 0xFF) shl 16) +
            ((buffer[offset+2].toInt() and 0xFF) shl 8) +
            (buffer[offset+3].toInt() and 0xFF)
}

fun readShort(buffer: ByteArray, offset: Int = 0) : Short {
    return (((buffer[offset].toInt() and 0xFF) shl 8) + (buffer[offset+1].toInt() and 0xFF)).toShort()
}

fun readByte(buffer: ByteArray, offset: Int = 0): Byte {
    return buffer[offset]
}

fun readFloat(buffer: ByteArray, offset: Int = 0) = Float.fromBits(readInt(buffer, offset))

fun readDouble(buffer: ByteArray, offset: Int = 0) = Double.fromBits(readLong(buffer, offset))