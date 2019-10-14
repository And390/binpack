package and390.binpack

import org.testng.Assert
import org.testng.annotations.BeforeClass
import org.testng.annotations.Test
import java.util.*


class SubclassSerializersTest : AbstractSerializerTest()
{
    interface Base

    data class SubclassA(
        val f: String
    ) : Base

    data class SubclassB(
        val a: Int,
        val b: Int?
    ) : Base

    @BeforeClass
    fun specifySubclasses() {
        serializer.getSerializer(SubclassA::class.java)
        serializer.getSerializer(SubclassB::class.java)
    }

    @Test
    fun testSubTypesBare()
    {
        readWriteTest(SubclassA("abc"), Base::class.java)
        readWriteTest(SubclassB(1, 2), Base::class.java)
    }

    @Test
    fun testSubTypes()
    {
        data class A(
            val link: Base
        )
        readWriteTest(A(SubclassA("abc")))
        readWriteTest(A(SubclassB(1, 2)))
    }

    @Test
    fun testSubTypesList()
    {
        data class A(
            val list: List<Base>
        )
        readWriteTest(A(listOf(SubclassA("abc"),SubclassB(1,2))))
    }

    @Test
    fun testSubTypesArray()
    {
        data class A(
            val list: Array<Base>
        ) {
            override fun equals(other: Any?): Boolean = this === other || other is A && Arrays.equals(list, other.list)
            override fun hashCode(): Int = Arrays.hashCode(list)
        }
        readWriteTest(A(arrayOf(SubclassA("abc"),SubclassB(1,2))))
    }


    open class Base2(
            val f: Float
    ) {
        override fun equals(other: Any?): Boolean = this === other || other is Base2 && f == other.f
        override fun hashCode(): Int = f.hashCode()
        override fun toString(): String = "Base2(f=$f)"
    }

    class Subclass2A(
            f: Float,
            val s: String
    ) : Base2(f) {
        override fun equals(other: Any?): Boolean = this === other || other is Subclass2A && f == other.f && s == other.s
        override fun hashCode(): Int = f.hashCode() * 31 + s.hashCode()
        override fun toString(): String = "Subclass2A(f=$f, s=$s)"
    }

    class Subclass2B(
            f: Float,
            val n: Int
    ) : Base2(f) {
        override fun equals(other: Any?): Boolean = this === other || other is Subclass2B && f == other.f && n == other.n
        override fun hashCode(): Int = f.hashCode() * 31 + n.hashCode()
        override fun toString(): String = "Subclass2B(f=$f, n=$n)"
    }

    @BeforeClass
    fun specifySubclasses2() {
        serializer.getSerializer(Subclass2A::class.java)
        serializer.getSerializer(Subclass2B::class.java)
    }

    @Test
    fun testSubTypesWithBaseImpl()
    {
        readWriteTest(Base2(1f), Base2::class.java)
        readWriteTest(Subclass2A(2f,"abc"), Base2::class.java)
        readWriteTest(Subclass2B(3f, 5), Base2::class.java)

        data class A(
                val link: Base2
        )
        readWriteTest(A(Base2(1f)))
        readWriteTest(A(Subclass2A(2f,"abc")))
        readWriteTest(A(Subclass2B(3f, 5)))
    }


    interface Base3

    abstract class Base3A : Base3

    data class Subclass3B(val f: Boolean) : Base3

    data class Subclass3C(
        val f: Int
    ) : Base3A()

    data class Subclass3D(
        val f: Int?
    ) : Base3A()

    @BeforeClass
    fun specifySubclasses3() {
        serializer.getSerializer(Subclass3C::class.java)
        serializer.getSerializer(Subclass3D::class.java)
        serializer.getSerializer(Base3A::class.java)
        serializer.getSerializer(Subclass3B::class.java)
        serializer.getSerializer(Base3::class.java)
    }

    @Test
    fun testParentChain() {
        readWriteTest(Subclass3B(true), Base3::class.java)
        readWriteTest(Subclass3C(8), Base3::class.java)
        readWriteTest(Subclass3D(null), Base3::class.java)
        readWriteTest(Subclass3C(16), Base3A::class.java)
        readWriteTest(Subclass3D(18), Base3A::class.java)
    }


    interface Base4

