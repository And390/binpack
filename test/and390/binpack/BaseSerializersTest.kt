package and390.binpack

import org.testng.Assert.*
import org.testng.annotations.Test


abstract class AbstractSerializerTest
{
    val serializer = Serializer()

    fun <T : Any> readWriteTest(obj: T, cls: Class<T>): ByteArray
    {
        val buf = serializer.write(obj, cls)
        val res = serializer.read(cls, buf, 0)
        println(res)
        assertEquals(res, obj)
        return buf
    }

    fun readWriteTest(obj: Any) = readWriteTest(obj, obj.javaClass)

    fun checkCircularDependency(cls: Class<out Any>, expected: Boolean) =
            assertEquals((serializer.getSerializer(cls) as BasicObjectSerializer).circularDependency, expected)
}

class BaseSerializersTest : AbstractSerializerTest()
{
    @Test  fun testLong() { readWriteTest(1234567890123456789L) }
    @Test  fun testInt() { readWriteTest(1234567890) }
    @Test  fun testShort() { readWriteTest(12345.toShort()) }
    @Test  fun testByte() { readWriteTest(123.toByte()) }
    @Test  fun testDouble() { readWriteTest(123.456) }
    @Test  fun testFloat() { readWriteTest(123.456f) }
    @Test  fun testBoolean0() { readWriteTest(false) }
    @Test  fun testBoolean1() { readWriteTest(true) }
    @Test  fun testString() { readWriteTest("abcd") }

    @Test  fun testSimplestObject()
    {
        data class A(
            val f1: Int,
            val f2: Float
        )
        readWriteTest(A(1,1.0f))
    }

    @Test  fun testObjectNullFields()
    {
        data class A(
            val a1: Long,
            val a2: Int,
            val a3: Short,
            val a4: Byte,
            val a5: Double,
            val a6: Float,
            val a7: Boolean,
            val a8: String,
            val b1: Long?,
            val b2: Int?,
            val b3: Short?,
            val b4: Byte?,
            val b5: Double?,
            val b6: Float?,
            val b7: Boolean?,
            val b8: String?
        )
        readWriteTest(A(1,2,3,4,5.0,6f,false,"xxx",11,12,13,14,15.0,16f,true,"yyy"))
        readWriteTest(A(1,2,3,4,5.0,6f,true,"xxx",null,null,null,null,null,null,null,null))
    }

    @Test  fun testNestedObjects()
    {
        data class A(
            val f1: Boolean,
            val f2: Int?
        )
        data class B(
            val a1: A,
            val f: Boolean?,
            val a2: A?
        )
        data class C(
            val b: B?,
            val a: A?
        )

        readWriteTest(B(A(true,1),false,null))
        readWriteTest(B(A(true,2),true,A(true,null)))
        readWriteTest(B(A(false,3),null,A(true,5)))
        readWriteTest(C(null,A(false,6)))
        readWriteTest(C(B(A(false,7),true,A(true,8)),A(false,9)))
    }

    @Test fun testCircularDependency1()
    {
        data class A(
            val a: A?
        )

        readWriteTest(A(null))
        readWriteTest(A(A(null)))
        readWriteTest(A(A(A(null))))
    }

    data class A2(
        val b: B2?
    )
    data class B2(
        val a: A2?
    )
    @Test fun testCircularDependency2()
    {
        readWriteTest(A2(null))
        readWriteTest(B2(null))
        readWriteTest(A2(B2(null)))
        readWriteTest(B2(A2(null)))
        readWriteTest(A2(B2(A2(null))))
        readWriteTest(B2(A2(B2(null))))
        readWriteTest(A2(B2(A2(B2(null)))))
    }

    data class A3(
        val b: B3?
    )
    data class B3(
        val c: C3?
    )
    data class C3(
        val a: A3?,
        val d: D3?
    )
    data class D3(
        val f: Boolean
    )
    @Test fun testCircularDependency3()
    {
        checkCircularDependency(C3::class.java, true)
        checkCircularDependency(D3::class.java, false)
        readWriteTest(A3(B3(null)))
        readWriteTest(A3(B3(C3(null,D3(false)))))
        readWriteTest(A3(B3(C3(A3(null),null))))
        readWriteTest(A3(B3(C3(A3(B3(C3(A3(B3(null)),D3(true)))),null))))
    }

    data class A4(
        val b: B4?
    )
    data class B4(
        val c: C4?
    )
    data class C4(
        val b: B4?
    )
    @Test fun testCircularDependency4() {
        checkCircularDependency(A4::class.java, false)
        readWriteTest(A4(B4(null)))
        readWriteTest(A4(B4(C4(null))))
        readWriteTest(A4(B4(C4(B4(null)))))
    }

    data class A5(
        val b: B5?,
        val c: C5?
    )
    data class B5(
        val a: A5?
    )
    data class C5(
        val d: D5?
    )
    data class D5(
        val e: E5?
    )
    data class E5(
        val f: F5?
    )
    data class F5(
        val d: D5?
    )
    @Test fun testCircularDependency5() {
        checkCircularDependency(C5::class.java, false)
        checkCircularDependency(E5::class.java, true)
        readWriteTest(A5(B5(A5(B5(null),null)),null))
        readWriteTest(A5(B5(null),C5(D5(E5(F5(D5(E5(F5(D5(null))))))))))
    }
}