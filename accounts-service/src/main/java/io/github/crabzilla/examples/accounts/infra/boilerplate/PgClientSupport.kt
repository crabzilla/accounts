package io.github.crabzilla.examples.accounts.infra.boilerplate

import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.pgclient.PgConnectOptions
import io.vertx.pgclient.PgPool
import io.vertx.sqlclient.PoolOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object PgClientSupport {

  private val log: Logger = LoggerFactory.getLogger(PgClientSupport::class.java)

  @JvmStatic
  fun writeModelPgPool(vertx: Vertx, config: JsonObject): PgPool {
    return pgPool(vertx, config, "WRITE")
  }

  @JvmStatic
  fun readModelPgPool(vertx: Vertx, config: JsonObject): PgPool {
    return pgPool(vertx, config, "READ")
  }

  @JvmStatic
  fun pgPool(vertx: Vertx, config: JsonObject, id: String): PgPool {
    val readOptions = PgConnectOptions()
      .setPort(config.getInteger("${id}_DATABASE_PORT"))
      .setHost(config.getString("${id}_DATABASE_HOST"))
      .setDatabase(config.getString("${id}_DATABASE_NAME"))
      .setUser(config.getString("${id}_DATABASE_USER"))
      .setPassword(config.getString("${id}_DATABASE_PASSWORD"))
    val pgPoolOptions = PoolOptions().setMaxSize(config.getInteger("${id}_DATABASE_POOL_MAX_SIZE"))
    return PgPool.pool(vertx, readOptions, pgPoolOptions)
  }

}
