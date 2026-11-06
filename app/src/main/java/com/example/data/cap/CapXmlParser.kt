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
data class CapReference(
    val sender: String,
    val identifier: String,
    val sentAt: Instant
)

data class CapFeedMessage(
    val identifier: String,
    val sender: String?,
    val sent: String?,
    val sentAt: Instant?,
    val status: String,
    val messageType: String,
    val scope: String,
    val references: List<String>,
    val parsedReferences: List<CapReference>,
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
    fun isPubliclyDisplayable(
        now: Instant = Instant.now(),
        reconciledReferences: Set<CapReference> = emptySet()
    ): Boolean {
        return status.equals("Actual", ignoreCase = true) &&
            scope.equals("Public", ignoreCase = true) &&
            (messageType.equals("Alert", ignoreCase = true) ||
                (messageType.equals("Update", ignoreCase = true) &&
                    parsedReferences.isNotEmpty() &&
                    parsedReferences.all { it in reconciledReferences })) &&
            sentAt?.let { !it.isAfter(now) } == true &&
            expiresAt?.let { expiry ->
                expiry.isAfter(now) && sentAt?.let(expiry::isAfter) == true
            } == true &&
            identifier.isNotBlank()
    }
}

object CapXmlParser {
    private const val CAP_NAMESPACE = "urn:oasis:names:tc:emergency:cap:1.2"
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
                setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", "")
                setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", "")
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
        require(
            root.namespaceURI == CAP_NAMESPACE && root.localTagName() == "alert"
        ) { "Root element is not a CAP 1.2 alert" }

        // CAP envelope fields are singleton elements. Reject duplicates rather than
        // silently trusting the first value and ignoring conflicting later values.
        val identifier = root.requiredSingleText("identifier")
        val sender = root.requiredSingleText("sender")
        val status = root.requiredSingleText("status")
        val messageType = root.requiredSingleText("msgType")
        val scope = root.requiredSingleText("scope")
        require(status in setOf("Actual", "Exercise", "System", "Test", "Draft")) {
            "CAP status is not a recognized CAP 1.2 value"
        }
        require(messageType in setOf("Alert", "Update", "Cancel", "Ack", "Error")) {
            "CAP message type is not a recognized CAP 1.2 value"
        }
        require(scope in setOf("Public", "Restricted", "Private")) {
            "CAP scope is not a recognized CAP 1.2 value"
        }

        val infos = root.directChildren("info")
        val info = infos.firstOrNull {
            val language = it.directText("language")
                ?: it.getAttribute("xml:lang")
            language.equals("ar", true) || language.startsWith("ar-", true)
        } ?: infos.firstOrNull()

        val area = info?.directChildren("area")?.firstOrNull()
        val sent = root.requiredSingleText("sent")
        val sentAt = try {
            OffsetDateTime.parse(sent).toInstant()
        } catch (error: Exception) {
            throw IllegalArgumentException("CAP sent timestamp must include a valid timezone offset", error)
        }
        val expires = info?.directText("expires")
        val expiresAt = expires?.let {
            try {
                OffsetDateTime.parse(it).toInstant()
            } catch (error: Exception) {
                throw IllegalArgumentException("CAP expiry must include a valid timezone offset", error)
            }
        }

        val references = root.optionalSingleText("references")
            ?.split(Regex("\\s+"))
            ?.filter { it.isNotBlank() }
            .orEmpty()
        val parsedReferences = references.map { rawReference ->
            val parts = rawReference.split(',', limit = 3).map(String::trim)
            require(parts.size == 3 && parts.all(String::isNotBlank)) {
                "CAP reference must contain sender, identifier and sent timestamp"
            }
            val referenceSentAt = try {
                OffsetDateTime.parse(parts[2]).toInstant()
            } catch (error: Exception) {
                throw IllegalArgumentException("CAP reference timestamp must include a valid timezone offset", error)
            }
            CapReference(sender = parts[0], identifier = parts[1], sentAt = referenceSentAt)
        }

        require(parsedReferences.distinct().size == parsedReferences.size) {
            "CAP references must not contain duplicate message triples"
        }

        return CapFeedMessage(
            identifier = identifier,
            sender = sender,
            sent = sent,
            sentAt = sentAt,
            status = status,
            messageType = messageType,
            scope = scope,
            references = references,
            parsedReferences = parsedReferences,
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
                if (element.namespaceURI == CAP_NAMESPACE && element.localTagName() == name) {
                    result += element
                }
            }
        }
        return result
    }

    private fun Element.requiredSingleText(name: String): String {
        val elements = directChildren(name)
        require(elements.size == 1) {
            "CAP $name must appear exactly once in the alert envelope"
        }
        return elements.single().textContent.trim().also {
            require(it.isNotEmpty()) { "CAP $name is empty" }
        }
    }

    private fun Element.optionalSingleText(name: String): String? {
        val elements = directChildren(name)
        require(elements.size <= 1) {
            "CAP $name must not appear more than once in the alert envelope"
        }
        return elements.firstOrNull()?.textContent?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun Element.directText(name: String): String? =
        directChildren(name).firstOrNull()?.textContent?.trim()?.takeIf { it.isNotEmpty() }
}
