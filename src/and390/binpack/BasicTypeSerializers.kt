package and390.binpack


abstract class BasicTypeSerializer<T>(override val bitCount: Int = 0) : ClassSerializer<T>

class LongSerializer : BasicTypeSerializer<Long>() {
    override fun bodySize(value: Long) = 8
    override fun write(value: Long, writer: BinaryWriter) = writer.writeLong(value)
    override fun read(reader: BinaryReader): Long = reader.readLong()
}

class IntSerializer : BasicTypeSerializer<Int>() {
    override fun bodySize(value: Int) = 4
    override fun write(value: Int, writer: BinaryWriter) = writer.writeInt(value)
    override fun read(reader: BinaryReader): Int = reader.readInt()
}

class ShortSerializer : BasicTypeSerializer<Short>() {
    override fun bodySize(value: Short) = 2
    override fun write(value: Short, writer: BinaryWriter) = writer.writeShort(value)
    override fun read(reader: BinaryReader): Short = reader.readShort()
}

class ByteSerializer : BasicTypeSerializer<Byte>() {
    override fun bodySize(value: Byte) = 1
    override fun write(value: Byte, writer: BinaryWriter) = writer.writeByte(value)
    override fun read(reader: BinaryReader): Byte = reader.readByte()
}

class DoubleSerializer : BasicTypeSerializer<Double>() {
    override fun bodySize(value: Double) = 8
    override fun write(value: Double, writer: BinaryWriter) = writer.writeDouble(value)
    override fun read(reader: BinaryReader): Double = reader.readDouble()
}

class FloatSerializer : BasicTypeSerializer<Float>() {
    override fun bodySize(value: Float) = 4
    override fun write(value: Float, writer: BinaryWriter) = writer.writeFloat(value)
    override fun read(reader: BinaryReader): Float = reader.readFloat()
}

class BooleanSerializer : BasicTypeSerializer<Boolean>(1) {
    override fun bodySize(value: Boolean) = 0
    override fun write(value: Boolean, writer: BinaryWriter) = writer.writeBit(value)
    override fun read(reader: BinaryReader): Boolean = reader.readBit()
}

class StringSerializer : BasicTypeSerializer<String>() {
    override fun bodySize(value: String) = BinaryWriter.stringLength(value)
    override fun write(value: String, writer: BinaryWriter) = writer.writeString(value)
    override fun read(reader: BinaryReader): String = reader.readString()
}


