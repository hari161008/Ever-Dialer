package com.modiprincekr.thedialer.controller

import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.controller.util.makeCall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

enum class UssdCategory(val label: String) {
    CUSTOM("Custom / Saved"),
    CARRIER("Carrier Codes"),
    STANDARD("Standard Network"),
    DIAGNOSTIC("Android System"),
    HARDWARE("Hardware Tests")
}

data class UssdCodeItem(
    val id: String,
    val title: String,
    val code: String,
    val category: UssdCategory,
    val countryCode: String? = null,
    val brand: String? = null,
    val isCustom: Boolean = false,
    val description: String? = null
)

data class UssdCountry(
    val code: String,
    val name: String,
    val flag: String
)

object UssdRepository {
    private val _response = MutableStateFlow<Pair<String, String>?>(null)
    val response: StateFlow<Pair<String, String>?> = _response.asStateFlow()

    fun post(request: String, response: String) {
        _response.value = request to response
    }

    fun clear() {
        _response.value = null
    }

    val SUPPORTED_COUNTRIES = listOf(
        UssdCountry("IN", "India", "🇮🇳"),
        UssdCountry("US", "United States", "🇺🇸"),
        UssdCountry("UK", "United Kingdom", "🇬🇧"),
        UssdCountry("AE", "United Arab Emirates", "🇦🇪"),
        UssdCountry("SA", "Saudi Arabia", "🇸🇦"),
        UssdCountry("BD", "Bangladesh", "🇧🇩"),
        UssdCountry("PK", "Pakistan", "🇵🇰"),
        UssdCountry("ID", "Indonesia", "🇮🇩"),
        UssdCountry("PH", "Philippines", "🇵🇭"),
        UssdCountry("NG", "Nigeria", "🇳🇬"),
        UssdCountry("KE", "Kenya", "🇰🇪"),
        UssdCountry("GH", "Ghana", "🇬🇭"),
        UssdCountry("ZA", "South Africa", "🇿🇦")
    )

    fun getCountryByCode(code: String): UssdCountry {
        return SUPPORTED_COUNTRIES.firstOrNull { it.code.equals(code, ignoreCase = true) }
            ?: SUPPORTED_COUNTRIES[0]
    }

    fun isCountrySupported(code: String): Boolean {
        return SUPPORTED_COUNTRIES.any { it.code.equals(code, ignoreCase = true) }
    }

    fun getSelectedCountry(context: Context, prefs: PreferenceManager): String {
        val saved = prefs.getString(PreferenceManager.KEY_USSD_COUNTRY, null)
        if (!saved.isNullOrBlank() && isCountrySupported(saved)) return saved
        // Try SIM Country ISO
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        val simCountry = tm?.simCountryIso?.uppercase()?.ifBlank { null }
        if (simCountry != null && isCountrySupported(simCountry)) return simCountry
        val networkCountry = tm?.networkCountryIso?.uppercase()?.ifBlank { null }
        if (networkCountry != null && isCountrySupported(networkCountry)) return networkCountry
        // Try Locale
        val localeCountry = Locale.getDefault().country.uppercase()
        if (isCountrySupported(localeCountry)) return localeCountry
        // Default to India
        return "IN"
    }

