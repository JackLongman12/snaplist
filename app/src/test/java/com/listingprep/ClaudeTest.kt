package com.listingprep

import com.listingprep.ml.Claude
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Builds the Claude request offline (no network, no key) to catch mistakes before they ship. */
class ClaudeTest {
    @Test
    fun requestHasPhotosThenPromptAndAtMostFourPhotos() {
        val params = Claude.params(List(6) { "AAAA" }, "Write the listing")
        assertEquals(Claude.MODEL, params.model().asString())
        val content = params.messages().single().content().asContentBlockParams()
        assertEquals(Claude.MAX_PHOTOS + 1, content.size)
        assertTrue(content.dropLast(1).all { it.isImage() })
        assertEquals("Write the listing", content.last().asText().text())
    }
}
