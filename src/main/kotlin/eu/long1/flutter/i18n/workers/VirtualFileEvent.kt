@file:Suppress("UNCHECKED_CAST")

package eu.long1.flutter.i18n.workers

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileCopyEvent
import com.intellij.openapi.vfs.VirtualFileEvent
import com.intellij.openapi.vfs.VirtualFileListener
import com.intellij.openapi.vfs.VirtualFileMoveEvent
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import eu.long1.flutter.i18n.files.FileHelpers
import io.flutter.pub.PubRoot
import io.flutter.sdk.FlutterSdk
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import org.yaml.snakeyaml.resolver.Resolver
import java.io.File

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
    private val undoManager = UndoManager.getInstance(project)!!
    private val documentManager = PsiDocumentManager.getInstance(project)!!

    private fun yaml(): Yaml {

        val options = DumperOptions()
        options.isPrettyFlow = true

        val yaml = Yaml(SafeConstructor(), Representer(), options, object : Resolver() {
            override fun addImplicitResolvers() {
                this.addImplicitResolver(Tag.BOOL, Resolver.BOOL, "yYnNtTfFoO")
                this.addImplicitResolver(Tag.NULL, Resolver.NULL, "~nN\u0000")
                this.addImplicitResolver(Tag.NULL, Resolver.EMPTY, null as String?)
                this.addImplicitResolver(Tag("tag:yaml.org,2002:value"), Resolver.VALUE, "=")
                this.addImplicitResolver(Tag.MERGE, Resolver.MERGE, "<")
            }
        })

        return yaml
    }

    override fun fileCreated(event: VirtualFileEvent) {
        if (!isRes(event.file.path)) return
        if (!acceptedExtensions.contains(event.file.extension)) return

        // check to see if this a different resolution for a file we already added
        if (event.parent!!.name.matches(Regex("\\d.\\dx"))) {
            if (event.parent!!.findChild(event.fileName) != null) {
                println("Base file exists. We don't do anything in this case.")
            } else {
                println("Base file doesn't exists. Should we consider this file as base? Not for now, so we do nothing.")
            }
            return
        }

        val newItem = event.file.path.substringAfter("${project.baseDir.path}/")

        val originalText = File(project.basePath + "/pubspec.yaml").readText()
        val pubspecInfo = loadPubspecInfo(originalText)
        val flutterValues = pubspecInfo!!["flutter"] as HashMap<String, Any>
        val hasAssets = flutterValues["assets"] != null
        val assets: ArrayList<String> = flutterValues["assets"] as? ArrayList<String> ?: arrayListOf()

        if (assets.contains(newItem)) return
        println("created")

        val yamlFile = PsiManager.getInstance(project).findFile(PubRoot.forFile(event.file)!!.pubspec) as YAMLFile

        runWriteAction {
            if (hasAssets) {
                val assetPsi =
                    PsiTreeUtil.findChildrenOfType(yamlFile, YAMLKeyValue::class.java).first { it.keyText == "assets" }
                val offset = assetPsi.textOffset + assetPsi.textLength
                val document = documentManager.getDocument(yamlFile)!!

                document.replaceString(offset, offset, "\n   - $newItem")
                documentManager.commitDocument(document)


                val sdk = FlutterSdk.getFlutterSdk(project)
                if (sdk != null) {
                    val root = PubRoot.forFile(event.file)
                    if (root != null) {
                        FlutterSdk.getFlutterSdk(project)!!.startPackagesGet(root, project)
                    }
                }
            }
        }


        println(newItem)
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

    private fun loadPubspecInfo(yamlContents: String): Map<String, Any>? {
        return yaml().load(yamlContents) as Map<String, Any>
    }

    companion object {
        val acceptedExtensions =
            arrayListOf("json", "jpeg", "jpg", "jpe", "jif", "jfif", "jfi", "webp", "gif", "png", "bmp", "dib", "wbmp")
    }
}


