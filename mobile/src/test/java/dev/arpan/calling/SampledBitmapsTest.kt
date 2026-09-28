package dev.arpan.calling

import org.junit.Assert.assertEquals
import org.junit.Test

class SampledBitmapsTest {
    @Test
    fun fitsInsideMaxSide() {
        assertEquals(4, inSampleSizeToFit(4000, 3000, 1024, 1024))
        assertEquals(1, inSampleSizeToFit(800, 600, 1024, 1024))
    }

    @Test
    fun keepsDecodedSizeAtLeastTheRequest() {
        assertEquals(2, inSampleSizeFor(1024, 1024, 360, 360))
        assertEquals(1, inSampleSizeFor(400, 400, 360, 360))
    }
}
