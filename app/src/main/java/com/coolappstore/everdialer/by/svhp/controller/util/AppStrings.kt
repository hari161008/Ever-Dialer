package com.coolappstore.everdialer.by.svhp.controller.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import java.util.Locale

/**
 * CompositionLocal holding the currently selected app language code.
 * Defaults to "system".
 */
val LocalAppLanguage = compositionLocalOf { PreferenceManager.LANGUAGE_SYSTEM }

/**
 * Convenience composable to translate a string key using the current [LocalAppLanguage].
 */
@Composable
fun tr(key: String): String {
    val lang = LocalAppLanguage.current
    return AppStrings.get(key, lang)
}

/**
 * String extension to translate inline within composables.
 */
@Composable
@JvmName("trExtension")
fun String.tr(): String {
    val lang = LocalAppLanguage.current
    return AppStrings.get(this, lang)
}

/**
 * Centralized in-memory translation repository for Ever Dialer UI strings.
 * Guarantees zero crashes and complete fallback to English whenever a translation key is missing.
 */
object AppStrings {

    fun get(key: String, langCode: String): String {
        val targetLang = if (langCode == PreferenceManager.LANGUAGE_SYSTEM) {
            Locale.getDefault().language.lowercase()
        } else {
            langCode.lowercase()
        }

        val map = when (targetLang) {
            PreferenceManager.LANGUAGE_ARABIC, "ar" -> ARABIC
            PreferenceManager.LANGUAGE_HINDI, "hi" -> HINDI
            PreferenceManager.LANGUAGE_TAMIL, "ta" -> TAMIL
            else -> null
        }

        return map?.get(key) ?: key
    }

