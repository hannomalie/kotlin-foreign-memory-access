A future Java version will get something [called foreign memory access](https://openjdk.java.net/jeps/370)
which shares some similarities with my [kotlin structs library](https://github.com/hannespernpeintner/kotlin-structs).
Different from how I modeled memory segments in my structs library, I wanted to give it another try,
this time more inspired by the JEP-370 approach, making segments and sub segments very explicit.
What Kotlin can do better than Java is abstract away those offset calculations, because properties are
a first class language construct and there is this feature called property delegation.
Better than words can an example show how struct objects can be used quite similar to regular classes,
while they act like a sliding window over a memory segment:

```kotlin

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
```

and similar to that, also array access and iteration works, based on a sliding window approach.

```kotlin
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
```

Feel free to take a look at my struct libary to read about advantages and disadvantages of this approach.