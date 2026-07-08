package helium314.keyboard.compat

import kotlin.test.Test
import kotlin.test.assertTrue

class AppWorkaroundsTest {
    @Test
    fun treatsJotaPlusAsPasteKeycodeProblemApp() {
        assertTrue(AppWorkarounds.doesntCareAboutKeycodePaste("jp.sblo.pandora.jota.plus"))
    }
}
