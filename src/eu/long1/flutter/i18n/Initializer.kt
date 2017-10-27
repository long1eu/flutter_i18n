package eu.long1.flutter.i18n

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import eu.long1.flutter.i18n.arb.ArbFileType
import io.flutter.utils.FlutterModuleUtils
import java.util.*

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

        runWriteAction {
            FileTypeManager.getInstance().associateExtension(ArbFileType, ArbFileType.defaultExtension)
            resFolder = baseDir.findChild("res") ?: baseDir.createChildDirectory(this, "res")
            valuesFolder = resFolder.findChild("values") ?: resFolder.createChildDirectory(this, "values")

            I18nFile.generate(project, valuesFolder)

            valuesFolder.children.forEach {
                documentManager.getDocument(psiManager.findFile(it)!!)!!.addDocumentListener(object : DocumentListener {
                    override fun documentChanged(event: DocumentEvent?) {
                        log.w(Date().time)
                        ApplicationManager.getApplication().invokeLater(
                                Runnable { I18nFile.generate(project, valuesFolder) }, project.disposed)
                    }
                })
            }
        }
    }

    companion object {
        private val log = Log()

    }
}
