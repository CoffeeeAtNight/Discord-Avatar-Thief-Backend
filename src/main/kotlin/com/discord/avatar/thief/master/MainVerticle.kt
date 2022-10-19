package com.discord.avatar.thief.master

import io.reactivex.Single
import io.vertx.core.impl.logging.LoggerFactory
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.reactivex.core.AbstractVerticle
import io.vertx.reactivex.ext.web.Route
import io.vertx.reactivex.ext.web.Router
import io.vertx.reactivex.ext.web.RoutingContext
import io.vertx.reactivex.ext.web.client.WebClient
import io.vertx.reactivex.ext.web.handler.BodyHandler
import io.vertx.reactivex.ext.web.handler.CorsHandler
import io.vertx.reactivex.ext.web.handler.StaticHandler

class MainVerticle : AbstractVerticle() {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val discordApiBaseUrl = "https://discord.com/api/v10"
  private val discordCdnBaseUrl = "https://cdn.discordapp.com"

  override fun start() {
    logger.info("Creating routes")
    val router = Router.router(vertx)


    router.route().handler(staticHandler())
    router.get("/api/getUser/:userId").handler(this::getUserHandler)

    router.route().handler(CorsHandler.create("http://localhost:3000")
      .allowedMethod(io.vertx.core.http.HttpMethod.GET)
      .allowCredentials(true)
      .allowedHeader("Access-Control-Allow-Headers")
      .allowedHeader("Authorization")
      .allowedHeader("Access-Control-Allow-Method")
      .allowedHeader("Access-Control-Allow-Origin")
      .allowedHeader("Access-Control-Allow-Credentials")
      .allowedHeader("Content-Type"));

    logger.info("Starting http server")
    vertx.createHttpServer()
      .requestHandler(router)
      .listen(8080)
  }

  private fun getUserHandler(routingContext: RoutingContext) {
    val user = routingContext.pathParam("userId")

    fetchUserInfoJsonById(user)
      .map { userInfoJson ->
        buildResponseJson(userInfoJson)
      }
      .subscribe(
        { userInfoJson ->
          logger.info("Json was successfuly built and fetched!")
          routingContext.response()
            .setStatusCode(200)
            .putHeader("contentType", "application/json")
            .putHeader("Access-Control-Allow-Origin", "http://localhost:3000")
            .end(userInfoJson.encodePrettily())
        },
        {
          logger.error("Error while building user info json", it)
          routingContext.response()
            .setStatusCode(500)
            .end(JsonObject().put("success", false).encode())
        }
      )
  }

  private fun buildResponseJson(userJson: JsonObject): JsonObject {
    return JsonObject()
      .put("userId", userJson.getString("id"))
      .put("username", userJson.getString("username"))
      .put("userIdentNum", userJson.getString("discriminator"))
      .put("avatarUrl", "$discordCdnBaseUrl/avatars/${userJson.getString("id")}/${userJson.getString("avatar")}?size=1024")
      .put("bannerUrl", "$discordCdnBaseUrl/banners/${userJson.getString("id")}/${userJson.getString("banner")}?size=2048")
      .put("bannerColor", userJson.getString("banner_color"))
  }

  private fun fetchUserInfoJsonById(userId: String) : Single<JsonObject> {
    val webclient: WebClient = WebClient.create(vertx)

    return webclient.getAbs("$discordApiBaseUrl/users/$userId")
      .putHeader("Authorization", "Bot USE_BOT_TOKEN_HERE")
      .rxSend()
      .map { httpResponse ->
        if(httpResponse.statusCode() in 200..299) {
          httpResponse.bodyAsJsonObject()
        } else {
          throw Exception("Error while fetching discord user information, Statuscode: ${httpResponse.statusCode()}")
        }
      }
  }

  private fun staticHandler(): StaticHandler {
    return StaticHandler.create()
      .setCachingEnabled(false)
  }
}