    // ── Preloaded Carrier Codes ──────────────────────────────────────────────────
    private val CARRIER_CODES: List<UssdCodeItem> = listOf(
        // India (IN)
        UssdCodeItem("in_airtel_bal", "Main Balance", "*123#", UssdCategory.CARRIER, "IN", "Airtel"),
        UssdCodeItem("in_airtel_offers", "Offers & Packs", "*121#", UssdCategory.CARRIER, "IN", "Airtel"),
        UssdCodeItem("in_airtel_num", "Your Number", "*282#", UssdCategory.CARRIER, "IN", "Airtel"),
        UssdCodeItem("in_airtel_data", "Data Balance", "*121*2#", UssdCategory.CARRIER, "IN", "Airtel"),
        UssdCodeItem("in_jio_bal", "Balance & Validity", "*333#", UssdCategory.CARRIER, "IN", "Jio"),
        UssdCodeItem("in_jio_num", "Your Number", "*1#", UssdCategory.CARRIER, "IN", "Jio"),
        UssdCodeItem("in_vi_bal", "Balance & Offers", "*199#", UssdCategory.CARRIER, "IN", "Vi"),
        UssdCodeItem("in_vi_num", "Your Number", "*199*2*2#", UssdCategory.CARRIER, "IN", "Vi"),
        UssdCodeItem("in_bsnl_bal", "Main Balance", "*123#", UssdCategory.CARRIER, "IN", "BSNL"),
        UssdCodeItem("in_bsnl_validity", "Check Validity", "*124#", UssdCategory.CARRIER, "IN", "BSNL"),
        UssdCodeItem("in_bsnl_num", "Your Number", "*888#", UssdCategory.CARRIER, "IN", "BSNL"),

        // United States (US)
        UssdCodeItem("us_att_bal", "Check Balance", "*225#", UssdCategory.CARRIER, "US", "AT&T"),
        UssdCodeItem("us_att_data", "Data Usage", "*3282#", UssdCategory.CARRIER, "US", "AT&T"),
        UssdCodeItem("us_att_bill", "View Bill Balance", "*8439#", UssdCategory.CARRIER, "US", "AT&T"),
        UssdCodeItem("us_tmo_bal", "Account Balance", "#225#", UssdCategory.CARRIER, "US", "T-Mobile"),
        UssdCodeItem("us_tmo_data", "Check Usage", "#932#", UssdCategory.CARRIER, "US", "T-Mobile"),
        UssdCodeItem("us_tmo_plan", "Plan Info", "#752#", UssdCategory.CARRIER, "US", "T-Mobile"),
        UssdCodeItem("us_vzw_bal", "Check Balance", "#225", UssdCategory.CARRIER, "US", "Verizon"),
        UssdCodeItem("us_vzw_data", "Data Usage", "#3282", UssdCategory.CARRIER, "US", "Verizon"),

        // United Kingdom (UK)
        UssdCodeItem("uk_ee_bal", "Check Balance & Usage", "*110#", UssdCategory.CARRIER, "UK", "EE"),
        UssdCodeItem("uk_voda_bal", "Balance & Allowances", "*#1345#", UssdCategory.CARRIER, "UK", "Vodafone"),
        UssdCodeItem("uk_o2_bal", "Check Balance", "*#10#", UssdCategory.CARRIER, "UK", "O2"),
        UssdCodeItem("uk_three_bal", "Check Balance & Allowances", "*111#", UssdCategory.CARRIER, "UK", "Three"),

        // United Arab Emirates (AE)
        UssdCodeItem("ae_eti_bal", "Check Balance", "*121#", UssdCategory.CARRIER, "AE", "Etisalat"),
        UssdCodeItem("ae_eti_data", "Data Balance", "*170#", UssdCategory.CARRIER, "AE", "Etisalat"),
        UssdCodeItem("ae_eti_num", "Own Number", "*101*1*3#", UssdCategory.CARRIER, "AE", "Etisalat"),
        UssdCodeItem("ae_du_bal", "Check Balance", "*135#", UssdCategory.CARRIER, "AE", "du"),
        UssdCodeItem("ae_du_data", "Data Usage", "*135*5#", UssdCategory.CARRIER, "AE", "du"),

        // Saudi Arabia (SA)
        UssdCodeItem("sa_stc_bal", "Check Balance", "*166#", UssdCategory.CARRIER, "SA", "STC"),
        UssdCodeItem("sa_stc_menu", "Sawa Services Menu", "*888#", UssdCategory.CARRIER, "SA", "STC"),
        UssdCodeItem("sa_mob_bal", "Check Balance", "*1411#", UssdCategory.CARRIER, "SA", "Mobily"),
        UssdCodeItem("sa_mob_menu", "Main Services Menu", "*1100#", UssdCategory.CARRIER, "SA", "Mobily"),
        UssdCodeItem("sa_zain_bal", "Check Balance", "*142#", UssdCategory.CARRIER, "SA", "Zain"),
        UssdCodeItem("sa_zain_menu", "Account Menu", "*959#", UssdCategory.CARRIER, "SA", "Zain"),

        // Bangladesh (BD)
        UssdCodeItem("bd_gp_bal", "Check Balance", "*566#", UssdCategory.CARRIER, "BD", "Grameenphone"),
        UssdCodeItem("bd_gp_data", "Internet Balance", "*121*1*4#", UssdCategory.CARRIER, "BD", "Grameenphone"),
        UssdCodeItem("bd_gp_num", "Own Number", "*2#", UssdCategory.CARRIER, "BD", "Grameenphone"),
        UssdCodeItem("bd_bl_bal", "Check Balance", "*124#", UssdCategory.CARRIER, "BD", "Banglalink"),
        UssdCodeItem("bd_bl_data", "Internet Balance", "*5000*500#", UssdCategory.CARRIER, "BD", "Banglalink"),
        UssdCodeItem("bd_bl_num", "Own Number", "*511#", UssdCategory.CARRIER, "BD", "Banglalink"),
        UssdCodeItem("bd_robi_bal", "Check Balance", "*222#", UssdCategory.CARRIER, "BD", "Robi"),
        UssdCodeItem("bd_robi_data", "Internet Balance", "*3#", UssdCategory.CARRIER, "BD", "Robi"),
        UssdCodeItem("bd_robi_num", "Own Number", "*140*2*4#", UssdCategory.CARRIER, "BD", "Robi"),
        UssdCodeItem("bd_tele_bal", "Check Balance", "*152#", UssdCategory.CARRIER, "BD", "Teletalk"),
        UssdCodeItem("bd_tele_num", "Own Number", "*551#", UssdCategory.CARRIER, "BD", "Teletalk"),

        // Pakistan (PK)
        UssdCodeItem("pk_jazz_bal", "Check Balance", "*111#", UssdCategory.CARRIER, "PK", "Jazz"),
        UssdCodeItem("pk_jazz_data", "Internet Balance", "*117*4*2#", UssdCategory.CARRIER, "PK", "Jazz"),
        UssdCodeItem("pk_jazz_num", "Own Number", "*99#", UssdCategory.CARRIER, "PK", "Jazz"),
        UssdCodeItem("pk_telenor_bal", "Check Balance", "*444#", UssdCategory.CARRIER, "PK", "Telenor"),
        UssdCodeItem("pk_telenor_data", "Internet Balance", "*999#", UssdCategory.CARRIER, "PK", "Telenor"),
        UssdCodeItem("pk_zong_bal", "Check Balance", "*222#", UssdCategory.CARRIER, "PK", "Zong"),
        UssdCodeItem("pk_zong_data", "Internet Balance", "*102#", UssdCategory.CARRIER, "PK", "Zong"),
        UssdCodeItem("pk_zong_num", "Own Number", "*8#", UssdCategory.CARRIER, "PK", "Zong"),
        UssdCodeItem("pk_ufone_bal", "Check Balance", "*124#", UssdCategory.CARRIER, "PK", "Ufone"),
        UssdCodeItem("pk_ufone_data", "Internet Balance", "*706#", UssdCategory.CARRIER, "PK", "Ufone"),

        // Indonesia (ID)
        UssdCodeItem("id_tsel_bal", "Check Balance & Validity", "*888#", UssdCategory.CARRIER, "ID", "Telkomsel"),
        UssdCodeItem("id_tsel_menu", "Main Internet Menu", "*363#", UssdCategory.CARRIER, "ID", "Telkomsel"),
        UssdCodeItem("id_isat_bal", "Check Balance", "*123#", UssdCategory.CARRIER, "ID", "Indosat"),
        UssdCodeItem("id_isat_menu", "Internet Package", "*363#", UssdCategory.CARRIER, "ID", "Indosat"),
        UssdCodeItem("id_xl_bal", "Check Balance & Info", "*808#", UssdCategory.CARRIER, "ID", "XL Axiata"),
        UssdCodeItem("id_xl_menu", "Main Services Menu", "*123#", UssdCategory.CARRIER, "ID", "XL Axiata"),

        // Philippines (PH)
        UssdCodeItem("ph_globe_menu", "Load & Promo Menu", "*143#", UssdCategory.CARRIER, "PH", "Globe"),
        UssdCodeItem("ph_smart_menu", "Balance & Services", "*123#", UssdCategory.CARRIER, "PH", "Smart"),
        UssdCodeItem("ph_dito_bal", "Account & Balance", "*185#", UssdCategory.CARRIER, "PH", "DITO"),

        // Nigeria (NG)
        UssdCodeItem("ng_bal", "Check Balance (All Networks)", "*310#", UssdCategory.CARRIER, "NG", "Harmonized"),
        UssdCodeItem("ng_data", "Data Balance (All Networks)", "*323#", UssdCategory.CARRIER, "NG", "Harmonized"),
        UssdCodeItem("ng_recharge", "Airtime Recharge", "*311#", UssdCategory.CARRIER, "NG", "Harmonized"),
        UssdCodeItem("ng_borrow", "Borrow Services", "*303#", UssdCategory.CARRIER, "NG", "Harmonized"),

        // Kenya (KE)
        UssdCodeItem("ke_safaricom_bal", "Check Balance", "*144#", UssdCategory.CARRIER, "KE", "Safaricom"),
        UssdCodeItem("ke_safaricom_menu", "Data & Services Menu", "*544#", UssdCategory.CARRIER, "KE", "Safaricom"),
        UssdCodeItem("ke_safaricom_mpesa", "M-PESA Menu", "*334#", UssdCategory.CARRIER, "KE", "Safaricom"),
        UssdCodeItem("ke_airtel_bal", "Check Balance", "*133#", UssdCategory.CARRIER, "KE", "Airtel"),
        UssdCodeItem("ke_airtel_menu", "Data Bundles Menu", "*544#", UssdCategory.CARRIER, "KE", "Airtel"),

        // Ghana (GH)
        UssdCodeItem("gh_mtn_bal", "Check Balance", "*124#", UssdCategory.CARRIER, "GH", "MTN"),
        UssdCodeItem("gh_mtn_momo", "Mobile Money", "*170#", UssdCategory.CARRIER, "GH", "MTN"),
        UssdCodeItem("gh_mtn_mashup", "Mashup Pulse", "*567#", UssdCategory.CARRIER, "GH", "MTN"),
        UssdCodeItem("gh_telecel_bal", "Check Balance", "*124#", UssdCategory.CARRIER, "GH", "Telecel"),
        UssdCodeItem("gh_telecel_menu", "Main Services Menu", "*700#", UssdCategory.CARRIER, "GH", "Telecel"),
        UssdCodeItem("gh_at_bal", "Check Balance", "*124#", UssdCategory.CARRIER, "GH", "AT"),
        UssdCodeItem("gh_at_menu", "Main Menu", "*111#", UssdCategory.CARRIER, "GH", "AT"),

        // South Africa (ZA)
        UssdCodeItem("za_voda_bal", "Airtime Balance", "*136#", UssdCategory.CARRIER, "ZA", "Vodacom"),
        UssdCodeItem("za_voda_data", "Data Balance", "*135#", UssdCategory.CARRIER, "ZA", "Vodacom"),
        UssdCodeItem("za_mtn_bal", "Check Balance", "*136#", UssdCategory.CARRIER, "ZA", "MTN"),
        UssdCodeItem("za_mtn_menu", "Main Menu", "*135#", UssdCategory.CARRIER, "ZA", "MTN"),
        UssdCodeItem("za_telkom_bal", "Check Balance", "*188#", UssdCategory.CARRIER, "ZA", "Telkom"),
        UssdCodeItem("za_cellc_bal", "Check Balance", "*147#", UssdCategory.CARRIER, "ZA", "Cell C")
    )

