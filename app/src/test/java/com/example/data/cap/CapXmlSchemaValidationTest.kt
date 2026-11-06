package com.example.data.cap

import org.junit.Assert.assertThrows
import org.junit.Test
import org.xml.sax.SAXException
import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

class CapXmlSchemaValidationTest {
    private fun validator() = javaClass.getResourceAsStream("/cap/CAP-v1.2.xsd").use { schemaStream ->
        requireNotNull(schemaStream) { "Bundled OASIS CAP 1.2 schema is missing" }
        val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).apply {
            setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        }
        factory.newSchema(StreamSource(schemaStream)).newValidator().apply {
            setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "")
            setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "")
        }
    }

    private val validAlert = """
        <?xml version="1.0" encoding="UTF-8"?>
        <alert xmlns="urn:oasis:names:tc:emergency:cap:1.2">
          <identifier>schema-test-001</identifier>
          <sender>alerts@example.gov.sy</sender>
          <sent>2026-11-04T06:00:00+03:00</sent>
          <status>Actual</status>
          <msgType>Alert</msgType>
          <scope>Public</scope>
          <info>
            <language>ar-SY</language>
            <category>Met</category>
            <event>أمطار غزيرة</event>
            <urgency>Expected</urgency>
            <severity>Moderate</severity>
            <certainty>Likely</certainty>
            <expires>2026-11-04T12:00:00+03:00</expires>
            <description>رسالة اختبار مطابقة للمخطط</description>
            <area>
              <areaDesc>دمشق</areaDesc>
            </area>
          </info>
        </alert>
    """.trimIndent()

    @Test
    fun acceptsAValidCap12MessageAgainstOasisSchema() {
        validator().validate(StreamSource(StringReader(validAlert)))
    }

    @Test
    fun rejectsChildrenOutsideTheSchemaSequence() {
        val invalidOrder = validAlert.replace(
            "<sender>alerts@example.gov.sy</sender>",
            ""
        ).replace(
            "<sent>2026-11-04T06:00:00+03:00</sent>",
            "<sent>2026-11-04T06:00:00+03:00</sent>\n  <sender>alerts@example.gov.sy</sender>"
        )
        assertThrows(SAXException::class.java) {
            validator().validate(StreamSource(StringReader(invalidOrder)))
        }
    }

    @Test
    fun rejectsZuluTimezoneWhereCap12SchemaRequiresNumericOffset() {
        val invalidTimestamp = validAlert.replace(
            "2026-11-04T06:00:00+03:00",
            "2026-11-04T03:00:00Z"
        )
        assertThrows(SAXException::class.java) {
            validator().validate(StreamSource(StringReader(invalidTimestamp)))
        }
    }
}
