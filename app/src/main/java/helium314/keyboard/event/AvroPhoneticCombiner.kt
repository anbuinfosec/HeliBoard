// SPDX-License-Identifier: GPL-3.0-only

package helium314.keyboard.event

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.Constants
import java.util.ArrayList

/**
 * A lightweight Avro-style transliteration combiner for Bengali.
 *
 * It keeps a Latin phonetic buffer while the user types and shows the converted Bengali
 * composition inline. The final Bengali word is committed on whitespace, Enter, punctuation,
 * or when the user picks a suggestion.
 */
class AvroPhoneticCombiner : Combiner {
    private val composingText = StringBuilder()

    override fun processEvent(previousEvents: ArrayList<Event>?, event: Event): Event {
        val codePoint = event.codePoint

        if (event.keyCode == KeyCode.SHIFT) return event

        if (event.keyCode == KeyCode.DELETE) {
            if (composingText.isNotEmpty()) {
                val cp = composingText.codePointBefore(composingText.length)
                composingText.delete(composingText.length - Character.charCount(cp), composingText.length)
                if (composingText.isEmpty()) {
                    reset()
                    return Event.createHardwareKeypressEvent(0x20, Constants.CODE_SPACE, 0, event, event.isKeyRepeat)
                }
                return Event.createConsumedEvent(event)
            }
            return event
        }

        val isValidCodePoint = codePoint != Integer.MAX_VALUE && Character.isValidCodePoint(codePoint)
        val isWhitespace = isValidCodePoint && Character.isWhitespace(codePoint)

        if (event.isFunctionalKeyEvent || isWhitespace) {
            return commitAndReset(event)
        }

        if (!isValidCodePoint) return Event.createConsumedEvent(event)

        composingText.append(Character.toChars(codePoint))
        return Event.createConsumedEvent(event)
    }

    override val combiningStateFeedback: CharSequence
        get() = engine.convert(composingText.toString())

    override fun reset() {
        composingText.setLength(0)
    }

    private fun commitAndReset(event: Event): Event {
        val converted = combiningStateFeedback
        reset()
        return Event.createSoftwareTextEvent(converted, KeyCode.MULTIPLE_CODE_POINTS, event)
    }

    companion object {
        private val engine = AvroPhoneticEngine()
    }
}

class AvroPhoneticEngine {
    private val consonantRules = mapOf(
        "kh" to "খ",
        "gh" to "ঘ",
        "ng" to "ঙ",
        "ch" to "চ",
        "jh" to "ঝ",
        "ny" to "ঞ",
        "th" to "থ",
        "dh" to "ধ",
        "ph" to "ফ",
        "bh" to "ভ",
        "sh" to "শ",
        "shh" to "ষ",
        "tth" to "ঠ",
        "ddh" to "ঢ",
        "tt" to "ট",
        "dd" to "ড",
        "nn" to "ণ",
        "rr" to "ড়",
        "rh" to "ঢ়",
        "kk" to "ক",
        "gg" to "গ",
        "cc" to "চ",
        "jj" to "জ",
        "pp" to "প",
        "bb" to "ব",
        "mm" to "ম",
        "yy" to "য",
        "rrh" to "ঢ়",
        "k" to "ক",
        "g" to "গ",
        "c" to "চ",
        "j" to "জ",
        "t" to "ত",
        "d" to "দ",
        "n" to "ন",
        "p" to "প",
        "b" to "ব",
        "m" to "ম",
        "r" to "র",
        "l" to "ল",
        "s" to "স",
        "h" to "হ",
        "y" to "য",
        "w" to "ও",
        "f" to "ফ",
        "v" to "ভ",
        "x" to "ক্স",
        "q" to "ক",
        "z" to "জ",
    )

    private val vowelRules = mapOf(
        "a" to "অ",
        "aa" to "আ",
        "i" to "ই",
        "ii" to "ঈ",
        "u" to "উ",
        "uu" to "উ",
        "e" to "এ",
        "oi" to "ঐ",
        "ou" to "ঔ",
        "o" to "ও",
    )

    private val vowelSignRules = mapOf(
        "a" to "া",
        "aa" to "া",
        "i" to "ি",
        "ii" to "ী",
        "u" to "ু",
        "uu" to "ূ",
        "e" to "ে",
        "oi" to "ৈ",
        "ou" to "ৌ",
        "o" to "ো",
    )

    private val specialCases = mapOf(
        "ami" to "আমি",
        "tumi" to "তুমি",
        "bangla" to "বাংলা",
        "amar" to "আমার",
        "sonar" to "সোনার",
        "shadhin" to "স্বাধীন",
        "bhasha" to "ভাষা",
        "krishno" to "কৃষ্ণ",
        "bangladesh" to "বাংলাদেশ",
    )

    fun convert(input: String): String {
        if (input.isBlank()) return ""

        val output = StringBuilder()
        val currentWord = StringBuilder()

        fun flushWord() {
            if (currentWord.isNotEmpty()) {
                output.append(convertWord(currentWord.toString()))
                currentWord.setLength(0)
            }
        }

        for (char in input) {
            if (char.isLetter()) {
                currentWord.append(char.lowercaseChar())
            } else {
                flushWord()
                output.append(char)
            }
        }
        flushWord()
        return output.toString()
    }

    private fun convertWord(word: String): String {
        val lower = word.lowercase()
        val special = specialCases[lower]
        if (special != null) return special

        val output = StringBuilder()
        var previousWasConsonant = false
        var i = 0
        while (i < lower.length) {
            val matchedConsonant = consonantRules.keys
                .filter { lower.startsWith(it, i) }
                .maxByOrNull { it.length }

            if (matchedConsonant != null) {
                output.append(consonantRules.getValue(matchedConsonant))
                previousWasConsonant = true
                i += matchedConsonant.length
                continue
            }

            val matchedVowel = vowelRules.keys
                .filter { lower.startsWith(it, i) }
                .maxByOrNull { it.length }

            if (matchedVowel != null) {
                output.append(if (previousWasConsonant) vowelSignRules.getValue(matchedVowel) else vowelRules.getValue(matchedVowel))
                previousWasConsonant = false
                i += matchedVowel.length
                continue
            }

            output.append(lower[i])
            previousWasConsonant = false
            i++
        }
        return output.toString()
    }
}