    private val ARABIC: Map<String, String> = mapOf(
        // Navigation & Tabs
        "Favourites" to "المفضلة",
        "Favorites" to "المفضلة",
        "Calls" to "المكالمات",
        "Recents" to "المكالمات الأخيرة",
        "Contacts" to "جهات الاتصال",
        "SMS" to "الرسائل",
        "Groups" to "المجموعات",
        "Recordings" to "التسجيلات",
        "Notes" to "الملاحظات",
        "Dialpad" to "لوحة الاتصال",
        "Search in Ever Dialer" to "البحث في إيفر دايلر",
        "Search" to "بحث",
        "Settings" to "الإعدادات",

        // Settings Sections
        "General" to "عام",
        "Appearance" to "المظهر",
        "Gestures" to "الإيماءات",
        "Call Screen" to "شاشة المكالمات",
        "Advanced" to "خيارات متقدمة",
        "Backup & Restore" to "النسخ الاحتياطي والاستعادة",
        "Languages" to "اللغات",
        "About" to "حول",

        // Settings Items
        "Default Phone App" to "تطبيق الهاتف الافتراضي",
        "Make Ever Dialer your primary phone application" to "اجعل إيفر دايلر تطبيق الهاتف الأساسي",
        "Speed Dial" to "الاتصال السريع",
        "Quick Responses" to "الردود السريعة",
        "Blocked Numbers" to "الأرقام المحظورة",
        "Manage blocked phone numbers" to "إدارة أرقام الهواتف المحظورة",
        "Sound & Vibration" to "الصوت والاهتزاز",
        "Display & Theme" to "العرض والسمة",
        "Swipe Actions" to "إجراءات السحب",
        "Call Recording" to "تسجيل المكالمات",
        "SIM Cards" to "بطاقات SIM",
        "Configure default SIM and preferences" to "ضبط بطاقة SIM الافتراضية والتفضيلات",
        "App language and layout direction" to "لغة التطبيق واتجاه التخطيط",
        "About Ever Dialer" to "حول إيفر دايلر",
        "Version" to "الإصدار",
        "Search settings" to "البحث في الإعدادات",
        "Back" to "رجوع",

        // Languages Screen
        "Choose App Language" to "اختر لغة التطبيق",
        "Right-to-Left (RTL)" to "من اليمين إلى اليسار (RTL)",
        "Selecting Arabic activates Right-to-Left layout throughout the app, mirroring navigation and swipes while preserving standard dialpad keys." to "اختيار اللغة العربية يفعّل تخطيط اليمين إلى اليسار في جميع أنحاء التطبيق مع عكس اتجاه التنقل وحفظ ترتيب مفاتيح لوحة الاتصال القياسية.",
        "System default" to "افتراضي النظام",
        "Follow System" to "حسب النظام",
        "English" to "الإنجليزية",
        "Arabic" to "العربية",
        "Hindi" to "الهندية",
        "Tamil" to "التاميلية",

        // Call Logs & Filters
        "All" to "الكل",
        "Missed" to "الفائتة",
        "Known" to "جهات معروفة",
        "Unknown" to "غير معروف",
        "Incoming" to "واردة",
        "Outgoing" to "صادرة",
        "Blocked" to "محظورة",
        "Today" to "اليوم",
        "Call Time" to "مدة المكالمات",
        "No recent calls" to "لا توجد مكالمات حديثة",
        "No call history found" to "لم يتم العثور على سجل مكالمات",
        "Clear call log" to "مسح سجل المكالمات",
        "Delete" to "حذف",
        "Cancel" to "إلغاء",
        "Delete entries?" to "حذف العناصر المحددة؟",
        "This will permanently delete the selected call log entries." to "سيؤدي هذا إلى حذف سجلات المكالمات المحددة نهائياً.",
        "Grouped based on date" to "مجمعة حسب التاريخ",

        // Contacts
        "Search contacts" to "البحث في جهات الاتصال",
        "Search contacts..." to "البحث في جهات الاتصال...",
        "Create contact" to "إنشاء جهة اتصال",
        "Create new contact" to "إنشاء جهة اتصال جديدة",
        "Add Contact" to "إضافة جهة اتصال",
        "Add a new contact" to "إضافة جهة اتصال جديدة",
        "Add to existing contact" to "إضافة إلى جهة اتصال موجودة",
        "Select contact" to "اختر جهة اتصال",
        "No contacts found" to "لم يتم العثور على جهات اتصال",
        "Ungrouped" to "غير مصنفة",
        "Call" to "اتصال",
        "Message" to "رسالة",
        "Edit" to "تعديل",
        "Share" to "مشاركة",
        "Add favorite" to "إضافة إلى المفضلة",
        "Add to favorites" to "إضافة إلى المفضلة",
        "Remove from favorites" to "إزالة من المفضلة",
        "No favorites yet" to "لا توجد جهات اتصال مفضلة بعد",
        "No favourites yet" to "لا توجد جهات اتصال مفضلة بعد",
        "No Favorites Yet" to "لا توجد جهات اتصال مفضلة بعد",
        "Star a contact to add them here" to "قم بتمييز جهة اتصال بنجمة لإضافتها هنا",
        "Add a new number to existing contact" to "إضافة رقم جديد لجهة اتصال موجودة",
        "Mobile" to "جوال",
        "Home" to "المنزل",
        "Work" to "العمل",
        "Other" to "أخرى",

        // In-Call & Call Activity
        "Incoming Call" to "مكالمة واردة",
        "Calling" to "جارٍ الاتصال",
        "Calling..." to "جارٍ الاتصال...",
        "Call Ended" to "انتهت المكالمة",
        "On Hold" to "قيد الانتظار",
        "Choose account" to "اختر حساباً",
        "Declined" to "تم الرفض",
        "Hanging up..." to "جارٍ إنهاء المكالمة...",
        "Connecting..." to "جارٍ الاتصال...",
        "Swipe to answer" to "اسحب للرد",
        "Swipe to decline" to "اسحب للرفض",
        "Answer" to "رد",
        "Decline" to "رفض",
        "Mute" to "كتم",
        "Unmute" to "إلغاء الكتم",
        "Speaker" to "مكبر الصوت",
        "Keypad" to "لوحة المفاتيح",
        "Hold" to "تعليق",
        "Unhold" to "استئناف",
        "Merge" to "دمج",
        "Merge Calls" to "دمج المكالمات",
        "Add Person" to "إضافة شخص",
        "Add call" to "إضافة مكالمة",
        "Note" to "ملاحظة",
        "Record" to "تسجيل",
        "Stop" to "إيقاف",
        "Recording" to "جارٍ التسجيل",
        "Bluetooth" to "بلوتوث",
        "Hang Up" to "إنهاء المكالمة",
        "End call" to "إنهاء المكالمة",
        "Auto Redial?" to "إعادة الاتصال التلقائي؟",
        "Redial" to "إعادة الاتصال",
        "Redial attempts" to "محاولات إعادة الاتصال",

        // Common Actions
        "OK" to "حسناً",
        "Done" to "تم",
        "Save" to "حفظ",
        "Close" to "إغلاق",
        "Apply" to "تطبيق",
        "Reset" to "إعادة ضبط",
        "Open" to "فتح",
        "Yes" to "نعم",
        "No" to "لا",
        "Voicemail" to "البريد الصوتي",
        "Clear" to "مسح"
    )

