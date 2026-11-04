package com.example.data.cap

import com.example.data.model.AlertSeverity
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import java.time.Instant
import java.time.OffsetDateTime
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Strict, side-effect-free CAP 1.2 XML parser.
 *
 * This parser does not fetch URLs and does not decide that an issuer is trusted.
 * Callers must obtain the document from the configured official source and apply
 * the display gate before presenting it as a public warning.
 */
data class CapFeedMessage(
    val identifier: String,
    val sender: String?,
    val sent: String?,
    val status: String,
    val messageType: String,
    val scope: String,
    val references: List<String>,
    val event: String?,
    val severity: AlertSeverity?,
    val urgency: String?,
    val certainty: String?,
    val areaDescription: String?,
    val description: String?,
    val instruction: String?,
    val expires: String?,
    val expiresAt: Instant?
) {
    fun isPubliclyDisplayable(now: Instant = Instant.now()): Boolean {
        return status.equals("Actual", ignoreCase = true) &&
            scope.equals("Public", ignoreCase = true) &&
            (messageType.equals("Alert", ignoreCase = true) ||
                messageType.equals("Update", ignoreCase = true)) &&
            expiresAt?.isAfter(now) == true &&
            identifier.isNotBlank()
    }
}

object CapXmlParser {
    private const val MAX_DOCUMENT_CHARS = 512 * 1024

    fun parse(xml: String): CapFeedMessage {
        require(xml.isNotBlank()) { "CAP document is empty" }
        require(xml.length <= MAX_DOCUMENT_CHARS) { "CAP document exceeds the size limit" }

        val factory = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = true
            isExpandEntityReferences = false
            setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
            setFeature("http://xml.org/sax/features/external-general-entities", false)
            setFeature("http://xml.org/sax/features/external-parameter-entities", false)
            setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
            try {
                setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "")
                setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
            } catch (_: IllegalArgumentException) {
                // External entity processing is disabled through the required features above.
            }
        }

        val builder = factory.newDocumentBuilder().apply {
            setEntityResolver { _, _ -> InputSource(StringReader("")) }
        }
        val document = try {
            builder.parse(InputSource(StringReader(xml)))
        } catch (error: Exception) {
            throw IllegalArgumentException("Invalid or unsafe CAP XML", error)
        }

        val root = document.documentElement
            ?: throw IllegalArgumentException("CAP root element is missing")
        require(root.localTagName() == "alert") { "Root element is not CAP alert" }

        val identifier = root.directText("identifier").orEmpty()
        require(identifier.isNotBlank()) { "CAP identifier is missing" }

        val infos = root.directChildren("info")
        val info = infos.firstOrNull {
            val language = it.directText("language")
                ?: it.getAttribute("xml:lang")
            language.equals("ar", true) || language.startsWith("ar-", true)
        } ?: infos.firstOrNull()

        val area = info?.directChildren("area")?.firstOrNull()
        val sent = root.directText("sent")
        val expires = info?.directText("expires")
        val expiresAt = expires?.let {
            try {
                OffsetDateTime.parse(it).toInstant()
            } catch (error: Exception) {
                throw IllegalArgumentException("CAP expiry must include a valid timezone offset", error)
            }
        }

        return CapFeedMessage(
            identifier = identifier,
            sender = root.directText("sender"),
            sent = sent,
            status = root.directText("status").orEmpty(),
            messageType = root.directText("msgType").orEmpty(),
            scope = root.directText("scope").orEmpty(),
            references = root.directText("references")
                ?.split(Regex("\\s+"))
                ?.filter { it.isNotBlank() }
                .orEmpty(),
            event = info?.directText("event"),
            severity = info?.directText("severity")?.toCapSeverity(),
            urgency = info?.directText("urgency"),
            certainty = info?.directText("certainty"),
            areaDescription = area?.directText("areaDesc"),
            description = info?.directText("description"),
            instruction = info?.directText("instruction"),
            expires = expires,
            expiresAt = expiresAt
        )
    }

    private fun String.toCapSeverity(): AlertSeverity? = when (lowercase()) {
        "extreme" -> AlertSeverity.EMERGENCY
        "severe" -> AlertSeverity.WARNING
        "moderate" -> AlertSeverity.WATCH
        "minor" -> AlertSeverity.ADVISORY
        else -> null
    }

    private fun Element.localTagName(): String =
        localName ?: tagName.substringAfter(':')

    private fun Element.directChildren(name: String): List<Element> {
        val result = mutableListOf<Element>()
        val nodes = childNodes
        for (index in 0 until nodes.length) {
            val node = nodes.item(index)
            if (node.nodeType == Node.ELEMENT_NODE) {
                val element = node as Element
                if (element.localTagName() == name) result += element
            }
        }
        return result
    }

    private fun Element.directText(name: String): String? =
        directChildren(name).firstOrNull()?.textContent?.trim()?.takeIf { it.isNotEmpty() }
}
