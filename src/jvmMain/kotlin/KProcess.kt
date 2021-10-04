package ru.spbstu

import com.zaxxer.nuprocess.NuProcessBuilder
import kotlinx.coroutines.Deferred
import java.io.*
import java.lang.Appendable
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel
import java.nio.channels.WritableByteChannel
import kotlin.experimental.ExperimentalTypeInference

interface KProcessIO<I, O, E>: Deferred<Int> {
    val input: I
    val output: O
    val errors: E
}


class KProcessBuilder<I, O, E> {
    val commandLine: MutableList<String> = mutableListOf()
    val env: MutableMap<String, String> = mutableMapOf()
    var cwd: File = File(".")

    private var inputObject: I? = null
    private var outputObject: O? = null
    private var errorObject: E? = null

    private var realInput: InputHandler = InputHandler.None
    private var realOutput: OutputHandler = OutputHandler.None
    private var realError: OutputHandler = OutputHandler.None

    internal fun setInput(i: I, ih: InputHandler) {
        inputObject = i
        realInput = ih
    }

    internal fun setOutput(o: O, oh: OutputHandler) {
        outputObject = o
        realOutput = oh
    }
    internal fun setError(e: E, eh: OutputHandler) {
        errorObject = e
        realError = eh
    }

    fun execute(): KProcessIO<I, O, E> {
        val handler = ProcessHandler(realOutput, realError, realInput)
        NuProcessBuilder(handler, commandLine).apply {
            setCwd(cwd.toPath())
            environment() += env
        }.start()
        return object : KProcessIO<I, O, E>, Deferred<Int> by handler.exitCode {
            override val input: I
                get() = inputObject ?: (null as I)
            override val output: O
                get() = outputObject ?: (null as O)
            override val errors: E
                get() = errorObject ?: (null as E)
        }
    }
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <O, E> KProcessBuilder<InputHandler.None, O, E>.input(ip: Nothing?) = input(InputHandler.None)

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <O, E> KProcessBuilder<InputHandler.None, O, E>.input(ip: InputHandler.None) {
    setInput(ip, ip)
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <O, E> KProcessBuilder<Pipe, O, E>.input(ip: Pipe) {
    setInput(ip, ip)
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <O, E> KProcessBuilder<InputHandler.Interactive, O, E>.input(ip: InputHandler.Interactive) {
    setInput(ip, ip)
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <O, E> KProcessBuilder<ReadableByteChannel, O, E>.input(ip: ReadableByteChannel) {
    setInput(ip, InputHandler.of(ip))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <O, E> KProcessBuilder<File, O, E>.input(ip: File) {
    setInput(ip, InputHandler.of(ip))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <O, E, IS: InputStream> KProcessBuilder<IS, O, E>.input(ip: IS) {
    setInput(ip, InputHandler.of(ip))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <O, E, R: Reader> KProcessBuilder<R, O, E>.input(ip: R) {
    setInput(ip, InputHandler.of(ip))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <O, E> KProcessBuilder<String, O, E>.input(ip: String) {
    setInput(ip, InputHandler.of(ip))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, E> KProcessBuilder<I, OutputHandler.None, E>.output(op: Nothing?) = output(OutputHandler.None)

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, E> KProcessBuilder<I, OutputHandler.None, E>.output(op: OutputHandler.None) {
    setOutput(op, op)
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, E> KProcessBuilder<I, Pipe, E>.output(op: Pipe) {
    setOutput(op, op)
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, E> KProcessBuilder<I, WritableByteChannel, E>.output(op: WritableByteChannel) {
    setOutput(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, E, A: Appendable> KProcessBuilder<I, A, E>.output(op: A) {
    setOutput(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, E, OS: OutputStream> KProcessBuilder<I, OS, E>.output(op: OS) {
    setOutput(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, E, PS: PrintStream> KProcessBuilder<I, PS, E>.output(op: PS) {
    setOutput(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, E, W: Writer> KProcessBuilder<I, W, E>.output(op: W) {
    setOutput(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, E> KProcessBuilder<I, File, E>.output(op: File) {
    setOutput(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, O> KProcessBuilder<I, O, OutputHandler.None>.errors(op: Nothing?) = errors(OutputHandler.None)

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, O> KProcessBuilder<I, O, OutputHandler.None>.errors(op: OutputHandler.None) {
    setError(op, op)
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, O> KProcessBuilder<I, O, Pipe>.errors(op: Pipe) {
    setError(op, op)
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, O> KProcessBuilder<I, O, WritableByteChannel>.errors(op: WritableByteChannel) {
    setError(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, O, A: Appendable> KProcessBuilder<I, O, A>.errors(op: A) {
    setError(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, O, OS: OutputStream> KProcessBuilder<I, O, OS>.errors(op: OS) {
    setError(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, O, PS: PrintStream> KProcessBuilder<I, O, PS>.errors(op: PS) {
    setError(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, O, W: Writer> KProcessBuilder<I, O, W>.errors(op: W) {
    setError(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <I, O> KProcessBuilder<I, O, File>.errors(op: File) {
    setError(op, OutputHandler.of(op))
}

@OptIn(ExperimentalTypeInference::class)
fun <I, O, E> execute(@BuilderInference body: KProcessBuilder<I, O, E>.() -> Unit): KProcessIO<I, O, E> =
    KProcessBuilder<I, O, E>().apply(body).execute()