    private val HINDI: Map<String, String> = mapOf(
        // Navigation & Tabs
        "Favourites" to "पसंदीदा",
        "Favorites" to "पसंदीदा",
        "Calls" to "कॉल",
        "Recents" to "हाल के",
        "Contacts" to "संपर्क",
        "SMS" to "एसएमएस",
        "Groups" to "समूह",
        "Recordings" to "रिकॉर्डिंग",
        "Notes" to "नोट्स",
        "Dialpad" to "डायलपैड",
        "Search in Ever Dialer" to "एवर डायलर में खोजें",
        "Search" to "खोजें",
        "Settings" to "सेटिंग्स",

        // Settings Sections
        "General" to "सामान्य",
        "Appearance" to "दिखावट",
        "Gestures" to "जेस्चर",
        "Call Screen" to "कॉल स्क्रीन",
        "Advanced" to "उन्नत",
        "Backup & Restore" to "बैकअप और रीस्टोर",
        "Languages" to "भाषाएं",
        "About" to "के बारे में",

        // Settings Items
        "Default Phone App" to "डिफ़ॉल्ट फ़ोन ऐप",
        "Make Ever Dialer your primary phone application" to "एवर डायलर को अपना मुख्य फ़ोन ऐप बनाएं",
        "Speed Dial" to "स्पीड डायल",
        "Quick Responses" to "त्वरित प्रतिक्रियाएं",
        "Blocked Numbers" to "ब्लॉक किए गए नंबर",
        "Manage blocked phone numbers" to "ब्लॉक किए गए फ़ोन नंबर प्रबंधित करें",
        "Sound & Vibration" to "ध्वनि और कंपन",
        "Display & Theme" to "प्रदर्शन और थीम",
        "Swipe Actions" to "स्वाइप क्रियाएं",
        "Call Recording" to "कॉल रिकॉर्डिंग",
        "SIM Cards" to "सिम कार्ड",
        "Configure default SIM and preferences" to "डिफ़ॉल्ट सिम और प्राथमिकताएं सेट करें",
        "App language and layout direction" to "ऐप भाषा और लेआउट दिशा",
        "About Ever Dialer" to "एवर डायलर के बारे में",
        "Version" to "संस्करण",
        "Search settings" to "सेटिंग्स खोजें",
        "Back" to "पीछे",

        // Languages Screen
        "Choose App Language" to "ऐप की भाषा चुनें",
        "Right-to-Left (RTL)" to "दाएँ से बाएँ (RTL)",
        "Selecting Arabic activates Right-to-Left layout throughout the app, mirroring navigation and swipes while preserving standard dialpad keys." to "अरबी चुनने से पूरे ऐप में दाएँ से बाएँ लेआउट सक्रिय हो जाता है, जबकि मानक डायलपैड कुंजियों को सुरक्षित रखा जाता है।",
        "System default" to "सिस्टम डिफ़ॉल्ट",
        "Follow System" to "सिस्टम के अनुसार",
        "English" to "अंग्रेज़ी",
        "Arabic" to "अरबी",
        "Hindi" to "हिन्दी",
        "Tamil" to "तमिल",

        // Call Logs & Filters
        "All" to "सभी",
        "Missed" to "मिस्ड",
        "Known" to "ज्ञात",
        "Unknown" to "अज्ञात",
        "Incoming" to "इनकमिंग",
        "Outgoing" to "आउटगोइंग",
        "Blocked" to "ब्लॉक किए गए",
        "Today" to "आज",
        "Call Time" to "कॉल का समय",
        "No recent calls" to "कोई हालिया कॉल नहीं",
        "No call history found" to "कोई कॉल इतिहास नहीं मिला",
        "Clear call log" to "कॉल लॉग साफ़ करें",
        "Delete" to "हटाएं",
        "Cancel" to "रद्द करें",
        "Delete entries?" to "प्रविष्टियां हटाएं?",
        "This will permanently delete the selected call log entries." to "यह चयनित कॉल लॉग प्रविष्टियों को स्थायी रूप से हटा देगा।",
        "Grouped based on date" to "तारीख के आधार पर समूहीकृत",

        // Contacts
        "Search contacts" to "संपर्क खोजें",
        "Search contacts..." to "संपर्क खोजें...",
        "Create contact" to "संपर्क बनाएं",
        "Create new contact" to "नया संपर्क बनाएं",
        "Add Contact" to "संपर्क जोड़ें",
        "Add a new contact" to "नया संपर्क जोड़ें",
        "Add to existing contact" to "मौजूदा संपर्क में जोड़ें",
        "Select contact" to "संपर्क चुनें",
        "No contacts found" to "कोई संपर्क नहीं मिला",
        "Ungrouped" to "बिना समूह के",
        "Call" to "कॉल",
        "Message" to "संदेश",
        "Edit" to "संपादित करें",
        "Share" to "साझा करें",
        "Add favorite" to "पसंदीदा में जोड़ें",
        "Add to favorites" to "पसंदीदा में जोड़ें",
        "Remove from favorites" to "पसंदीदा से हटाएं",
        "No favorites yet" to "अभी तक कोई पसंदीदा नहीं",
        "No favourites yet" to "अभी तक कोई पसंदीदा नहीं",
        "No Favorites Yet" to "अभी तक कोई पसंदीदा नहीं",
        "Star a contact to add them here" to "उन्हें यहाँ जोड़ने के लिए किसी संपर्क को स्टार करें",
        "Add a new number to existing contact" to "मौजूदा संपर्क में नया नंबर जोड़ें",
        "Mobile" to "मोबाइल",
        "Home" to "घर",
        "Work" to "कार्य",
        "Other" to "अन्य",

        // In-Call & Call Activity
        "Incoming Call" to "इनकमिंग कॉल",
        "Calling" to "कॉल हो रही है",
        "Calling..." to "कॉल हो रही है...",
        "Call Ended" to "कॉल समाप्त",
        "On Hold" to "होल्ड पर",
        "Choose account" to "खाता चुनें",
        "Declined" to "अस्वीकार किया गया",
        "Hanging up..." to "कॉल कट रही है...",
        "Connecting..." to "कनेक्ट हो रहा है...",
        "Swipe to answer" to "उत्तर देने के लिए स्वाइप करें",
        "Swipe to decline" to "अस्वीकार करने के लिए स्वाइप करें",
        "Answer" to "उत्तर दें",
        "Decline" to "अस्वीकार करें",
        "Mute" to "म्यूट",
        "Unmute" to "अनम्यूट",
        "Speaker" to "स्पीकर",
        "Keypad" to "कीपैड",
        "Hold" to "होल्ड",
        "Unhold" to "अनहोल्ड",
        "Merge" to "मर्ज करें",
        "Merge Calls" to "कॉल मर्ज करें",
        "Add Person" to "व्यक्ति जोड़ें",
        "Add call" to "कॉल जोड़ें",
        "Note" to "नोट",
        "Record" to "रिकॉर्ड",
        "Stop" to "रोकें",
        "Recording" to "रिकॉर्डिंग जारी",
        "Bluetooth" to "ब्लूटूथ",
        "Hang Up" to "कॉल काटें",
        "End call" to "कॉल समाप्त करें",
        "Auto Redial?" to "ऑटो रीडायल?",
        "Redial" to "रीडायल",
        "Redial attempts" to "रीडायल प्रयास",

        // Common Actions
        "OK" to "ठीक है",
        "Done" to "हो गया",
        "Save" to "सहेजें",
        "Close" to "बंद करें",
        "Apply" to "लागू करें",
        "Reset" to "रीसेट करें",
        "Open" to "खोलें",
        "Yes" to "हाँ",
        "No" to "नहीं",
        "Voicemail" to "वॉइसमेल",
        "Clear" to "साफ़ करें"
    )

