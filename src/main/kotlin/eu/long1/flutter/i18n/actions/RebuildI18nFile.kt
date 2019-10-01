package eu.long1.flutter.i18n.actions

import FlutterI18nIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import eu.long1.flutter.i18n.files.FileHelpers
import eu.long1.flutter.i18n.workers.I18nFileGenerator

class RebuildI18nFile : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            WriteCommandAction.runWriteCommandAction(project) {
                I18nFileGenerator(project).generate()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        e.project?.let { project ->
            e.presentation.isEnabled = FileHelpers.shouldActivateFor(project)
        }
        e.presentation.icon = FlutterI18nIcons.ArbRefreshAction
    }
}
