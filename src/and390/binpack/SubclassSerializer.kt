package and390.binpack


class SubclassSerializer<T : Any>(private val cls: Class<T>, subclasses: Collection<Class<out T>>, resolver: CircularDependencyResolver) : ClassSerializer<T>
{
    private val baseSerializer: BasicObjectSerializer<T>?
    private val subclassIndices: HashMap<Class<out T>, Int>
    private val subclassSerializers: Array<BasicObjectSerializer<T>>
    private val typeBitCount = 32 - Integer.numberOfLeadingZeros(subclasses.size-1)

    init  {
        if (subclasses.isEmpty())  throw IllegalArgumentException("No subclasses passed for abstract class or interface $cls")
        //SubclassSerializer creates a separate writer/reader, so it doesn't need to resolve parent circular dependencies
        val oldMark = resolver.markNoCircularDependency()
        subclassIndices = subclasses.withIndex().associateTo(HashMap()) { it.value to it.index }
        var baseSerializer: BasicObjectSerializer<T>? = null
        subclassSerializers = subclasses.map { subclass ->
            if (subclass == cls) BasicObjectSerializer(cls, resolver).also { baseSerializer = it }
            else {
                val subSerializer = resolver.needSerializer(subclass)
                @Suppress("unchecked_cast")
                ((if (subSerializer is SubclassSerializer) subSerializer.baseSerializer else subSerializer) as BasicObjectSerializer<T>)
            }
        }.toTypedArray()
        this.baseSerializer = baseSerializer
        resolver.restoreNoCircularDependencyMark(oldMark)
    }

    override val bitCount = typeBitCount

    override fun bodySize(value: T): Int = subclassSerializers[getIndex(value.javaClass)].fullSize(value)

    override fun write(value: T, writer: BinaryWriter) {
        val typeIndex = getIndex(value.javaClass)
        val serializer = subclassSerializers[typeIndex]
        writer.writeBits(typeIndex, typeBitCount)
        writer.withSubWriter(serializer.bitCount) { subWriter ->
            serializer.write(value, subWriter)
        }
    }

    override fun read(reader: BinaryReader): T {
        val type = reader.readBits(typeBitCount)
        val serializer = subclassSerializers[type]
        return reader.withSubReader(serializer.bitCount) { subReader ->
            serializer.read(subReader)
        }
    }

    private fun getIndex(subclass: Class<*>) = subclassIndices[subclass] ?: throw IllegalArgumentException(
            "Unknown subclass $subclass of $cls, you should specify all used subclasses before using base class serializer")
}

class OneSubclassSerializer<T : Any>(private val cls: Class<T>, private val subclass: Class<out T>, resolver: CircularDependencyResolver) : ClassSerializer<T>
{
    private val serializer = @Suppress("unchecked_cast") (resolver.needSerializer(subclass) as BasicObjectSerializer<T>)

    override val bitCount get() = serializer.bitCount  //serializer could be not initialized right now (in this constructor), so we must read the value later (on demand in this implementation)
    override fun bodySize(value: T): Int = serializer.bodySize(checkValue(value))
    override fun write(value: T, writer: BinaryWriter) = serializer.write(checkValue(value), writer)
    override fun read(reader: BinaryReader): T = serializer.read(reader)

    private fun checkValue(value: T) = value.also { if (value.javaClass != subclass) throw IllegalArgumentException(
                "Unknown subclass $subclass of $cls, you should specify all subclasses before using base class serializer") }
}