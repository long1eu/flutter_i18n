/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.ide.common.resources.configuration

import com.android.ide.common.resources.LocaleManager
import com.google.common.collect.Maps
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.util.Function
import io.flutter.FlutterI18nIcons
import org.jetbrains.annotations.Nullable
import java.lang.reflect.Field
import javax.swing.Icon
import javax.swing.JList
import javax.swing.ListCellRenderer

/**
 * The [FlagManager] provides access to flags for regions known
 * to [LocaleManager]. It also contains some locale related display
 * functions.
 *
 *
 * All the flag images came from the WindowBuilder subversion repository
 * http://dev.eclipse.org/svnroot/tools/org.eclipse.windowbuilder/trunk (and in
 * particular, a snapshot of revision 424). However, it appears that the icons
 * are from http://www.famfamfam.com/lab/icons/flags/ which states that "these
 * flag icons are available for free use for any purpose with no requirement for
 * attribution." Adding the URL here such that we can check back occasionally
 * and see if there are corrections or updates. Also note that the flag names
 * are in ISO 3166-1 alpha-2 country codes.
 */
class FlagManager
/**
 * Use the [.get] factory method
 */
private constructor() {
    /**
     * Map from region to flag icon
     */
    private val myImageMap = Maps.newHashMap<String, Icon>()

    /**
     * Returns a [ListCellRenderer] suitable for displaying languages when the list model contains String language codes
     */
    val languageCodeCellRenderer: ListCellRenderer<Any>
        get() {
            val nameMapper = languageNameMapper
            return object : ColoredListCellRenderer<Any>() {
                override fun customizeCellRenderer(list: JList<*>, value: Any, index: Int, selected: Boolean,
                                                   hasFocus: Boolean) {
                    append(nameMapper.`fun`(value))
                    icon = getFlag(value as String, null)
                }
            }
        }

    /**
     * Returns a [ListCellRenderer] suitable for displaying regions when the list model contains String region codes
     */
    val regionCodeCellRenderer: ListCellRenderer<Any>
        get() {
            val nameMapper = regionNameMapper
            return object : ColoredListCellRenderer<Any>() {
                override fun customizeCellRenderer(list: JList<*>, value: Any, index: Int, selected: Boolean,
                                                   hasFocus: Boolean) {
                    append(nameMapper.`fun`(value))
                    icon = getFlag(null, value as String)
                }
            }
        }

    /**
     * Returns the flag for the given language and region.
     *
     * @param language the language, or null (if null, region must not be null),
     * the 2 letter language code (ISO 639-1), in lower case
     * @param reg   the region, or null (if null, language must not be null),
     * the 2 letter region code (ISO 3166-1 alpha-2), in upper case
     * @return a suitable flag icon, or null
     */
    @Nullable
    fun getFlag(@Nullable language: String?, @Nullable reg: String?): Icon? {
        var region = reg
        assert(region != null || language != null)
        if (region == null || region.isEmpty()) {
            // Look up the region for a given language
            assert(language != null)

            if (!showFlagsForLanguages()) {
                return null
            }

            // Special cases where we have a dedicated flag available:
            if (language == "ca") {        //$NON-NLS-1$
                return getIcon("catalonia")      //$NON-NLS-1$
            } else if (language == "gd") { //$NON-NLS-1$
                return getIcon("scotland")     //$NON-NLS-1$
            } else if (language == "cy") { //$NON-NLS-1$
                return getIcon("wales")        //$NON-NLS-1$
            }

            // Pick based on various heuristics
            region = LocaleManager.getLanguageRegion(language!!)
        }

        return if (region == null || region.isEmpty() || region.length == 3) {
            // No country specified, and the language is for a country we
            // don't have a flag for
            null
        } else getIcon(region)
    }

    @Nullable
    private fun getIcon(base: String): Icon? {
        var flagImage: Icon? = myImageMap[base]
        if (flagImage == null) {
            // TODO: Special case locale currently running on system such
            // that the current country matches the current locale
            if (myImageMap.containsKey(base)) {
                // Already checked: there's just no image there
                return null
            }
            val flagFileName = base.toLowerCase(java.util.Locale.US) + ".png" //$NON-NLS-1$
            flagImage = IconLoader.findIcon("/icons/flags/" + flagFileName, FlutterI18nIcons::class.java)
            if (flagImage == null) {
                flagImage = FlutterI18nIcons.EmptyFlag
            }
            myImageMap.put(base, flagImage)
        }

        return flagImage
    }

    companion object {
        private val ourInstance = FlagManager()

        /**
         * Returns the [FlagManager] singleton
         *
         * @return the [FlagManager] singleton, never null
         */
        fun get(): FlagManager {
            return ourInstance
        }

        private var ourFlagSettingAvailable = true
        private var ourLanguageFlagField: Field? = null

        /**
         * Whether users want to use flags to represent languages when possible
         */
        private fun showFlagsForLanguages(): Boolean {
            if (ourFlagSettingAvailable) {
                return try {
                    if (ourLanguageFlagField == null) {
                        ourLanguageFlagField = UISettings::class.java.getDeclaredField("LANGUAGE_FLAGS")
                    }
                    ourLanguageFlagField!!.getBoolean(UISettings.instance)
                } catch (t: Throwable) {
                    ourFlagSettingAvailable = false
                    true
                }

            }
            return true
        }

        /**
         * A function which maps from language code to a language label: code + name
         */
        val languageNameMapper: Function<Any, String>
            get() = Function { value ->
                val languageCode = value as String
                if (languageCode == "__") {
                    return@Function "Any Language"
                }
                var languageName = LocaleManager.getLanguageName(languageCode)
                if (languageName != null && languageName.length > 30) {
                    languageName = languageName.substring(0, 27) + "..."
                }
                String.format("%1\$s: %2\$s", languageCode, languageName)
            }

        /**
         * A function which maps from language code to a language label: code + name
         */
        val regionNameMapper: Function<Any, String>
            get() = Function { value ->
                val regionCode = value as String
                if (regionCode == "__") {
                    return@Function "Any Region"
                }
                var regionName = LocaleManager.getRegionName(regionCode)
                if (regionName != null && regionName.length > 30) {
                    regionName = regionName.substring(0, 27) + "..."
                }
                String.format("%1\$s: %2\$s", regionCode, regionName)
            }
    }
}
