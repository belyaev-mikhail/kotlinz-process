package ru.spbstu

import com.zaxxer.nuprocess.NuProcess
import java.io.File
import java.io.OutputStream
import java.io.PrintStream
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.channels.Channel
import java.nio.channels.Channels
import java.nio.channels.WritableByteChannel

fun interface OutputHandler {
    fun onStartOutput(process: NuProcess) {}
    fun onOutput(buffer: ByteBuffer, closed: Boolean)

    object None: OutputHandler {
        override fun onOutput(buffer: ByteBuffer, closed: Boolean) {
            buffer.position = buffer.limit
        }
    }

    companion object {

        fun of(appendable: Appendable): OutputHandler = OutputHandler { buffer, _ ->
            appendable.append(Charsets.UTF_8.decode(buffer))
        }

        fun of(channel: WritableByteChannel, autoClose: Boolean = false): OutputHandler = OutputHandler { buffer, closed ->
            channel.write(buffer)
            if (autoClose && closed) channel.close()
        }

        fun of(stream: OutputStream, autoClose: Boolean = false): OutputHandler = of(Channels.newChannel(stream), autoClose)
        fun of(stream: PrintStream, autoClose: Boolean = false): OutputHandler = of(Channels.newChannel(stream), autoClose)

        fun of(file: File): OutputHandler = of(file.outputStream(), autoClose = true)

        fun of(writer: Writer, autoClose: Boolean = false): OutputHandler = OutputHandler { buffer, closed ->
            val chBuf = Charsets.UTF_8.decode(buffer)
            val arr = if (chBuf.hasArray()) chBuf.array() else CharArray(chBuf.remaining).also { chBuf.get(it) }
            writer.write(arr)
            if (autoClose && closed) {
                writer.close()
            }
        }
    }
}
