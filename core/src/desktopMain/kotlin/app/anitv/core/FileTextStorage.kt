package app.anitv.core

import app.anitv.core.progress.TextStorage
import java.io.File

/** Stores text in a file, writing atomically through a temp file. */
class FileTextStorage(private val file: File) : TextStorage {
    override fun read(): String? = file.takeIf { it.exists() }?.readText()

    override fun write(text: String) {
        file.parentFile?.mkdirs()
        val tmp = File(file.parentFile, "${file.name}.tmp")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
