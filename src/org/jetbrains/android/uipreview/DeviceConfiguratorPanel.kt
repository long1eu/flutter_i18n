/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jetbrains.android.uipreview

import com.android.ide.common.resources.LocaleManager
import com.android.ide.common.resources.configuration.FlagManager
import com.intellij.openapi.util.Ref
import com.intellij.ui.SortedListModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.speedSearch.ListWithFilter
import com.intellij.util.ui.JBUI
import eu.long1.flutter.i18n.Log
import java.awt.*
import java.util.*
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

/**
 * @author Base file created by Eugene.Kudelevsky and cleaned by long1eu
 */
class DeviceConfiguratorPanel : JPanel(BorderLayout()) {

    private var myQualifierOptionsPanel: JPanel? = null
    val editor = MyLocaleEditor()

    init {
        createUIComponents()
        myQualifierOptionsPanel!!.add(editor.component, "locale")
    }

    private fun createUIComponents() {
        myQualifierOptionsPanel = JPanel(CardLayout())
        val listsPanel = JPanel()
        add(listsPanel, BorderLayout.CENTER)
        add(myQualifierOptionsPanel!!, BorderLayout.EAST)
    }

    inner class MyLocaleEditor {

        private val myLanguageList = JBList<Any>()
        private val myRegionList = JBList<Any>()
        private var myShowAllRegions: JBCheckBox? = null

        val component: JComponent
            get() {
                var gridBagConstraints = GridBagConstraints()
                val pane = JPanel(GridBagLayout())
                pane.border = JBUI.Borders.empty(0, 20, 0, 0)!!

                myShowAllRegions = JBCheckBox("Show All Regions", false)
                val languageLabel = JBLabel("Language:")
                val regionLabel = JBLabel("Specific Region Only:")

                val languageModel = SortedListModel<Any>(Comparator<Any> { s1, s2 ->
                    s1 as String
                    s2 as String
                    val delta = s1.length - s2.length
                    if (delta != 0) {
                        delta
                    } else String.CASE_INSENSITIVE_ORDER.compare(s1, s2)
                })
                languageModel.addAll(LocaleManager.getLanguageCodes())
                myLanguageList.model = languageModel
                myLanguageList.selectionMode = ListSelectionModel.SINGLE_SELECTION
                myLanguageList.cellRenderer = FlagManager.get().languageCodeCellRenderer
                val scroll = JBScrollPane(myLanguageList)
                val languagePane = ListWithFilter.wrap(myLanguageList, scroll, FlagManager.languageNameMapper)
                languageLabel.labelFor = myLanguageList

                myRegionList.selectionMode = ListSelectionModel.SINGLE_SELECTION
                myRegionList.cellRenderer = FlagManager.get().regionCodeCellRenderer
                updateRegionList(null)
                val regionPane = JBScrollPane(myRegionList)

                val insets = Insets(0, 20, 0, 0)
                gridBagConstraints.anchor = GridBagConstraints.NORTHWEST
                pane.add(languageLabel, gridBagConstraints)
                gridBagConstraints = GridBagConstraints()
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER
                gridBagConstraints.anchor = GridBagConstraints.LINE_START
                gridBagConstraints.insets = insets
                pane.add(regionLabel, gridBagConstraints)
                gridBagConstraints = GridBagConstraints()
                gridBagConstraints.fill = GridBagConstraints.BOTH
                gridBagConstraints.weightx = 1.0
                gridBagConstraints.weighty = 1.0
                pane.add(languagePane, gridBagConstraints)
                gridBagConstraints = GridBagConstraints()
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER
                gridBagConstraints.fill = GridBagConstraints.BOTH
                gridBagConstraints.weightx = 1.0
                gridBagConstraints.weighty = 1.0
                gridBagConstraints.insets = insets
                pane.add(regionPane, gridBagConstraints)
                gridBagConstraints = GridBagConstraints()
                gridBagConstraints.anchor = GridBagConstraints.EAST
                gridBagConstraints = GridBagConstraints()
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER
                gridBagConstraints.anchor = GridBagConstraints.LINE_START
                gridBagConstraints.insets = insets
                pane.add(myShowAllRegions!!, gridBagConstraints)
                gridBagConstraints = GridBagConstraints()
                gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER
                gridBagConstraints.anchor = GridBagConstraints.LINE_START

                myLanguageList.addListSelectionListener({ updateRegionList(myLanguageList.selectedValue as String) })
                myShowAllRegions!!.addChangeListener { updateRegionList(myLanguageList.selectedValue as String) }

                return pane
            }

        /**
         * Populate the region list based on an optional language selection
         */
        private fun updateRegionList(languageCode: String?) {
            val preferred = Ref<String>(null)
            val regionModel = SortedListModel<Any>(Comparator<Any> { s1, s2 ->
                // Sort "Any Region" to the top
                if (s1 == FAKE_VALUE) {
                    return@Comparator -1
                } else if (s2 == FAKE_VALUE) {
                    return@Comparator 1
                }
                if (s1 == preferred.get()) {
                    return@Comparator -1
                } else if (s2 == preferred.get()) {
                    return@Comparator 1
                }
                // Special language comparator: We want to prefer 2-letter language codes.
                s1 as String
                s2 as String
                val delta = s1.length - s2.length
                if (delta != 0) {
                    delta
                } else String.CASE_INSENSITIVE_ORDER.compare(s1, s2)
            })

            regionModel.add(FAKE_VALUE)
            if (!myShowAllRegions!!.isSelected && languageCode != null) {
                preferred.set(LocaleManager.getLanguageRegion(languageCode))
                val relevant = LocaleManager.getRelevantRegions(languageCode)
                for (code in relevant) {
                    regionModel.add(code)
                }
            } else {
                for (code in LocaleManager.getRegionCodes(true)) {
                    regionModel.add(code)
                }
            }


            myRegionList.model = regionModel
            if (languageCode != null && regionModel.size > 0) {
                myRegionList.selectedIndex = 0
            }
        }

        fun apply(): Locale {
            val selectedLanguage = myLanguageList.selectedValue as String
            var selectedRegion: String? = myRegionList.selectedValue as String?
            if (FAKE_VALUE == selectedRegion) selectedRegion = null

            if (selectedRegion == null) selectedRegion = ""
            log.w(selectedLanguage, selectedRegion)
            return Locale(selectedLanguage, selectedRegion)
        }
    }

    companion object {
        val log = Log()
        val FAKE_VALUE = "__"
    }
}
