import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ArrayTest {

    private class NestedStruct: Struct() {
        var xyz by Int
    }

    private class TestStruct: Struct() {
        var foo by Int
        var bar by Float
        val nested by NestedStruct()
    }


    @Test
    fun arrayWorks() {
        val array = StructArray(12) { TestStruct() }
        AllocatedMemorySegment(1000.bytes).use { segment ->
            array.inSegment(segment) {
                array.forEachIndexed { index, element ->
                    element.foo = index
                    element.nested.xyz = index
                }
                array.forEachIndexed { index, element ->
                    assertThat(element.foo).isEqualTo(index)
                    assertThat(element.nested.xyz).isEqualTo(index)
                }
            }
        }
    }
}