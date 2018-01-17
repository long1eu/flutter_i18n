package eu.long1.flutter.i18n.workers

import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartClass
import com.jetbrains.lang.dart.psi.DartFile

object ArbDocumentListener : DocumentListener {

    private lateinit var project: Project
    private lateinit var psiManager: PsiManager
    private lateinit var documentManager: PsiDocumentManager
    private lateinit var resFolder: VirtualFile
    private lateinit var valuesFolder: VirtualFile

    fun init(project: Project) {
        this.project = project
        psiManager = PsiManager.getInstance(project)
        documentManager = PsiDocumentManager.getInstance(project)
        resFolder = project.baseDir.findChild("res") ?: project.baseDir.createChildDirectory(this, "res")
        valuesFolder = resFolder.findChild("values") ?: resFolder.createChildDirectory(this, "values")
    }

    override fun documentChanged(event: DocumentEvent) {
        val jsonPsi = documentManager.getPsiFile(event.document)!!
        jsonPsi as? JsonFile ?: return

        val className = jsonPsi.virtualFile.nameWithoutExtension.substringAfter("_")

        val classSB = StringBuilder()
        val jsonFile = PsiFileFactory.getInstance(project).createFileFromText(JsonLanguage.INSTANCE,
                event.document.text) as JsonFile

        when (className) {
            "en" -> {
                val stringsMap = I18nFile.getLanguageStrings(jsonFile) ?: return

                I18nFile.generateSClass(stringsMap, classSB)
            }

            else -> {
                val enDocument = documentManager.getDocument(
                        psiManager.findFile(valuesFolder.findChild("strings_en.arb")!!)!!)!!
                val enJsonFile = PsiFileFactory.getInstance(project).createFileFromText(JsonLanguage.INSTANCE,
                        enDocument.text) as JsonFile


                val enStringMap = I18nFile.getLanguageStrings(enJsonFile) ?: return
                val langStringMap = I18nFile.getLanguageStrings(jsonFile) ?: return


                val map = hashMapOf("en".to(enStringMap), className.to(langStringMap))


                I18nFile.generateLangClass(className, map, classSB)
            }
        }
        //drop the last two line breaks
        classSB.setLength(classSB.length - 2)

        val dartFile = getDartFile(project)

        val originalDartClass = when (className) {
            "en" -> {
                PsiTreeUtil.findChildrenOfType(dartFile, DartClass::class.java).first { it.name == "S" }
            }
            else -> {
                PsiTreeUtil.findChildrenOfType(dartFile, DartClass::class.java).first { it.name == className }
            }
        }

        val start = originalDartClass.startOffsetInParent
        val end = originalDartClass.nextSibling.startOffsetInParent

        val dartDocument = documentManager.getDocument(dartFile)!!

        runWriteAction {
            CommandProcessor.getInstance().executeCommand(project, {
                dartDocument.setReadOnly(false)
                dartDocument.replaceString(start, end, classSB.toString())
                dartDocument.setReadOnly(true)
            }, "Update i18n.dart", "", dartDocument)
        }
    }

    private fun getDartFile(project: Project): DartFile {
        val dartVF = I18nFile.getI18nFile(project)
        return DartFile(PsiManager.getInstance(project).findFile(dartVF)!!.viewProvider)
    }
}