    // ── Preloaded Standard Network Codes ────────────────────────────────────────
    val STANDARD_CODES: List<UssdCodeItem> = listOf(
        UssdCodeItem("std_imei", "Show IMEI", "*#06#", UssdCategory.STANDARD, description = "Displays device IMEI identity numbers"),
        UssdCodeItem("std_cf_all", "Call Forwarding Status", "*#21#", UssdCategory.STANDARD, description = "Checks status of unconditional call forwarding"),
        UssdCodeItem("std_cf_noanswer", "Forwarding When Unanswered", "*#61#", UssdCategory.STANDARD, description = "Checks forwarding status when call is unanswered"),
        UssdCodeItem("std_cf_unreachable", "Forwarding When Unreachable", "*#62#", UssdCategory.STANDARD, description = "Checks forwarding status when out of coverage or off"),
        UssdCodeItem("std_cf_busy", "Forwarding When Busy", "*#67#", UssdCategory.STANDARD, description = "Checks forwarding status when line is busy"),
        UssdCodeItem("std_cf_cancel", "Cancel All Call Forwarding", "##002#", UssdCategory.STANDARD, description = "Disables and cancels all active call forwardings"),
        UssdCodeItem("std_cw_status", "Call Waiting Status", "*#43#", UssdCategory.STANDARD, description = "Checks whether Call Waiting is active"),
        UssdCodeItem("std_cw_on", "Turn Call Waiting On", "*43#", UssdCategory.STANDARD, description = "Enables Call Waiting for incoming calls"),
        UssdCodeItem("std_cw_off", "Turn Call Waiting Off", "#43#", UssdCategory.STANDARD, description = "Disables Call Waiting"),
        UssdCodeItem("std_clid_out", "Outgoing Caller ID Status", "*#31#", UssdCategory.STANDARD, description = "Checks whether your number is hidden or shown when calling"),
        UssdCodeItem("std_clid_in", "Incoming Caller ID Status", "*#30#", UssdCategory.STANDARD, description = "Checks presentation status of incoming caller ID")
    )

