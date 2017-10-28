package eu.long1.flutter.i18n.workers

import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonProperty
import com.intellij.json.psi.JsonStringLiteral
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import eu.long1.flutter.i18n.Log
import eu.long1.flutter.i18n.arb.ArbFileType
import io.flutter.pub.PubRoot
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap


object I18nFile {

    private val log = Log()

    private val PLURAL_MATCHER = Pattern.compile("(.*)(Zero|One|Two|Few|Many|Other)").matcher("")
    private val PARAMETER_MATCHER = Pattern.compile(
            "\\$[^\\p{Punct}\\p{Space}\\p{sc=Han}\\p{sc=Hiragana}\\p{sc=Katakana}–]*").matcher("")

    fun generate(project: Project, valuesFolder: VirtualFile) {
        log.w(Date().time)
        runWriteAction {
            val psiManager = PsiManager.getInstance(project)
            val documentManager = PsiDocumentManager.getInstance(project)

            val children = valuesFolder.children.filter {
                it.extension == ArbFileType.defaultExtension && it.name.startsWith("strings_", true)
            } as ArrayList

            if (children.isEmpty()) children.add(
                    createStringsFile("en", valuesFolder, documentManager,
                            psiManager))

            val map = HashMap<String, HashMap<String, String>>()

            children.forEach {
                val lang = it.nameWithoutExtension.substringAfter("_", "")
                val jsonFile = psiManager.findFile(it) as JsonFile
                map[lang] = getLanguageStrings(jsonFile) ?: HashMap()
            }

            //
            val fileBuilder = StringBuilder()
            generateImportStatements(fileBuilder)
            generateSClass(map["en"]!!, fileBuilder)

            //
            map.keys.forEach {
                if (it == "en") fileBuilder.append("class en extends S {\n  en(Locale locale) : super(locale);\n}\n")
                else generateLangClass(it, map, fileBuilder)
            }

            //
            generateDelegateClass(map, fileBuilder)

            val i18nFile = fileBuilder.toString()

            ApplicationManager.getApplication().invokeLater {
                runWriteAction {
                    val file = getI18nFile(project)
                    val psiFile = psiManager.findFile(file)!!
                    val document = documentManager.getDocument(psiFile)!!
                    document.setReadOnly(false)
                    document.setText(i18nFile)
                    document.setReadOnly(true)
                    documentManager.commitDocument(document)
                    log.i("i18n.dart is up to date")
                    log.w(Date().time)
                }
            }
        }
    }

    private fun generateImportStatements(fileBuilder: StringBuilder) {
        fileBuilder.append(i18nFileImports)
    }

    fun generateSClass(mapEn: HashMap<String, String>, fileBuilder: StringBuilder) {
        val ids = ArrayList<String>(mapEn.keys)
        val pluralsMaps = HashMap<String, ArrayList<String>>()
        val plurals = findPluralsKeys(ids, pluralsMaps)
        ids -= plurals
        val parametrized = ids.filter { mapEn[it]!!.contains("$") }
        ids -= parametrized


        fileBuilder.append(sClassHeader)
        ids.forEach {
            getStrings(it, mapEn, fileBuilder, false)
        }
        parametrized.forEach {
            getParametrized(it, mapEn, fileBuilder, false)
        }
        pluralsMaps.keys.forEach {
            getPlurals(it, pluralsMaps, mapEn, fileBuilder, false)
        }
        fileBuilder.append("}\n\n")
    }

    fun generateLangClass(lang: String, map: HashMap<String, HashMap<String, String>>,
                          fileBuilder: StringBuilder) {
        val idsEn = ArrayList<String>(map["en"]!!.keys)
        val mapLang = map[lang]!!
        val ids = ArrayList(mapLang.keys).filter { idsEn.contains(it) } as ArrayList

        val pluralsMaps = HashMap<String, ArrayList<String>>()
        val plurals = findPluralsKeys(ids, pluralsMaps)
        ids -= plurals
        val parametrized = ids.filter { mapLang[it]!!.contains("$") }
        ids -= parametrized

        //if the value for an ids is empty, remove it?
        //val ids = ids.filter { !mapLang[it].isNullOrBlank() }
        fileBuilder.append("class $lang extends S {\n  $lang(Locale locale) : super(locale);\n\n")

        ids.forEach {
            getStrings(it, mapLang, fileBuilder)
        }

        parametrized.forEach {
            getParametrized(it, mapLang, fileBuilder)
        }

        pluralsMaps.keys.forEach {
            getPlurals(it, pluralsMaps, mapLang, fileBuilder)
        }

        fileBuilder.append("}\n\n")
    }

