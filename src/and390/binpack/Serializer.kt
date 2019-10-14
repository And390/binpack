package and390.binpack

import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


// Not thread safe - use ThreadLocal in multithreaded environment
class Serializer
{
    fun write(obj: Any): ByteArray = getSerializer(obj.javaClass).write(obj)
    fun write(obj: Any, type: Type): ByteArray = getSerializer<Any>(type).write(obj)
    fun <T : Any> write(obj: T, cls: Class<T>): ByteArray = getSerializer(cls).write(obj)

    fun write(obj: Any, buffer: ByteArray, offset: Int): Int = getSerializer(obj.javaClass).write(obj, buffer, offset)
    fun write(obj: Any, type: Type, buffer: ByteArray, offset: Int): Int = getSerializer<Any>(type).write(obj, buffer, offset)
    fun <T : Any> write(obj: T, cls: Class<T>, buffer: ByteArray, offset: Int): Int = getSerializer(cls).write(obj, buffer, offset)

    inline fun <reified T : Any> read(buffer: ByteArray, offset: Int = 0): T = read(T::class.java, buffer, offset)
    fun <T : Any> read(type: Type, buffer: ByteArray, offset: Int = 0): T = getSerializer<T>(type).read(buffer, offset)
    fun <T : Any> read(type: Type, buffer: ByteArray, offset: Int, lenght: Int): T = getSerializer<T>(type).read(buffer, offset, lenght)
    fun <T : Any> read(cls: Class<T>, buffer: ByteArray, offset: Int = 0): T = getSerializer(cls).read(buffer, offset)
    fun <T : Any> read(cls: Class<T>, buffer: ByteArray, offset: Int, lenght: Int): T = getSerializer(cls).read(buffer, offset, lenght)

    fun size(value: Any, type: Type) = getSerializer<Any>(type).fullSize(value)
    fun <T : Any> size(value: T, cls: Class<out T> = value::class.java) = getSerializer<T>(cls).fullSize(value)

    fun <T : Any> getSerializer(type: Type) = getSerializer<T>(type, null)

    fun <T : Any> getSerializer(cls: Class<T>): ClassSerializer<T> {
        @Suppress("unchecked_cast")
        return cache.getOrPut(cls) { create(cls, null) } as ClassSerializer<T>
    }

    private fun <T : Any> getSerializer(type: Type, resolver: CircularDependencyResolver?): ClassSerializer<T> {
        @Suppress("unchecked_cast")
        return cache.getOrPut(type) { create(type, resolver) } as ClassSerializer<T>
    }

    private fun <T : Any> create(type: Type, resolverOrNull: CircularDependencyResolver?): ClassSerializer<T> {
        val cls = @Suppress("unchecked_cast") when (type) {
            is Class<*> -> type as Class<T>
            is ParameterizedType -> type.rawType as Class<T>
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }

        val resolver = resolverOrNull ?: CircularDependencyResolver { t,r -> getSerializer(t, r) }

        if (cls.isArray) {
            @Suppress("unchecked_cast")
            return ArraySerializer(cls.componentType, resolver) as ClassSerializer<T>
        }
        else if (Collection::class.java.isAssignableFrom(cls))  {
            if (!CollectionSerializer.isSupported(cls))  throw IllegalArgumentException("This collection type is not supported: $cls")
            if (type !is ParameterizedType) throw IllegalArgumentException("You must provide a ParameterizedType instance for Collection class (${type.typeName})")
            @Suppress("unchecked_cast")
            return CollectionSerializer(cls as Class<Collection<Any>>, type.actualTypeArguments[0], resolver) as ClassSerializer<T>
        }

        addSubclass(cls)
        val subclasses = buildSubclasses(cls)
        return  if (subclasses != null)
                    if (subclasses.size == 1)  OneSubclassSerializer(cls, subclasses.first(), resolver)
                    else  SubclassSerializer(cls, subclasses, resolver)
                else if (!isAbstract(cls)) BasicObjectSerializer(cls, resolver)
                else throw IllegalArgumentException("$cls is abstract or interface and no one subclass is specified, so it is impossible to read or write this class")
    }

