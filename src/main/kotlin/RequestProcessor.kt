import com.LuceneSentenceSearch

enum class Status {
    OK, FAILED
}
data class Meta(val uid: String, val language: String)


class TMDelete {
    data class Response(val status: Status, val errorMessage: String?)
}

class TMQuery {
    data class Response(
        val sourceContext: List<String>,
        val targetContext: List<String>,
        val status: Status,
        val errorMessage: String?
    )

    data class Request(val input: String, val meta: Meta)
}

class TMSave {
    data class Request(val source: String, val target: String, val meta: Meta)
    data class Response(val status: Status, val errorMessage: String?)
}

class RequestProcessor(private val indexDir: String, private val blueRescoringThreshold: Float) {

    private val indices = mutableMapOf<String, LuceneSentenceSearch>()

    fun processRetrieve(request: TMQuery.Request): TMQuery.Response {
        // Query index
        val index = getIndex(request.meta.language)
        val documents = index.queryTM(request.input, request.meta.uid, false, 50)

        val sourceContext = documents.map { it.getField("srcBPE").stringValue() }
        val targetContext = documents.map { it.getField("trgBPE").stringValue() }

        return TMQuery.Response(sourceContext, targetContext, Status.OK, null)
    }

    fun processSave(request: TMSave.Request): TMSave.Response {
        val index = getIndex(request.meta.language)
        index.addSentenceToIndex(request.source, request.target, request.meta.uid)
        return TMSave.Response(Status.OK, null)
    }

    fun processDelete(request: Meta): TMDelete.Response {
        val index = getIndex(request.language)
        index.deleteSentenceFromIndexByUID(request.uid)
        return TMDelete.Response(Status.OK, null)
    }

    private fun getIndex(lang: String): LuceneSentenceSearch {
        if (lang !in indices) {
            indices[lang] = LuceneSentenceSearch.createIndexInDir(indexDir, lang)
            indices[lang]!!.setBleu_similarity_threshold(blueRescoringThreshold)
            indices[lang]!!.addSentenceToIndex("", "", "")
        }
        return indices[lang]!!
    }

}


