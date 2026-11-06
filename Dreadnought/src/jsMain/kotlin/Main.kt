import kotlinx.browser.document
import kotlinx.browser.localStorage
import kotlinx.browser.window
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.set
import org.w3c.dom.*
import org.w3c.dom.events.Event
import org.w3c.files.Blob
import org.w3c.files.BlobPropertyBag
import org.w3c.files.File
import org.w3c.files.FileList
import org.w3c.files.FileReader
import kotlin.js.Date
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// ---------- state ----------
var tab = 0                 // 0 cards, 1 progress, 2 schedule, 3 settings
var mode = ""               // "", "delete", "move"
var openOpus: Opus? = null
var selDay = 0
var draft: Settings? = null
var dirHandle: dynamic = null
var saveTimer = 0

val lights = listOf(
    "Snow white" to "#FFFFFF", "Pastel blue" to "#DCEBFA", "Pastel pink" to "#FBE1EA",
    "Pastel green" to "#DDF3E0", "Pastel yellow" to "#FFF6CC", "Beige" to "#F3EAD8"
)
val darks = listOf(
    "AMOLED black" to "#000000", "Eigengrau" to "#16161D", "Dark purple" to "#1E1530", "Navy" to "#0F1B33",
    "Burgundy" to "#2E0B17", "Maroon" to "#2B0D0D", "Dark beige" to "#2B261C", "Walnut brown" to "#2A1F17"
)
val colors = listOf("#F28B82", "#FBBC74", "#F7E26B", "#81C995", "#78B9F2", "#C58AF9")
val cbColors = listOf("#E69F00", "#56B4E9", "#009E73", "#F0E442", "#0072B2", "#D55E00")
val symbols = listOf("●", "▲", "■", "◆", "★", "✚")

fun palette() = if (Store.st.cb) cbColors else colors

// ---------- dom helpers ----------
fun h(tag: String, cls: String = "", text: String = ""): HTMLElement {
    val e = document.createElement(tag) as HTMLElement
    if (cls.isNotEmpty()) e.className = cls
    if (text.isNotEmpty()) e.textContent = text
    return e
}

fun HTMLElement.add(vararg kids: HTMLElement?): HTMLElement {
    kids.forEach { if (it != null) appendChild(it) }
    return this
}

fun HTMLElement.onTap(f: () -> Unit): HTMLElement {
    addEventListener("click", { f() })
    return this
}

fun HTMLElement.stop(): HTMLElement {
    addEventListener("click", { it.stopPropagation() })
    return this
}

fun btn(label: String, cls: String, f: () -> Unit): HTMLElement = h("button", cls, label).onTap(f)

fun input(type: String, value: String): HTMLInputElement {
    val i = document.createElement("input") as HTMLInputElement
    i.type = type
    i.value = value
    return i
}

fun textarea(value: String): HTMLTextAreaElement {
    val t = document.createElement("textarea") as HTMLTextAreaElement
    t.value = value
    return t
}

fun field(label: String, e: HTMLElement): HTMLElement = h("label", "field").add(h("span", "lbl", label), e)

fun el(id: String) = document.getElementById(id) as HTMLElement

