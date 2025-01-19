package space.kscience.dataforge.values

import space.kscience.dataforge.meta.DoubleArrayValue
import space.kscience.dataforge.meta.Meta
import space.kscience.dataforge.meta.doubleArray
import space.kscience.dataforge.meta.get
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DoubleArrayValue {
    @Test
    fun doubleArrayWriteRead(){
        val meta = Meta{
            "doubleArray" put doubleArrayOf(1.0,2.0,3.0)
        }

        assertTrue {
            meta["doubleArray"]?.value is DoubleArrayValue
        }

        assertEquals(2.0, meta["doubleArray"].doubleArray?.get(1))
    }
}