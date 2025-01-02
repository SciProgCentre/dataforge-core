package space.kscience.dataforge.workspace

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import space.kscience.dataforge.data.value
import space.kscience.dataforge.misc.DFExperimental
import java.nio.file.Files

@OptIn(ExperimentalCoroutinesApi::class, DFExperimental::class)
class FileWorkspaceCacheTest {

    @Test
    fun testCaching() = runTest {
        val workspace = Workspace {
            data {
                //statically initialize data
                repeat(5) {
                    value("myData[$it]", it)
                }
            }
            fileCache(Files.createTempDirectory("dataforge-temporary-cache"))

            val echo by task<String> {
                transformEach(dataByType<String>()) { value }
            }
        }

        workspace.produce("echo").launchIn(this)

    }
}