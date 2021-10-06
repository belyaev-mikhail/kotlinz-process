package ru.spbstu

import com.zaxxer.nuprocess.NuProcess
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

abstract class OutputMultiplexer {
    private inner class OHandler: OutputHandler {
        var isClosed: Boolean = false
        override fun onOutput(src: ByteBuffer, closed: Boolean) {
            if (closed) isClosed = true
            this@OutputMultiplexer.onOutput(src)
        }
    }
    private val outputHandlers: MutableList<OHandler> = mutableListOf()
    fun newOutput(): OutputHandler = OHandler().also { outputHandlers += it }

    val outputClosed: Boolean
        @Synchronized
        get() = outputHandlers.all { it.isClosed }

    abstract fun onOutput(src: ByteBuffer)
}

class Pipe: InputHandler, OutputMultiplexer() {
    private var inputProcess: KProcess? = null
    private var buffer = ByteBuffer.allocateDirect(NuProcess.BUFFER_CAPACITY).flip()

    @Synchronized
    override fun onStartInput(process: KProcess) {
        inputProcess = process
        if (buffer.hasRemaining()) inputProcess?.wantWrite()
    }

    @Synchronized
    override fun onOutput(src: ByteBuffer) {
        buffer.compact()
        while (true) {
            try {
                buffer.put(src)
                break
            } catch (ex: BufferOverflowException) {
                val newBuffer = ByteBuffer.allocateDirect(buffer.capacity() * 2)
                buffer.flip()
                newBuffer.put(buffer)
                buffer = newBuffer
            }
        }
        buffer.flip()
        inputProcess?.wantWrite()
    }

    @Synchronized
    override fun onInput(dst: ByteBuffer): InputHandler.InputResult {
        dst.tryPut(buffer)
        dst.flip()
        return when {
            buffer.hasRemaining() -> InputHandler.InputResult.NEED_MORE
            outputClosed -> {
                InputHandler.InputResult.FINISHED
            }
            else -> InputHandler.InputResult.ENOUGH
        }
    }


}

class Tee: OutputMultiplexer() {
    private val pipes: MutableList<OutputHandler> = mutableListOf()

    fun pipe(): InputHandler {
        val pipe = Pipe()
        pipes.add(pipe.newOutput())
        return pipe
    }

    fun addOutputHandler(oh: OutputHandler): OutputHandler {
        pipes.add(oh)
        return oh
    }

    @Synchronized
    override fun onOutput(src: ByteBuffer) {
        for (pipe in pipes) {
            pipe.onOutput(src.duplicate(), outputClosed)
        }
    }
}
