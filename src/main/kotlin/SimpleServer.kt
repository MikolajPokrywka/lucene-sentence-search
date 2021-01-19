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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext


fun main(args: Array<String>) {
    val requestProcessor = RequestProcessor("TM_index")

    if (args.count() < 1 || args[0].toIntOrNull() == null ){
        throw Exception("Port number not provided")
    }

    val server = embeddedServer(Netty, args[0].toInt()) {

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
    }

    server.start(wait = false)
    println("TM Server started")
}
