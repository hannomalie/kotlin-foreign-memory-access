import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Struct {
    internal val children = mutableListOf<Delegate<*,*>>()
    internal var allocatedSegment: AllocatedMemorySegment? = null
        set(value) {
            field = value
            children.forEach { if(it.owner != this) it.owner.allocatedSegment = value }
        }
    internal open var internalSegment: MutableMemorySegment = MutableMemorySegment(0.bytes, 0.bytes)
    val segment: MemorySegment
        get() = internalSegment

    internal val baseAddress: Bytes
        get() = internalSegment.baseAddress
    val size: Bytes
        get() = internalSegment.size

    operator fun Int.Companion.provideDelegate(thisRef: Struct, prop: KProperty<*>): ReadWriteProperty<Struct, Int> {
        return IntDelegate(thisRef).apply {
            this@Struct.internalSegment.size += size
            children.add(this)
        }
    }
    operator fun Float.Companion.provideDelegate(thisRef: Struct, prop: KProperty<*>): ReadWriteProperty<Struct, Float> {
        return FloatDelegate(thisRef).apply {
            this@Struct.internalSegment.size += size
            children.add(this)
        }
    }

    operator fun <T: Struct> T.provideDelegate(thisRef: Struct, prop: KProperty<*>): StructDelegate<T> {
        val surroundingStruct = this@Struct
        val nestedStruct = this@provideDelegate

        return StructDelegate(nestedStruct).apply {
            nestedStruct.internalSegment = NestedMemorySegment(surroundingStruct.segment, surroundingStruct.size, nestedStruct.size)
            surroundingStruct.internalSegment.size += nestedStruct.size
            surroundingStruct.children.add(this)
        }
    }
}

fun <T: Struct> T.inSegment(segment: AllocatedMemorySegment, block: T.() -> Unit) {
    allocatedSegment = segment
    try {
        block()
    } finally {
        allocatedSegment = null
    }
}

sealed class Delegate<R: Struct, T>(val owner: Struct): ReadWriteProperty<R, T> {
    val currentOffset = owner.internalSegment.size
}

class StructDelegate<T: Struct>(val theOwner: T) : Delegate<Struct, T>(theOwner) {
    override fun getValue(thisRef: Struct, property: KProperty<*>): T {
        return theOwner
    }

    override fun setValue(thisRef: Struct, property: KProperty<*>, value: T) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class IntDelegate(underlying: Struct) : Delegate<Struct, Int>(underlying) {
    override fun getValue(thisRef: Struct, property: KProperty<*>): Int {
        return thisRef.allocatedSegment!!.buffer.getInt(thisRef.baseAddress.value + currentOffset.value)
    }

    override fun setValue(thisRef: Struct, property: KProperty<*>, value: Int) {
        thisRef.allocatedSegment!!.buffer.putInt(thisRef.baseAddress.value + currentOffset.value, value)
    }

    val size: Bytes
        get() = Bytes(4)
}
class FloatDelegate(underlying: Struct) : Delegate<Struct, Float>(underlying) {
    override fun getValue(thisRef: Struct, property: KProperty<*>): Float {
        return thisRef.allocatedSegment!!.buffer.getFloat(thisRef.baseAddress.value + currentOffset.value)
    }

    override fun setValue(thisRef: Struct, property: KProperty<*>, value: Float) {
        thisRef.allocatedSegment!!.buffer.putFloat(thisRef.baseAddress.value + currentOffset.value, value)
    }
    val size: Bytes
        get() = Bytes(4)
}

class StructArray<T: Struct>(size: Int, val factory: () -> T): Struct(), Iterable<T> {

    override var internalSegment: MutableMemorySegment = MutableMemorySegment(0.bytes, Bytes(factory().size.value * size))

    override fun iterator(): Iterator<T> {
        return object: Iterator<T> {
            var offset = 0.bytes
            val element = factory().apply {
                internalSegment = NestedMemorySegment(internalSegment, 0.bytes, this.size)
            }
            override fun hasNext(): Boolean {
                return offset.value < internalSegment.size.value
            }

            override fun next(): T {
                element.allocatedSegment = allocatedSegment
                element.internalSegment.move(element.size)
                offset += element.size
                return element
            }
        }
    }
}