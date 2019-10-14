package and390.binpack

import org.testng.Assert.assertEquals
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.lang.Math.random


class ReadWriteTest
{
    private fun <T> readWriteValueTest(value: T, size: Int, write: (T, ByteArray, Int) -> Int, read: (ByteArray, Int) -> T)
    {
        fun test(buffer: ByteArray, offset: Int) {
            val end = write(value, buffer, offset)
            assertEquals(end, offset+size, "offset")
            val result = read(buffer, offset)
            assertEquals(result, value, "read written value")
        }
        val buffer = ByteArray(size)
        test(buffer,0)
        val buffer2 = ByteArray(size+2) { (1+(random()*255).toInt()).toByte() }
        test(buffer2,1)
    }

    private fun <T> readWriteValueTest(value: T, size: Int, write: BinaryWriter.(T) -> Unit, read: BinaryReader.() -> T) =
            readWriteTest(size, 0, { write(value) }, {
                val result = read()
                assertEquals(result, value, "read written value")
            })

    private fun readWriteTest(fullSize: Int, bits: Int, write: BinaryWriter.() -> Unit, read: BinaryReader.() -> Unit)
    {
        fun test(buffer: ByteArray, offset: Int) {
            val writer = BinaryWriter(buffer, offset, bits)
            write(writer)
            writer.finish()
            assertEquals(writer.offset, offset+fullSize, "offset")
            val reader = BinaryReader(buffer, offset, bits)
            read(reader)
            assertEquals(reader.offset, offset+fullSize, "offset")
        }
        val buffer = ByteArray(fullSize)
        test(buffer,0)
        val buffer2 = ByteArray(fullSize+2) { (1+(random()*255).toInt()).toByte() }
        test(buffer2,1)
    }

    @Test  fun testLong1() = readWriteValueTest(1234567890123456789L, Long.SIZE_BYTES, ::writeLong, ::readLong)
    @Test  fun testLong2() = readWriteValueTest(-1234567890123456789L, Long.SIZE_BYTES, ::writeLong, ::readLong)
    @Test  fun testLong3() = readWriteValueTest(1234567890123456789L, Long.SIZE_BYTES, BinaryWriter::writeLong, BinaryReader::readLong)
    @Test  fun testInt1() { readWriteValueTest(1234567890, Int.SIZE_BYTES, ::writeInt, ::readInt) }
    @Test  fun testInt2() { readWriteValueTest(-1234567890, Int.SIZE_BYTES, ::writeInt, ::readInt) }
    @Test  fun testInt3() = readWriteValueTest(1234567890, Int.SIZE_BYTES, BinaryWriter::writeInt, BinaryReader::readInt)
    @Test  fun testShort1() { readWriteValueTest(12345.toShort(), Short.SIZE_BYTES, ::writeShort, ::readShort) }
    @Test  fun testShort2() { readWriteValueTest((-12345).toShort(), Short.SIZE_BYTES, ::writeShort, ::readShort) }
    @Test  fun testShort3() { readWriteValueTest(12345.toShort(), Short.SIZE_BYTES, BinaryWriter::writeShort, BinaryReader::readShort) }
    @Test  fun testByte1() { readWriteValueTest(123.toByte(), Byte.SIZE_BYTES, ::writeByte, ::readByte) }
    @Test  fun testByte2() { readWriteValueTest((-123).toByte(), Byte.SIZE_BYTES, ::writeByte, ::readByte) }
    @Test  fun testByte3() { readWriteValueTest(123.toByte(), Byte.SIZE_BYTES, BinaryWriter::writeByte, BinaryReader::readByte) }
    @Test  fun testDouble1() { readWriteValueTest(123.456, Long.SIZE_BYTES, ::writeDouble, ::readDouble) }
    @Test  fun testDouble2() { readWriteValueTest(123.456, Long.SIZE_BYTES, BinaryWriter::writeDouble, BinaryReader::readDouble) }
    @Test  fun testFloat1() { readWriteValueTest(123.456f, Int.SIZE_BYTES, BinaryWriter::writeFloat, BinaryReader::readFloat) }
    @Test  fun testFloat2() { readWriteValueTest(123.456f, Int.SIZE_BYTES, BinaryWriter::writeFloat, BinaryReader::readFloat) }

    @DataProvider(name = "varInt")
    fun varIntTestCases(): Array<Array<out Any>> = arrayOf(
            arrayOf(0, 1),
            arrayOf(127, 1),
            arrayOf(128, 2),
            arrayOf(16383, 2),
            arrayOf(16384, 3),
            arrayOf(0x1FFFFF, 3),
            arrayOf(0x200000, 4),
            arrayOf(0x0FFFFFFF, 4),
            arrayOf(0x10000000, 5),
            arrayOf(Int.MAX_VALUE, 5),
            arrayOf(-1, 5),
            arrayOf(-2, 5),
            arrayOf(-3, 5),
            arrayOf(-0x10000000, 5),
            arrayOf(Int.MIN_VALUE, 5)
    )

    @Test(dataProvider = "varInt")
    fun testVarIntLength(value: Int, size: Int) = assertEquals(varIntLength(value), size)

    @Test(dataProvider = "varInt")
    fun testVarInt(value: Int, size: Int) = readWriteValueTest(value, size, BinaryWriter::writeVarInt, BinaryReader::readVarInt)

