package eu.long1.flutter.i18n

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import eu.long1.flutter.i18n.actions.NewArbFileAction
import eu.long1.flutter.i18n.actions.RebuildI18nFile
import io.flutter.utils.FlutterModuleUtils

class Register(private val project: Project) : ProjectComponent {

    override fun initComponent() {}

    override fun disposeComponent() {}

    override fun getComponentName(): String {
        return "Flutter i18n"
    }

    override fun projectOpened() {
        if (!FlutterModuleUtils.hasFlutterModule(project)) {
            return
        }

        val am = ActionManager.getInstance()

        if (am.getAction(REBUILD_FILE_ACTION_ID) != null) {
            return
        }

        val newFileAction = NewArbFileAction()
        am.registerAction(NEW_FILE_ACTION_ID, newFileAction)

        val rebuildFileAction = RebuildI18nFile()
        am.registerAction(REBUILD_FILE_ACTION_ID, rebuildFileAction)

        val windowM = am.getAction("ToolbarRunGroup") as DefaultActionGroup
        windowM.addSeparator()
        windowM.add(newFileAction)
        windowM.add(rebuildFileAction)
        windowM.addSeparator()
    }

    override fun projectClosed() {
        val am = ActionManager.getInstance()
        am.getAction(NEW_FILE_ACTION_ID)?.templatePresentation?.isEnabled = false
        am.getAction(REBUILD_FILE_ACTION_ID)?.templatePresentation?.isEnabled = false
    }

    companion object {
        private const val NEW_FILE_ACTION_ID = "FlutterI18n.NewArbFileAction"
        private const val REBUILD_FILE_ACTION_ID = "FlutterI18n.RebuildI18nFile"
    }
}
