import com.LuceneSentenceSearch

enum class Status {
    OK, FAILED
}

class TMDelete {
    data class Response(val status: Status, val errorMessage: String?)
    data class Request(val uid: String, val language: String)
}

class TMQuery {
    data class Response(
        val sourceContext: List<String>,
        val targetContext: List<String>,
        val status: Status,
        val errorMessage: String?
    )

    data class Request(val uid: String, val language: String, val text: String)
}

class TMSave {
    data class Request(val uid: String, val language: String, val source: String, val target: String)
    data class Response(val status: Status, val errorMessage: String?)
}

class RequestProcessor(private val indexName: String, private val blueRescoringThreshold: Float) {

    private val indices = mutableMapOf<String, LuceneSentenceSearch>()

    fun processRetrieve(request: TMQuery.Request): TMQuery.Response {
        // Query index
        val index = getIndex(request.language)
        val documents = index.queryTM(request.text, request.uid, false, 50)

        val sourceContext = documents.map { it.getField("srcBPE").stringValue() }
        val targetContext = documents.map { it.getField("trgBPE").stringValue() }

        return TMQuery.Response(sourceContext, targetContext, Status.OK, null)
    }

    fun processSave(request: TMSave.Request): TMSave.Response {
        val index = getIndex(request.language)
        index.addSentenceToIndex(request.source, request.target, request.uid)
        return TMSave.Response(Status.OK, null)
    }

    fun processDelete(request: TMDelete.Request): TMDelete.Response {
        val index = getIndex(request.language)
        index.deleteSentenceFromIndexByUID(request.uid)
        return TMDelete.Response(Status.FAILED, null)
    }

    private fun getIndex(lang: String): LuceneSentenceSearch {
        if (lang !in indices) {
            indices[lang] = LuceneSentenceSearch.createNamedIndex(indexName, lang)
            indices[lang]!!.setBleu_similarity_threshold(blueRescoringThreshold)
            indices[lang]!!.addSentenceToIndex("", "", "")
        }
        return indices[lang]!!
    }

}