    @DataProvider(name = "varIntNeg")
    fun varIntNegTestCases(): Array<Array<out Any>> = arrayOf(
            arrayOf(0, 1),
            arrayOf(63, 1),
            arrayOf(64, 2),
            arrayOf(8191, 2),
            arrayOf(8192, 3),
            arrayOf(0x0FFFFF, 3),
            arrayOf(0x100000, 4),
            arrayOf(0x7FFFFFF, 4),
            arrayOf(0x8000000, 5),
            arrayOf(Int.MAX_VALUE, 5),
            arrayOf(-1, 1),
            arrayOf(-2, 1),
            arrayOf(-3, 1),
            arrayOf(-64, 1),
            arrayOf(-65, 2),
            arrayOf(-8192, 2),
            arrayOf(-8193, 3),
            arrayOf(-0x100000, 3),
            arrayOf(-0x100001, 4),
            arrayOf(-0x8000000, 4),
            arrayOf(-0x8000001, 5),
            arrayOf(Int.MIN_VALUE, 5)
    )

    @Test(dataProvider = "varIntNeg")
    fun testVarIntNegLength(value: Int, size: Int) = assertEquals(varIntNegLength(value), size)

    @Test(dataProvider = "varIntNeg")
    fun testVarIntNeg(value: Int, size: Int) = readWriteValueTest(value, size, BinaryWriter::writeVarIntNeg, BinaryReader::readVarIntNeg)

    @Test  fun testUtf8Length0() = assertEquals(utf8Length(""), 0)
    @Test  fun testUtf8Length1() = assertEquals(utf8Length("apple"), 5)
    @Test  fun testUtf8Length2() = assertEquals(utf8Length("яблоко"), 12)

    @Test  fun testStringLength0() = assertEquals(BinaryWriter.stringLength(""), 1)
    @Test  fun testStringLength1() = assertEquals(BinaryWriter.stringLength(String(CharArray(63){'я'})), 126+1)
    @Test  fun testStringLength2() = assertEquals(BinaryWriter.stringLength(String(CharArray(64){'я'})), 128+2)

    @Test  fun testString1() = readWriteValueTest("", 1, BinaryWriter::writeString, BinaryReader::readString)
    @Test  fun testString2() = readWriteValueTest("xyz яблоко", 17, BinaryWriter::writeString, BinaryReader::readString)
    @Test  fun testString3() = readWriteValueTest(String(CharArray(16384){'a' + it%('z'-'a'+1)}), 3+16384, BinaryWriter::writeString, BinaryReader::readString)

    @DataProvider(name = "bit")
    fun bitTestCases(): Array<Array<out Any>> = arrayOf(
            arrayOf(1, 1, booleanArrayOf(true)),
            arrayOf(1, 1, booleanArrayOf(false)),
            arrayOf(1, 8, booleanArrayOf(true,false,true,false,false,true,true,true)),
            arrayOf(2, 9, booleanArrayOf(true,false,true,false,false,true,true,true,false)),
            arrayOf(2, 16, BooleanArray(16) { it%2 == 1 }),
            arrayOf(3, 17, BooleanArray(17) { it%2 == 1 }),
            arrayOf(3, 24, BooleanArray(24) { it%2 == 1 }),
            arrayOf(4, 25, BooleanArray(25) { it%2 == 1 }),
            arrayOf(4, 32, BooleanArray(32) { it%2 == 1 }),
            arrayOf(5, 33, BooleanArray(33) { it%2 == 1 })
    )
    @Test(dataProvider = "bit")
    fun testBit(fullSize: Int, bitCount: Int, values: BooleanArray) = readWriteTest(fullSize, bitCount, { values.forEach { writeBit(it) } }) {
        for ((i,expected) in values.withIndex())  {
            val bit = readBit()
            assertEquals(bit, expected, "read written bit[$i]")
        }
    }

    @DataProvider(name = "bits")
    fun bitsTestCases(): Array<Array<Array<Pair<Int,Int>>>> = arrayOf(
            arrayOf(arrayOf(0 to 0)),
            arrayOf(arrayOf(1 to 1)),
            arrayOf(arrayOf(3 to 5)),
            arrayOf(arrayOf(5 to 26, 4 to 11, 1 to 1)),
            arrayOf(arrayOf(5 to 27, 31 to 33554464, 32 to 33554465)),
            arrayOf(arrayOf(8 to 0, 8 to 127, 7 to 2)),
            arrayOf(arrayOf(32 to 2854503626.toInt(), 31 to 879336341, 3 to 5, 30 to 154557292))
    )
    @Test(dataProvider = "bits")
    fun testBits(bitCountToValueArray: Array<Pair<Int,Int>>) = bitCountToValueArray.sumBy { it.first }.let { totalBits ->
        readWriteTest((totalBits + 7) / 8, totalBits, { bitCountToValueArray.forEach { writeBits(it.second, it.first) } }) {
            bitCountToValueArray.forEachIndexed { i, (count, expected) ->
                val result = readBits(count)
                assertEquals(result, expected, "read written bits[$i]")
            }
        }
    }

    @Test  fun testSubWriter() = readWriteTest(1+1+4*3, 1, {
        writeBit(true)
        writeInt(16)
        withSubWriter(1) { subWriter ->
            subWriter.writeBit(true)
            subWriter.writeInt(32)
        }
        writeBit(true)
        writeInt(48)
    }, {
        assertEquals(readBit(), true)
        assertEquals(readInt(), 16)
        withSubReader(1) { subReader ->
            assertEquals(subReader.readBit(), true)
            assertEquals(subReader.readInt(), 32)
        }
        assertEquals(readBit(), true)
        assertEquals(readInt(), 48)
    })
}