var lang = 0

val langNames = listOf("English (US)", "English (UK)", "Bahasa Indonesia", "Français", "العربية", "עברית")
val langCodes = listOf("en-US", "en-GB", "id", "fr", "ar", "he")

private fun s(en: String, id: String, fr: String, ar: String, he: String) = listOf(en, en, id, fr, ar, he)
private fun s(en: String, gb: String, id: String, fr: String, ar: String, he: String) = listOf(en, gb, id, fr, ar, he)

val dayNames = listOf(
    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
    listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
    listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab"),
    listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"),
    listOf("الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"),
    listOf("ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")
)

val S = mapOf(
    "cards" to s("Cards", "Kartu", "Cartes", "البطاقات", "כרטיסים"),
    "progress" to s("Progress", "Progres", "Progrès", "التقدم", "התקדמות"),
    "schedule" to s("Schedule", "Jadwal", "Planning", "الجدول", "מערכת"),
    "settings" to s("Settings", "Setelan", "Réglages", "الإعدادات", "הגדרות"),
    "add" to s("Add", "Tambah", "Ajouter", "إضافة", "הוספה"),
    "delete" to s("Delete", "Hapus", "Supprimer", "حذف", "מחיקה"),
    "rearrange" to s("Rearrange", "Atur ulang", "Réorganiser", "إعادة ترتيب", "סידור מחדש"),
    "save" to s("Save", "Simpan", "Enregistrer", "حفظ", "שמירה"),
    "cancel" to s("Cancel", "Batal", "Annuler", "إلغاء", "ביטול"),
    "apply" to s("Apply", "Terapkan", "Appliquer", "تطبيق", "החלה"),
    "applyAsk" to s("Accept changes?", "Terima perubahan?", "Accepter les modifications ?", "قبول التغييرات؟", "לאשר שינויים?"),
    "subject" to s("Subject", "Mata pelajaran", "Matière", "المادة", "מקצוע"),
    "deadline" to s("Deadline", "Tenggat", "Échéance", "الموعد النهائي", "מועד הגשה"),
    "date" to s("Date", "Tanggal", "Date", "التاريخ", "תאריך"),
    "type" to s("Type", "Jenis", "Type", "النوع", "סוג"),
    "homework" to s("Homework", "PR", "Devoir", "واجب", "שיעורי בית"),
    "exam" to s("Exam", "Ujian", "Examen", "امتحان", "מבחן"),
    "prepping" to s("Prepping", "Persiapan", "Préparation", "تحضير", "הכנה"),
    "note" to s("Note", "Catatan", "Note", "ملاحظة", "הערה"),
    "files" to s("Files", "File", "Fichiers", "الملفات", "קבצים"),
    "drop" to s("Drop files here or tap", "Letakkan file di sini atau ketuk", "Déposez des fichiers ou touchez ici", "أسقط الملفات هنا أو اضغط", "גררו קבצים לכאן או הקישו"),
    "color" to s("Color", "Colour", "Warna", "Couleur", "اللون", "צבע"),
    "empty" to s("Nothing here yet", "Belum ada apa-apa", "Rien pour l'instant", "لا شيء بعد", "עדיין אין כלום"),
    "holdHint" to s("Hold a card to preview", "Tahan kartu untuk pratinjau", "Maintenez une carte pour l'aperçu", "اضغط مطولًا على البطاقة للمعاينة", "לחצו לחיצה ארוכה על כרטיס לתצוגה מקדימה"),
    "notifFreq" to s("Notifications", "Notifikasi", "Notifications", "الإشعارات", "התראות"),
    "off" to s("Off", "Mati", "Désactivé", "إيقاف", "כבוי"),
    "on" to s("On", "Aktif", "Activé", "تشغيل", "פעיל"),
    "onDue" to s("On due day", "Pada hari tenggat", "Le jour de l'échéance", "في يوم الموعد", "ביום ההגשה"),
    "dMinus1" to s("D-1 (day before)", "H-1 (sehari sebelumnya)", "J-1 (la veille)", "قبل يوم", "יום לפני"),
    "daily" to s("Every day until done", "Setiap hari sampai selesai", "Chaque jour jusqu'à la fin", "كل يوم حتى الإنجاز", "כל יום עד לסיום"),
    "remTime" to s("Reminder time", "Waktu pengingat", "Heure du rappel", "وقت التذكير", "שעת תזכורת"),
    "allow" to s("Allow notifications", "Izinkan notifikasi", "Autoriser les notifications", "السماح بالإشعارات", "אפשר התראות"),
    "notifDenied" to s("Notifications are blocked in browser settings.", "Notifikasi diblokir di pengaturan browser.", "Notifications bloquées dans le navigateur.", "الإشعارات محظورة في إعدادات المتصفح.", "ההתראות חסומות בהגדרות הדפדפן."),
    "language" to s("Language", "Bahasa", "Langue", "اللغة", "שפה"),
    "appearance" to s("Appearance", "Tampilan", "Apparence", "المظهر", "מראה"),
    "light" to s("Light", "Terang", "Clair", "فاتح", "בהיר"),
    "dark" to s("Dark", "Gelap", "Sombre", "داكن", "כהה"),
    "storage" to s("Storage folder", "Folder penyimpanan", "Dossier de stockage", "مجلد التخزين", "תיקיית אחסון"),
    "chooseFolder" to s("Choose folder", "Pilih folder", "Choisir un dossier", "اختيار مجلد", "בחירת תיקייה"),
    "noFolder" to s("Not supported here. Files will download instead.", "Tidak didukung di sini. File akan diunduh.", "Non pris en charge ici. Les fichiers seront téléchargés.", "غير مدعوم هنا. سيتم تنزيل الملفات.", "לא נתמך כאן. הקבצים יורדו."),
    "colorblind" to s("Colorblind mode", "Colourblind mode", "Mode buta warna", "Mode daltonien", "وضع عمى الألوان", "מצב עיוורון צבעים"),
    "remTitle" to s("Homework reminder", "Pengingat PR", "Rappel de devoirs", "تذكير بالواجبات", "תזכורת שיעורי בית"),
    "title" to s("Title", "Judul", "Titre", "العنوان", "כותרת"),
    "write" to s("Write", "Tulis", "Écrire", "كتابة", "כתיבה"),
    "import" to s("Open file", "Buka file", "Ouvrir un fichier", "فتح ملف", "פתיחת קובץ"),
    "export" to s("Export", "Ekspor", "Exporter", "تصدير", "ייצוא"),
    "back" to s("Back", "Kembali", "Retour", "رجوع", "חזרה"),
    "nul" to s("nul", "nul", "nul", "nul", "nul"),
    "time" to s("Time", "Waktu", "Heure", "الوقت", "שעה"),
    "tapDel" to s("Tap an item to delete", "Ketuk untuk menghapus", "Touchez pour supprimer", "اضغط للحذف", "הקישו למחיקה"),
    "tapMove" to s("Use arrows to move", "Gunakan panah untuk memindah", "Utilisez les flèches", "استخدم الأسهم للنقل", "השתמשו בחצים להזזה"),
    "ok" to s("OK", "OK", "OK", "موافق", "אישור"),
    "confirmDel" to s("Delete this?", "Hapus ini?", "Supprimer ceci ?", "حذف هذا؟", "למחוק?"),
    "cantRead" to s("Cannot read this file here.", "Tidak dapat membaca file ini.", "Impossible de lire ce fichier.", "تعذرت قراءة هذا الملف.", "לא ניתן לקרוא קובץ זה."),
    "download" to s("Download", "Unduh", "Télécharger", "تنزيل", "הורדה"),
    "tooBig" to s("File too big for storage.", "File terlalu besar untuk disimpan.", "Fichier trop volumineux.", "الملف كبير جدًا.", "הקובץ גדול מדי.")
)

fun t(k: String): String = S[k]?.get(lang) ?: k
