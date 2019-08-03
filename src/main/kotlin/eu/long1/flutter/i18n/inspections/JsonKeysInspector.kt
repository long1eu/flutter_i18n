package eu.long1.flutter.i18n.inspections

import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ex.BaseLocalInspectionTool
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.psi.PsiFile
import eu.long1.flutter.i18n.arb.ArbFileType

class JsonKeysInspector : BaseLocalInspectionTool() {

    override fun checkFile(file: PsiFile, manager: InspectionManager, isOnTheFly: Boolean): Array<ProblemDescriptor>? {
        if (!isOnTheFly) return null
        if (file.fileType == ArbFileType) {
            return file.children.first().children.filterIsInstance<JsonProperty>()
                .filter { !it.name.startsWith("@") }
                .mapNotNull {
                    val key = (it.nameElement as JsonStringLiteral).value
                    if (!keyPattern.containsMatchIn(key)) {
                        manager.createProblemDescriptor(
                            it.nameElement,
                            displayName,
                            isOnTheFly,
                            null,
                            ProblemHighlightType.GENERIC_ERROR
                        )
                    } else null
                }.toTypedArray()
        }
        return null
    }

    override fun getDisplayName(): String = "The string key can start with a letter or underscore (_), followed by any combination of those characters plus digits."

    override fun getGroupDisplayName(): String = "Flutter i18n"

    companion object {
        private val keyPattern = Regex("^[a-zA-Z_\$][a-zA-Z_\$0-9]*\$")
    }
}