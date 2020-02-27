import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MemorySegmentTest {

    private class NestedStruct: Struct() {
        var xyz by Int
    }

    private class TestStruct: Struct() {
        var foo by Int
        var bar by Float
        val nested by NestedStruct()
    }

    @Test
    fun `nested structs have correct sizes, values and offsets`() {
        AllocatedMemorySegment(100.bytes).use { segment ->
            TestStruct().inSegment(segment) {
                assertThat(size).isEqualTo(12.bytes)

                assertThat(foo).isEqualTo(0)
                foo = 10
                assertThat(foo).isEqualTo(10)
                bar = 2f
                assertThat(bar).isEqualTo(2f)

                assertThat(nested.baseAddress).isEqualTo(8.bytes)
                assertThat(nested.segment.baseAddress).isEqualTo(8.bytes)
                assertThat(nested.size).isEqualTo(4.bytes)
                assertThat((nested.internalSegment as NestedMemorySegment).parent == segment)
                assertThat(nested.xyz).isEqualTo(0)
                nested.xyz = 5
                assertThat(nested.xyz).isEqualTo(5)
            }
        }
    }
}