package eu.long1.flutter.i18n.files

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import io.flutter.pub.PubRoot

object FileHelpers {

    fun getResourceFolder(project: Project): VirtualFile =
            project.baseDir.findChild("res")
                    ?: project.baseDir.createChildDirectory(this, "res")

    @JvmStatic
    fun getValuesFolder(project: Project): VirtualFile =
            getResourceFolder(project).findChild("values")
                    ?: getResourceFolder(project).createChildDirectory(this, "values")

    fun getI18nFile(project: Project): VirtualFile {
        val lib = PubRoot.singleForProject(project)!!.lib!!
        val generated = lib.findChild("generated") ?: lib.createChildDirectory(this, "generated")

        return generated.findOrCreateChildData(this, "i18n.dart")
    }
}