    private val cache = HashMap<Type, ClassSerializer<out Any>>()
    init  {
        LongSerializer().let { cache[Long::class.java] = it; cache[java.lang.Long::class.java] = it }
        IntSerializer().let { cache[Int::class.java] = it; cache[java.lang.Integer::class.java] = it }
        ShortSerializer().let { cache[Short::class.java] = it; cache[java.lang.Short::class.java] = it }
        ByteSerializer().let { cache[Byte::class.java] = it; cache[java.lang.Byte::class.java] = it }
        DoubleSerializer().let { cache[Double::class.java] = it; cache[java.lang.Double::class.java] = it }
        FloatSerializer().let { cache[Float::class.java] = it; cache[java.lang.Float::class.java] = it }
        BooleanSerializer().let { cache[Boolean::class.java] = it; cache[java.lang.Boolean::class.java] = it }
        cache[String::class.java] = StringSerializer()
    }

    private class SubclassesInfo(
        val children: ArrayList<Class<*>> = ArrayList(),
        var built: Boolean = false
    )
    private val childrenMap = HashMap<Class<*>, SubclassesInfo>()  //parent -> (children,builtOrNot)

    private fun addSubclass(cls: Class<*>) {
        fun addParent(parent: Class<*>) = childrenMap.computeIfAbsent(parent){ SubclassesInfo() }.let {
            if (it.built) throw Exception("A list of subclasses of $parent already built, you should specify all used subclasses before using base class serializer")
            it.children.add(cls)
        }
        cls.superclass?.let { addParent(it) }
        for (parent in cls.interfaces)  addParent(parent)
    }

    private fun <T> buildSubclasses(cls: Class<T>): List<Class<out T>>?
    {
        val childrenEntry = childrenMap[cls] ?: return null
        val subclasses = LinkedHashSet<Class<out T>>()
        subclasses.add(cls)
        fun addChildClass(childClass: Class<*>)  {
            if (subclasses.add(@Suppress("unchecked_cast") (childClass as Class<out T>)))
                childrenMap[childClass]?.let { entry ->
                    entry.built = true
                    entry.children.forEach { addChildClass(it) }
                }
        }
        childrenEntry.built = true
        childrenEntry.children.forEach { addChildClass(it) }

        return subclasses.filter { !isAbstract(it) }
    }

    private fun isAbstract(cls: Class<*>) = cls.isInterface || Modifier.isAbstract(cls.modifiers)
}

class CircularDependencyResolver(
    val serializerGetter: (Type, CircularDependencyResolver) -> ClassSerializer<Any>
) {
    private val initStack = ArrayList<StackItem>()
    private var noCircularDependencyMark = 0

    fun begin(cls: Class<*>, serializer: BasicObjectSerializer<*>) {
        initStack.add(StackItem(cls, serializer))
    }

    fun end() {
        initStack.removeAt(initStack.lastIndex)
    }

    fun markNoCircularDependency(): Int {
        val old = noCircularDependencyMark
        noCircularDependencyMark = initStack.size
        return old
    }

    fun restoreNoCircularDependencyMark(value: Int) {
        noCircularDependencyMark = value
    }

    fun needSerializer(type: Type): ClassSerializer<Any> {
        val i = initStack.indexOfFirst { it.type == type }
        return if (i != -1)  {
            if (i >= noCircularDependencyMark)  (i .. initStack.lastIndex).forEach { initStack[it].circularDependency = true }
            @Suppress("unchecked_cast")  (initStack[i].serializer as ClassSerializer<Any>)
        }
        else  {
            serializerGetter(type, this)  //can change initStack
        }
    }

    fun isCircularDependency() = initStack.last().circularDependency

    class StackItem(
        val type: Type,
        val serializer: BasicObjectSerializer<out Any>,
        var circularDependency: Boolean = false
    )
}


interface ClassSerializer<T>
{
    val bitCount: Int
    fun bodySize(value: T): Int
    fun write(value: T, writer: BinaryWriter)
    fun read(reader: BinaryReader): T

    fun fullSize(value: T) = bodySize(value) + (bitCount + 7) / 8

    fun write(value: T): ByteArray {
        val size = fullSize(value)
        val buffer = ByteArray(size)
        write(value, buffer, 0)
        return buffer
    }

    fun write(value: T, buffer: ByteArray, offset: Int): Int {
        val writer = BinaryWriter(buffer, offset, bitCount)
        write(value, writer)
        writer.finish()
        return writer.offset
    }

    fun read(buffer: ByteArray, offset: Int): T {
        val reader = BinaryReader(buffer, offset, bitCount)
        return read(reader)
    }

    fun read(buffer: ByteArray, offset: Int, len: Int): T {
        val reader = BinaryReader(buffer, offset, bitCount)
        val result = read(reader)
        if (reader.offset != offset + len)  throw ArrayIndexOutOfBoundsException("${reader.offset} must be = ${offset + len} (message data range is [$offset..${offset+len}) )")
        return result
    }
}