    // ── Preloaded Android System & Diagnostics ──────────────────────────────────
    val DIAGNOSTIC_CODES: List<UssdCodeItem> = listOf(
        UssdCodeItem("diag_testing", "Android Testing & Phone Info", "*#*#4636#*#*", UssdCategory.DIAGNOSTIC, description = "Testing menu: Phone info, Battery, Usage stats, Wi-Fi info"),
        UssdCodeItem("diag_fcm", "Google Play Services Diagnostics", "*#*#426#*#*", UssdCategory.DIAGNOSTIC, description = "FCM client events and Google Play Services push status"),
        UssdCodeItem("diag_calendar", "Calendar Storage Info", "*#*#225#*#*", UssdCategory.DIAGNOSTIC, description = "Calendar event counts and account storage statistics"),
        UssdCodeItem("diag_wifi_mac", "Wi-Fi MAC Address", "*#*#232338#*#*", UssdCategory.DIAGNOSTIC, description = "Displays device Wi-Fi MAC physical address"),
        UssdCodeItem("diag_bt_addr", "Bluetooth Address", "*#*#232337#*#", UssdCategory.DIAGNOSTIC, description = "Displays device Bluetooth physical hardware address")
    )

    // ── Preloaded Brand Hardware Diagnostic Menus ───────────────────────────────
    val HARDWARE_CODES: List<UssdCodeItem> = listOf(
        // Vivo / iQOO
        UssdCodeItem("brand_vivo_factory", "Engineering / Factory Test", "*#*#558#*#*", UssdCategory.HARDWARE, brand = "Vivo / iQOO", description = "Factory hardware test suite and calibration"),
        UssdCodeItem("brand_vivo_info", "Software & Hardware Info", "*#*#4838#*#*", UssdCategory.HARDWARE, brand = "Vivo / iQOO", description = "Vivo system diagnostics and testing"),

        // Nothing
        UssdCodeItem("brand_nothing_cit", "Nothing Diagnostic / CIT", "*#*#9#*#*", UssdCategory.HARDWARE, brand = "Nothing", description = "Nothing OS hardware diagnostic and CIT menu"),
        UssdCodeItem("brand_nothing_test", "Hardware Test Mode", "*#*#0#*#*", UssdCategory.HARDWARE, brand = "Nothing", description = "Component tests for display, touch, and sensors"),

        // Lava
        UssdCodeItem("brand_lava_cit", "Lava Hardware Test Mode", "*#*#3646633#*#*", UssdCategory.HARDWARE, brand = "Lava", description = "Lava Engineering & CIT test mode"),
        UssdCodeItem("brand_lava_factory", "Factory Test Mode", "*#87#", UssdCategory.HARDWARE, brand = "Lava", description = "Factory hardware checkout menu"),

        // Motorola
        UssdCodeItem("brand_moto_cqa", "CQA Hardware Test Menu", "*#*#2486#*#*", UssdCategory.HARDWARE, brand = "Motorola", description = "Motorola CQA engineering and factory diagnostics"),

        // Samsung
        UssdCodeItem("brand_sam_test", "Hardware Diagnostic Suite", "*#0*#", UssdCategory.HARDWARE, brand = "Samsung", description = "Full hardware testing suite: LCD, Touch, Sensors, Audio"),
        UssdCodeItem("brand_sam_dump", "SysDump System Utility", "*#9900#", UssdCategory.HARDWARE, brand = "Samsung", description = "System dump logs, RAM dumps, and diagnostic tools"),

        // Xiaomi / Redmi / Poco
        UssdCodeItem("brand_mi_cit", "CIT Hardware Test Menu", "*#*#6484#*#*", UssdCategory.HARDWARE, brand = "Xiaomi / Poco", description = "MIUI/HyperOS CIT hardware diagnostic suite"),
        UssdCodeItem("brand_mi_qc", "Alternate Hardware Test", "*#*#64663#*#*", UssdCategory.HARDWARE, brand = "Xiaomi / Poco", description = "Component quality control diagnostic menu"),

        // OnePlus / Oppo / Realme
        UssdCodeItem("brand_oppo_diag", "Engineering Diagnostics", "*#800#", UssdCategory.HARDWARE, brand = "OnePlus / Oppo / Realme", description = "ColorOS / OxygenOS / RealmeUI feedback & test menu"),
        UssdCodeItem("brand_oppo_version", "Software Version Info", "*#6776#", UssdCategory.HARDWARE, brand = "OnePlus / Oppo / Realme", description = "Detailed software build and hardware version info")
    )

