package helium314.keyboard.event

import kotlin.test.Test
import kotlin.test.assertEquals

class AvroPhoneticEngineTest {
    private val engine = AvroPhoneticEngine()

    @Test
    fun transliteratesCommonWords() {
        assertEquals("আমি", engine.convert("ami"))
        assertEquals("তুমি", engine.convert("tumi"))
        assertEquals("বাংলা", engine.convert("bangla"))
        assertEquals("আমার সোনার বাংলা", engine.convert("amar sonar bangla"))
        assertEquals("স্বাধীন", engine.convert("shadhin"))
        assertEquals("ভাষা", engine.convert("bhasha"))
        assertEquals("কৃষ্ণ", engine.convert("krishno"))
    }
}
