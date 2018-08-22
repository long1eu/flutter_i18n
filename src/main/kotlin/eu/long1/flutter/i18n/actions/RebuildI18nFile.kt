package eu.long1.flutter.i18n.actions

import io.flutter.FlutterI18nIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import eu.long1.flutter.i18n.workers.I18nFileGenerator
import io.flutter.utils.FlutterModuleUtils

class RebuildI18nFile : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        WriteCommandAction.runWriteCommandAction(e.project) { I18nFileGenerator(e.project!!).generate() }
    }

    override fun update(e: AnActionEvent) {
        if (e.project != null && !FlutterModuleUtils.hasFlutterModule(e.project!!)) {
            e.presentation.isEnabled = false
            return
        }

        e.presentation.icon = FlutterI18nIcons.ArbRefreshAction
    }
}