    fun getCarrierCodesForCountry(countryCode: String): List<UssdCodeItem> {
        return CARRIER_CODES.filter { it.countryCode.equals(countryCode, ignoreCase = true) }
    }

    fun getAllCodesForCountry(
        context: Context,
        prefs: PreferenceManager,
        countryCode: String? = null
    ): List<UssdCodeItem> {
        val activeCountry = countryCode ?: getSelectedCountry(context, prefs)
        val custom = prefs.getCustomUssdCodes()
        val carriers = getCarrierCodesForCountry(activeCountry)
        return custom + carriers + STANDARD_CODES + DIAGNOSTIC_CODES + HARDWARE_CODES
    }

    /**
     * Filters active USSD / Secret codes matching a dialed query (e.g. "*", "*1", "*#", "*#*#46").
     */
    fun getMatchingCodes(
        context: Context,
        prefs: PreferenceManager,
        query: String
    ): List<UssdCodeItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()

        val allCodes = getAllCodesForCountry(context, prefs)
        return allCodes.filter { item ->
            item.code.startsWith(q, ignoreCase = true) ||
            item.code.contains(q, ignoreCase = true) ||
            (q.length >= 2 && item.title.contains(q.removePrefix("*").removePrefix("#"), ignoreCase = true))
        }.sortedWith(
            compareByDescending<UssdCodeItem> { it.isCustom }
                .thenByDescending { it.code.startsWith(q, ignoreCase = true) }
                .thenBy { it.code.length }
        ).take(12)
    }

    /**
     * Executes a code appropriately:
     * - IMEI / SAR opens native device settings
     * - *#*#DIGITS#*#* broadcasts secret code
     * - USSD / MMI / phone numbers placed through Telecom makeCall
     */
    fun executeCode(context: Context, code: String) {
        val trimmed = code.trim()
        if (trimmed.isEmpty()) return

        if (trimmed == "*#06#") {
            try {
                context.startActivity(
                    Intent(android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {}
            return
        }

        if (trimmed == "*#07#") {
            val sarActions = listOf(
                "android.settings.SAR_INFORMATION",
                "android.settings.RF_EXPOSURE_SETTINGS",
                android.provider.Settings.ACTION_DEVICE_INFO_SETTINGS
            )
            for (action in sarActions) {
                try {
                    context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    return
                } catch (_: Exception) {}
            }
        }

        val secretMatch = Regex("^\\*#\\*#(\\d+)#\\*#\\*$").find(trimmed)
        if (secretMatch != null) {
            val digits = secretMatch.groupValues[1]
            try {
                val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                telephonyManager?.sendDialerSpecialCode(digits)
            } catch (_: Exception) {}
            val uri = android.net.Uri.parse("android_secret_code://$digits")
            try {
                context.sendBroadcast(
                    Intent("android.provider.Telephony.SECRET_CODE", uri).apply {
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    }
                )
            } catch (_: Exception) {}
            try {
                context.sendBroadcast(
                    Intent("android.telephony.action.SECRET_CODE", uri).apply {
                        addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES)
                    }
                )
            } catch (_: Exception) {}
            return
        }

        makeCall(context, trimmed)
    }
}