    private val TAMIL: Map<String, String> = mapOf(
        // Navigation & Tabs
        "Favourites" to "விருப்பங்கள்",
        "Favorites" to "விருப்பங்கள்",
        "Calls" to "அழைப்புகள்",
        "Recents" to "சமீபத்தியவை",
        "Contacts" to "தொடர்புகள்",
        "SMS" to "குறுஞ்செய்தி",
        "Groups" to "குழுக்கள்",
        "Recordings" to "பதிவுகள்",
        "Notes" to "குறிப்புகள்",
        "Dialpad" to "டயல்பேடு",
        "Search in Ever Dialer" to "எவர் டயலரில் தேடுக",
        "Search" to "தேடுக",
        "Settings" to "அமைப்புகள்",

        // Settings Sections
        "General" to "பொதுவானவை",
        "Appearance" to "தோற்றம்",
        "Gestures" to "சைகைகள்",
        "Call Screen" to "அழைப்புத் திரை",
        "Advanced" to "மேம்பட்டவை",
        "Backup & Restore" to "காப்பு & மீட்டமை",
        "Languages" to "மொழிகள்",
        "About" to "பற்றி",

        // Settings Items
        "Default Phone App" to "இயல்புநிலை தொலைபேசி பயன்பாடு",
        "Make Ever Dialer your primary phone application" to "எவர் டயலரை உங்கள் முதன்மை தொலைபேசி பயன்பாடாக அமைக்கவும்",
        "Speed Dial" to "விரைவு டயல்",
        "Quick Responses" to "விரைவு பதில்கள்",
        "Blocked Numbers" to "தடுக்கப்பட்ட எண்கள்",
        "Manage blocked phone numbers" to "தடுக்கப்பட்ட எண்களை நிர்வகிக்கவும்",
        "Sound & Vibration" to "ஒலி & அதிர்வு",
        "Display & Theme" to "காட்சி & தீம்",
        "Swipe Actions" to "ஸ்வைப் செயல்கள்",
        "Call Recording" to "அழைப்புப் பதிவு",
        "SIM Cards" to "சிம் கார்டுகள்",
        "Configure default SIM and preferences" to "இயல்புநிலை சிம் மற்றும் விருப்பங்களை உள்ளமைக்கவும்",
        "App language and layout direction" to "பயன்பாட்டு மொழி மற்றும் தளவமைப்பு திசை",
        "About Ever Dialer" to "எவர் டயலர் பற்றி",
        "Version" to "பதிப்பு",
        "Search settings" to "அமைப்புகளைத் தேடுக",
        "Back" to "பின்செல்",

        // Languages Screen
        "Choose App Language" to "பயன்பாட்டு மொழியைத் தேர்வுசெய்க",
        "Right-to-Left (RTL)" to "வலமிருந்து இடமாக (RTL)",
        "Selecting Arabic activates Right-to-Left layout throughout the app, mirroring navigation and swipes while preserving standard dialpad keys." to "அரபியைத் தேர்ந்தெடுப்பது பயன்பாட்டில் வலமிருந்து இடமாக அமைப்பைச் செயல்படுத்துகிறது, அதே சமயம் நிலையான டயல்பேட் விசைகளைப் பாதுகாக்கிறது.",
        "System default" to "கணினி இயல்புநிலை",
        "Follow System" to "கணினி அமைப்புப்படி",
        "English" to "ஆங்கிலம்",
        "Arabic" to "அரபு",
        "Hindi" to "இந்தி",
        "Tamil" to "தமிழ்",

        // Call Logs & Filters
        "All" to "அனைத்தும்",
        "Missed" to "தவறியவை",
        "Known" to "தெரிந்தவை",
        "Unknown" to "தெரியாதவை",
        "Incoming" to "உள்வரும்",
        "Outgoing" to "வெளிச்செல்லும்",
        "Blocked" to "தடுக்கப்பட்டவை",
        "Today" to "இன்று",
        "Call Time" to "அழைப்பு நேரம்",
        "No recent calls" to "சமீபத்திய அழைப்புகள் எதுவும் இல்லை",
        "No call history found" to "அழைப்பு வரலாறு எதுவும் இல்லை",
        "Clear call log" to "அழைப்புப் பதிவை அழி",
        "Delete" to "நீக்கு",
        "Cancel" to "ரத்துசெய்",
        "Delete entries?" to "பதிவுகளை நீக்கவா?",
        "This will permanently delete the selected call log entries." to "இது தேர்ந்தெடுக்கப்பட்ட அழைப்புப் பதிவுகளை நிரந்தரமாக நீக்கும்.",
        "Grouped based on date" to "தேதி அடிப்படையில் தொகுக்கப்பட்டது",

        // Contacts
        "Search contacts" to "தொடர்புகளைத் தேடுக",
        "Search contacts..." to "தொடர்புகளைத் தேடுக...",
        "Create contact" to "தொடர்பை உருவாக்கு",
        "Create new contact" to "புதிய தொடர்பை உருவாக்கு",
        "Add Contact" to "தொடர்பைச் சேர்",
        "Add a new contact" to "புதிய தொடர்பைச் சேர்",
        "Add to existing contact" to "ஏற்கனவே உள்ள தொடர்பில் சேர்",
        "Select contact" to "தொடர்பைத் தேர்வுசெய்க",
        "No contacts found" to "தொடர்புகள் எதுவும் இல்லை",
        "Ungrouped" to "குழுவிலா",
        "Call" to "அழை",
        "Message" to "செய்தி",
        "Edit" to "திருத்து",
        "Share" to "பகிர்",
        "Add favorite" to "விருப்பத்தில் சேர்",
        "Add to favorites" to "விருப்பத்தில் சேர்",
        "Remove from favorites" to "விருப்பத்திலிருந்து நீக்கு",
        "No favorites yet" to "இன்னும் விருப்பங்கள் எதுவும் இல்லை",
        "No favourites yet" to "இன்னும் விருப்பங்கள் எதுவும் இல்லை",
        "No Favorites Yet" to "இன்னும் விருப்பங்கள் எதுவும் இல்லை",
        "Star a contact to add them here" to "அவர்களை இங்கே சேர்க்க தொடர்பை நட்சத்திரமிடுங்கள்",
        "Add a new number to existing contact" to "ஏற்கனவே உள்ள தொடர்பில் புதிய எண்ணைச் சேர்",
        "Mobile" to "மொபைல்",
        "Home" to "வீடு",
        "Work" to "பணி",
        "Other" to "மற்றவை",

        // In-Call & Call Activity
        "Incoming Call" to "உள்வரும் அழைப்பு",
        "Calling" to "அழைக்கிறது",
        "Calling..." to "அழைக்கிறது...",
        "Call Ended" to "அழைப்பு முடிந்தது",
        "On Hold" to "ஹோல்டில் உள்ளது",
        "Choose account" to "கணக்கைத் தேர்வுசெய்க",
        "Declined" to "நிராகரிக்கப்பட்டது",
        "Hanging up..." to "அழைப்பு முடிகிறது...",
        "Connecting..." to "இணைகிறது...",
        "Swipe to answer" to "பதிலளிக்க ஸ்வைப் செய்க",
        "Swipe to decline" to "நிராகரிக்க ஸ்வைப் செய்க",
        "Answer" to "பதில்",
        "Decline" to "நிராகரி",
        "Mute" to "ஒலி நிறுத்து",
        "Unmute" to "ஒலி இயக்கு",
        "Speaker" to "ஸ்பீக்கர்",
        "Keypad" to "விசைப்பலகை",
        "Hold" to "ஹோல்டு",
        "Unhold" to "தொடர்க",
        "Merge" to "இணை",
        "Merge Calls" to "அழைப்புகளை இணை",
        "Add Person" to "நபரைச் சேர்",
        "Add call" to "அழைப்பைச் சேர்",
        "Note" to "குறிப்பு",
        "Record" to "பதிவு செய்",
        "Stop" to "நிறுத்து",
        "Recording" to "பதிவாகிறது",
        "Bluetooth" to "புளூடூத்",
        "Hang Up" to "இணைப்பைத் துண்டி",
        "End call" to "அழைப்பை முடி",
        "Auto Redial?" to "தானியங்கு மறுஅழைப்பு?",
        "Redial" to "மறுஅழைப்பு",
        "Redial attempts" to "மறுஅழைப்பு முயற்சிகள்",

        // Common Actions
        "OK" to "சரி",
        "Done" to "முடிந்தது",
        "Save" to "சேமி",
        "Close" to "மூடு",
        "Apply" to "பயன்படுத்து",
        "Reset" to "மீட்டமை",
        "Open" to "திற",
        "Yes" to "ஆம்",
        "No" to "இல்லை",
        "Voicemail" to "குரலஞ்சல்",
        "Clear" to "அழி"
    )
}
