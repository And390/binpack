package and390.binpack

import org.testng.Assert
import org.testng.annotations.Test
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashSet


class CollectionSerializersTest : AbstractSerializerTest()
{
    @Test
    fun testArraySerializerTypes()
    {
        val a1 = serializer.getSerializer(arrayOf<Int>().javaClass)
        val b = serializer.getSerializer(arrayOf<Long>().javaClass)
        val a2 = serializer.getSerializer(arrayOf(1).javaClass)
        val aa1 = serializer.getSerializer(arrayOf<Array<String>>().javaClass)
        val aa2 = serializer.getSerializer(arrayOf(arrayOf("")).javaClass)
        Assert.assertNotEquals(b, a1)
        Assert.assertEquals(a2, a1)
        Assert.assertNotEquals(aa1, a1)
        Assert.assertEquals(aa2, aa1)
    }

    @Test
    fun testArray0() { readWriteTest(arrayOf<Int>()) }
    @Test
    fun testArray1() { readWriteTest(arrayOf(1,2,3)) }
    @Test
    fun testArray2() { readWriteTest(arrayOf(true,false,true,false,true,false,true,false,true)) }
    @Test
    fun testArray3() { readWriteTest( arrayOf(arrayOf(arrayOf(1,2),arrayOf(3,4))) ) }
    @Test
    fun testArray4() {
        data class A(
            val f1: Int,
            val f2: Boolean,
            val f3: String?
        )
        readWriteTest(arrayOf(A(1,true,null),A(2,true,"xxx")))
    }

