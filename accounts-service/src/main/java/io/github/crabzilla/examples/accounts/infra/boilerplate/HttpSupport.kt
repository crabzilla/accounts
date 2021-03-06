package io.github.crabzilla.examples.accounts.infra.boilerplate

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Promise
import io.vertx.core.http.HttpServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.ServerSocket

object HttpSupport {

  private val log: Logger = LoggerFactory.getLogger(HttpSupport::class.java)

  @JvmStatic
  @Throws(IOException::class)
  fun findFreePort(ports: List<Int>, exceptPorts: List<Int>): Int {
    for (port: Int in ports) {
      if (exceptPorts.contains(port)) {
        continue
      }
      try {
        val socket = ServerSocket(port)
        val httpPort = socket.localPort
        socket.close()
        return httpPort
      } catch (ex: IOException) {
        continue  // try next port
      }
    }
    throw IOException("no free port found")
  }

  @JvmStatic
  fun findFreeHttpPort(): Int {
    var httpPort = 0
    try {
      val socket = ServerSocket(0)
      httpPort = socket.localPort
      socket.close()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return httpPort
  }

  @JvmStatic
  fun listenHandler(promise: Promise<Void>): Handler<AsyncResult<HttpServer>> {
    return Handler { startedFuture ->
      if (startedFuture.succeeded()) {
        log.info("Server started on port " + startedFuture.result().actualPort())
        promise.complete()
      } else {
        log.error("oops, something went wrong during server initialization", startedFuture.cause())
        promise.fail(startedFuture.cause())
      }
    }
  }


}

