package eu.long1.flutter.i18n.inspections

import com.intellij.codeInspection.InspectionToolProvider
import com.intellij.codeInspection.LocalInspectionTool

class FlutterI18nInspectionProvider : InspectionToolProvider {

    override fun getInspectionClasses(): Array<Class<out LocalInspectionTool>> =
        arrayOf(JsonKeysInspector::class.java, CreateStringInspector::class.java)
}