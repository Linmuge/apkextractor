package info.muge.appshare.utils

import java.io.InputStream
import java.util.Stack

/**
 * A robust AXML (Android Binary XML) Printer/Decoder.
 * Adapted from AXMLPrinter2 and other open source implementations.
 */
object AXMLPrinter {

    private const val AXML_CHUNK_TYPE = 0x00080003
    private const val CHUNK_STRINGPOOL = 0x001C0001
    private const val CHUNK_XML_START_TAG = 0x00100102
    private const val CHUNK_XML_END_TAG = 0x00100103
    private const val CHUNK_XML_END_NAMESPACE = 0x00100101
    private const val CHUNK_XML_START_NAMESPACE = 0x00100100
    private const val CHUNK_XML_TEXT = 0x00100104
    private const val CHUNK_RESOURCEIDS = 0x00080180
    
    // Attribute types
    private const val TYPE_NULL = 0
    private const val TYPE_REFERENCE = 1
    private const val TYPE_ATTRIBUTE = 2
    private const val TYPE_STRING = 3
    private const val TYPE_FLOAT = 4
    private const val TYPE_DIMENSION = 5
    private const val TYPE_FRACTION = 6
    private const val TYPE_FIRST_INT = 16
    private const val TYPE_INT_DEC = 16
    private const val TYPE_INT_HEX = 17
    private const val TYPE_INT_BOOLEAN = 18
    private const val TYPE_FIRST_COLOR_INT = 28
    private const val TYPE_INT_COLOR_ARGB8 = 28
    private const val TYPE_INT_COLOR_RGB8 = 29
    private const val TYPE_INT_COLOR_ARGB4 = 30
    private const val TYPE_INT_COLOR_RGB4 = 31
    private const val TYPE_LAST_COLOR_INT = 31
    private const val TYPE_LAST_INT = 31

    fun decode(inputStream: InputStream): String {
        val parser = AXmlResourceParser()
        parser.open(inputStream)
        val sb = StringBuilder()
        val indent = StringBuilder()
        
        while (true) {
            val type = parser.next()
            if (type == -1) break
            
            when (type) {
                CHUNK_XML_START_TAG -> {
                    sb.append(indent).append("<").append(getNamespacePrefix(parser.prefix)).append(parser.name)
                    
                    val namespaceCountBefore = parser.namespaces.size
                    // We don't have direct access to namespaces added *just now* in this loop easily
                    // But we can check if we should print xmlns
                    // For simplicity, we might skip xmlns declarations or try to infer them.
                    // Let's iterate attributes
                    
                    for (i in 0 until parser.attributeCount) {
                        sb.append("\n").append(indent).append("    ")
                        val prefix = getNamespacePrefix(parser.getAttributePrefix(i))
                        sb.append(prefix).append(parser.getAttributeName(i))
                            .append("=\"")
                            .append(getAttributeValue(parser, i))
                            .append("\"")
                    }
                    sb.append(">\n")
                    indent.append("    ")
                }
                CHUNK_XML_END_TAG -> {
                    if (indent.length >= 4) indent.setLength(indent.length - 4)
                    sb.append(indent).append("</").append(getNamespacePrefix(parser.prefix)).append(parser.name).append(">\n")
                }
                CHUNK_XML_TEXT -> {
                    // Ignore text usually
                }
            }
        }
        return sb.toString()
    }

    private fun getNamespacePrefix(prefix: String?): String {
        return if (prefix == null || prefix.isEmpty()) "" else "$prefix:"
    }

    private fun getAttributeValue(parser: AXmlResourceParser, index: Int): String {
        val type = parser.getAttributeValueType(index)
        val data = parser.getAttributeValueData(index)
        
        return when (type) {
            TYPE_STRING -> parser.getAttributeValueString(index)
            TYPE_REFERENCE -> "@${getResourceIdName(data)} (${Integer.toHexString(data)})"
            TYPE_ATTRIBUTE -> "?${getResourceIdName(data)} (${Integer.toHexString(data)})"
            TYPE_INT_DEC -> data.toString()
            TYPE_INT_HEX -> "0x${Integer.toHexString(data)}"
            TYPE_INT_BOOLEAN -> if (data != 0) "true" else "false"
            TYPE_DIMENSION -> java.lang.Float.toString(complexToFloat(data)) + DIMENSION_UNITS[data and COMPLEX_UNIT_MASK]
            TYPE_FRACTION -> java.lang.Float.toString(complexToFloat(data)) + FRACTION_UNITS[data and COMPLEX_UNIT_MASK]
            TYPE_FLOAT -> java.lang.Float.toString(java.lang.Float.intBitsToFloat(data))
            
            TYPE_INT_COLOR_ARGB8 -> String.format("#%08X", data)
            TYPE_INT_COLOR_RGB8 -> String.format("#%06X", data and 0xFFFFFF)
            TYPE_INT_COLOR_ARGB4 -> String.format("#%04X", data and 0xFFFF)
            TYPE_INT_COLOR_RGB4 -> String.format("#%03X", data and 0xFFF)
            
            else -> {
                if (type >= TYPE_FIRST_COLOR_INT && type <= TYPE_LAST_COLOR_INT) {
                     String.format("#%08X", data)
                } else {
                     "<0x${Integer.toHexString(data)}, type 0x${Integer.toHexString(type)}>"
                }
            }
        }
    }
    
