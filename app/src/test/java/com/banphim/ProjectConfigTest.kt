package com.banphim

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectConfigTest {
    @Test
    fun `package and provider constants match spec`() {
        assertEquals("com.banphim.fileprovider", BuildConfig.APPLICATION_ID + ".fileprovider")
    }
}
