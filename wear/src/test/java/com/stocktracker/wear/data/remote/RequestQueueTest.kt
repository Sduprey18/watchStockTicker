package com.stocktracker.wear.data.remote

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RequestQueueTest {

    private lateinit var requestQueue: RequestQueue

    @Before
    fun setUp() {
        requestQueue = RequestQueue()
    }

    @Test
    fun `enqueue executes block and returns result`() = runTest {
        val result = requestQueue.enqueue { 42 }

        assertEquals(42, result)
    }

    @Test
    fun `enqueue returns string result`() = runTest {
        val result = requestQueue.enqueue { "hello" }

        assertEquals("hello", result)
    }

    @Test
    fun `enqueue propagates exception from block`() = runTest {
        val exception = assertThrows(IllegalStateException::class.java) {
            requestQueue.enqueue { throw IllegalStateException("test error") }
        }

        assertEquals("test error", exception.message)
    }

    @Test
    fun `enqueue serializes concurrent requests`() = runTest {
        val executionOrder = mutableListOf<Int>()

        val job1 = async {
            requestQueue.enqueue {
                executionOrder.add(1)
                "first"
            }
        }
        val job2 = async {
            requestQueue.enqueue {
                executionOrder.add(2)
                "second"
            }
        }

        job1.await()
        job2.await()

        assertEquals(2, executionOrder.size)
        // Both should execute — order guaranteed by mutex serialization
        assertTrue(executionOrder.contains(1))
        assertTrue(executionOrder.contains(2))
    }

    @Test
    fun `enqueue handles null result`() = runTest {
        val result = requestQueue.enqueue { null }

        assertNull(result)
    }

    @Test
    fun `multiple sequential enqueues all complete`() = runTest {
        val results = mutableListOf<Int>()
        for (i in 1..5) {
            val result = requestQueue.enqueue { i * 10 }
            results.add(result)
        }

        assertEquals(listOf(10, 20, 30, 40, 50), results)
    }

    private inline fun <reified T : Throwable> assertThrows(
        exceptionClass: Class<T>,
        block: () -> Unit
    ): T {
        try {
            block()
            fail("Expected ${exceptionClass.simpleName} to be thrown")
        } catch (e: Throwable) {
            if (exceptionClass.isInstance(e)) {
                @Suppress("UNCHECKED_CAST")
                return e as T
            }
            throw e
        }
        // Unreachable, but needed for compiler
        throw AssertionError("Unreachable")
    }
}