    private fun getResourceIdName(id: Int): String {
        // In a real decompiler, we would map this ID to a name from resources.arsc
        // Here we just return empty string or partial hint
        if (id == 0) return "null"
        return "" 
    }

    private fun complexToFloat(complex: Int): Float {
        return (complex and 0xFFFFFF00.toInt()) * RADIX_MULTS[(complex shr 4) and 3]
    }

    private val RADIX_MULTS = floatArrayOf(
        0.00390625f, 3.051758E-005f, 1.192093E-007f, 4.656613E-010f
    )
    
    private val DIMENSION_UNITS = arrayOf(
        "px", "dip", "sp", "pt", "in", "mm", "", ""
    )
    
    private val FRACTION_UNITS = arrayOf(
        "%", "%p", "", "", "", "", "", ""
    )
    
    private const val COMPLEX_UNIT_MASK = 0xf

    private class AXmlResourceParser {
        private lateinit var reader: IntReader
        var name: String = ""
        var prefix: String? = null
        var attributeCount = 0
        var namespaces = Stack<Namespace>()
        
        // String pool
        private var stringTable = ArrayList<String>()
        
        // Resource IDs
        private var resourceIds = IntArray(0)
        
        // Attributes for current tag: [ns, name, validRaw, type, data] * count
        private var attributes = IntArray(0) 

        fun open(stream: InputStream) {
            val bytes = stream.readBytes()
            reader = IntReader(bytes, false) // APKs are usually little-endian
        }

        fun next(): Int {
            if (reader.position >= reader.bytes.size) return -1
            
            val type = reader.readInt()
            val size = reader.readInt()
            
            // We need to handle chunks based on type
            when (type) {
                AXML_CHUNK_TYPE -> {
                    // Header, just move on
                    return next()
                }
                CHUNK_STRINGPOOL -> {
                    parseStringPool(size)
                    return next()
                }
                CHUNK_RESOURCEIDS -> {
                    parseResourceMap(size)
                    return next()
                }
                CHUNK_XML_START_TAG -> {
                    val lineNumber = reader.readInt()
                    reader.skip(4) // comment
                    val nsIdx = reader.readInt()
                    val nameIdx = reader.readInt()
                    val attributeStart = reader.readShort() // usually 20
                    val attributeSize = reader.readShort() // usually 20
                    attributeCount = reader.readShort().toInt()
                    val idIndex = reader.readShort()
                    val classIndex = reader.readShort()
                    val styleIndex = reader.readShort()
                    
                    name = getString(nameIdx)
                    prefix = getString(nsIdx)
                    
                    // Read attributes
                    attributes = IntArray(attributeCount * 5)
                    for (i in 0 until attributeCount) {
                        for (j in 0 until 5) {
                            attributes[i * 5 + j] = reader.readInt()
                        }
                    }
                    return type
                }
                CHUNK_XML_END_TAG -> {
                    val lineNumber = reader.readInt()
                    reader.skip(4) // comment
                    val nsIdx = reader.readInt()
                    val nameIdx = reader.readInt()
                    name = getString(nameIdx)
                    prefix = getString(nsIdx)
                    return type
                }
                CHUNK_XML_TEXT -> {
                    // skip
                    reader.skip(size - 8)
                    return CHUNK_XML_TEXT
                }
                CHUNK_XML_START_NAMESPACE -> {
                    val lineNumber = reader.readInt()
                    reader.skip(4) // comment
                    val prefixIdx = reader.readInt()
                    val uriIdx = reader.readInt()
                    namespaces.push(Namespace(getString(prefixIdx), getString(uriIdx)))
                    return next() // continue to next chunk (usually start tag)
                }
                CHUNK_XML_END_NAMESPACE -> {
                     val lineNumber = reader.readInt()
                     reader.skip(4) // comment
                     val prefixIdx = reader.readInt()
                     val uriIdx = reader.readInt()
                     if (namespaces.isNotEmpty()) namespaces.pop()
                     return next()
                }
                else -> {
                    reader.skip(size - 8) // Skip unknown chunk body
                    return next()
                }
            }
        }
        