    private fun generateDelegateClass(map: HashMap<String, HashMap<String, String>>,
                                      fileBuilder: StringBuilder) {
        fileBuilder.append(delegateClassHeader)
        map.keys.forEach {
            val langParts = it.split("_")
            val lang = langParts[0]
            val country = if (langParts.size == 2) langParts[1] else ""

            fileBuilder.append("      new Locale(\"$lang\", \"$country\"),\n")
        }

        fileBuilder.append(delegateClassResolution)
        map.keys.forEach {
            fileBuilder.append("      case \"$it\":\n        return new SynchronousFuture<S>(new $it(locale));\n")
        }

        fileBuilder.append(delegateClassEnd)
    }


    private fun getStrings(id: String, mapLang: HashMap<String, String>, fileBuilder: StringBuilder,
                           isOverride: Boolean = true) {
        val value = mapLang[id]!!
        if (isOverride) fileBuilder.append("  @override\n")
        fileBuilder.append("  String get $id => \"$value\";\n")
    }

    private fun getParametrized(id: String, mapLang: HashMap<String, String>, fileBuilder: StringBuilder,
                                isOverride: Boolean = true) {
        val value = mapLang[id]!!
        PARAMETER_MATCHER.reset(value)

        if (isOverride) fileBuilder.append("  @override\n")
        fileBuilder.append("  String $id(")
        while (PARAMETER_MATCHER.find()) {
            val parameter = PARAMETER_MATCHER.group().substring(1)
            fileBuilder.append("String $parameter, ")
        }
        fileBuilder.setLength(fileBuilder.length - 2)
        fileBuilder.append(") => \"$value\";\n")
    }

    private fun getPlurals(id: String, pluralsMaps: HashMap<String, ArrayList<String>>,
                           mapLang: HashMap<String, String>,
                           fileBuilder: StringBuilder, isOverride: Boolean = true) {
        val list = pluralsMaps[id]!!

        val zero = list.contains("Zero")
        val one = list.contains("One")
        val two = list.contains("Two")
        val other = list.contains("Other")
        val few = list.contains("Few")
        val many = list.contains("Many")


        val parameterName = if (other) {
            PARAMETER_MATCHER.reset(mapLang["${id}Other"]!!).find();PARAMETER_MATCHER.group().substring(1)
        } else "quantity"

        if (isOverride) fileBuilder.append("  @override\n")
        fileBuilder.append("  String $id(String $parameterName) {\n    switch ($parameterName) {\n")

        if (zero) fileBuilder.append("      case \"0\":\n        return \"${mapLang["${id}Zero"]!!}\";\n")
        if (one) fileBuilder.append("      case \"1\":\n        return \"${mapLang["${id}One"]!!}\";\n")
        if (two) fileBuilder.append("      case \"2\":\n        return \"${mapLang["${id}Two"]!!}\";\n")
        if (few) fileBuilder.append("      case \"few\":\n        return \"${mapLang["${id}Few"]!!}\";\n")
        if (many) fileBuilder.append("      case \"many\":\n        return \"${mapLang["${id}Many"]!!}\";\n")
        if (other) fileBuilder.append("      default:\n        return \"${mapLang["${id}Other"]!!}\";\n")
        else fileBuilder.append("      default:\n        return \"$\";\n")
        fileBuilder.append("    }\n  }\n")
    }

    fun getI18nFile(project: Project): VirtualFile {
        val lib = PubRoot.singleForProject(project)!!.lib!!
        val generated = lib.findChild("generated") ?: lib.createChildDirectory(this, "generated")

        return generated.findOrCreateChildData(this, "i18n.dart")
    }