    @Test
    fun testArray5() {
        data class B(
            val f1: Array<Int>?,
            val f2: Boolean
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as B
                if (f1 != null) {
                    if (other.f1 == null) return false
                    if (!f1.contentEquals(other.f1)) return false
                } else if (other.f1 != null) return false
                if (f2 != other.f2) return false
                return true
            }
            override fun hashCode() = throw NotImplementedError()
        }
        data class A(
            val f3: Boolean,
            val f4: Int,
            val b: Array<B>
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as A
                if (f3 != other.f3) return false
                if (f4 != other.f4) return false
                if (!b.contentEquals(other.b)) return false
                return true
            }
            override fun hashCode() = throw NotImplementedError()
        }
        checkCircularDependency(B::class.java, false)
        readWriteTest(A(true, 8, arrayOf( B(null,true), B(arrayOf(1,2,3),false) )))
    }

    @Test
    fun testArray6() {
        data class A(
            val a: Array<A>
        ) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as A
                if (!a.contentEquals(other.a)) return false
                return true
            }
            override fun hashCode() = throw NotImplementedError()
        }
        checkCircularDependency(A::class.java, false)  //circular dependency optimized
        readWriteTest( A(arrayOf()) )
        readWriteTest( A(arrayOf( A(arrayOf()), A(arrayOf( A(arrayOf()), A(arrayOf()) )) )) )
    }

    @Test
    fun testArray7() {
        data class A(
            val a: Array<A>?,
            val b: A?
        ) {
            override fun hashCode() = throw NotImplementedError()
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as A
                if (a != null) {
                    if (other.a == null) return false
                    if (!a.contentEquals(other.a)) return false
                } else if (other.a != null) return false
                if (b != other.b) return false
                return true
            }

        }
        checkCircularDependency(A::class.java, true)  //additional recursive link prevents optimization
        readWriteTest( A( arrayOf(), A( arrayOf( A(arrayOf(),A(null,null)) ), null ) ) )
    }

    data class A8(
        val b: B8?
    )
    data class B8(
        val f: Array<A8>?
    ) {
        override fun hashCode() = throw NotImplementedError()
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as B8
            if (f != null) {
                if (other.f == null) return false
                if (!f.contentEquals(other.f)) return false
            } else if (other.f != null) return false
            return true
        }
    }
    @Test
    fun testArray8() {
        checkCircularDependency(A8::class.java, false)  //circular dependency optimized
        checkCircularDependency(B8::class.java, false)
        readWriteTest( A8(B8(arrayOf())) )
        readWriteTest( A8(B8(arrayOf( A8(null), A8(B8(arrayOf(A8(null),A8(null)))), A8(B8(null)) ))) )
        readWriteTest( B8(arrayOf( A8(null), A8(null) )) )
    }


    @Test
    fun testCollectionSerializerTypes()
    {
        class A {
            val a1 = listOf<Int>()
            val b = listOf<Long>()
            val a2 = listOf(1)
            val aa1 = listOf<Array<String>>()
            val aa2 = listOf<Array<String>>()
        }

        val a1 = serializer.getSerializer<Any>(A::class.java.declaredFields.find { it.name == "a1" }!!.genericType)
        val a2 = serializer.getSerializer<Any>(A::class.java.declaredFields.find { it.name == "a2" }!!.genericType)
        val b = serializer.getSerializer<Any>(A::class.java.declaredFields.find { it.name == "b" }!!.genericType)
        val aa1 = serializer.getSerializer<Any>(A::class.java.declaredFields.find { it.name == "aa1" }!!.genericType)
        val aa2 = serializer.getSerializer<Any>(A::class.java.declaredFields.find { it.name == "aa2" }!!.genericType)
        Assert.assertNotEquals(b, a1)
        Assert.assertEquals(a2, a1)
        Assert.assertNotEquals(aa1, a1)
        Assert.assertEquals(aa2, aa1)
    }

    @Test
    fun testCollectionClasses()
    {
        data class A (
                val collection: Collection<Int>,
                val mutableCollection: MutableCollection<Int>,
                val list: List<Int>,
                val mutableList: MutableList<Int>,
                val arrayList: ArrayList<Int>,
                val set: Set<Int>,
                val mutableSet: MutableSet<Int>,
                val hashSet: HashSet<Int>,
                val linkedHashSet: LinkedHashSet<Int>,
                val sortedSet: SortedSet<Int>,
                val navigableSet: NavigableSet<Int>,
                val treeSet: TreeSet<Int>
        )
        val a = A(listOf(1,2), mutableListOf(1,2), listOf(1,2), mutableListOf(1,2), arrayListOf(1,2), setOf(1,2), mutableSetOf(1,2), hashSetOf(1,2), linkedSetOf(1,2), sortedSetOf(1,2), sortedSetOf(1,2), sortedSetOf(1,2))
        readWriteTest(a)
    }

    @Test
    fun testUnsupportedCustomCollections()
    {
        class MyCollection1<T> : Collection<T> {
            override val size: Int get() = 0
            override fun contains(element: T): Boolean = false
            override fun containsAll(elements: Collection<T>): Boolean = false
            override fun isEmpty(): Boolean = false
            override fun iterator(): Iterator<T> = Collections.emptyIterator()
            override fun toString() = "MyCollection1"
        }
        data class B (
                val collection: MyCollection1<Int>
        )
        Assert.assertThrows(IllegalArgumentException::class.java) { serializer.write(B(MyCollection1())) }

        class MyCollection2 : Collection<Int> {
            override val size: Int get() = 2
            override fun contains(element: Int): Boolean = false
            override fun containsAll(elements: Collection<Int>): Boolean = false
            override fun isEmpty(): Boolean = false
            override fun iterator(): Iterator<Int> = listOf(1,2).iterator()
            override fun toString() = "MyCollection2"
        }
        data class C (
            val collection: MyCollection2
        )
        Assert.assertThrows(IllegalArgumentException::class.java) { serializer.write(C(MyCollection2())) }

        class CustomArrayList<T> : ArrayList<T>() {
            val additionalField: Int = 5
        }
        data class D (
            val list: CustomArrayList<Int>
        )
        Assert.assertThrows(IllegalArgumentException::class.java) { serializer.write(D(CustomArrayList())) }
    }

    @Test
    fun testNestedLists()
    {
        data class A (
            val f: List<List<Int>>
        )

        readWriteTest(A(listOf()))
        readWriteTest(A(listOf(listOf())))
        readWriteTest(A(listOf(listOf(), listOf())))
        readWriteTest(A(listOf(listOf(1,2), listOf(3,4))))
    }

    @Test
    fun testNestedObjectLists()
    {
        data class A (
            val a: Byte,
            val f: List<A>
        ) {
            constructor(f: List<A>) : this(255.toByte(), f)
        }

        readWriteTest(A(listOf()))
        readWriteTest(A(listOf(A(listOf()),A(listOf()))))
        readWriteTest(A(listOf(A(listOf(A(listOf()),A(listOf()))), A(listOf(A(listOf()))))))
    }

    @Test
    fun testListOfBooleans()
    {
        data class A (
            val f: List<Boolean>
        )

        readWriteTest(A(listOf()))
        readWriteTest(A(listOf(false, true, false)))
        readWriteTest(A(listOf(false, true, false, false, true, false, false, true, false)))
    }


    data class A9(
        val b: B9?
    )
    data class B9(
        val f: List<A9>?
    )
    @Test
    fun testCircularDependencyThroughList() {
        checkCircularDependency(A9::class.java, false)  //circular dependency optimized
        checkCircularDependency(B9::class.java, false)
        readWriteTest( A9(B9(listOf())) )
        readWriteTest( A9(B9(listOf( A9(null), A9(B9(listOf(A9(null),A9(null)))), A9(B9(null)) ))) )
        readWriteTest( B9(listOf( A9(null), A9(null) )) )
    }
}