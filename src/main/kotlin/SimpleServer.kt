import com.LuceneSentenceSearch
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*


fun main() {
    val requestProcessor = RequestProcessor("index")

    embeddedServer(Netty, 8080) {

        install(StatusPages) {
            exception<Throwable> { e ->
                call.respondText(e.localizedMessage, ContentType.Text.Plain, HttpStatusCode.InternalServerError)
            }
        }

        install(ContentNegotiation) {
            jackson {
                enable(SerializationFeature.INDENT_OUTPUT)
            }
        }

        routing {
            post("/retrieve") {
                try {
                    val request = call.receive<TMQuery.Request>()
                    val response = requestProcessor.processRetrieve(request)
                    call.respond(response)
                } catch (e: Exception) {
                    call.respond(TMQuery.Response(listOf(), listOf(), Status.FAILED, e.localizedMessage))
                }
            }

            post("/save") {
                try {
                    val request = call.receive<TMSave.Request>()
                    val response = requestProcessor.processSave(request)
                    call.respond(response)
                } catch (e: Exception) {
                    call.respond(TMSave.Response(Status.FAILED, e.localizedMessage))
                }

            }

            post("/delete") {
                try {
                    val request = call.receive<TMDelete.Request>()
                    val response = requestProcessor.processDelete(request)
                    call.respond(response)
                } catch (e: Exception) {
                    call.respond(TMDelete.Response(Status.FAILED, e.localizedMessage))
                }
            }
        }

    }.start(wait = true)
}
