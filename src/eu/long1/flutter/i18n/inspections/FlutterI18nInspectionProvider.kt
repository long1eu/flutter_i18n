package eu.long1.flutter.i18n.inspections

import com.intellij.codeInspection.InspectionToolProvider

class FlutterI18nInspectionProvider : InspectionToolProvider {
    override fun getInspectionClasses(): Array<Class<*>> = arrayOf(CreateStringInspector::class.java)
}