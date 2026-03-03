package com.Otter.app.data.newpipe

import okhttp3.Headers
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

class OkHttpNewPipeDownloader(
    private val okHttpClient: OkHttpClient,
) : Downloader() {
    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val reqBuilder = okhttp3.Request.Builder().url(request.url())

        val headersBuilder = Headers.Builder()
        request.headers().forEach { (name, values) ->
            if (name != null) {
                values.forEach { v -> headersBuilder.add(name, v) }
            }
        }
        reqBuilder.headers(headersBuilder.build())

        val method = request.httpMethod().uppercase()
        val bodyBytes = request.dataToSend()

        when (method) {
            "GET" -> reqBuilder.get()
            "HEAD" -> reqBuilder.head()
            "POST" -> {
                val body = (bodyBytes ?: ByteArray(0)).toRequestBody("application/octet-stream".toMediaTypeOrNull())
                reqBuilder.post(body)
            }
            else -> {
                val body = (bodyBytes ?: ByteArray(0)).toRequestBody("application/octet-stream".toMediaTypeOrNull())
                reqBuilder.method(method, body)
            }
        }

        val call = okHttpClient.newCall(reqBuilder.build())
        call.execute().use { resp ->
            val responseBody = resp.body?.string().orEmpty()

            // Basic ReCaptcha detection used by NewPipe
            if (resp.code == 429 || responseBody.contains("recaptcha", ignoreCase = true)) {
                throw ReCaptchaException("ReCaptcha or rate limit triggered", request.url())
            }

            val headersMap: Map<String, List<String>> = resp.headers.toMultimap()

            return Response(
                resp.code,
                resp.message,
                headersMap,
                responseBody,
                resp.request.url.toString(),
            )
        }
    }
}
