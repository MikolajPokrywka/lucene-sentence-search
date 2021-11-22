import com.fasterxml.jackson.databind.SerializationFeature
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.file.Paths

class Args(parser: ArgParser) {
    val port by parser.storing("Port number") { toInt() }

    val bleuRescoringThreshold: Float by parser
        .storing(
            "--bleu-rescoring-threshold",
            help = "Blue rescoring threshold"
        ) { toFloat() }.default(0.05f)

    val indexDir by parser
        .storing("--index-dir", help = "Index store location") { File(this).also { it.mkdirs() } }
        .default(File("tm-index/").also { it.mkdirs() })
        .addValidator { if (!value.isDirectory) throw IllegalArgumentException("index-dir must be a directory") }
}

fun main(args: Array<String>) {
    ArgParser(args).parseInto(::Args).run {
        val requestProcessor = RequestProcessor(indexDir.toString(), bleuRescoringThreshold)

        embeddedServer(Netty, port, configure = {
            workerGroupSize = 1
        }) {


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
                post("/get") {
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
                        val request = call.receive<UID>()
                        val response = requestProcessor.processDelete(request)
                        call.respond(response)
                    } catch (e: Exception) {
                        call.respond(TMDelete.Response(Status.FAILED, e.localizedMessage))
                    }
                }
            }
        }.start(wait = true)
    }
}
