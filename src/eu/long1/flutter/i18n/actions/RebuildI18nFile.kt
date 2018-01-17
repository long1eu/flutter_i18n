package eu.long1.flutter.i18n.actions

import FlutterI18nIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import eu.long1.flutter.i18n.workers.I18nFile
import io.flutter.utils.FlutterModuleUtils

class RebuildI18nFile : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        I18nFile(e.project!!).generate()
    }

    override fun update(e: AnActionEvent) {
        e.project?.let {
            if (!FlutterModuleUtils.usesFlutter(it)) {
                e.presentation.isEnabled = false
                return
            }
        }

        e.presentation.icon = FlutterI18nIcons.ArbRefreshAction
    }
}
