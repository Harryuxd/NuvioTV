package com.nuvio.tv.data.iptv.epg

import com.nuvio.tv.domain.model.iptv.IptvEpgProgram
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.SAXParserFactory

object XmltvParser {

    private val DATE_FORMAT_WITH_TZ = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val DATE_FORMAT_NO_TZ = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(inputStream: InputStream, playlistId: String, maxPrograms: Int = 100000): List<IptvEpgProgram> {
        val programs = ArrayList<IptvEpgProgram>()

        val factory = SAXParserFactory.newInstance()
        factory.isNamespaceAware = false
        val saxParser = factory.newSAXParser()

        val handler = object : DefaultHandler() {
            private var currentChannel: String? = null
            private var currentStartMs: Long? = null
            private var currentStopMs: Long? = null
            private var currentCategory: String? = null
            private var currentIcon: String? = null
            private var insideProgramme = false
            private val currentText = StringBuilder()
            private var currentTitle: String? = null
            private var currentDesc: String? = null

            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                val tag = qName ?: localName ?: ""
                currentText.setLength(0)

                if (tag.equals("programme", ignoreCase = true)) {
                    insideProgramme = true
                    currentChannel = attributes?.getValue("channel")
                    val startStr = attributes?.getValue("start")
                    val stopStr = attributes?.getValue("stop")
                    currentStartMs = startStr?.let { parseXmltvDate(it) }
                    currentStopMs = stopStr?.let { parseXmltvDate(it) }
                    currentTitle = null
                    currentDesc = null
                    currentCategory = null
                    currentIcon = null
                } else if (insideProgramme && tag.equals("icon", ignoreCase = true)) {
                    currentIcon = attributes?.getValue("src")
                }
            }

            override fun characters(ch: CharArray?, start: Int, length: Int) {
                if (ch != null && length > 0) {
                    currentText.append(ch, start, length)
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                val tag = qName ?: localName ?: ""
                val text = currentText.toString().trim()

                if (insideProgramme) {
                    when (tag.lowercase()) {
                        "title" -> currentTitle = text
                        "desc" -> currentDesc = text
                        "category" -> currentCategory = text
                        "programme" -> {
                            insideProgramme = false
                            val ch = currentChannel
                            val start = currentStartMs
                            val stop = currentStopMs
                            val title = currentTitle

                            if (!ch.isNullOrBlank() && start != null && stop != null && !title.isNullOrBlank() && programs.size < maxPrograms) {
                                val programId = "${playlistId}_${ch}_${start}"
                                programs.add(
                                    IptvEpgProgram(
                                        id = programId,
                                        channelTvgId = ch,
                                        title = title,
                                        description = currentDesc,
                                        startEpochMs = start,
                                        endEpochMs = stop,
                                        category = currentCategory,
                                        posterUrl = currentIcon
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        val inputSource = InputSource(inputStream).apply { encoding = "UTF-8" }
        saxParser.parse(inputSource, handler)
        return programs
    }

    fun parseXmltvDate(dateStr: String): Long? {
        val trimmed = dateStr.trim()
        return try {
            if (trimmed.contains(" ") || trimmed.contains("+") || trimmed.contains("-")) {
                synchronized(DATE_FORMAT_WITH_TZ) {
                    DATE_FORMAT_WITH_TZ.parse(trimmed)?.time
                }
            } else {
                synchronized(DATE_FORMAT_NO_TZ) {
                    DATE_FORMAT_NO_TZ.parse(trimmed)?.time
                }
            }
        } catch (_: Exception) {
            null
        }
    }
}
