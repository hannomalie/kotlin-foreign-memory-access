import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MemorySegmentTest {

    private class NestedStruct: Struct() {
        var MemorySegment.xyz by Int
    }

    private class TestStruct: Struct() {
        var MemorySegment.foo by Int
        var MemorySegment.bar by Float
        val MemorySegment.nested by NestedStruct()
    }

    @Test
    fun `nested structs have correct sizes, values and offsets`() {
        val segment = MemorySegment.allocate(100.bytes)
        segment.withUse {
            TestStruct().run {
                assertThat(size).isEqualTo(12.bytes)

                assertThat(foo).isEqualTo(0)
                foo = 10
                assertThat(foo).isEqualTo(10)
                bar = 2f
                assertThat(bar).isEqualTo(2f)

                assertThat(nested.baseAddress).isEqualTo(8.bytes)
                assertThat(nested.currentOffset).isEqualTo(4.bytes)
                with(nested) {
                    assertThat(xyz).isEqualTo(0)
                    xyz = 5
                    assertThat(xyz).isEqualTo(5)
                }
            }
        }
    }
}