package com.memorius.app;

import java.util.HashMap;
import java.util.Map;

final class Str {
    static int lang = 0;

    static final String[] NAMES = {"English (US)", "English (UK)", "Bahasa Indonesia", "Français", "العربية", "עברית"};

    static final String[][] DAYS = {
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"},
            {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat"},
            {"Sen", "Sel", "Rab", "Kam", "Jum", "Sab"},
            {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam"},
            {"الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"},
            {"ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳"}
    };

    private static final Map<String, String[]> M = new HashMap<>();

    // en (also used for en-GB), id, fr, ar, he
    private static void p(String k, String en, String id, String fr, String ar, String he) {
        M.put(k, new String[]{en, en, id, fr, ar, he});
    }

    // en-US, en-GB, id, fr, ar, he
    private static void p6(String k, String en, String gb, String id, String fr, String ar, String he) {
        M.put(k, new String[]{en, gb, id, fr, ar, he});
    }

    static {
        p("cards", "Cards", "Kartu", "Cartes", "البطاقات", "כרטיסים");
        p("progress", "Projects", "Proyek", "Projets", "المشاريع", "פרויקטים");
        p("schedule", "Schedule", "Jadwal", "Planning", "الجدول", "מערכת");
        p("settings", "Settings", "Setelan", "Réglages", "الإعدادات", "הגדרות");
        p("add", "Add", "Tambah", "Ajouter", "إضافة", "הוספה");
        p("delete", "Delete", "Hapus", "Supprimer", "حذف", "מחיקה");
        p("rearrange", "Rearrange", "Atur ulang", "Réorganiser", "إعادة ترتيب", "סידור מחדש");
        p("save", "Save", "Simpan", "Enregistrer", "حفظ", "שמירה");
        p("cancel", "Cancel", "Batal", "Annuler", "إلغاء", "ביטול");
        p("apply", "Apply", "Terapkan", "Appliquer", "تطبيق", "החלה");
        p("applyAsk", "Accept changes?", "Terima perubahan?", "Accepter les modifications ?", "قبول التغييرات؟", "לאשר שינויים?");
        p("subject", "Subject", "Mata pelajaran", "Matière", "المادة", "מקצוע");
        p("deadline", "Deadline", "Tenggat", "Échéance", "الموعد النهائي", "מועד הגשה");
        p("date", "Date", "Tanggal", "Date", "التاريخ", "תאריך");
        p("type", "Type", "Jenis", "Type", "النوع", "סוג");
        p("homework", "Homework", "PR", "Devoir", "واجب", "שיעורי בית");
        p("exam", "Exam", "Ujian", "Examen", "امتحان", "מבחן");
        p("prepping", "Prepping", "Persiapan", "Préparation", "تحضير", "הכנה");
        p("note", "Note", "Catatan", "Note", "ملاحظة", "הערה");
        p("files", "Files", "File", "Fichiers", "الملفات", "קבצים");
        p("drop", "Tap to add files", "Ketuk untuk menambah file", "Touchez pour ajouter des fichiers", "اضغط لإضافة ملفات", "הקישו להוספת קבצים");
        p6("color", "Color", "Colour", "Warna", "Couleur", "اللون", "צבע");
        p("empty", "Nothing here yet", "Belum ada apa-apa", "Rien pour l'instant", "لا شيء بعد", "עדיין אין כלום");
        p("holdHint", "Hold a card to preview", "Tahan kartu untuk pratinjau", "Maintenez une carte pour l'aperçu", "اضغط مطولًا على البطاقة للمعاينة", "לחצו לחיצה ארוכה על כרטיס לתצוגה מקדימה");
        p("notifFreq", "Notifications", "Notifikasi", "Notifications", "الإشعارات", "התראות");
        p("off", "Off", "Mati", "Désactivé", "إيقاف", "כבוי");
        p("on", "On", "Aktif", "Activé", "تشغيل", "פעיל");
        p("onDue", "On due day", "Pada hari tenggat", "Le jour de l'échéance", "في يوم الموعد", "ביום ההגשה");
        p("dMinus1", "D-1 (day before)", "H-1 (sehari sebelumnya)", "J-1 (la veille)", "قبل يوم", "יום לפני");
        p("daily", "Every day until done", "Setiap hari sampai selesai", "Chaque jour jusqu'à la fin", "كل يوم حتى الإنجاز", "כל יום עד לסיום");
        p("remTime", "Reminder time", "Waktu pengingat", "Heure du rappel", "وقت التذكير", "שעת תזכורת");
        p("allow", "Allow notifications", "Izinkan notifikasi", "Autoriser les notifications", "السماح بالإشعارات", "אפשר התראות");
        p("language", "Language", "Bahasa", "Langue", "اللغة", "שפה");
        p("appearance", "Appearance", "Tampilan", "Apparence", "المظهر", "מראה");
        p("light", "Light", "Terang", "Clair", "فاتح", "בהיר");
        p("dark", "Dark", "Gelap", "Sombre", "داكن", "כהה");
        p("storage", "Storage folder", "Folder penyimpanan", "Dossier de stockage", "مجلد التخزين", "תיקיית אחסון");
        p("chooseFolder", "Choose folder", "Pilih folder", "Choisir un dossier", "اختيار مجلد", "בחירת תיקייה");
        p6("colorblind", "Colorblind mode", "Colourblind mode", "Mode buta warna", "Mode daltonien", "وضع عمى الألوان", "מצב עיוורון צבעים");
        p("remTitle", "Homework reminder", "Pengingat PR", "Rappel de devoirs", "تذكير بالواجبات", "תזכורת שיעורי בית");
        p("title", "Title", "Judul", "Titre", "العنوان", "כותרת");
        p("write", "Write", "Tulis", "Écrire", "كتابة", "כתיבה");
        p("import", "Open file", "Buka file", "Ouvrir un fichier", "فتح ملف", "פתיחת קובץ");
        p("export", "Export", "Ekspor", "Exporter", "تصدير", "ייצוא");
        p("back", "Back", "Kembali", "Retour", "رجوع", "חזרה");
        p("nul", "nul", "nul", "nul", "nul", "nul");
        p("time", "Time", "Waktu", "Heure", "الوقت", "שעה");
        p("tapDel", "Tap an item to delete", "Ketuk untuk menghapus", "Touchez pour supprimer", "اضغط للحذف", "הקישו למחיקה");
        p("tapMove", "Use arrows to move", "Gunakan panah untuk memindah", "Utilisez les flèches", "استخدم الأسهم للنقل", "השתמשו בחצים להזזה");
        p("ok", "OK", "OK", "OK", "موافق", "אישור");
        p("confirmDel", "Delete this?", "Hapus ini?", "Supprimer ceci ?", "حذف هذا؟", "למחוק?");
        p("cantRead", "Cannot read this file here.", "Tidak dapat membaca file ini.", "Impossible de lire ce fichier.", "تعذرت قراءة هذا الملف.", "לא ניתן לקרוא קובץ זה.");
        p("download", "Download", "Unduh", "Télécharger", "تنزيل", "הורדה");
        p("saved", "Saved", "Tersimpan", "Enregistré", "تم الحفظ", "נשמר");
    }

    static String t(String k) {
        String[] a = M.get(k);
        return a == null ? k : a[lang];
    }

    static boolean rtl() {
        return lang >= 4;
    }
}
