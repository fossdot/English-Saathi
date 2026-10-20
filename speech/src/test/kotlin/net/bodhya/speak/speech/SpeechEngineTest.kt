package net.bodhya.speak.speech

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.FileNotFoundException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class SpeechEngineTest {
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "speech-test") }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val scope = CoroutineScope(SupervisorJob())

    @After
    fun shutDown() {
        executor.shutdownNow()
    }

    private fun engine(clock: () -> Long = { 0L }, load: () -> Transcriber) = SpeechEngine(scope, dispatcher, clock, load)

    // In debug mode kotlinx.coroutines appends " @coroutine#N" to thread names.
    private fun threadName() = Thread.currentThread().name.substringBefore(" @")

    private fun awaitState(engine: SpeechEngine): EngineState {
        val deadline = System.currentTimeMillis() + 5_000
        while (engine.state.value == EngineState.Loading && System.currentTimeMillis() < deadline) Thread.sleep(5)
        return engine.state.value
    }

    @Test
    fun `loads once, then recognizes on its own thread`() = runBlocking {
        val threads = mutableListOf<String>()
        val loads = AtomicInteger()
        val times = ArrayDeque(listOf(100L, 350L))
        val e = engine(clock = { times.removeFirst() }) {
            loads.incrementAndGet()
            threads += threadName()
            Transcriber { threads += threadName(); "heard ${it.size}" }
        }
        assertEquals("heard 3", e.transcribe(FloatArray(3)))
        assertEquals("heard 5", e.transcribe(FloatArray(5)))
        assertEquals(EngineState.Ready(loadMs = 250), e.state.value)
        assertEquals(1, loads.get())
        assertEquals(listOf("speech-test", "speech-test", "speech-test"), threads)
    }

    @Test
    fun `recognizing waits for the model to finish loading`() = runBlocking {
        val loading = CountDownLatch(1)
        val e = engine {
            loading.await(5, TimeUnit.SECONDS)
            Transcriber { "ok" }
        }
        val answer = async { e.transcribe(FloatArray(1)) }
        Thread.sleep(50)
        assertEquals(EngineState.Loading, e.state.value)
        assertTrue(answer.isActive)
        loading.countDown()
        assertEquals("ok", answer.await())
    }

    @Test
    fun `a failed load is reported, and recognizing throws its error`() = runBlocking {
        val missing = FileNotFoundException("asr/whisper-tiny.en/tiny.en-tokens.txt")
        val e = engine { throw missing }
        val state = awaitState(e)
        assertTrue(state is EngineState.Failed)
        assertSame(missing, (state as EngineState.Failed).error)
        try {
            e.transcribe(FloatArray(1))
            fail("expected the load error")
        } catch (thrown: FileNotFoundException) {
            assertEquals(missing.message, thrown.message)
        }
    }

    @Test
    fun `a speech library that can't load is reported, not a crash`() = runBlocking {
        val e = engine { throw UnsatisfiedLinkError("dlopen failed: library \"libsherpa-onnx-jni.so\" not found") }
        val state = awaitState(e)
        assertTrue(state is EngineState.Failed && state.error is UnsatisfiedLinkError)
        try {
            e.transcribe(FloatArray(1))
            fail("expected the load error")
        } catch (thrown: UnsatisfiedLinkError) {
        }
    }

    @Test
    fun `recognitions never overlap`() = runBlocking {
        val active = AtomicInteger()
        val mostAtOnce = AtomicInteger()
        val e = engine {
            Transcriber {
                mostAtOnce.accumulateAndGet(active.incrementAndGet(), ::maxOf)
                Thread.sleep(10)
                active.decrementAndGet()
                "ok"
            }
        }
        (1..8).map { async(kotlinx.coroutines.Dispatchers.Default) { e.transcribe(FloatArray(1)) } }.awaitAll()
        assertEquals(1, mostAtOnce.get())
    }
}
