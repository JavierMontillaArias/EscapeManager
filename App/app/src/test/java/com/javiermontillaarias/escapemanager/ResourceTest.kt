package com.javiermontillaarias.escapemanager

import com.javiermontillaarias.escapemanager.util.Resource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceTest {

    @Test
    fun `Success holds data`() {
        val resource = Resource.Success("hello")
        assertEquals("hello", resource.data)
    }

    @Test
    fun `Error holds message`() {
        val resource = Resource.Error("something went wrong")
        assertEquals("something went wrong", resource.message)
    }

    @Test
    fun `Loading is singleton`() {
        assertTrue(Resource.Loading === Resource.Loading)
    }

    @Test
    fun `Idle is singleton`() {
        assertTrue(Resource.Idle === Resource.Idle)
    }

    @Test
    fun `Success with null data`() {
        val resource = Resource.Success<String?>(null)
        assertNull(resource.data)
    }
}
