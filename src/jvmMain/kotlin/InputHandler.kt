package ru.spbstu

import com.zaxxer.nuprocess.NuProcess
import java.io.Closeable
import java.io.File
import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.*
import java.nio.channels.ReadableByteChannel
import java.nio.charset.CodingErrorAction

fun interface InputHandler {
    fun onStartInput(process: NuProcess) {
        process.wantWrite()
    }

    enum class InputResult { NEED_MORE, ENOUGH, FINISHED }

    fun onInput(buffer: ByteBuffer): InputResult

    object None: InputHandler {
        override fun onStartInput(process: NuProcess) {}
        override fun onInput(buffer: ByteBuffer): InputResult = InputResult.FINISHED
    }

    class Interactive: InputHandler, Appendable, Closeable {
        val encoder = Charsets.UTF_8.newEncoder()

        var process: NuProcess? = null
        override fun onStartInput(process: NuProcess) {
            this.process = process
        }
        val buffer: ByteBuffer = ByteBuffer.allocateDirect(NuProcess.BUFFER_CAPACITY)

        override fun onInput(dst: ByteBuffer): InputResult {
            buffer.flip()
            dst.tryPut(buffer)
            dst.flip()
            if (buffer.hasRemaining()) {
                buffer.compact()
                return InputResult.NEED_MORE
            } else {
                return InputResult.ENOUGH
            }
        }

        override fun append(csq: CharSequence): Appendable {
            propose(CharBuffer.wrap(csq))
            return this
        }

        override fun append(csq: CharSequence, start: Int, end: Int): Appendable {
            propose(CharBuffer.wrap(csq, start, end))
            return this
        }

        override fun append(char: Char): Appendable {
            propose(CharBuffer.wrap(charArrayOf(char)))
            return this
        }

        fun propose(value: CharBuffer) {
            encoder.encode(value, buffer, false)
            process?.wantWrite()
        }

        override fun close() {
            process?.closeStdin(false)
        }
    }

    companion object {
        fun of(src: ByteBuffer) = InputHandler { dst ->
            dst.tryPut(src)
            dst.flip()
            if (src.hasRemaining()) InputResult.NEED_MORE
            else InputResult.FINISHED
        }
        fun of(string: String) = of(string.utf8ToBuffer())
        fun of(channel: ReadableByteChannel, eager: Boolean = false) = InputHandler { dst ->
            val readRes = channel.read(dst)
            dst.flip()

            when (readRes) {
                -1 -> InputResult.FINISHED
                else -> if (eager) InputResult.NEED_MORE
                else InputResult.ENOUGH
            }
        }
        fun of(stream: InputStream) = of(Channels.newChannel(stream))

        fun of(reader: Reader) = run {
            val encoder = Charsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPLACE)
                .onUnmappableCharacter(CodingErrorAction.REPLACE)
            val charBuf = CharBuffer.allocate(NuProcess.BUFFER_CAPACITY)
            charBuf.flip()
            val buffer = ByteBuffer.allocate(NuProcess.BUFFER_CAPACITY)
            buffer.flip()
            InputHandler { dst ->
                if (!buffer.hasRemaining()) {
                    if (!charBuf.hasRemaining()) {
                        charBuf.reset()
                        val readRes = reader.read(charBuf)
                        if (readRes < 0) return@InputHandler InputResult.FINISHED
                        charBuf.flip()
                    }
                    buffer.reset()
                    encoder.encode(charBuf, buffer, false)
                    buffer.flip()
                }

                dst.tryPut(buffer)
                if (buffer.hasRemaining() || charBuf.hasRemaining() || reader.ready()) InputResult.NEED_MORE
                else InputResult.ENOUGH
            }
        }

        fun of(ip: File): InputHandler = of(Channels.newChannel(ip.inputStream()), eager = true)
    }

}