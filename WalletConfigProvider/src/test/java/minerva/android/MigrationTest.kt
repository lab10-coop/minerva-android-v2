package minerva.android

import minerva.android.configProvider.error.IncompatibleModelThrowable
import minerva.android.configProvider.migration.Migration
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

class MigrationTest {

    @Test
    fun `map wallet config when model version is compatible with current version model`() {
        val result = Migration.migrateIfNeeded("{modelVersion:1.0}")
        assertEquals(result.modelVersion, 1.0)
    }

    @Test
    fun `return exception when model version from server or from local storage is incompatible with current version model`() {
        val exception = assertFails {
            Migration.migrateIfNeeded("{modelVersion:0.9}")
        }
        assertTrue(exception is IncompatibleModelThrowable)
    }
}