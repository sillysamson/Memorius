package com.memorius.app;

import java.util.HashMap;
import java.util.Map;

// GENERATED text table (see strings_data.py in the project notes). Indexes follow NAMES.
// Dialect languages (pirate, lolcat, jamaican, shakespearean) use a few hand-written labels
// and Dialects.java for everything else.
final class Str {
    static int lang = 0;

    static final String[] NAMES = {"English (US)", "English (UK)", "Español", "Français", "Bahasa Indonesia",
            "中文（繁體）", "中文（简体）", "العربية", "עברית", "Deutsch", "Nederlands",
            "Pirate Speak", "LOLCAT", "Jamaican English", "Shakespearean English"};

    // old saved language numbers (before the 15-language list) -> new numbers
    static final int[] MIGRATE = {0, 1, 4, 3, 7, 8};

    static final int AR = 7, HE = 8, FIRST_DIALECT = 11;

    static final String[][] DAYS = {
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"},
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"},
            {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb"},
            {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"},
            {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab"},
            {"週一", "週二", "週三", "週四", "週五", "週六"},
            {"周一", "周二", "周三", "周四", "周五", "周六"},
            {"الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"},
            {"ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳"},
            {"Mo", "Di", "Mi", "Do", "Fr", "Sa"},
            {"Ma", "Di", "Wo", "Do", "Vr", "Za"},
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"},
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"},
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"},
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"}
    };

    private static final Map<String, String[]> M = new HashMap<>();
    private static final Map<String, String> CACHE = new HashMap<>();

    private static void p(String k, String... v) {
        M.put(k, v);
    }

    static {
        p("cards", "Cards", "Cards", "Tarjetas", "Cartes", "Kartu", "卡片", "卡片", "البطاقات", "כרטיסים", "Karten", "Kaarten", "Plunder", "cardz", "Cards", "Tablets");
        p("progress", "Projects", "Projects", "Proyectos", "Projets", "Proyek", "專案", "项目", "المشاريع", "פרויקטים", "Projekte", "Projecten", "Voyages", "projekts", "Projeck dem", "Endeavours");
        p("schedule", "Schedule", "Schedule", "Horario", "Planning", "Jadwal", "課表", "课表", "الجدول", "מערכת", "Stundenplan", "Rooster", "Tide Chart", "skedjool", "Di Plan", "Almanac");
        p("settings", "Settings", "Settings", "Ajustes", "Réglages", "Setelan", "設定", "设置", "الإعدادات", "הגדרות", "Einstellungen", "Instellingen", "Ship's Rigging", "settinz", "Settins", "Ordinances");
        p("add", "Add", "Add", "Añadir", "Ajouter", "Tambah", "新增", "添加", "إضافة", "הוספה", "Hinzufügen", "Toevoegen", "Hoist", "add plz", "Add it", "Append");
        p("delete", "Delete", "Delete", "Eliminar", "Supprimer", "Hapus", "刪除", "删除", "حذف", "מחיקה", "Löschen", "Verwijderen", "Scuttle", "nom nom", "Dash weh", "Smite");
        p("rearrange", "Rearrange", "Rearrange", "Reordenar", "Réorganiser", "Atur ulang", "重新排序", "重新排序", "إعادة ترتيب", "סידור מחדש", "Neu anordnen", "Herschikken", null, null, null, null);
        p("save", "Save", "Save", "Guardar", "Enregistrer", "Simpan", "儲存", "保存", "حفظ", "שמירה", "Speichern", "Opslaan", "Stow", "kthxbai", "Save it", "Preserve");
        p("cancel", "Cancel", "Cancel", "Cancelar", "Annuler", "Batal", "取消", "取消", "إلغاء", "ביטול", "Abbrechen", "Annuleren", "Belay", "nope", "Cancel", "Forsooth, nay");
        p("apply", "Apply", "Apply", "Aplicar", "Appliquer", "Terapkan", "套用", "应用", "تطبيق", "החלה", "Anwenden", "Toepassen", "Aye aye", "yes plz", "Do it", "So be it");
        p("applyAsk", "Accept changes?", "Accept changes?", "¿Aceptar los cambios?", "Accepter les modifications ?", "Terima perubahan?", "要接受變更嗎？", "接受更改？", "قبول التغييرات؟", "לאשר שינויים?", "Änderungen übernehmen?", "Wijzigingen accepteren?", null, null, null, null);
        p("subject", "Subject", "Subject", "Asignatura", "Matière", "Mata pelajaran", "科目", "科目", "المادة", "מקצוע", "Fach", "Vak", null, null, null, null);
        p("deadline", "Deadline", "Deadline", "Fecha límite", "Échéance", "Tenggat", "截止時間", "截止时间", "الموعد النهائي", "מועד הגשה", "Frist", "Deadline", null, null, null, null);
        p("date", "Date", "Date", "Fecha", "Date", "Tanggal", "日期", "日期", "التاريخ", "תאריך", "Datum", "Datum", null, null, null, null);
        p("type", "Type", "Type", "Tipo", "Type", "Jenis", "類型", "类型", "النوع", "סוג", "Typ", "Type", null, null, null, null);
        p("homework", "Homework", "Homework", "Tarea", "Devoir", "PR", "作業", "作业", "واجب", "שיעורי בית", "Hausaufgabe", "Huiswerk", null, null, null, null);
        p("exam", "Exam", "Exam", "Examen", "Examen", "Ujian", "考試", "考试", "امتحان", "מבחן", "Prüfung", "Toets", null, null, null, null);
        p("prepping", "Prepping", "Prepping", "Preparación", "Préparation", "Persiapan", "準備", "准备", "تحضير", "הכנה", "Vorbereitung", "Voorbereiding", null, null, null, null);
        p("note", "Note", "Note", "Nota", "Note", "Catatan", "備註", "备注", "ملاحظة", "הערה", "Notiz", "Notitie", null, null, null, null);
        p("files", "Files", "Files", "Archivos", "Fichiers", "File", "檔案", "文件", "الملفات", "קבצים", "Dateien", "Bestanden", null, null, null, null);
        p("drop", "Drop files here or tap to add", "Drop files here or tap to add", "Suelta archivos aquí o toca para añadir", "Déposez des fichiers ici ou touchez pour ajouter", "Seret file ke sini atau ketuk untuk menambah", "將檔案拖放到此處，或點按新增", "将文件拖放到此处，或点按添加", "أسقط الملفات هنا أو اضغط للإضافة", "שחררו קבצים כאן או הקישו להוספה", "Dateien hier ablegen oder zum Hinzufügen tippen", "Sleep bestanden hierheen of tik om toe te voegen", null, null, null, null);
        p("color", "Color", "Colour", "Color", "Couleur", "Warna", "顏色", "颜色", "اللون", "צבע", "Farbe", "Kleur", null, null, null, null);
        p("empty", "Nothing here yet", "Nothing here yet", "Aún no hay nada", "Rien pour l'instant", "Belum ada apa-apa", "這裡還沒有內容", "这里还没有内容", "لا شيء بعد", "עדיין אין כלום", "Noch nichts da", "Nog niets hier", null, null, null, null);
        p("holdHint", "Hold a card to preview", "Hold a card to preview", "Mantén pulsada una tarjeta para previsualizar", "Maintenez une carte pour l'aperçu", "Tahan kartu untuk pratinjau", "長按卡片即可預覽", "长按卡片即可预览", "اضغط مطولًا على البطاقة للمعاينة", "לחצו לחיצה ארוכה על כרטיס לתצוגה מקדימה", "Karte gedrückt halten für Vorschau", "Houd een kaart ingedrukt voor een voorbeeld", null, null, null, null);
        p("off", "Off", "Off", "Desactivado", "Désactivé", "Mati", "關閉", "关闭", "إيقاف", "כבוי", "Aus", "Uit", null, null, null, null);
        p("on", "On", "On", "Activado", "Activé", "Aktif", "開啟", "开启", "تشغيل", "פעיל", "An", "Aan", null, null, null, null);
        p("onDue", "On due day", "On due day", "El día de entrega", "Le jour de l'échéance", "Pada hari tenggat", "截止當天", "截止当天", "في يوم الموعد", "ביום ההגשה", "Am Fälligkeitstag", "Op de deadline", null, null, null, null);
        p("daily", "Every day until done", "Every day until done", "Cada día hasta terminar", "Chaque jour jusqu'à la fin", "Setiap hari sampai selesai", "每天提醒直到完成", "每天提醒直到完成", "كل يوم حتى الإنجاز", "כל יום עד לסיום", "Täglich bis erledigt", "Elke dag tot klaar", null, null, null, null);
        p("remTime", "Reminder time", "Reminder time", "Hora del recordatorio", "Heure du rappel", "Waktu pengingat", "提醒時間", "提醒时间", "وقت التذكير", "שעת תזכורת", "Erinnerungszeit", "Herinneringstijd", null, null, null, null);
        p("allow", "Allow notifications", "Allow notifications", "Permitir notificaciones", "Autoriser les notifications", "Izinkan notifikasi", "允許通知", "允许通知", "السماح بالإشعارات", "אפשר התראות", "Benachrichtigungen erlauben", "Meldingen toestaan", null, null, null, null);
        p("language", "Language", "Language", "Idioma", "Langue", "Bahasa", "語言", "语言", "اللغة", "שפה", "Sprache", "Taal", null, null, null, null);
        p("light", "Light", "Light", "Claro", "Clair", "Terang", "淺色", "浅色", "فاتح", "בהיר", "Hell", "Licht", null, null, null, null);
        p("dark", "Dark", "Dark", "Oscuro", "Sombre", "Gelap", "深色", "深色", "داكن", "כהה", "Dunkel", "Donker", null, null, null, null);
        p("storage", "Storage folder", "Storage folder", "Carpeta de almacenamiento", "Dossier de stockage", "Folder penyimpanan", "儲存資料夾", "存储文件夹", "مجلد التخزين", "תיקיית אחסון", "Speicherordner", "Opslagmap", null, null, null, null);
        p("chooseFolder", "Choose folder", "Choose folder", "Elegir carpeta", "Choisir un dossier", "Pilih folder", "選擇資料夾", "选择文件夹", "اختيار مجلد", "בחירת תיקייה", "Ordner wählen", "Map kiezen", null, null, null, null);
        p("colorblind", "Colorblind mode", "Colour-blind mode", "Modo daltónico", "Mode daltonien", "Mode buta warna", "色盲模式", "色盲模式", "وضع عمى الألوان", "מצב עיוורון צבעים", "Farbenblind-Modus", "Kleurenblindmodus", null, null, null, null);
        p("remTitle", "Homework reminder", "Homework reminder", "Recordatorio de tarea", "Rappel de devoirs", "Pengingat PR", "作業提醒", "作业提醒", "تذكير بالواجبات", "תזכורת שיעורי בית", "Hausaufgaben-Erinnerung", "Huiswerkherinnering", null, null, null, null);
        p("title", "Title", "Title", "Título", "Titre", "Judul", "標題", "标题", "العنوان", "כותרת", "Titel", "Titel", null, null, null, null);
        p("write", "Write", "Write", "Escribir", "Écrire", "Tulis", "撰寫", "撰写", "كتابة", "כתיבה", "Schreiben", "Schrijven", null, null, null, null);
        p("import", "Open file", "Open file", "Abrir archivo", "Ouvrir un fichier", "Buka file", "開啟檔案", "打开文件", "فتح ملف", "פתיחת קובץ", "Datei öffnen", "Bestand openen", null, null, null, null);
        p("export", "Export", "Export", "Exportar", "Exporter", "Ekspor", "匯出", "导出", "تصدير", "ייצוא", "Exportieren", "Exporteren", null, null, null, null);
        p("back", "Back", "Back", "Atrás", "Retour", "Kembali", "返回", "返回", "رجوع", "חזרה", "Zurück", "Terug", null, null, null, null);
        p("time", "Time", "Time", "Hora", "Heure", "Waktu", "時間", "时间", "الوقت", "שעה", "Uhrzeit", "Tijd", null, null, null, null);
        p("tapDel", "Tap an item to delete", "Tap an item to delete", "Toca un elemento para eliminarlo", "Touchez pour supprimer", "Ketuk untuk menghapus", "點按項目即可刪除", "点按项目即可删除", "اضغط للحذف", "הקישו למחיקה", "Element zum Löschen antippen", "Tik op een item om te verwijderen", null, null, null, null);
        p("tapMove", "Use arrows to move", "Use arrows to move", "Usa las flechas para mover", "Utilisez les flèches", "Gunakan panah untuk memindah", "用箭頭移動", "用箭头移动", "استخدم الأسهم للنقل", "השתמשו בחצים להזזה", "Mit den Pfeilen verschieben", "Gebruik de pijlen om te verplaatsen", null, null, null, null);
        p("ok", "OK", "OK", "Aceptar", "OK", "OK", "確定", "确定", "موافق", "אישור", "OK", "OK", null, null, null, null);
        p("confirmDel", "Delete this?", "Delete this?", "¿Eliminar esto?", "Supprimer ceci ?", "Hapus ini?", "要刪除嗎？", "要删除吗？", "حذف هذا؟", "למחוק?", "Das löschen?", "Dit verwijderen?", null, null, null, null);
        p("cantRead", "Cannot read this file here.", "Cannot read this file here.", "No se puede leer este archivo aquí.", "Impossible de lire ce fichier.", "Tidak dapat membaca file ini.", "無法在此讀取此檔案。", "无法在此读取此文件。", "تعذرت قراءة هذا الملف.", "לא ניתן לקרוא קובץ זה.", "Diese Datei kann hier nicht gelesen werden.", "Dit bestand kan hier niet worden gelezen.", null, null, null, null);
        p("download", "Download", "Download", "Descargar", "Télécharger", "Unduh", "下載", "下载", "تنزيل", "הורדה", "Herunterladen", "Downloaden", null, null, null, null);
        p("saved", "Saved", "Saved", "Guardado", "Enregistré", "Tersimpan", "已儲存", "已保存", "تم الحفظ", "נשמר", "Gespeichert", "Opgeslagen", null, null, null, null);
        p("accessibility", "Accessibility", "Accessibility", "Accesibilidad", "Accessibilité", "Aksesibilitas", "無障礙", "无障碍", "إمكانية الوصول", "נגישות", "Barrierefreiheit", "Toegankelijkheid", null, null, null, null);
        p("textures", "Textures", "Textures", "Texturas", "Textures", "Tekstur", "紋理", "纹理", "الخامات", "מרקמים", "Texturen", "Texturen", null, null, null, null);
        p("experimental", "Experimental", "Experimental", "Experimental", "Expérimental", "Eksperimental", "實驗功能", "实验功能", "تجريبي", "ניסיוני", "Experimentell", "Experimenteel", null, null, null, null);
        p("textSize", "Text size", "Text size", "Tamaño del texto", "Taille du texte", "Ukuran teks", "文字大小", "文字大小", "حجم النص", "גודל טקסט", "Textgröße", "Tekstgrootte", null, null, null, null);
        p("edit", "Edit", "Edit", "Editar", "Modifier", "Ubah", "編輯", "编辑", "تعديل", "עריכה", "Bearbeiten", "Bewerken", "Mend", "fix", "Fix it", "Amend");
        p("editCard", "Edit card", "Edit card", "Editar tarjeta", "Modifier la carte", "Ubah kartu", "編輯卡片", "编辑卡片", "تعديل البطاقة", "עריכת כרטיס", "Karte bearbeiten", "Kaart bewerken", null, null, null, null);
        p("close", "Close", "Close", "Cerrar", "Fermer", "Tutup", "關閉", "关闭", "إغلاق", "סגירה", "Schließen", "Sluiten", "Furl", "close teh box", "Close", "Withdraw");
        p("more", "More options", "More options", "Más opciones", "Plus d'options", "Opsi lain", "更多選項", "更多选项", "خيارات أخرى", "אפשרויות נוספות", "Weitere Optionen", "Meer opties", null, null, null, null);
        p("moveUp", "Move up", "Move up", "Subir", "Monter", "Naik", "上移", "上移", "نقل لأعلى", "הזזה למעלה", "Nach oben", "Omhoog", null, null, null, null);
        p("moveDown", "Move down", "Move down", "Bajar", "Descendre", "Turun", "下移", "下移", "نقل لأسفل", "הזזה למטה", "Nach unten", "Omlaag", null, null, null, null);
        p("chHomework", "Homework reminders", "Homework reminders", "Recordatorios de tareas", "Rappels de devoirs", "Pengingat PR", "作業提醒", "作业提醒", "تذكيرات الواجبات", "תזכורות שיעורי בית", "Hausaufgaben-Erinnerungen", "Huiswerkherinneringen", null, null, null, null);
        p("chExam", "Exam reminders", "Exam reminders", "Recordatorios de exámenes", "Rappels d'examens", "Pengingat ujian", "考試提醒", "考试提醒", "تذكيرات الامتحانات", "תזכורות מבחנים", "Prüfungserinnerungen", "Toetsherinneringen", null, null, null, null);
        p("chPrep", "Prepping reminders", "Prepping reminders", "Recordatorios de preparación", "Rappels de préparation", "Pengingat persiapan", "準備提醒", "准备提醒", "تذكيرات التحضير", "תזכורות הכנה", "Vorbereitungserinnerungen", "Voorbereidingsherinneringen", null, null, null, null);
        p("remExam", "Exam reminder", "Exam reminder", "Recordatorio de examen", "Rappel d'examen", "Pengingat ujian", "考試提醒", "考试提醒", "تذكير بالامتحان", "תזכורת למבחן", "Prüfungserinnerung", "Toetsherinnering", null, null, null, null);
        p("remPrep", "Prepping reminder", "Prepping reminder", "Recordatorio de preparación", "Rappel de préparation", "Pengingat persiapan", "準備提醒", "准备提醒", "تذكير بالتحضير", "תזכורת הכנה", "Vorbereitungserinnerung", "Voorbereidingsherinnering", null, null, null, null);
        p("theme", "Theme", "Theme", "Tema", "Thème", "Tema", "主題", "主题", "المظهر", "ערכת נושא", "Design", "Thema", "Colors o' the Fleet", null, null, null);
        p("display", "Display", "Display", "Pantalla", "Affichage", "Tampilan", "顯示", "显示", "العرض", "תצוגה", "Anzeige", "Weergave", null, null, null, null);
        p("notifs", "Notifications", "Notifications", "Notificaciones", "Notifications", "Notifikasi", "通知", "通知", "الإشعارات", "התראות", "Benachrichtigungen", "Meldingen", null, null, null, null);
        p("reminders", "Reminders", "Reminders", "Recordatorios", "Rappels", "Pengingat", "提醒", "提醒", "التذكيرات", "תזכורות", "Erinnerungen", "Herinneringen", null, null, null, null);
        p("directory", "Directory", "Directory", "Directorio", "Répertoire", "Direktori", "目錄", "目录", "المجلد", "ספרייה", "Verzeichnis", "Map", null, null, null, null);
        p("about", "About", "About", "Acerca de", "À propos", "Tentang", "關於", "关于", "حول", "אודות", "Info", "Over", "Ship's Log", "bout", "Bout", "Of this App");
        p("grpLook", "Look", "Look", "Aspecto", "Apparence", "Tampilan", "外觀", "外观", "الشكل", "מראה", "Aussehen", "Uiterlijk", null, null, null, null);
        p("grpAlerts", "Alerts", "Alerts", "Avisos", "Alertes", "Peringatan", "提示", "提示", "التنبيهات", "התרעות", "Hinweise", "Waarschuwingen", null, null, null, null);
        p("grpSystem", "System", "System", "Sistema", "Système", "Sistem", "系統", "系统", "النظام", "מערכת", "System", "Systeem", null, null, null, null);
        p("grpMore", "More", "More", "Más", "Plus", "Lainnya", "更多", "更多", "المزيد", "עוד", "Mehr", "Meer", null, null, null, null);
        p("texPaper", "Paper", "Paper", "Papel", "Papier", "Kertas", "紙張", "纸张", "ورق", "נייר", "Papier", "Papier", null, null, null, null);
        p("texConcrete", "Concrete", "Concrete", "Hormigón", "Béton", "Beton", "混凝土", "混凝土", "خرسانة", "בטון", "Beton", "Beton", null, null, null, null);
        p("texGrain", "Grain", "Grain", "Grano", "Grain", "Butiran", "顆粒", "颗粒", "حبيبات", "גרגיר", "Körnung", "Korrel", null, null, null, null);
        p("contrast", "Increased contrast", "Increased contrast", "Contraste alto", "Contraste renforcé", "Kontras tinggi", "提高對比度", "提高对比度", "تباين أعلى", "ניגודיות מוגברת", "Erhöhter Kontrast", "Hoger contrast", null, null, null, null);
        p("colors", "Colors", "Colours", "Colores", "Couleurs", "Warna", "顏色", "颜色", "الألوان", "צבעים", "Farben", "Kleuren", null, null, null, null);
        p("gradients", "Gradients", "Gradients", "Degradados", "Dégradés", "Gradasi", "漸層", "渐变", "تدرجات", "מעברי צבע", "Verläufe", "Kleurverlopen", null, null, null, null);
        p("typography", "Typography", "Typography", "Tipografía", "Typographie", "Tipografi", "字體", "字体", "الخطوط", "טיפוגרפיה", "Typografie", "Typografie", null, null, null, null);
        p("warmth", "Warmth reading mode", "Warmth reading mode", "Modo lectura cálida", "Mode lecture chaud", "Mode baca hangat", "暖色閱讀模式", "暖色阅读模式", "وضع القراءة الدافئ", "מצב קריאה חם", "Wärmemodus zum Lesen", "Warme leesmodus", null, null, null, null);
        p("warmthHint", "Warmer colors for evening reading", "Warmer colours for evening reading", "Colores más cálidos para leer de noche", "Couleurs plus chaudes pour le soir", "Warna lebih hangat untuk malam hari", "夜間閱讀更舒適的暖色", "夜间阅读更舒适的暖色", "ألوان أدفأ للقراءة مساءً", "צבעים חמים יותר לקריאה בערב", "Wärmere Farben für den Abend", "Warmere kleuren voor 's avonds", null, null, null, null);
        p("battery", "Battery saver", "Battery saver", "Ahorro de batería", "Économiseur de batterie", "Penghemat baterai", "省電模式", "省电模式", "توفير البطارية", "חיסכון בסוללה", "Energiesparmodus", "Batterijbesparing", null, null, null, null);
        p("batteryHint", "Limits rendering to 30 FPS", "Limits rendering to 30 FPS", "Limita el renderizado a 30 FPS", "Limite le rendu à 30 FPS", "Membatasi render hingga 30 FPS", "將畫面更新限制在 30 FPS", "将画面刷新限制在 30 FPS", "يحد من العرض إلى 30 إطارًا/ث", "מגביל עיבוד ל-30 FPS", "Begrenzt das Rendering auf 30 FPS", "Beperkt weergave tot 30 FPS", null, null, null, null);
        p("notifPerm", "Notification permission", "Notification permission", "Permiso de notificaciones", "Autorisation de notification", "Izin notifikasi", "通知權限", "通知权限", "إذن الإشعارات", "הרשאת התראות", "Benachrichtigungsberechtigung", "Meldingsrechten", null, null, null, null);
        p("granted", "Allowed", "Allowed", "Permitido", "Autorisé", "Diizinkan", "已允許", "已允许", "مسموح", "מותר", "Erlaubt", "Toegestaan", null, null, null, null);
        p("denied", "Not allowed", "Not allowed", "No permitido", "Non autorisé", "Tidak diizinkan", "未允許", "未允许", "غير مسموح", "לא מותר", "Nicht erlaubt", "Niet toegestaan", null, null, null, null);
        p("openAndroid", "Android notification settings", "Android notification settings", "Ajustes de notificaciones de Android", "Réglages de notification Android", "Setelan notifikasi Android", "Android 通知設定", "Android 通知设置", "إعدادات إشعارات أندرويد", "הגדרות ההתראות של אנדרואיד", "Android-Benachrichtigungseinstellungen", "Android-meldingsinstellingen", null, null, null, null);
        p("soundGeneral", "General notification sound", "General notification sound", "Sonido de notificaciones generales", "Son des notifications générales", "Nada notifikasi umum", "一般通知音效", "常规通知提示音", "نغمة الإشعارات العامة", "צליל התראות כלליות", "Ton für allgemeine Benachrichtigungen", "Geluid voor algemene meldingen", null, null, null, null);
        p("soundDeadline", "Card deadline sound", "Card deadline sound", "Sonido de fecha límite de tarjetas", "Son des échéances de cartes", "Nada tenggat kartu", "卡片截止音效", "卡片截止提示音", "نغمة مواعيد البطاقات", "צליל מועדי כרטיסים", "Ton für Karten-Fristen", "Geluid voor kaartdeadlines", null, null, null, null);
        p("soundDefault", "Default", "Default", "Predeterminado", "Par défaut", "Bawaan", "預設", "默认", "افتراضي", "ברירת מחדל", "Standard", "Standaard", null, null, null, null);
        p("soundSilent", "Silent", "Silent", "Silencio", "Silencieux", "Senyap", "靜音", "静音", "صامت", "שקט", "Stumm", "Stil", null, null, null, null);
        p("choose", "Choose", "Choose", "Elegir", "Choisir", "Pilih", "選擇", "选择", "اختيار", "בחירה", "Wählen", "Kiezen", null, null, null, null);
        p("test", "Test", "Test", "Probar", "Tester", "Uji", "測試", "测试", "اختبار", "בדיקה", "Testen", "Testen", null, null, null, null);
        p("freq", "Frequency", "Frequency", "Frecuencia", "Fréquence", "Frekuensi", "頻率", "频率", "التكرار", "תדירות", "Häufigkeit", "Frequentie", null, null, null, null);
        p("dMinusX", "D-X (days before the deadline)", "D-X (days before the deadline)", "D-X (días antes de la fecha límite)", "J-X (jours avant l'échéance)", "D-X (hari sebelum tenggat)", "D-X（截止前幾天）", "D-X（截止前几天）", "D-X (أيام قبل الموعد)", "D-X (ימים לפני המועד)", "D-X (Tage vor der Frist)", "D-X (dagen voor de deadline)", null, null, null, null);
        p("daysBefore", "Days before", "Days before", "Días antes", "Jours avant", "Hari sebelumnya", "提前天數", "提前天数", "أيام قبل", "ימים לפני", "Tage vorher", "Dagen eerder", null, null, null, null);
        p("hour", "Hour", "Hour", "Hora", "Heure", "Jam", "時", "时", "ساعة", "שעה", "Stunde", "Uur", null, null, null, null);
        p("minute", "Minute", "Minute", "Minuto", "Minute", "Menit", "分", "分", "دقيقة", "דקה", "Minute", "Minuut", null, null, null, null);
        p("second", "Second", "Second", "Segundo", "Seconde", "Detik", "秒", "秒", "ثانية", "שנייה", "Sekunde", "Seconde", null, null, null, null);
        p("exactHint", "Exact times need the alarm permission.", "Exact times need the alarm permission.", "Las horas exactas necesitan el permiso de alarmas.", "Les heures exactes nécessitent l'autorisation d'alarme.", "Waktu tepat memerlukan izin alarm.", "精確時間需要鬧鐘權限。", "精确时间需要闹钟权限。", "التوقيت الدقيق يتطلب إذن المنبّه.", "זמן מדויק דורש הרשאת שעון מעורר.", "Genaue Zeiten benötigen die Alarm-Berechtigung.", "Exacte tijden hebben alarmrechten nodig.", null, null, null, null);
        p("exactBtn", "Allow exact alarms", "Allow exact alarms", "Permitir alarmas exactas", "Autoriser les alarmes exactes", "Izinkan alarm tepat", "允許精確鬧鐘", "允许精确闹钟", "السماح بالمنبهات الدقيقة", "לאפשר התראות מדויקות", "Exakte Alarme erlauben", "Exacte alarmen toestaan", null, null, null, null);
        p("reduceMotion", "Reduce motion", "Reduce motion", "Reducir movimiento", "Réduire les animations", "Kurangi gerakan", "減少動態效果", "减少动态效果", "تقليل الحركة", "הפחתת תנועה", "Bewegung reduzieren", "Beweging verminderen", null, null, null, null);
        p("haptics", "Haptic feedback", "Haptic feedback", "Respuesta háptica", "Retour haptique", "Umpan balik haptik", "觸覺回饋", "触感反馈", "الاهتزاز التفاعلي", "משוב רטט", "Haptisches Feedback", "Haptische feedback", null, null, null, null);
        p("windowBlur", "Window blur", "Window blur", "Desenfoque de ventana", "Flou de fenêtre", "Blur jendela", "視窗模糊", "窗口模糊", "تمويه النافذة", "טשטוש חלון", "Fensterunschärfe", "Venstervervaging", null, null, null, null);
        p("windowBlurHint", "Frosted glass using Android blur (Android 12+)", "Frosted glass using Android blur (Android 12+)", "Cristal esmerilado con el desenfoque de Android (Android 12+)", "Verre dépoli avec le flou Android (Android 12+)", "Efek kaca buram dengan blur Android (Android 12+)", "使用 Android 模糊的毛玻璃效果（Android 12 以上）", "使用 Android 模糊的毛玻璃效果（Android 12 及以上）", "زجاج مصنفر بتمويه أندرويد (أندرويد 12+)", "זכוכית מט עם טשטוש של אנדרואיד (אנדרואיד 12+)", "Milchglas mit Android-Weichzeichner (Android 12+)", "Matglas met Android-blur (Android 12+)", null, null, null, null);
        p("motionBlur", "Blur in motion", "Blur in motion", "Desenfoque en movimiento", "Flou en mouvement", "Blur saat bergerak", "動態模糊", "动态模糊", "تمويه أثناء الحركة", "טשטוש בתנועה", "Unschärfe bei Bewegung", "Vervaging in beweging", null, null, null, null);
        p("motionBlurHint", "Moving items blur slightly, then sharpen", "Moving items blur slightly, then sharpen", "Los elementos en movimiento se desenfocan y luego se nitidizan", "Les éléments en mouvement se floutent puis redeviennent nets", "Item bergerak sedikit buram lalu kembali tajam", "移動中的項目會略微模糊，靜止後恢復清晰", "移动中的项目会略微模糊，静止后恢复清晰", "العناصر المتحركة تتموه قليلًا ثم تعود حادة", "פריטים בתנועה מיטשטשים מעט וחוזרים לחדות", "Bewegte Elemente werden kurz unscharf und dann wieder scharf", "Bewegende items vervagen even en worden dan weer scherp", null, null, null, null);
        p("version", "Version", "Version", "Versión", "Version", "Versi", "版本", "版本", "الإصدار", "גרסה", "Version", "Versie", null, null, null, null);
        p("credits", "Credits", "Credits", "Créditos", "Crédits", "Kredit", "製作人員", "制作人员", "الشكر والتقدير", "קרדיטים", "Mitwirkende", "Credits", null, null, null, null);
        p("roleDesign", "Design", "Design", "Diseño", "Conception", "Desain", "設計", "设计", "التصميم", "עיצוב", "Design", "Ontwerp", null, null, null, null);
        p("roleCode", "Code", "Code", "Código", "Code", "Kode", "程式", "代码", "البرمجة", "קוד", "Code", "Code", null, null, null, null);
        p("roleIdea", "Idea", "Idea", "Idea", "Idée", "Ide", "創意", "创意", "الفكرة", "רעיון", "Idee", "Idee", null, null, null, null);
        p("repo", "GitHub repository", "GitHub repository", "Repositorio de GitHub", "Dépôt GitHub", "Repositori GitHub", "GitHub 儲存庫", "GitHub 仓库", "مستودع GitHub", "מאגר GitHub", "GitHub-Repository", "GitHub-repository", null, null, null, null);
        p("left", "%s left", "%s left", "quedan %s", "reste %s", "sisa %s", "剩餘 %s", "剩余 %s", "متبقٍ %s", "נותרו %s", "noch %s", "nog %s", null, null, null, null);
        p("overdue", "Overdue", "Overdue", "Vencido", "En retard", "Terlambat", "已逾期", "已逾期", "متأخر", "באיחור", "Überfällig", "Te laat", null, null, null, null);
        p("noDeadline", "No deadline", "No deadline", "Sin fecha límite", "Sans échéance", "Tanpa tenggat", "無截止時間", "无截止时间", "بلا موعد", "ללא מועד", "Keine Frist", "Geen deadline", null, null, null, null);
        p("openFile", "Open file", "Open file", "Abrir archivo", "Ouvrir le fichier", "Buka berkas", "開啟檔案", "打开文件", "فتح الملف", "פתיחת קובץ", "Datei öffnen", "Bestand openen", "Open the chest", "open teh file", "Open di file", "Unseal the scroll");
        p("finish", "Finish", "Finish", "Terminar", "Terminer", "Selesai", "完成", "完成", "إنهاء", "סיום", "Erledigt", "Afronden", "Plundered!", "dun!", "Done, seen", "Complete");
        p("reopen", "Reopen", "Reopen", "Reabrir", "Rouvrir", "Buka lagi", "重新開啟", "重新打开", "إعادة فتح", "פתיחה מחדש", "Wieder öffnen", "Heropenen", null, null, null, null);
        p("unitD", "d", "d", "d", "j", "h", "天", "天", "ي", "י׳", "T", "d", null, null, null, null);
        p("unitH", "h", "h", "h", "h", "j", "小時", "小时", "س", "ש׳", "Std", "u", null, null, null, null);
        p("unitM", "m", "m", "min", "min", "m", "分", "分", "د", "ד׳", "Min", "m", null, null, null, null);
    }

    static String t(String k) {
        String[] a = M.get(k);
        if (a == null) return k;
        String s = a[lang];
        if (s != null) return s;
        if (lang >= FIRST_DIALECT) {
            String key = lang + "|" + k;
            String c = CACHE.get(key);
            if (c == null) {
                c = Dialects.apply(lang, a[0]);
                CACHE.put(key, c);
            }
            return c;
        }
        return a[0];
    }

    static boolean rtl() {
        return lang == AR || lang == HE;
    }
}
