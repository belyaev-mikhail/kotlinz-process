package ru.spbstu

import com.zaxxer.nuprocess.NuProcess
import com.zaxxer.nuprocess.NuProcessBuilder
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import java.nio.ByteBuffer

interface NuProcessHandler: com.zaxxer.nuprocess.NuProcessHandler {
    override fun onPreStart(nuProcess: NuProcess) {}

    override fun onStart(nuProcess: NuProcess) {}

    override fun onExit(exitCode: Int) {}

    override fun onStdout(buffer: ByteBuffer, closed: Boolean) {
        buffer.position = buffer.limit
    }

    override fun onStderr(buffer: ByteBuffer, closed: Boolean) {
        buffer.position = buffer.limit
    }

    override fun onStdinReady(buffer: ByteBuffer): Boolean = false
}

class ProcessHandler(
    val outputHandler: OutputHandler = OutputHandler.None,
    val errorHandler: OutputHandler = OutputHandler.None,
    val inputHandler: InputHandler = InputHandler.None
): NuProcessHandler {
    private lateinit var process: KNuProcess
    private val exitCodeDeferred: CompletableDeferred<Int> = CompletableDeferred()
    val exitCode: Deferred<Int> get() = exitCodeDeferred
    private val runnerDeferred: CompletableDeferred<Unit> = CompletableDeferred()

    override fun onStart(nuProcess: NuProcess) {
        inputHandler.onStartInput(process)
        outputHandler.onStartOutput(process)
        errorHandler.onStartOutput(process)
        runnerDeferred.complete(Unit)
    }

    suspend fun awaitStart() = runnerDeferred.await()

    override fun onPreStart(nuProcess: NuProcess) {
        process = KNuProcess(nuProcess)
    }

    override fun onExit(exitCode: Int) {
        exitCodeDeferred.complete(exitCode)
    }

    override fun onStdout(buffer: ByteBuffer, closed: Boolean) = outputHandler.onOutput(buffer, closed)
    override fun onStderr(buffer: ByteBuffer, closed: Boolean) = errorHandler.onOutput(buffer, closed)
    override fun onStdinReady(buffer: ByteBuffer): Boolean {
        val ir = inputHandler.onInput(buffer)
        return when (ir) {
            InputHandler.InputResult.ENOUGH -> false
            InputHandler.InputResult.NEED_MORE -> true
            InputHandler.InputResult.FINISHED -> {
                process.closeStdin(false)
                false
            }
        }
    }
}

suspend fun exec(vararg cmd: String,
                 outputHandler: OutputHandler = OutputHandler.None,
         errorHandler: OutputHandler = OutputHandler.None,
         inputHandler: InputHandler = InputHandler.None): Int =
    execDeferred(
        cmd = cmd,
        outputHandler = outputHandler,
        errorHandler = errorHandler,
        inputHandler = inputHandler
    ).await()

suspend fun execDeferred(vararg cmd: String,
                 outputHandler: OutputHandler = OutputHandler.None,
                 errorHandler: OutputHandler = outputHandler,
                 inputHandler: InputHandler = InputHandler.None): Deferred<Int> {
    val ph = ProcessHandler(outputHandler, errorHandler, inputHandler)
    val builder = NuProcessBuilder(*cmd)
    builder.setProcessListener(ph)
    builder.start()
    ph.awaitStart()
    return ph.exitCode
}
