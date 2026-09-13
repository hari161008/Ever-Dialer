package com.coolappstore.everdialer.by.svhp.controller.util

/**
 * Reads the per-section customization saved by Settings → Appearance → "Context Menu Elements"
 * (show/hide + reorder) and resolves it into the final ordered list of visible item keys that
 * the real long-press context menus (Favourites, Call Logs, Contacts) should render.
 *
 * Keys/format here must stay in sync with InterfaceScreen's contextMenuShowKey/contextMenuOrderKey.
 */
object ContextMenuPrefs {

    const val SECTION_FAVORITES = "favorites"
    const val SECTION_CALL_LOGS = "call_logs"
    const val SECTION_CONTACTS  = "contacts"

    private fun orderKey(section: String) = "context_menu_${section}_order"
    private fun showKey(section: String, itemKey: String) = "context_menu_${section}_show_$itemKey"

    /**
     * Returns the ordered, visibility-filtered list of item keys for [section], falling back to
     * [defaultOrder] for any keys the user hasn't customized (or a fresh install with no saved
     * preference at all).
     */
    fun resolvedKeys(prefs: PreferenceManager, section: String, defaultOrder: List<String>): List<String> {
        val saved = prefs.getString(orderKey(section), null)
        val savedKeys = saved?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        val ordered = mutableListOf<String>()
        ordered.addAll(savedKeys.filter { it in defaultOrder })
        defaultOrder.forEach { key -> if (key !in ordered) ordered.add(key) }
        if (section == SECTION_CALL_LOGS) {
            val copyIndex = ordered.indexOf("copy_number")
            val addIndex = ordered.indexOf("add_to_contacts")
            val deleteIndex = ordered.indexOf("delete_call_log")
            if (copyIndex != -1 && addIndex != -1) {
                if (deleteIndex != -1 && addIndex > deleteIndex) {
                    ordered.removeAt(addIndex)
                    val newCopyIndex = ordered.indexOf("copy_number")
                    ordered.add(newCopyIndex + 1, "add_to_contacts")
                    prefs.setString(orderKey(section), ordered.joinToString(","))
                } else if (addIndex != copyIndex + 1 && (deleteIndex == -1 || copyIndex < deleteIndex)) {
                    ordered.removeAt(addIndex)
                    val newCopyIndex = ordered.indexOf("copy_number")
                    ordered.add(newCopyIndex + 1, "add_to_contacts")
                    prefs.setString(orderKey(section), ordered.joinToString(","))
                }
            }
        }
        if (section == SECTION_CONTACTS && "call" in ordered) {
            val selectIndex = ordered.indexOf("select")
            val callIndex = ordered.indexOf("call")
            val deleteIndex = ordered.indexOf("delete_contact")
            if (deleteIndex != -1 && callIndex > deleteIndex) {
                ordered.removeAt(callIndex)
                val targetIndex = if (selectIndex != -1) selectIndex + 1 else 0
                ordered.add(targetIndex, "call")
                prefs.setString(orderKey(section), ordered.joinToString(","))
            }
        }
        return ordered.filter { prefs.getBoolean(showKey(section, it), true) }
    }
}
