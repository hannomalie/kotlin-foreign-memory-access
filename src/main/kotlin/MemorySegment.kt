import java.nio.ByteBuffer
import java.nio.ByteOrder

inline class Bytes(val value: Int) {
    operator fun plus(bytes: Bytes) = Bytes(value + bytes.value)
}
val Int.bytes
    get() = Bytes(this)

interface MemorySegment: AutoCloseable {
    val buffer: ByteBuffer
    val baseAddress: Bytes
    val size: Bytes
    companion object
}

inline fun <T : AutoCloseable?, R> T.withUse(block: T.() -> R): R = use(block)

class DirectByteBufferMemorySegment(override val size: Bytes): MemorySegment {
    override val buffer = ByteBuffer.allocateDirect(size.value).order(ByteOrder.nativeOrder())
    override val baseAddress = Bytes(0)
    override fun close() { }
}

fun MemorySegment.Companion.allocate(size: Bytes): MemorySegment = DirectByteBufferMemorySegment(size)