        private fun parseStringPool(chunkSize: Int) {
             val stringCount = reader.readInt()
             val styleCount = reader.readInt()
             val flags = reader.readInt()
             val stringsStart = reader.readInt()
             val stylesStart = reader.readInt()
             
             val offsets = IntArray(stringCount)
             for (i in 0 until stringCount) {
                 offsets[i] = reader.readInt()
             }
             
             // Skip style offsets
             if (styleCount != 0) {
                 reader.skip(styleCount * 4)
             }
             
             // Read strings
             val rawBytes = reader.bytes
             val isUtf8 = (flags and 0x00000100) != 0
             val offsetsSize = stringCount * 4 + styleCount * 4
             val chunkStart = reader.position - (20 + offsetsSize) - 8
             val globalStringsStart = chunkStart + stringsStart
             
             for (i in 0 until stringCount) {
                 val strOffset = globalStringsStart + offsets[i]
                 if (strOffset < 0 || strOffset >= rawBytes.size) {
                     stringTable.add("")
                     continue
                 }
                 
                 // Decode string
                 if (isUtf8) {
                     // UTF-8 logic (simplified)
                     // There is a length prefix?
                     // Android specific: length is encoded.
                     // First byte is char count. If high bit set, 2 bytes.
                     // Then byte count. same logic.
                     // Then bytes.
                     
                     var pos = strOffset
                     // Skip character count
                     var charCount = rawBytes[pos].toInt() and 0xFF
                     pos++
                     if ((charCount and 0x80) != 0) {
                         pos++
                     }
                     
                     // Read byte count
                     var byteCount = rawBytes[pos].toInt() and 0xFF
                     pos++
                     if ((byteCount and 0x80) != 0) {
                         byteCount = ((byteCount and 0x7F) shl 8) or (rawBytes[pos].toInt() and 0xFF)
                         pos++
                     }
                     
                     val str = String(rawBytes, pos, byteCount, java.nio.charset.StandardCharsets.UTF_8)
                     stringTable.add(str)
                 } else {
                     // UTF-16
                     // Length is char count (2 byte units). 
                     // Stored as short. if high bit set, 2 shorts.
                     var pos = strOffset
                     var charCount = (rawBytes[pos].toInt() and 0xFF) or ((rawBytes[pos+1].toInt() and 0xFF) shl 8)
                     pos += 2
                     if ((charCount and 0x8000) != 0) {
                          val low = (rawBytes[pos].toInt() and 0xFF) or ((rawBytes[pos+1].toInt() and 0xFF) shl 8)
                          pos += 2
                          charCount = ((charCount and 0x7FFF) shl 16) or low
                     }
                     
                     val str = String(rawBytes, pos, charCount * 2, java.nio.charset.StandardCharsets.UTF_16LE)
                     stringTable.add(str)
                 }
             }
             
             // Move reader past the whole chunk
             reader.position = chunkStart + chunkSize
        }

        private fun parseResourceMap(size: Int) {
            val count = (size - 8) / 4
            resourceIds = IntArray(count)
            for (i in 0 until count) resourceIds[i] = reader.readInt()
        }
        
        fun getAttributePrefix(index: Int): String? {
            val uriIdx = attributes[index * 5 + 0]
            val uri = getString(uriIdx)
            
            if (uri.isNotEmpty()) {
                // Find prefix for this URI in our stack (search backwards)
                for (i in namespaces.indices.reversed()) {
                    val ns = namespaces[i]
                    if (ns.uri == uri) {
                        return ns.prefix
                    }
                }
                // Fallback: use the URI itself if no prefix found? Or empty?
                // Usually empty if not found, but Manifest attributes almost always have a prefix if they have a URI.
                return ""
            }
            return ""
        }

        fun getAttributeName(index: Int): String {
            val idx = attributes[index * 5 + 1]
            return getString(idx)
        }
        
        fun getAttributeValueType(index: Int): Int {
             return attributes[index * 5 + 3] shr 24
        }
        
        fun getAttributeValueData(index: Int): Int {
             return attributes[index * 5 + 4]
        }
        
        fun getAttributeValueString(index: Int): String {
             val idx = attributes[index * 5 + 2]
             return getString(idx)
        }
        
        fun getString(index: Int): String {
             if (index >= 0 && index < stringTable.size) return stringTable[index]
             if (index >= 0) return "res/$index" // Fallback
             return ""
        }
        
        // ....
    }
    
    // ... Helper classes ...
    data class Namespace(val prefix: String, val uri: String)

    private class IntReader(val bytes: ByteArray, val bigEndian: Boolean) {
        var position = 0
        
        fun readInt(): Int {
            if (position + 4 > bytes.size) return 0
            val b0 = bytes[position].toInt() and 0xFF
            val b1 = bytes[position+1].toInt() and 0xFF
            val b2 = bytes[position+2].toInt() and 0xFF
            val b3 = bytes[position+3].toInt() and 0xFF
            position += 4
            
            return if (bigEndian) {
                (b0 shl 24) or (b1 shl 16) or (b2 shl 8) or b3
            } else {
                b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)
            }
        }
        
        fun readShort(): Short {
            if (position + 2 > bytes.size) return 0
            val b0 = bytes[position].toInt() and 0xFF
            val b1 = bytes[position+1].toInt() and 0xFF
            position += 2
             return if (bigEndian) {
                ((b0 shl 8) or b1).toShort()
            } else {
                (b0 or (b1 shl 8)).toShort()
            }
        }

        fun skip(n: Int) {
            position += n
        }
    }
}
