package space.kscience.dataforge.descriptors

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import space.kscience.dataforge.meta.Scheme
import space.kscience.dataforge.meta.SchemeSpec
import space.kscience.dataforge.meta.descriptors.MetaDescriptor
import space.kscience.dataforge.meta.int
import space.kscience.dataforge.meta.string

private class TestScheme: Scheme(){

    @Description("A")
    val a by string()

    @Description("B")
    val b by int()

    companion object: SchemeSpec<TestScheme>(::TestScheme){
        override val descriptor: MetaDescriptor = autoDescriptor()
    }
}

class TestAutoDescriptors {
    @Test
    fun autoDescriptor(){
        val autoDescriptor = MetaDescriptor.forClass(TestScheme::class)
        println(Json{prettyPrint = true}.encodeToString(autoDescriptor))
    }
}