    private fun createStringsFile(lang: String, valuesFolder: VirtualFile, documentManager: PsiDocumentManager,
                                  psiManager: PsiManager): VirtualFile {
        val strings = valuesFolder.findOrCreateChildData(this, "strings_$lang.arb")
        val doc = documentManager.getDocument(psiManager.findFile(strings)!!)!!
        doc.setText("{}")
        documentManager.commitDocument(doc)
        return strings
    }

    private fun findPluralsKeys(ids: ArrayList<String>, pluralsMaps: HashMap<String, ArrayList<String>>): List<String> {
        return ids.filter {
            PLURAL_MATCHER.reset(it)
            val find = PLURAL_MATCHER.find()
            if (find) {
                val id = PLURAL_MATCHER.group(1)
                val quantity = PLURAL_MATCHER.group(2)
                val list = pluralsMaps[id] ?: ArrayList()
                list.add(quantity)
                pluralsMaps[id] = list
            }
            find
        }
    }


    fun getLanguageStrings(jsonFile: JsonFile): HashMap<String, String>? {
        if (PsiTreeUtil.findChildrenOfType(jsonFile, PsiErrorElement::class.java).isNotEmpty()) {
            log.i("i18n.dart didn't due to errors in ${jsonFile.name}")
            return null
        }

        val langMap = HashMap<String, String>()
        jsonFile.allTopLevelValues[0].children
                .filter { it is JsonProperty }
                .filter { (it as JsonProperty).nameElement is JsonStringLiteral && it.value is JsonStringLiteral }
                .forEach {
                    it as JsonProperty
                    val id = it.nameElement as JsonStringLiteral
                    val value = it.value as JsonStringLiteral

                    langMap[id.value] = value.value
                }

        return langMap
    }

    private const val i18nFileImports =
            "import 'dart:async';\n\n" +
                    "import 'package:flutter/foundation.dart';\n" +
                    "import 'package:flutter/material.dart';\n\n"

    private const val sClassHeader = "class S {\n" +
            "  Locale _locale;\n" +
            "  String _lang;\n" +
            "\n" +
            "  S(this._locale) {\n" +
            "    _lang = getLang(_locale);\n" +
            "    print(_lang);\n" +
            "  }\n" +
            "\n" +
            "  static final GeneratedLocalizationsDelegate delegate =\n" +
            "      new GeneratedLocalizationsDelegate();\n" +
            "\n" +
            "  static S of(BuildContext context) {\n" +
            "    var s = Localizations.of<S>(context, S);\n" +
            "    s._lang = getLang(s._locale);\n" +
            "    return s;\n" +
            "  }\n\n"

    private const val delegateClassHeader = "class GeneratedLocalizationsDelegate extends LocalizationsDelegate<S> {\n" +
            "  const GeneratedLocalizationsDelegate();\n" +
            "\n" +
            "  List<Locale> get supportedLocales {\n" +
            "    return [\n"


    private const val delegateClassResolution = "    ];\n" +
            "  }\n" +
            "\n" +
            "  LocaleResolutionCallback resolution({Locale fallback}) {\n" +
            "    return (Locale locale, Iterable<Locale> supported) {\n" +
            "      var languageLocale = new Locale(locale.languageCode, \"\");\n" +
            "      if (supported.contains(locale))\n" +
            "        return locale;\n" +
            "      else if (supported.contains(languageLocale))\n" +
            "        return languageLocale;\n" +
            "      else {\n" +
            "        var fallbackLocale = fallback ?? supported.first;\n" +
            "        assert(supported.contains(fallbackLocale));\n" +
            "        return fallbackLocale;\n" +
            "      }\n" +
            "    };\n" +
            "  }\n" +
            "\n" +
            "  Future<S> load(Locale locale) {\n" +
            "    String lang = getLang(locale);\n" +
            "    switch (lang) {\n"

    private const val delegateClassEnd = "      default:\n" +
            "        return new SynchronousFuture<S>(new S(locale));\n" +
            "    }\n" +
            "  }\n" +
            "\n" +
            "  bool isSupported(Locale locale) => supportedLocales.contains(getLang(locale));\n" +
            "\n" +
            "  bool shouldReload(GeneratedLocalizationsDelegate old) => false;\n" +
            "}\n" +
            "\n" +
            "String getLang(Locale l) =>\n" +
            "    l.countryCode.isEmpty ? l.languageCode : l.toString();\n"
}