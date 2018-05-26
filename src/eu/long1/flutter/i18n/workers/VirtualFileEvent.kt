package eu.long1.flutter.i18n.workers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileCopyEvent
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import eu.long1.flutter.i18n.files.FileHelpers

/**
 * Supported files:
 * - json
 * - jpeg
 * - webP
 * - gif
 * - png
 * - bmp
 * - wbmp
 *
 * Keep in mind that flutter has a resolution aware system:
 *   .../my_icon.png
 *   .../2.0x/my_icon.png
 *   .../3.0x/my_icon.png
 * */

class VirtualFileEvent(private val project: Project) : VirtualFileListener {
    private val resFolder = FileHelpers.getResourceFolder(project)

    override fun fileCreated(event: VirtualFileEvent) {
        if (!isRes(event.file.path)) return

        println("created")
        println(event.file.path)
    }

    override fun fileDeleted(event: VirtualFileEvent) {
        if (!isRes(event.file.path)) return

        println("deleted")
        println(event.file.path)
    }

    override fun fileCopied(event: VirtualFileCopyEvent) {
        if (isRes(event.file.path)) {
            println("copied")
            println(event.file.path)
        }
    }

    override fun fileMoved(event: VirtualFileMoveEvent) {
        val add = isRes(event.newParent.path)
        val delete = isRes(event.oldParent.path)

        if (add) {
            println("moved add")
            println(event.newParent.path + "/" + event.fileName)
        } else if (delete) {
            println("moved delete")
            println(event.oldParent.path + "/" + event.fileName)
        }
    }

    private fun isRes(path: String): Boolean = path.contains(resFolder.path)
}