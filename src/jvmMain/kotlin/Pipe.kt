package ru.spbstu

import com.zaxxer.nuprocess.NuProcess
import java.nio.BufferOverflowException
import java.nio.ByteBuffer

class Pipe: InputHandler, OutputHandler {
    private var inputProcess: NuProcess? = null
    private var outputClosed: Boolean = false
    private var buffer = ByteBuffer.allocateDirect(NuProcess.BUFFER_CAPACITY).flip()

    @Synchronized
    override fun onStartInput(process: NuProcess) {
        inputProcess = process
        if (buffer.hasRemaining()) inputProcess?.wantWrite()
    }

    @Synchronized
    override fun onOutput(src: ByteBuffer, closed: Boolean) {
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
        outputClosed = closed
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