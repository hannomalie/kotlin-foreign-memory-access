import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class Struct {
    var baseAddress = Bytes(0)
        internal set
    var currentOffset = Bytes(0)
        internal set
    val size: Bytes
        get() = currentOffset

    operator fun Int.Companion.provideDelegate(thisRef: Struct, prop: KProperty<*>): ReadWriteProperty<MemorySegment, Int> {
        return IntDelegate(this@Struct).apply {
            this@Struct.currentOffset += size
        }
    }
    operator fun Float.Companion.provideDelegate(thisRef: Struct, prop: KProperty<*>): ReadWriteProperty<MemorySegment, Float> {
        return FloatDelegate(currentOffset).apply {
            this@Struct.currentOffset += size
        }
    }

    operator fun <T: Struct> T.provideDelegate(thisRef: Struct, prop: KProperty<*>): ReadOnlyProperty<MemorySegment, T> {
        val nestedStruct = this@provideDelegate
        val surroundingStruct = this@Struct

        return StructDelegate(nestedStruct).apply {
            nestedStruct.baseAddress = surroundingStruct.currentOffset
            surroundingStruct.currentOffset += nestedStruct.size
        }
    }
}

class StructDelegate<T: Struct>(var underlying: T) : ReadOnlyProperty<MemorySegment, T> {
    override fun getValue(thisRef: MemorySegment, property: KProperty<*>): T {
        return underlying
    }
}
class IntDelegate(val parent: Struct) : ReadWriteProperty<MemorySegment, Int> {
    private val offset = parent.currentOffset
    override fun getValue(thisRef: MemorySegment, property: KProperty<*>): Int {
        return thisRef.buffer.getInt(calculateResultAddress().value)
    }

    override fun setValue(thisRef: MemorySegment, property: KProperty<*>, value: Int) {
        thisRef.buffer.putInt(calculateResultAddress().value, value)
    }

    private fun calculateResultAddress() = (parent.baseAddress + offset)
    val size: Bytes
        get() = Bytes(4)
}
inline class FloatDelegate(val offset: Bytes) : ReadWriteProperty<MemorySegment, Float> {
    override fun getValue(thisRef: MemorySegment, property: KProperty<*>): Float {
        return thisRef.buffer.getFloat(offset.value)
    }

    override fun setValue(thisRef: MemorySegment, property: KProperty<*>, value: Float) {
        thisRef.buffer.putFloat(offset.value, value)
    }
    val size: Bytes
        get() = Bytes(4)
}