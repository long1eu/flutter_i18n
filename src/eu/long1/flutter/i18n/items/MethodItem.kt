package eu.long1.flutter.i18n.items

import com.jetbrains.lang.dart.psi.DartComponent

data class MethodItem(val method: String?, val dartMethod: DartComponent?) {
    companion object {
        val empty = MethodItem(null, null)
    }
}