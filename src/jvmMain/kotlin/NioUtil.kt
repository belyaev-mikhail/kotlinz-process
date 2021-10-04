package ru.spbstu

import java.nio.Buffer
import java.nio.ByteBuffer

inline var Buffer.position
    get() = position()
    set(value) { position(value) }

inline var Buffer.limit
    get() = limit()
    set(value) { limit(value) }

inline val Buffer.remaining
    get() = remaining()

fun ByteBuffer.utf8ToString(): String = Charsets.UTF_8.decode(this).toString()
fun String.utf8ToBuffer(): ByteBuffer = Charsets.UTF_8.encode(this)

fun ByteBuffer.tryPut(other: ByteBuffer): ByteBuffer {
    if (remaining >= other.remaining) put(other)
    else {
        val allowed = remaining
        put(other.slice().limit(allowed))
        other.position += allowed
    }
    return this
}

fun ByteBuffer.flipForWriting() {
    compact()
    position = limit
    limit = capacity()
}