    open class Base4A(
        val a: Int?
    ) : Base4 {
        override fun equals(other: Any?): Boolean {
            if (javaClass != other?.javaClass) return false
            return a == (other as Base4A).a
        }
        override fun hashCode() = throw NotImplementedError()
    }

    data class Subclass4B(val b: Int) : Base4

    class Subclass4C(
        a: Int,
        val c: Int?
    ) : Base4A(a) {
        override fun equals(other: Any?): Boolean {
            if (javaClass != other?.javaClass) return false
            return c == (other as Subclass4C).c
        }
        override fun hashCode() = throw NotImplementedError()
    }

    data class Subclass4D(
        val d: Int?
    ) : Base4A(null)

    @BeforeClass
    fun specifySubclasses4() {
        serializer.getSerializer(Subclass4C::class.java)
        serializer.getSerializer(Subclass4D::class.java)
        serializer.getSerializer(Base4A::class.java)
        serializer.getSerializer(Subclass4B::class.java)
        serializer.getSerializer(Base4::class.java)
    }

    @Test
    fun testParentChain2() {
        readWriteTest(Subclass4B(4), Base4::class.java)
        readWriteTest(Subclass4C(8,16), Base4::class.java)
        readWriteTest(Subclass4D(null), Base4::class.java)
        readWriteTest(Base4A(32), Base4::class.java)
        readWriteTest(Subclass4C(8,16), Base4A::class.java)
        readWriteTest(Subclass4D(24), Base4A::class.java)
    }


    interface Base7A
    interface Base7B
    data class Subclass7A(val a: Int) : Base7A
    data class Subclass7B(val a: Int) : Base7A, Base7B
    data class Subclass7C(val a: Int) : Base7B

    @BeforeClass
    fun specifySubclasses7() {
        serializer.getSerializer(Subclass7A::class.java)
        serializer.getSerializer(Subclass7B::class.java)
        serializer.getSerializer(Subclass7C::class.java)
    }

    @Test
    fun testMultipleParents() {
        readWriteTest(Subclass7A(4), Base7A::class.java)
        readWriteTest(Subclass7B(4), Base7A::class.java)
        readWriteTest(Subclass7B(4), Base7B::class.java)
        readWriteTest(Subclass7C(4), Base7B::class.java)
    }


    interface Base5

    data class Subclass5A(
        val f: Base5?
    ) : Base5

    @BeforeClass
    fun specifySubclasses5() {
        serializer.getSerializer(Subclass5A::class.java)
    }

    @Test
    fun testOneSubclassSerializerBitCountAndCircularDependency()
    {
        checkCircularDependency(Subclass5A::class.java, true)
        Assert.assertEquals(serializer.getSerializer(Base5::class.java).bitCount, 1)
        readWriteTest(Subclass5A(Subclass5A(null)))
        readWriteTest(Subclass5A(Subclass5A(null)), Base5::class.java)
    }


    data class Container6(
        val base: Base6?
    )

    interface Base6

    data class Subclass6A(
        val f: Int
    ) : Base6

    data class Subclass6B(
        val a: Container6?
    ) : Base6

    @BeforeClass
    fun specifySubclasses6() {
        serializer.getSerializer(Subclass6A::class.java)
        serializer.getSerializer(Subclass6B::class.java)  //todo in such case Subclass6B can't be asked before Subclass6A with current implementation
    }

    @Test
    fun testSubclassesNoCircularDependencyMark()
    {
        checkCircularDependency(Container6::class.java, false)  //circular dependency optimized here
        checkCircularDependency(Subclass6B::class.java, false)
        readWriteTest(Container6(Subclass6B(Container6(Subclass6A(24)))))
    }


    interface Base8

    data class Subclass8(
        val f: List<Base8>?
    ): Base8

    data class Subclass8B(
        val f: Int
    ): Base8

    @BeforeClass
    fun specifySubclasses8() {
        serializer.getSerializer(Subclass8B::class.java)
        serializer.getSerializer(Subclass8::class.java)
    }

    @Test
    fun testSubclassesBaseList() {
        checkCircularDependency(Subclass8::class.java, false)  //circular dependency optimized here
        readWriteTest(Subclass8(listOf( Subclass8(listOf(Subclass8B(16),Subclass8(null))), Subclass8B(12), Subclass8(listOf()) )), Base8::class.java)
    }
}