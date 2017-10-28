package eu.long1.flutter.i18n

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import eu.long1.flutter.i18n.workers.ArbDocumentListener
import eu.long1.flutter.i18n.workers.I18nFile
import io.flutter.utils.FlutterModuleUtils

class Initializer : StartupActivity {

    private lateinit var psiManager: PsiManager
    private lateinit var documentManager: PsiDocumentManager

    private lateinit var baseDir: VirtualFile
    private lateinit var resFolder: VirtualFile
    private lateinit var valuesFolder: VirtualFile

    override fun runActivity(project: Project) {
        if (!FlutterModuleUtils.hasFlutterModule(project)) {
            log.i("This is not a Flutter project.")
            return
        }

        psiManager = PsiManager.getInstance(project)
        documentManager = PsiDocumentManager.getInstance(project)
        baseDir = project.baseDir
        ArbDocumentListener.init(project)


        runWriteAction {
            resFolder = baseDir.findChild("res") ?: baseDir.createChildDirectory(this, "res")
            valuesFolder = resFolder.findChild("values") ?: resFolder.createChildDirectory(this, "values")

            I18nFile.generate(project, valuesFolder)

            valuesFolder.children.forEach {
                val document = documentManager.getDocument(psiManager.findFile(it)!!)
                document!!.addDocumentListener(ArbDocumentListener)
            }
        }
    }

    companion object {
        private val log = Log()
    }
}
