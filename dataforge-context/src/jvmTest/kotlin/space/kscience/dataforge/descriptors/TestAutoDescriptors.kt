@file:OptIn(DFExperimental::class)

package space.kscience.dataforge.descriptors

import space.kscience.dataforge.misc.DFExperimental

//
//class TestScheme : Scheme() {
//
//    @Description("A")
//    val a by string()
//
//    @Description("B")
//    val b by int()
//
//    val c by int()
//
//    companion object : SchemeSpec<TestScheme>(::TestScheme) {
//        override val descriptor: MetaDescriptor = autoDescriptor()
//    }
//}
//
//class TestAutoDescriptors {
//    @Test
//    fun autoDescriptor() {
//        val autoDescriptor = MetaDescriptor.forScheme(TestScheme)
//        println(Json { prettyPrint = true }.encodeToString(autoDescriptor))
//    }
//}