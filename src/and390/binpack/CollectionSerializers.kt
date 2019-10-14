package and390.binpack

import java.lang.reflect.Type
import java.util.*
import kotlin.ConcurrentModificationException
import kotlin.collections.AbstractCollection
import kotlin.collections.AbstractList
import kotlin.collections.AbstractSet
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.collections.LinkedHashMap
import kotlin.collections.LinkedHashSet


class ArraySerializer(private val itemClass: Class<out Any>, resolver: CircularDependencyResolver) : ClassSerializer<Array<out Any>>
{
    private val itemSerializer : ClassSerializer<Any>
    init {
        val oldMark = resolver.markNoCircularDependency()
        itemSerializer = resolver.needSerializer(itemClass)
        resolver.restoreNoCircularDependencyMark(oldMark)
    }

    override val bitCount = 0

    override fun bodySize(value: Array<out Any>): Int {
        val bits = itemSerializer.bitCount * value.size
        val bytes = value.sumBy {  itemSerializer.bodySize(it) }
        return varIntLength(value.size) + bytes + (bits + 7) / 8
    }
    override fun write(value: Array<out Any>, writer: BinaryWriter)
    {
        writer.writeVarInt(value.size)
        if (value.isNotEmpty())  writer.withSubWriter(itemSerializer.bitCount * value.size) { subWriter ->
            for (item in value)  itemSerializer.write(item, subWriter)
        }
    }
    override fun read(reader: BinaryReader): Array<out Any>
    {
        val count = reader.readVarInt()
        @Suppress("unchecked_cast")  val result = java.lang.reflect.Array.newInstance(itemClass, count) as Array<Any>
        if (count > 0)  reader.withSubReader(itemSerializer.bitCount * count) { subReader ->
            for (i in 0 until count)  result[i] = itemSerializer.read(subReader)
        }
        return result
    }
}

class CollectionSerializer(cls: Class<Collection<Any>>, itemClass: Type, resolver: CircularDependencyResolver) : ClassSerializer<Collection<Any>>
{
    private val factory: Factory = factories[cls] ?: throw IllegalArgumentException("This collection type is not supported: $cls")
    //private val factoryIndex = factoryIndices[factory]!!

    private val itemSerializer : ClassSerializer<Any>
    init {
        val oldMark = resolver.markNoCircularDependency()
        itemSerializer = resolver.needSerializer(itemClass)
        resolver.restoreNoCircularDependencyMark(oldMark)
    }

    companion object {
        private val factories = LinkedHashMap<Class<out Any>, Factory>()
        private val factoryIndices = HashMap<Factory, Int>()
        init {
            factories[Collection::class.java] = ListFactory
            factories[AbstractCollection::class.java] = ListFactory
            factories[List::class.java] = ListFactory
            factories[AbstractList::class.java] = ListFactory
            factories[ArrayList::class.java] = ListFactory
            factories[LinkedList::class.java] = LinkedListFactory
            factories[Set::class.java] = SetFactory
            factories[AbstractSet::class.java] = SetFactory
            factories[HashSet::class.java] = SetFactory
            factories[LinkedHashSet::class.java] = LinkedSetFactory
            factories[SortedSet::class.java] = TreeSetFactory
            factories[NavigableSet::class.java] = TreeSetFactory
            factories[TreeSet::class.java] = TreeSetFactory

            factories.values.distinct().forEachIndexed { i, factory -> factoryIndices[factory] = i }
        }
        fun isSupported(cls: Class<out Any>) = factories.containsKey(cls)
    }

    override val bitCount = 0

    override fun bodySize(value: Collection<Any>): Int {
        val count = value.size
        val bits = itemSerializer.bitCount * count
        val bytes = value.sumBy {  itemSerializer.bodySize(it) }
        return varIntLength(count) + bytes + (bits + 7) / 8
    }
    override fun write(value: Collection<Any>, writer: BinaryWriter)
    {
        var count = value.size
        writer.writeVarInt(count)
        if (value.isNotEmpty())  writer.withSubWriter(itemSerializer.bitCount * count) { subWriter ->
            for (item in value)  {  count--;  itemSerializer.write(item, subWriter)  }
        }
        if (count != 0)  throw ConcurrentModificationException("Collection size was changed during iteration")
    }
    override fun read(reader: BinaryReader): Collection<Any>
    {
        val count = reader.readVarInt()
        val result = factory.create()
        if (count > 0)  reader.withSubReader(itemSerializer.bitCount * count) { subReader ->
            for (i in 0 until count)  result.add(itemSerializer.read(subReader))
        }
        return result
    }

    interface Factory {
        fun create(): MutableCollection<Any>
    }
    object ListFactory : Factory { override fun create() = ArrayList<Any>()  }
    object LinkedListFactory : Factory { override fun create() = LinkedList<Any>()  }
    object SetFactory : Factory { override fun create() = HashSet<Any>()  }
    object LinkedSetFactory : Factory { override fun create() = LinkedHashSet<Any>()  }
    object TreeSetFactory : Factory { override fun create() = TreeSet<Any>()  }
}
