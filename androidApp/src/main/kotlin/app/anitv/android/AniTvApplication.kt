package app.anitv.android

import android.app.Application
import app.anitv.core.AniTvCore
import app.anitv.core.FileTextStorage
import java.io.File

class AniTvApplication : Application() {
    val core: AniTvCore by lazy {
        AniTvCore(progressStorage = FileTextStorage(File(filesDir, "progress.json")))
    }
}
