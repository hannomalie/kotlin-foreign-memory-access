import java.nio.ByteBuffer
import java.nio.ByteOrder

inline class Bytes(val value: Int) {
    operator fun plus(bytes: Bytes) = Bytes(value + bytes.value)
}
val Int.bytes
    get() = Bytes(this)

interface MemorySegment {
    val baseAddress: Bytes
    val size: Bytes
}
interface MutableMemorySegment: MemorySegment {
    fun move(size: Bytes) {
        val sizeTemp = size
        baseAddress += sizeTemp
    }

    override var baseAddress: Bytes
    override var size: Bytes
    companion object {
        operator fun invoke(baseAddress: Bytes, size: Bytes) = MutableMemorySegmentImpl(baseAddress, size)
    }
}
data class MutableMemorySegmentImpl(override var baseAddress: Bytes, override var size: Bytes): MutableMemorySegment
class NestedMemorySegment(val parent: MemorySegment, internal var offset: Bytes, override var size: Bytes): MutableMemorySegment {
    override var baseAddress: Bytes
        get() = parent.baseAddress + offset
        set(value) { // This is strange...
            offset = value
        }
}

interface AllocatedMemorySegment: AutoCloseable, MemorySegment {
    val buffer: ByteBuffer
    companion object
}

class DirectByteBufferMemorySegment(override val size: Bytes): AllocatedMemorySegment {
    override val buffer = ByteBuffer.allocateDirect(size.value).order(ByteOrder.nativeOrder())
    override val baseAddress = Bytes(0)
    override fun close() { }
}

operator fun AllocatedMemorySegment.Companion.invoke(size: Bytes): AllocatedMemorySegment = DirectByteBufferMemorySegment(size)