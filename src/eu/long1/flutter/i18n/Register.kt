package eu.long1.flutter.i18n

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import eu.long1.flutter.i18n.actions.RebuildI18nFile
import io.flutter.utils.FlutterModuleUtils

class Register(private val project: Project) : ProjectComponent {

    override fun initComponent() {}

    override fun disposeComponent() {}

    override fun getComponentName(): String {
        return "Register Flutter i18n"
    }

    override fun projectOpened() {
        FlutterModuleUtils.getModules(project)

        if (!FlutterModuleUtils.hasFlutterModule(project)) return
        val am = ActionManager.getInstance()
        val action = RebuildI18nFile()
        am.registerAction("FlutterI18n.RebuildI18nFile", action)
        val windowM = am.getAction("ToolbarRunGroup") as DefaultActionGroup
        windowM.addSeparator()
        windowM.add(action)
        windowM.addSeparator()
    }

    override fun projectClosed() {

    }
}
