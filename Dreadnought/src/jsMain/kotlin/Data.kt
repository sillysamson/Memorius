import kotlinx.browser.localStorage
import kotlinx.browser.window

class FileRef(val name: String, val data: String)

class Card(
    var id: String,
    var subject: String,
    var due: String,
    var color: Int,
    var type: String,
    var note: String,
    val files: MutableList<FileRef>,
    var done: Boolean
)

class Opus(var id: String, var title: String, var ext: String, var kind: String, var content: String, var progress: Int)

class Slot(var id: String, var day: Int, var subject: String, var time: String, var note: String)

data class Settings(
    var freq: String = "due",
    var time: String = "08:00",
    var lang: Int = 0,
    var mode: String = "light",
    var theme: Int = 0,
    var cb: Boolean = false,
    var folder: String = ""
)

fun obj(): dynamic = js("({})")
fun arr(): dynamic = js("[]")
fun newId(): String = js("Date.now().toString(36) + Math.random().toString(36).slice(2, 6)") as String

object Store {
    var cards = mutableListOf<Card>()
    var opus = mutableListOf<Opus>()
    var slots = mutableListOf<Slot>()
    var st = Settings()

    fun save() {
        try {
            val c = arr()
            cards.forEach { x ->
                val o = obj()
                o.id = x.id; o.subject = x.subject; o.due = x.due; o.color = x.color
                o.type = x.type; o.note = x.note; o.done = x.done
                val fs = arr()
                x.files.forEach { f ->
                    val fo = obj()
                    fo.name = f.name; fo.data = f.data
                    fs.push(fo)
                }
                o.files = fs
                c.push(o)
            }
            localStorage.setItem("m.cards", JSON.stringify(c))

            val ops = arr()
            opus.forEach { x ->
                val o = obj()
                o.id = x.id; o.title = x.title; o.ext = x.ext; o.kind = x.kind
                o.content = x.content; o.progress = x.progress
                ops.push(o)
            }
            localStorage.setItem("m.opus", JSON.stringify(ops))

            val sl = arr()
            slots.forEach { x ->
                val o = obj()
                o.id = x.id; o.day = x.day; o.subject = x.subject; o.time = x.time; o.note = x.note
                sl.push(o)
            }
            localStorage.setItem("m.slots", JSON.stringify(sl))

            val so = obj()
            so.freq = st.freq; so.time = st.time; so.lang = st.lang; so.mode = st.mode
            so.theme = st.theme; so.cb = st.cb; so.folder = st.folder
            localStorage.setItem("m.st", JSON.stringify(so))
        } catch (e: Throwable) {
            window.alert(t("tooBig"))
        }
    }

    private fun <T> readList(key: String, f: (dynamic) -> T): MutableList<T> {
        val raw = localStorage.getItem(key) ?: return mutableListOf()
        val out = mutableListOf<T>()
        try {
            val a = JSON.parse<dynamic>(raw)
            val n = a.length as Int
            for (i in 0 until n) out.add(f(a[i]))
        } catch (e: Throwable) {
        }
        return out
    }

    fun load() {
        cards = readList("m.cards") { o ->
            val fl = mutableListOf<FileRef>()
            val fa = o.files
            if (fa != null) {
                val n = fa.length as Int
                for (i in 0 until n) fl.add(FileRef(fa[i].name as String, fa[i].data as String))
            }
            Card(o.id as String, o.subject as String, o.due as String, o.color as Int, o.type as String, o.note as String, fl, o.done as Boolean)
        }
        opus = readList("m.opus") { o ->
            Opus(o.id as String, o.title as String, o.ext as String, o.kind as String, o.content as String, o.progress as Int)
        }
        slots = readList("m.slots") { o ->
            Slot(o.id as String, o.day as Int, o.subject as String, o.time as String, o.note as String)
        }
        val raw = localStorage.getItem("m.st")
        if (raw != null) {
            try {
                val o = JSON.parse<dynamic>(raw)
                st = Settings(
                    (o.freq ?: "due") as String,
                    (o.time ?: "08:00") as String,
                    (o.lang ?: 0) as Int,
                    (o.mode ?: "light") as String,
                    (o.theme ?: 0) as Int,
                    (o.cb ?: false) as Boolean,
                    (o.folder ?: "") as String
                )
            } catch (e: Throwable) {
            }
        }
    }
}
