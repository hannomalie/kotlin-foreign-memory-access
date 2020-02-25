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
    override var baseAddress: Bytes
    override var size: Bytes
    companion object {
        operator fun invoke(baseAddress: Bytes, size: Bytes) = MutableMemorySegmentImpl(baseAddress, size)
    }
}
data class MutableMemorySegmentImpl(override var baseAddress: Bytes, override var size: Bytes): MutableMemorySegment
class NestedMemorySegment(val parent: MemorySegment, val offset: Bytes, override var size: Bytes): MutableMemorySegment {
    override var baseAddress: Bytes
        get() = parent.baseAddress + offset
        set(value) = TODO()
}

interface AllocatedMemorySegment: AutoCloseable, MemorySegment {
    val buffer: ByteBuffer
    companion object
}

inline fun <T : AutoCloseable?, R> T.withUse(block: T.() -> R): R = use(block)

class DirectByteBufferMemorySegment(override val size: Bytes): AllocatedMemorySegment {
    override val buffer = ByteBuffer.allocateDirect(size.value).order(ByteOrder.nativeOrder())
    override val baseAddress = Bytes(0)
    override fun close() { }
}

fun AllocatedMemorySegment.Companion.allocate(size: Bytes): AllocatedMemorySegment = DirectByteBufferMemorySegment(size)