fun esc(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

fun pad(n: Int) = if (n < 10) "0$n" else "$n"
fun dayStr(d: Date) = "${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}"
fun todayIdx(): Int {
    val g = Date().getDay()
    return if (g == 0) 0 else g - 1
}

fun saveSoon() {
    window.clearTimeout(saveTimer)
    saveTimer = window.setTimeout({ Store.save() }, 500)
}

// ---------- layer ----------
var layerTimer = 0
var sink = 0
fun reflow(e: HTMLElement) { sink = e.offsetHeight }

fun layer(): HTMLElement {
    window.clearTimeout(layerTimer)
    val l = el("layer")
    l.onclick = null
    l.innerHTML = ""
    l.className = "layer"
    window.requestAnimationFrame {
        reflow(l)
        l.className = "layer on"
    }
    return l
}

fun closeLayer() {
    val l = el("layer")
    l.className = "layer"
    window.clearTimeout(layerTimer)
    layerTimer = window.setTimeout({ if (!l.classList.contains("on")) l.innerHTML = "" }, 320)
}

fun formSheet(rows: List<HTMLElement>, onSave: () -> Boolean) {
    val l = layer()
    val s = h("div", "sheet form").stop()
    rows.forEach { s.add(it) }
    s.add(
        h("div", "actions").add(
            btn(t("cancel"), "ghost") { closeLayer() },
            btn(t("save"), "solid") { if (onSave()) { closeLayer(); render() } }
        )
    )
    l.add(s)
}

// ---------- theme / notifications ----------
fun applyTheme(s: Settings) {
    val list = if (s.mode == "dark") darks else lights
    val c = list[s.theme.coerceIn(0, list.size - 1)].second
    val r = document.documentElement as HTMLElement
    r.style.setProperty("--bg", c)
    r.style.setProperty("--fg", if (s.mode == "dark") "#F1F1F1" else "#1A1A1A")
    r.style.setProperty("color-scheme", s.mode)
    document.querySelector("meta[name=theme-color]")?.setAttribute("content", c)
}

fun rememberTheme() {
    val st = Store.st
    val list = if (st.mode == "dark") darks else lights
    localStorage.setItem("m.bg", list[st.theme.coerceIn(0, list.size - 1)].second)
    localStorage.setItem("m.fg", if (st.mode == "dark") "#F1F1F1" else "#1A1A1A")
}

fun perm(): String =
    if (jsTypeOf(window.asDynamic().Notification) == "undefined") "denied"
    else window.asDynamic().Notification.permission as String

fun askPerm() {
    if (perm() == "default") window.asDynamic().Notification.requestPermission().then({ render() })
}

fun notify(body: String) {
    val sw = window.navigator.asDynamic().serviceWorker
    if (jsTypeOf(sw) == "undefined") return
    val o = obj()
    o.body = body
    o.icon = "icon-192.png"
    sw.ready.then({ reg: dynamic -> reg.showNotification(t("remTitle"), o) })
}

fun tick() {
    val s = Store.st
    if (s.freq == "off" || perm() != "granted") return
    val now = Date()
    val parts = s.time.split(":")
    val mins = (parts.getOrNull(0)?.toIntOrNull() ?: 8) * 60 + (parts.getOrNull(1)?.toIntOrNull() ?: 0)
    if (now.getHours() * 60 + now.getMinutes() < mins) return
    val today = dayStr(now)
    if (localStorage.getItem("m.last") == today) return
    val tomorrow = dayStr(Date(now.getTime() + 86400000.0))
    val due = Store.cards.filter {
        !it.done && when (s.freq) {
            "due" -> it.due.take(10) == today
            "d1" -> it.due.take(10) == tomorrow
            else -> true
        }
    }
    localStorage.setItem("m.last", today)
    if (due.isEmpty()) return
    notify(due.joinToString(", ") { it.subject })
}

// ---------- render ----------
lateinit var track: HTMLElement
lateinit var ind: HTMLElement
lateinit var dotsBtn: HTMLElement
lateinit var menuEl: HTMLElement
lateinit var subEl: HTMLElement
val panes = mutableListOf<HTMLElement>()
val navBtns = mutableListOf<HTMLElement>()
val navLabels = mutableListOf<HTMLElement>()
var menuOpen = false
val tabKeys = listOf("cards", "progress", "schedule", "settings")

val icons = listOf(
    """<svg viewBox="0 0 24 24"><rect x="4" y="8" width="16" height="11" rx="3"/><path d="M8 4.5h8"/></svg>""",
    """<svg viewBox="0 0 24 24"><path d="M3.5 8a2 2 0 0 1 2-2h4l2 2.5h7a2 2 0 0 1 2 2V17a2 2 0 0 1-2 2h-13a2 2 0 0 1-2-2z"/></svg>""",
    """<svg viewBox="0 0 24 24"><rect x="4" y="5.5" width="16" height="14" rx="3"/><path d="M4 10h16M8.5 3.5v4M15.5 3.5v4"/></svg>""",
    """<svg viewBox="0 0 24 24"><path d="M4 8h9M17 8h3M4 16h3M11 16h9"/><circle cx="15" cy="8" r="2"/><circle cx="9" cy="16" r="2"/></svg>"""
)
val dotsSvg = """<svg viewBox="0 0 24 24"><circle cx="5" cy="12" r="1.9"/><circle cx="12" cy="12" r="1.9"/><circle cx="19" cy="12" r="1.9"/></svg>"""

// pager moves toward -x in LTR and +x in RTL
fun dirSign(): Double = if (lang >= 4) 1.0 else -1.0

fun initChrome() {
    subEl = h("div", "sub")
    el("top").add(h("div", "brand", "Memorius"), subEl)

    track = el("track")
    for (i in 0..3) {
        val p = h("section", "pane")
        track.add(p)
        panes.add(p)
    }

    val d = el("dock")
    val pill = h("nav", "pill")
    ind = h("div", "ind")
    pill.add(ind)
    for (i in 0..3) {
        val b = h("button", "nav")
        b.innerHTML = icons[i] + "<span></span>"
        navLabels.add(b.querySelector("span") as HTMLElement)
        b.onTap { go(i) }
        navBtns.add(b)
        pill.add(b)
    }
    dotsBtn = h("button", "dots").onTap { toggleMenu() }
    dotsBtn.innerHTML = dotsSvg
    dotsBtn.setAttribute("aria-label", "menu")
    menuEl = h("div", "menu")
    d.add(pill, dotsBtn, menuEl)

    el("scrim").onTap { closeMenu() }
    initSwipe()
}

// p = pager position as a float tab index (0..3); keeps pager, pill indicator and labels in sync
fun syncPos(p: Double) {
    track.style.transform = "translateX(${dirSign() * p * 100}%)"
    ind.style.transform = "translateX(${-dirSign() * p * 100}%)"
    val near = p.roundToInt().coerceIn(0, 3)
    navBtns.forEachIndexed { i, b ->
        if (i == near) b.classList.add("on") else b.classList.remove("on")
    }
}

fun initSwipe() {
    val pg = el("pager")
    val dk = el("dock")
    var sx = 0.0
    var sy = 0.0
    var dx = 0.0
    var lastDx = 0.0
    var lastT = 0.0
    var vx = 0.0
    var active = false
    var locked = false
    var swiped = false

    fun stopDrag() {
        locked = false
        pg.classList.remove("drag")
        dk.classList.remove("drag")
    }

    pg.addEventListener("pointerdown", { e ->
        val d = e.asDynamic()
        val tg = d.target as Element
        active = openOpus == null && tg.closest("input,textarea,select") == null
        locked = false
        sx = d.clientX as Double
        sy = d.clientY as Double
        dx = 0.0
        lastDx = 0.0
        vx = 0.0
        lastT = Date().getTime()
    })

    pg.addEventListener("pointermove", { e ->
        if (active) {
            val d = e.asDynamic()
            dx = (d.clientX as Double) - sx
            val dy = (d.clientY as Double) - sy
            if (!locked && abs(dx) > 10 && abs(dx) > abs(dy) * 1.2) {
                locked = true
                pg.classList.add("drag")
                dk.classList.add("drag")
                pg.asDynamic().setPointerCapture(d.pointerId)
            }
            if (locked) {
                val now = Date().getTime()
                if (now > lastT) {
                    vx = (dx - lastDx) / (now - lastT)
                    lastT = now
                    lastDx = dx
                }
                val w = pg.clientWidth.toDouble()
                var p = tab + dx * dirSign() / w
                if (p < 0) p *= 0.3
                if (p > 3) p = 3 + (p - 3) * 0.3
                syncPos(p)
            }
        }
    })

    pg.addEventListener("pointerup", {
        if (active && locked) {
            val w = pg.clientWidth.toDouble()
            val flick = abs(vx) > 0.45 && abs(dx) > 24
            var target = tab
            if (abs(dx) > w * 0.25 || flick) {
                val dir = if (flick) vx else dx
                target = if (dir * dirSign() > 0) tab + 1 else tab - 1
            }
            target = target.coerceIn(0, 3)
            stopDrag()
            swiped = true
            window.setTimeout({ swiped = false }, 120)
            if (target != tab) go(target) else syncPos(tab.toDouble())
        }
        active = false
    })

    pg.addEventListener("pointercancel", {
        if (active && locked) {
            stopDrag()
            syncPos(tab.toDouble())
        }
        active = false
    })

    // a swipe must never count as a tap
    pg.addEventListener("click", { e -> if (swiped) e.stopPropagation() }, true)
}

fun render() {
    lang = Store.st.lang
    applyTheme(draft ?: Store.st)
    val root = document.documentElement as HTMLElement
    root.setAttribute("lang", langCodes[lang])
    root.setAttribute("dir", if (lang >= 4) "rtl" else "ltr")

    tabKeys.forEachIndexed { i, k -> navLabels[i].textContent = t(k) }
    val title = t(tabKeys[tab])
    if (subEl.textContent != title) {
        subEl.textContent = title
        subEl.classList.remove("in")
        reflow(subEl)
        subEl.classList.add("in")
    }

    panes.forEachIndexed { i, p ->
        p.innerHTML = ""
        when (i) {
            0 -> renderCards(p)
            1 -> renderProgress(p)
            2 -> renderSchedule(p)
            else -> renderSettings(p)
        }
        if (i == tab) p.removeAttribute("inert") else p.setAttribute("inert", "")
    }

    val dk = el("dock")
    if (tab == 3) dk.classList.add("merged") else dk.classList.remove("merged")
    buildBanner()
    syncPos(tab.toDouble())
}

fun go(i: Int) {
    closeMenu()
    tab = i
    mode = ""
    openOpus = null
    draft = Store.st.copy()
    render()
}

fun buildBanner() {
    val b = el("banner")
    val dr = draft
    if (tab == 3 && dr != null && dr != Store.st) {
        b.innerHTML = ""
        b.add(
            h("span", "", t("applyAsk")),
            h("div", "btns").add(
                btn(t("cancel"), "") { draft = Store.st.copy(); render() },
                btn(t("apply"), "") {
                    Store.st = dr.copy()
                    Store.save()
                    rememberTheme()
                    draft = Store.st.copy()
                    render()
                }
            )
        )
        b.className = "banner on"
    } else if (mode != "") {
        b.innerHTML = ""
        b.add(h("span", "", t(if (mode == "delete") "tapDel" else "tapMove")), btn(t("ok"), "") { mode = ""; render() })
        b.className = "banner on"
    } else {
        b.className = "banner"
    }
}

fun closeMenu() {
    if (!menuOpen) return
    menuOpen = false
    menuEl.classList.remove("open")
    dotsBtn.classList.remove("open")
    el("scrim").classList.remove("on")
}

fun toggleMenu() {
    if (menuOpen) { closeMenu(); return }
    if (tab == 3 || openOpus != null) return
    val items = mutableListOf<Pair<String, () -> Unit>>()
    when (tab) {
        0 -> items.add(t("add") to { addCard() })
        1 -> {
            items.add(t("write") to { writeOpus() })
            items.add(t("import") to { importOpus() })
        }
        else -> items.add(t("add") to { addSlot() })
    }
    items.add(t("delete") to { mode = "delete"; render() })
    items.add(t("rearrange") to { mode = "move"; render() })
    menuEl.innerHTML = ""
    items.forEach { (label, f) -> menuEl.add(btn(label, "item") { closeMenu(); f() }) }
    menuOpen = true
    menuEl.classList.add("open")
    dotsBtn.classList.add("open")
    el("scrim").classList.add("on")
}

fun <T> moveBtns(list: MutableList<T>, i: Int, rev: Boolean, same: (T, T) -> Boolean = { _, _ -> true }): HTMLElement {
    fun mv(d: Int) {
        var j = i + d
        while (j in list.indices && !same(list[i], list[j])) j += d
        if (j in list.indices) {
            val x = list[i]
            list[i] = list[j]
            list[j] = x
            Store.save()
            render()
        }
    }
    return h("div", "moves").add(
        btn("↑", "mv") { mv(if (rev) 1 else -1) },
        btn("↓", "mv") { mv(if (rev) -1 else 1) }
    )
}

// ---------- cards ----------
fun fmtDue(c: Card): String {
    if (c.due.isEmpty()) return "—"
    return (if (c.type == "prepping") t("date") else t("deadline")) + ": " + c.due.replace("T", " ")
}

fun renderCards(scr: HTMLElement) {
    if (Store.cards.isEmpty()) {
        scr.add(h("div", "empty", t("empty")))
        return
    }
    val stack = h("div", "stack")
    Store.cards.forEachIndexed { i, c -> stack.add(cardView(c, i)) }
    scr.add(stack)
    if (mode == "") scr.add(h("div", "hint", t("holdHint")))
}

fun cardHead(c: Card, ci: Int): HTMLElement {
    val head = h("div", "row")
    if (Store.st.cb) head.add(h("span", "sym", symbols[ci]))
    head.add(h("b", "", c.subject), h("span", "tag", t(c.type)))
    return head
}

fun cardView(c: Card, i: Int): HTMLElement {
    val ci = c.color.coerceIn(0, 5)
    val v = h("div", if (c.done) "card done" else "card")
    v.style.setProperty("--c", palette()[ci])
    val head = cardHead(c, ci)
    when (mode) {
        "delete" -> v.onTap {
            if (window.confirm(t("confirmDel"))) { Store.cards.remove(c); Store.save(); render() }
        }
        "move" -> head.add(moveBtns(Store.cards, i, true))
        else -> {
            var fired = false
            head.add(btn("✓", "check") { c.done = !c.done; Store.save(); render() }.stop())
            v.addEventListener("pointerdown", { fired = false })
            longPress(v) { fired = true; openCardView(c, v, false) }
            v.onTap { if (fired) fired = false else openCardView(c, v, true) }
        }
    }
    v.add(head, h("div", "due", fmtDue(c)))
    return v
}

fun longPress(v: HTMLElement, f: () -> Unit) {
    var timer = 0
    var sx = 0.0
    var sy = 0.0
    v.addEventListener("pointerdown", { e ->
        val d = e.asDynamic()
        sx = d.clientX as Double
        sy = d.clientY as Double
        window.clearTimeout(timer)
        timer = window.setTimeout({ f() }, 450)
    })
    v.addEventListener("pointermove", { e ->
        val d = e.asDynamic()
        if (abs((d.clientX as Double) - sx) > 8 || abs((d.clientY as Double) - sy) > 8) window.clearTimeout(timer)
    })
    val cancel: (Event) -> Unit = { window.clearTimeout(timer) }
    v.addEventListener("pointerup", cancel)
    v.addEventListener("pointerleave", cancel)
    v.addEventListener("pointercancel", cancel)
    v.addEventListener("contextmenu", { it.preventDefault() })
}

fun fileLink(f: FileRef): HTMLElement {
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = f.data
    a.download = f.name
    a.className = "file"
    a.textContent = f.name
    return a
}

// The expanded view and the preview both grow out of the tapped card itself.
fun openCardView(c: Card, v: HTMLElement, full: Boolean) {
    val r = v.getBoundingClientRect()
    val l = layer()
    val ci = c.color.coerceIn(0, 5)
    val x = h("div", "xcard")
    x.style.setProperty("--c", palette()[ci])
    x.add(cardHead(c, ci), h("div", "due", fmtDue(c)))
    val extra = h("div", "extra")
    extra.add(h("p", if (full) "note" else "note clamp", if (c.note.isEmpty()) "—" else c.note))
    if (c.files.isNotEmpty()) {
        if (full) {
            extra.add(h("div", "lbl", t("files")))
            c.files.forEach { extra.add(fileLink(it)) }
        } else {
            extra.add(h("div", "due", t("files") + ": " + c.files.size))
        }
    }
    x.add(extra)

    // start exactly on top of the original card
    x.style.left = "${r.left}px"
    x.style.width = "${r.width}px"
    x.style.top = "${r.top}px"
    l.add(x)
    val vh = window.innerHeight.toDouble()
    val th = min(max(x.offsetHeight.toDouble(), r.height), vh - 48.0)
    x.style.height = "${r.height}px"
    reflow(x)
    val top = max(24.0, (vh - th) / 2)
    v.style.visibility = "hidden"
    window.requestAnimationFrame {
        x.style.top = "${top}px"
        x.style.height = "${th}px"
        x.classList.add("lift")
    }

    fun close() {
        x.classList.remove("lift")
        val r2 = v.getBoundingClientRect()
        x.style.top = "${r2.top}px"
        x.style.height = "${r2.height}px"
        closeLayer()
        window.setTimeout({ v.style.visibility = "" }, 300)
    }
    l.onclick = { close() }
    x.addEventListener("click", { e ->
        e.stopPropagation()
        if ((e.asDynamic().target as Element).closest("a") == null) close()
    })
}

fun readFiles(list: FileList?, into: MutableList<FileRef>, done: () -> Unit) {
    if (list == null) return
    for (i in 0 until list.length) {
        val f = list.item(i) ?: continue
        if (f.size.toInt() > 2000000) { window.alert(t("tooBig")); continue }
        val r = FileReader()
        r.onload = { into.add(FileRef(f.name, r.result as String)); done() }
        r.readAsDataURL(f)
    }
}

fun addCard() {
    var type = "homework"
    var color = 0
    val files = mutableListOf<FileRef>()
    val subj = input("text", "")
    subj.placeholder = t("subject")
    val due = input("datetime-local", "")
    val note = textarea("")
    note.placeholder = t("note")
    val typeRow = h("div", "seg")
    val colorRow = h("div", "swatches")
    val fileList = h("div", "files")

    fun paint() {
        typeRow.innerHTML = ""
        listOf("homework", "exam", "prepping").forEach { k ->
            typeRow.add(btn(t(k), if (k == type) "on" else "") {
                type = k
                val v = due.value
                due.type = if (k == "prepping") "date" else "datetime-local"
                due.value = when {
                    v.isEmpty() -> ""
                    k == "prepping" -> v.take(10)
                    else -> v.take(10) + "T" + (if (v.length > 10) v.substring(11, 16) else "23:59")
                }
                paint()
            })
        }
        colorRow.innerHTML = ""
        palette().forEachIndexed { i, col ->
            val b = btn(if (Store.st.cb) symbols[i] else "", if (i == color) "sw on" else "sw") { color = i; paint() }
            b.style.background = col
            colorRow.add(b)
        }
        fileList.innerHTML = ""
        files.forEach { fileList.add(h("div", "file", it.name)) }
    }
    paint()

    val fi = input("file", "")
    fi.multiple = true
    fi.style.display = "none"
    fi.addEventListener("change", { readFiles(fi.files, files) { paint() } })
    val drop = h("div", "drop", t("drop")).onTap { fi.click() }
    drop.addEventListener("dragover", { it.preventDefault() })
    drop.addEventListener("drop", { e ->
        e.preventDefault()
        readFiles((e as DragEvent).dataTransfer?.files, files) { paint() }
    })

    formSheet(
        listOf(
            field(t("subject"), subj), typeRow, field(t("deadline"), due),
            h("div", "field").add(h("span", "lbl", t("color")), colorRow),
            field(t("note"), note),
            h("div", "field").add(h("span", "lbl", t("files")), drop, fi, fileList)
        )
    ) {
        val sj = subj.value.trim()
        if (sj.isEmpty()) {
            subj.focus()
            false
        } else {
            Store.cards.add(Card(newId(), sj, due.value, color, type, note.value, files, false))
            Store.save()
            true
        }
    }
}

// ---------- progress ----------
fun renderProgress(scr: HTMLElement) {
    val cur = openOpus
    if (cur != null) { renderOpus(scr, cur); return }
    if (Store.opus.isEmpty()) {
        scr.add(h("div", "empty", t("empty")))
        return
    }
    val list = h("div", "list")
    Store.opus.forEachIndexed { i, op -> list.add(opusRow(op, i)) }
    scr.add(list)
}

fun opusRow(op: Opus, i: Int): HTMLElement {
    val r = h("div", "row2")
    val top = h("div", "row")
    top.add(h("b", "", op.title), h("span", "tag", op.ext.uppercase()))
    val fill = h("i")
    fill.style.width = "${op.progress}%"
    when (mode) {
        "delete" -> r.onTap {
            if (window.confirm(t("confirmDel"))) { Store.opus.remove(op); Store.save(); render() }
        }
        "move" -> top.add(moveBtns(Store.opus, i, false))
        else -> r.onTap { openOpus = op; render() }
    }
    r.add(top, h("div", "bar").add(fill), h("div", "due", "${op.progress}%"))
    return r
}

fun renderOpus(scr: HTMLElement, op: Opus) {
    scr.add(h("div", "bar2").add(btn("← " + t("back"), "ghost") { openOpus = null; render() }, h("b", "", op.title)))
    val rng = input("range", "")
    rng.min = "0"
    rng.max = "100"
    rng.step = "5"
    rng.value = op.progress.toString()
    val pct = h("span", "due", "${op.progress}%")
    rng.addEventListener("input", {
        op.progress = rng.value.toInt()
        pct.textContent = "${op.progress}%"
        saveSoon()
    })
    scr.add(h("div", "row").add(rng, pct))
    if (op.kind == "pdf") {
        val box = h("div", "pdf")
        scr.add(box)
        renderPdf(op.content, box)
        scr.add(h("div", "exp").add(btn(t("download"), "ghost") { saveData(op.title + ".pdf", op.content) }))
    } else {
        val ta = textarea(op.content)
        ta.className = "editor"
        ta.addEventListener("input", { op.content = ta.value; saveSoon() })
        scr.add(ta)
        scr.add(
            h("div", "exp").add(
                btn("TXT", "ghost") { exportOpus(op, "txt") },
                btn("DOC", "ghost") { exportOpus(op, "doc") },
                btn("PDF", "ghost") { exportOpus(op, "pdf") }
            )
        )
    }
}

fun renderPdf(dataUrl: String, box: HTMLElement) {
    val lib = window.asDynamic().pdfjsLib
    if (jsTypeOf(lib) == "undefined") { box.textContent = t("cantRead"); return }
    lib.GlobalWorkerOptions.workerSrc = "https://cdnjs.cloudflare.com/ajax/libs/pdf.js/3.11.174/pdf.worker.min.js"
    val bin = window.atob(dataUrl.substringAfter(","))
    val bytes = Uint8Array(bin.length)
    for (i in 0 until bin.length) bytes[i] = bin[i].code.toByte()
    val p = obj()
    p.data = bytes
    lib.getDocument(p).promise.then({ pdf: dynamic ->
        val n = pdf.numPages as Int
        for (pg in 1..n) {
            val cv = document.createElement("canvas") as HTMLCanvasElement
            box.appendChild(cv)
            pdf.getPage(pg).then({ page: dynamic ->
                val so = obj()
                so.scale = 1.5
                val vp = page.getViewport(so)
                cv.width = (vp.width as Double).toInt()
                cv.height = (vp.height as Double).toInt()
                val rp = obj()
                rp.canvasContext = cv.getContext("2d")
                rp.viewport = vp
                page.render(rp)
            })
        }
    }, { e: dynamic -> box.textContent = t("cantRead") })
}

fun writeOpus() {
    var ext = "txt"
    val ti = input("text", "")
    ti.placeholder = t("title")
    val seg = h("div", "seg")
    fun paint() {
        seg.innerHTML = ""
        listOf("txt", "doc", "pdf").forEach { k ->
            seg.add(btn(k.uppercase(), if (k == ext) "on" else "") { ext = k; paint() })
        }
    }
    paint()
    formSheet(listOf(field(t("title"), ti), seg)) {
        val tt = ti.value.trim()
        if (tt.isEmpty()) {
            ti.focus()
            false
        } else {
            val o = Opus(newId(), tt, ext, "text", "", 0)
            Store.opus.add(o)
            openOpus = o
            Store.save()
            true
        }
    }
}

fun importOpus() {
    val fi = input("file", "")
    fi.accept = ".pdf,.txt,.doc,.docx"
    fi.style.display = "none"
    document.body?.appendChild(fi)
    fi.addEventListener("change", {
        val f = fi.files?.item(0)
        if (f != null) readOpus(f)
        document.body?.removeChild(fi)
    })
    fi.click()
}

fun readOpus(f: File) {
    if (f.size.toInt() > 2000000) { window.alert(t("tooBig")); return }
    val name = f.name
    val dot = name.lastIndexOf('.')
    val base = if (dot > 0) name.substring(0, dot) else name
    val ext = if (dot > 0) name.substring(dot + 1).lowercase() else "txt"
    val r = FileReader()
    fun finish(kind: String, e2: String, content: String) {
        val o = Opus(newId(), base, e2, kind, content, 0)
        Store.opus.add(o)
        openOpus = o
        Store.save()
        render()
    }
    when (ext) {
        "pdf" -> {
            r.onload = { finish("pdf", "pdf", r.result as String) }
            r.readAsDataURL(f)
        }
        "docx" -> {
            r.onload = {
                val o = obj()
                o.arrayBuffer = r.result
                window.asDynamic().mammoth.extractRawText(o).then({ x: dynamic -> finish("text", "doc", x.value as String) })
            }
            r.readAsArrayBuffer(f)
        }
        else -> {
            r.onload = {
                var txt = r.result as String
                if (txt.contains("\u0000")) {
                    window.alert(t("cantRead"))
                } else {
                    if (ext == "doc" && txt.lowercase().contains("<body")) {
                        val d = document.implementation.createHTMLDocument("")
                        (d.body as HTMLElement).innerHTML = txt
                        txt = d.body?.textContent ?: ""
                    }
                    finish("text", if (ext == "doc") "doc" else "txt", txt)
                }
            }
            r.readAsText(f)
        }
    }
}

fun download(name: String, blob: Blob) {
    val url = window.asDynamic().URL.createObjectURL(blob) as String
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = url
    a.download = name
    a.click()
}

fun saveData(name: String, data: String) {
    val a = document.createElement("a") as HTMLAnchorElement
    a.href = data
    a.download = name
    a.click()
}

fun saveBlob(name: String, mime: String, text: String) {
    val blob = Blob(arrayOf<dynamic>(text), BlobPropertyBag(mime))
    val dir = dirHandle
    if (dir != null) {
        val o = obj()
        o.create = true
        dir.getFileHandle(name, o)
            .then({ fh: dynamic -> fh.createWritable() })
            .then({ w: dynamic -> w.write(blob).then({ w.close() }) })
            .then({ null }, { e: dynamic -> download(name, blob) })
        return
    }
    download(name, blob)
}

fun exportOpus(op: Opus, ext: String) {
    when (ext) {
        "txt" -> saveBlob(op.title + ".txt", "text/plain", op.content)
        "doc" -> saveBlob(
            op.title + ".doc", "application/msword",
            "<html><head><meta charset=\"utf-8\"></head><body>" + esc(op.content).replace("\n", "<br>") + "</body></html>"
        )
        else -> {
            val w: dynamic = window.open("", "_blank")
            if (w != null) {
                w.document.write(
                    "<html><head><meta charset=\"utf-8\"><title>" + esc(op.title) +
                        "</title></head><body style=\"font:14pt/1.5 serif;white-space:pre-wrap\">" + esc(op.content) + "</body></html>"
                )
                w.document.close()
                w.focus()
                w.print()
            }
        }
    }
}

// ---------- schedule ----------
fun renderSchedule(scr: HTMLElement) {
    val today = todayIdx()
    val tabs = h("div", "days")
    dayNames[lang].forEachIndexed { i, n ->
        val cls = "day" + (if (i == selDay) " on" else "") + (if (i == today) " today" else "")
        tabs.add(btn(n, cls) { selDay = i; render() })
    }
    scr.add(tabs)
    val list = h("div", "list")
    var any = false
    Store.slots.forEachIndexed { i, s ->
        if (s.day == selDay) { any = true; list.add(slotRow(s, i)) }
    }
    if (!any) list.add(h("div", "empty", t("nul")))
    scr.add(list)
}

fun slotRow(s: Slot, i: Int): HTMLElement {
    val r = h("div", "row2")
    val top = h("div", "row")
    if (s.time.isNotEmpty()) top.add(h("span", "tag", s.time))
    top.add(h("b", "", s.subject))
    when (mode) {
        "delete" -> r.onTap {
            if (window.confirm(t("confirmDel"))) { Store.slots.remove(s); Store.save(); render() }
        }
        "move" -> top.add(moveBtns(Store.slots, i, false) { a, b -> a.day == b.day })
    }
    r.add(top)
    if (s.note.isNotEmpty()) r.add(h("div", "due", s.note))
    return r
}

fun addSlot() {
    val sel = mutableSetOf(selDay)
    val days = h("div", "days")
    val subj = input("text", "")
    subj.placeholder = t("subject")
    val tm = input("time", "")
    val nt = input("text", "")
    nt.placeholder = t("note")
    fun paint() {
        days.innerHTML = ""
        dayNames[lang].forEachIndexed { i, n ->
            days.add(btn(n, if (i in sel) "day on" else "day") {
                if (i in sel) { if (sel.size > 1) sel.remove(i) } else sel.add(i)
                paint()
            })
        }
    }
    paint()
    formSheet(listOf(days, field(t("subject"), subj), field(t("time"), tm), field(t("note"), nt))) {
        val sj = subj.value.trim()
        if (sj.isEmpty()) {
            subj.focus()
            false
        } else {
            sel.sorted().forEach { Store.slots.add(Slot(newId(), it, sj, tm.value, nt.value)) }
            selDay = sel.sorted().first()
            Store.save()
            true
        }
    }
}

// ---------- settings ----------
fun group(title: String, kids: List<HTMLElement>): HTMLElement {
    val g = h("section", "group")
    g.add(h("div", "lbl", title))
    kids.forEach { g.add(it) }
    return g
}

fun pickFolder(d: Settings) {
    val w = window.asDynamic()
    if (jsTypeOf(w.showDirectoryPicker) == "undefined") { window.alert(t("noFolder")); return }
    w.showDirectoryPicker().then({ dh: dynamic ->
        dirHandle = dh
        d.folder = dh.name as String
        render()
    }, { e: dynamic -> null })
}

fun renderSettings(scr: HTMLElement) {
    val d = draft ?: return

    // notifications
    val n = mutableListOf<HTMLElement>()
    listOf("off" to "off", "due" to "onDue", "d1" to "dMinus1", "daily" to "daily").forEach { (k, key) ->
        n.add(btn(t(key), if (d.freq == k) "opt on" else "opt") {
            d.freq = k
            if (k != "off") askPerm()
            render()
        })
    }
    if (d.freq != "off") {
        if (perm() == "default") n.add(btn(t("allow"), "ghost") { askPerm() })
        if (perm() == "denied") n.add(h("div", "due", t("notifDenied")))
    }
    val ti = input("time", d.time)
    ti.addEventListener("change", { d.time = ti.value; render() })
    n.add(field(t("remTime"), ti))
    scr.add(group(t("notifFreq"), n))

    // language
    val sel = document.createElement("select") as HTMLSelectElement
    langNames.forEachIndexed { i, name ->
        val o = document.createElement("option") as HTMLOptionElement
        o.value = i.toString()
        o.text = name
        o.selected = i == d.lang
        sel.appendChild(o)
    }
    sel.addEventListener("change", { d.lang = sel.value.toInt(); render() })
    scr.add(group(t("language"), listOf(sel)))

    // appearance
    val modeSeg = h("div", "seg")
    listOf("light", "dark").forEach { m ->
        modeSeg.add(btn(t(m), if (d.mode == m) "on" else "") { d.mode = m; d.theme = 0; render() })
    }
    val list = if (d.mode == "dark") darks else lights
    val sw = h("div", "swatches")
    list.forEachIndexed { i, p ->
        val b = btn("", if (i == d.theme) "sw on" else "sw") { d.theme = i; render() }
        b.style.background = p.second
        b.setAttribute("title", p.first)
        sw.add(b)
    }
    val themeName = list[d.theme.coerceIn(0, list.size - 1)].first
    scr.add(group(t("appearance"), listOf(modeSeg, sw, h("div", "due", themeName))))

    // colorblind
    scr.add(
        group(
            t("colorblind"),
            listOf(h("div", "seg").add(
                btn(t("off"), if (!d.cb) "on" else "") { d.cb = false; render() },
                btn(t("on"), if (d.cb) "on" else "") { d.cb = true; render() }
            ))
        )
    )

    // storage
    scr.add(
        group(
            t("storage"),
            listOf(
                h("div", "due", if (d.folder.isEmpty()) "—" else d.folder),
                btn(t("chooseFolder"), "ghost") { pickFolder(d) }
            )
        )
    )
}

// ---------- start ----------
// Splash: brand mark only. Leaves as soon as the app is ready, but not before ~0.9 s
// have passed since launch (counted from page start, so no extra delay on slow starts).
fun hideSplash() {
    val sp = document.getElementById("splash") ?: return
    val wait = max(0.0, 900.0 - window.performance.now())
    window.setTimeout({
        sp.classList.add("out")
        window.setTimeout({ sp.parentNode?.removeChild(sp) }, 450)
    }, wait.toInt())
}

fun main() {
    Store.load()
    lang = Store.st.lang
    selDay = todayIdx()
    draft = Store.st.copy()
    rememberTheme()
    initChrome()
    render()
    val nav = window.navigator.asDynamic()
    if (jsTypeOf(nav.serviceWorker) != "undefined") nav.serviceWorker.register("sw.js")
    tick()
    window.setInterval({ tick() }, 60000)
    hideSplash()
}
