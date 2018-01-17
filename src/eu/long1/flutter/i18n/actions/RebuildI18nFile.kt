package eu.long1.flutter.i18n.actions

import FlutterI18nIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.long1.flutter.i18n.workers.I18nFile
import io.flutter.utils.FlutterModuleUtils

class RebuildI18nFile : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val resFolder = e.project!!.baseDir.findChild("res") ?: e.project!!.baseDir.createChildDirectory(this, "res")
        val valuesFolder = resFolder.findChild("values") ?: resFolder.createChildDirectory(this, "values")
        I18nFile.generate(e.project!!, valuesFolder)
    }

    override fun update(e: AnActionEvent) {
        if (!FlutterModuleUtils.usesFlutter(e.project!!)) {
            e.presentation.isEnabled = false
            return
        }

        e.presentation.icon = FlutterI18nIcons.ArbRefreshAction
    }
}
