package com.nuvio.tv.data.iptv.epg

import com.nuvio.tv.domain.model.iptv.IptvEpgProgram
import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.InputStream
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.SAXParserFactory

object XmltvParser {

    private val DATE_FORMAT_WITH_TZ = SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US)
    private val DATE_FORMAT_NO_TZ = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parse(
        inputStream: InputStream,
        playlistId: String,
        targetChannelKeys: Set<String> = emptySet(),
        maxPrograms: Int = 200000
    ): List<IptvEpgProgram> {
        val programs = ArrayList<IptvEpgProgram>()
        val channelDisplayNames = HashMap<String, ArrayList<String>>()

        val factory = SAXParserFactory.newInstance()
        factory.isNamespaceAware = false
        val saxParser = factory.newSAXParser()

        val handler = object : DefaultHandler() {
            private var currentChannelTagId: String? = null
            private var currentProgrammeChannel: String? = null
            private var currentStartMs: Long? = null
            private var currentStopMs: Long? = null
            private var currentCategory: String? = null
            private var currentIcon: String? = null
            private var insideChannel = false
            private var insideProgramme = false
            private val currentText = StringBuilder()
            private var currentTitle: String? = null
            private var currentDesc: String? = null

            override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
                val tag = qName ?: localName ?: ""
                currentText.setLength(0)

                if (tag.equals("channel", ignoreCase = true)) {
                    insideChannel = true
                    currentChannelTagId = attributes?.getValue("id")?.trim()
                } else if (tag.equals("programme", ignoreCase = true)) {
                    insideProgramme = true
                    currentProgrammeChannel = attributes?.getValue("channel")?.trim()
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

                if (insideChannel) {
                    if (tag.equals("display-name", ignoreCase = true) && text.isNotBlank()) {
                        val chId = currentChannelTagId
                        if (!chId.isNullOrBlank()) {
                            val list = channelDisplayNames.getOrPut(chId.lowercase()) { ArrayList() }
                            list.add(text)
                        }
                    } else if (tag.equals("channel", ignoreCase = true)) {
                        insideChannel = false
                        currentChannelTagId = null
                    }
                } else if (insideProgramme) {
                    when (tag.lowercase()) {
                        "title" -> currentTitle = text
                        "desc" -> currentDesc = text
                        "category" -> currentCategory = text
                        "programme" -> {
                            insideProgramme = false
                            val ch = currentProgrammeChannel
                            val start = currentStartMs
                            val stop = currentStopMs
                            val title = currentTitle

                            if (!ch.isNullOrBlank() && start != null && stop != null && !title.isNullOrBlank() && programs.size < maxPrograms) {
                                val chLower = ch.lowercase()
                                val chBase = chLower.substringBeforeLast('.')
                                val chNorm = normalize(chLower)

                                val displayNames = channelDisplayNames[chLower].orEmpty()
                                val displayNorms = displayNames.map { normalize(it) }

                                val isMatched = if (targetChannelKeys.isEmpty()) true else {
                                    chLower in targetChannelKeys ||
                                    chBase in targetChannelKeys ||
                                    chNorm in targetChannelKeys ||
                                    displayNorms.any { it in targetChannelKeys } ||
                                    displayNames.any { it.lowercase() in targetChannelKeys }
                                }

                                if (isMatched) {
                                    val programId = "${playlistId}_${ch}_${start}"
                                    val program = IptvEpgProgram(
                                        id = programId,
                                        channelTvgId = ch,
                                        title = title,
                                        description = currentDesc,
                                        startEpochMs = start,
                                        endEpochMs = stop,
                                        category = currentCategory,
                                        posterUrl = currentIcon
                                    )
                                    programs.add(program)

                                    // Index normalized variations so queries by channel name / tvgName hit the cache instantly
                                    if (chNorm.isNotBlank() && chNorm != chLower) {
                                        programs.add(program.copy(id = "${programId}_norm", channelTvgId = chNorm))
                                    }
                                    for (dispNorm in displayNorms) {
                                        if (dispNorm.isNotBlank() && dispNorm != chNorm && dispNorm != chLower) {
                                            programs.add(program.copy(id = "${programId}_${dispNorm.hashCode()}", channelTvgId = dispNorm))
                                        }
                                    }
                                }
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
        val raw = dateStr.trim()
        if (raw.isEmpty()) return null

        // Standardize timezone offset with colon: "+02:00" -> "+0200"
        val tzCleaned = if (raw.matches(Regex(""".*[+-]\d{2}:\d{2}$"""))) {
            val lastColon = raw.lastIndexOf(':')
            raw.substring(0, lastColon) + raw.substring(lastColon + 1)
        } else raw

        // Clean ISO timestamps "2026-08-22 12:00:00" or "2026-08-22T12:00:00Z"
        val compact = tzCleaned.replace("-", "").replace(":", "").replace("T", " ")
        return try {
            if (compact.contains(" ") || compact.contains("+") || (compact.contains("-") && compact.indexOf('-') > 8)) {
                synchronized(DATE_FORMAT_WITH_TZ) {
                    DATE_FORMAT_WITH_TZ.parse(compact)?.time
                }
            } else {
                val digitsOnly = compact.take(14)
                synchronized(DATE_FORMAT_NO_TZ) {
                    DATE_FORMAT_NO_TZ.parse(digitsOnly)?.time
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    fun normalize(raw: String): String {
        if (raw.isBlank()) return ""
        val decomposed = Normalizer.normalize(raw, Normalizer.Form.NFKD)
            .replace(Regex("""\p{M}"""), "")
            .lowercase()

        return decomposed
            .replace(Regex("""^(\[[^\]]*\]|\([^)]*\)|[a-z0-9+/&.-]{1,15}\s*[:|/|-]\s*)"""), " ")
            .replace(Regex("""\([^)]*\)"""), " ")
            .replace(Regex("""\[[^\]]*\]"""), " ")
            .replace(Regex("""\{[^}]*\}"""), " ")
            .replace(Regex("""\b(uhd|fhd|hd|sd|4k|8k|hevc|h264|h265|2160p|3840p|4320p|1080p|1080i|720p|576p|480p|50fps|60fps|fps|raw|backup|back|alt|live|feed|stream|vip|now|direct|east|west|central|pacific|mountain|dolby|atmos|hdr|hdr10|sdr)\b"""), " ")
            .replace(Regex("""\b\d+\b"""), " ")
            .replace(Regex("""\b(us|uk|ca|au|nz|ie|de|fr|es|it|nl|pt|br|mx|ar|in)\b"""), " ")
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
            .replace(Regex("""\s+"""), " ")
    }
}
