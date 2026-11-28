package com.listingprep.ml

import android.graphics.Bitmap
import android.util.Base64
import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.core.JsonValue
import com.anthropic.errors.AnthropicIoException
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.StopReason
import java.io.ByteArrayOutputStream
import java.time.Duration

/**
 * Optional cloud writing with Claude (off by default). Uses your own Anthropic API key, kept
 * encrypted on the phone. Only when it's on: the item's photos (up to [MAX_PHOTOS], shrunk to
 * 1024 px JPEGs) and the same prompt the on-phone AI gets are sent over HTTPS to
 * api.anthropic.com. Nothing else leaves the phone.
 */
object Claude {
    const val MODEL = "claude-opus-5-5"
    const val MAX_PHOTOS = 4
    private const val PHOTO_PX = 1024

    class Failed(message: String) : Exception(message)

    /** A photo shrunk and compressed for upload (about 100–200 KB). */
    fun jpeg(bmp: Bitmap): ByteArray {
        val s = PHOTO_PX.toFloat() / maxOf(bmp.width, bmp.height)
        val small = if (s < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * s).toInt(), (bmp.height * s).toInt(), true) else bmp
        return ByteArrayOutputStream().use { out ->
            small.compress(Bitmap.CompressFormat.JPEG, 85, out)
            if (small !== bmp) small.recycle()
            out.toByteArray()
        }
    }

    private fun client(key: String): AnthropicClient =
        AnthropicOkHttpClient.builder()
            .apiKey(key)
            .timeout(Duration.ofSeconds(120))
            .maxRetries(2)
            .build()

    /** The request: photos first, then the prompt. Kept separate so a unit test can build it. */
    fun params(photos: List<String>, prompt: String): MessageCreateParams =
        MessageCreateParams.builder()
            .model(MODEL)
            .maxTokens(16000L)
            // Thinking is always on for this model; medium effort is plenty for a listing.
            .outputConfig(OutputConfig.builder().effort(OutputConfig.Effort.MEDIUM).build())
            // If Claude's safety check declines a request, the API retries it on another model.
            .putAdditionalHeader("anthropic-beta", "server-side-fallback-2026-07-01")
            .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"))
            .addUserMessageOfContentBlockParams(
                photos.take(MAX_PHOTOS).map { b64 ->
                    ContentBlockParam.ofImage(
                        Base64ImageSource.builder()
                            .data(b64)
                            .mediaType(Base64ImageSource.MediaType.IMAGE_JPEG)
                            .build()
                    )
                } + ContentBlockParam.ofText(prompt)
            )
            .build()

    /** Writes the answer text (same TITLE/DESCRIPTION format as the on-phone AI). Blocking: call off the main thread. */
    fun write(key: String, photos: List<ByteArray>, prompt: String): String {
        val encoded = photos.map { Base64.encodeToString(it, Base64.NO_WRAP) }
        val client = client(key)
        try {
            val msg = client.messages().create(params(encoded, prompt))
            if (msg.stopReason().orElse(null) == StopReason.REFUSAL) throw Failed("Claude declined to write this one.")
            return msg.content().mapNotNull { it.text().orElse(null)?.text() }.joinToString("\n").trim()
        } catch (e: UnauthorizedException) {
            throw Failed("Claude didn't accept the API key. Check it in Settings.")
        } catch (e: PermissionDeniedException) {
            throw Failed("This API key isn't allowed to use Claude. Check it in the Anthropic Console.")
        } catch (e: RateLimitException) {
            throw Failed("Claude is busy or the key hit its limit. Try again in a minute.")
        } catch (e: AnthropicServiceException) {
            val type = e.errorType().map { it.toString() }.orElse("")
            throw Failed(
                if (type.contains("billing")) "The Anthropic account has no credit left. Top it up in the Anthropic Console."
                else "Claude had a problem (${e.statusCode()}). Try again."
            )
        } catch (e: AnthropicIoException) {
            throw Failed("Couldn't reach Claude. Check the internet connection.")
        } finally {
            client.close()
        }
    }
}
