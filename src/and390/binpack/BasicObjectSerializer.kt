package and390.binpack

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaField


class BasicObjectSerializer<T : Any>(cls: Class<T>, resolver: CircularDependencyResolver) : ClassSerializer<T>
{
    override val bitCount: Int
    private val constructor: Constructor<T>
    private val fieldWriters: Array<FieldWriter>
    val circularDependency: Boolean

    init
    {
        val kclass = cls.kotlin
        val kfields = kclass.memberProperties
        val kconstructor = kclass.primaryConstructor ?: throw IllegalArgumentException("Class have no primary constructor: $kclass")
        val constructor = kconstructor.javaConstructor ?: throw IllegalArgumentException("Class have no java Constructor for primary constructor: $kclass")
        val kparameters = kconstructor.parameters
        if (kparameters.isEmpty() && kfields.isNotEmpty()) throw IllegalArgumentException("Empty constructors are allowed only for classes without properties: $kclass")

        resolver.begin(cls, this)

        val writerList = ArrayList<FieldWriter>(kfields.size)
        for (kpar in kparameters) {
            val kfield = kfields.firstOrNull { it.name == kpar.name } ?: throw IllegalArgumentException("Non-property constructor parameters are not supported, but $kclass has $kpar")
            val field = kfield.javaField ?: continue
            val nullable = kfield.returnType.isMarkedNullable
            if (!field.isAccessible)  field.isAccessible = true
            val writer = FieldWriter.create(field, nullable, resolver)
            writerList.add(writer)
        }

        resolver.end()

        @Suppress("unchecked_cast")
        this.constructor = constructor
        val writers = writerList.toTypedArray()
        this.fieldWriters = writers
        this.circularDependency = writers.any { it.circularDependency }
        this.bitCount = writers.sumBy { it.bitCount }
    }

    override fun bodySize(value: T): Int
    {
        return fieldWriters.sumBy { it.size(value) }
    }

    override fun write(value: T, writer: BinaryWriter) {
        for (fieldWriter in fieldWriters)  fieldWriter.write(value, writer)
    }

    override fun read(reader: BinaryReader): T {
        val pars = arrayOfNulls<Any>(constructor.parameterCount)
        fieldWriters.forEachIndexed  { index, fieldWriter ->
            val value = fieldWriter.read(reader)
            pars[index] = value
        }
        return constructor.newInstance(*pars)
    }

    private class FieldWriter(
            val field: Field,
            val nullable: Boolean,
            val serializer: ClassSerializer<Any>,
            val circularDependency: Boolean,
            val bitCount: Int
    )
    {
        fun size(obj: Any): Int {
            val value = getValue(obj)
            return if (nullable && value == null) 0 else {
                if (value == null)  throw NullPointerException()
                if (circularDependency) serializer.fullSize(value) else serializer.bodySize(value)
            }
        }

        fun write(obj: Any, writer: BinaryWriter)  {
            val value = getValue(obj)
            if (value == null && !nullable)  throw NullPointerException()
            if (nullable)  writer.writeBit(value != null)
            if (value != null)  {
                val needNewWriter = circularDependency
                if (needNewWriter)  writer.withSubWriter(serializer.bitCount) { serializer.write(value, it) }
                else  serializer.write(value, writer)
            }
        }

        fun read(reader: BinaryReader): Any? = if (nullable && !reader.readBit()) null else {
            val needNewReader = circularDependency
            val result = if (needNewReader)  reader.withSubReader(serializer.bitCount) { serializer.read(it) }
                         else  serializer.read(reader)
            result
        }

        fun getValue(obj: Any): Any? = field.get(obj)

        companion object {
            fun create(field: Field, nullable: Boolean, resolver: CircularDependencyResolver)
                    : FieldWriter
            {
                val type = field.genericType
                val serializer = resolver.needSerializer(type)
                val circularDependency = resolver.isCircularDependency()
                val bitCount = (if (nullable) 1 else 0) + (if (circularDependency) 0 else serializer.bitCount)
                return FieldWriter(field, nullable, serializer, circularDependency, bitCount)
            }
        }
    }
}
