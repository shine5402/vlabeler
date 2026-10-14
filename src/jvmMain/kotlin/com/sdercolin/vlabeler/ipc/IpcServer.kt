package com.sdercolin.vlabeler.ipc

import com.sdercolin.vlabeler.env.Log
import com.sdercolin.vlabeler.hasUncaughtError
import com.sdercolin.vlabeler.ipc.request.IpcRequest
import com.sdercolin.vlabeler.ipc.response.IpcResponse
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.zeromq.SocketType
import org.zeromq.ZContext
import org.zeromq.ZMQ
import org.zeromq.ZMQException
import zmq.ZError

/**
 * IPC server implemented with ZeroMQ.
 */
class IpcServer(val coroutineScope: CoroutineScope) {

    private val zContext = ZContext()
    private lateinit var socket: ZMQ.Socket
    private var job: Job? = null

    fun bind() {
        socket = zContext.createSocket(SocketType.REP)
        socket.receiveTimeOut = THROTTLE_PERIOD_MS.toInt()
        try {
            socket.bind("tcp://*:$PORT")
            Log.debug("IpcServer bound to port $PORT")
        } catch (t: Throwable) {
            Log.error("Failed to bind socket:")
            Log.error(t)
        }
    }

    fun startReceive(requestFlow: MutableSharedFlow<IpcRequest>) {
        job?.cancel()
        job = coroutineScope.launch(Dispatchers.IO) {
            // `isActive` refers to this coroutine; do not read `job` here, because this body may start running
            // before the assignment to `job` is visible.
            while (isActive && zContext.isClosed.not()) {
                if (hasUncaughtError) {
                    return@launch
                }
                try {
                    delay(THROTTLE_PERIOD_MS)
                    val message = socket.recvStr() ?: continue
                    Log.info("Received request: $message")
                    val request = jsonForIpc.decodeFromString<IpcRequest>(message)
                    requestFlow.emit(request)
                } catch (t: Throwable) {
                    val zmqErrorCode = (t as? ZMQException)?.errorCode
                    // ETERM: the context is being closed.
                    // EFSM: the REP socket is waiting for `send` to be called for the previously received request,
                    // which is expected while a request is still being handled.
                    if (t !is CancellationException && zmqErrorCode != ZError.ETERM && zmqErrorCode != ZError.EFSM) {
                        Log.error("Failed to receive an IPC request:")
                        Log.error(t)
                    }
                }
            }
        }
    }

    fun send(response: IpcResponse) {
        try {
            socket.send(jsonForIpc.encodeToString(response))
        } catch (t: Throwable) {
            Log.error("Failed to send an IPC response:")
            Log.error(t)
        }
    }

    fun close() {
        runCatching {
            val receiveJob = job
            receiveJob?.cancel()
            runBlocking { receiveJob?.join() }
            // ZMQ waits for every socket to be closed before the context can be destroyed.
            if (::socket.isInitialized) socket.close()
            zContext.destroy()
            Log.debug("IpcServer closed")
        }
    }

    companion object {
        private const val THROTTLE_PERIOD_MS = 200L
        private const val PORT = 32342
    }
}
