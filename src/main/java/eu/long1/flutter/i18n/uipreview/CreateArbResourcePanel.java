package eu.long1.flutter.i18n.uipreview;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileChooser.actions.VirtualFileDeleteProvider;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ArrayUtil;
import com.intellij.util.PlatformIcons;
import eu.long1.flutter.i18n.actions.NewArbFileAction;
import eu.long1.flutter.i18n.files.FileHelpers;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

public class CreateArbResourcePanel {

    private Module module;
    private VirtualFile valuesDir;

    private JPanel panel;
    private JTextField idField;
    private JPanel stringsFilesPanel;
    private JBLabel stringsFilesDescriptionAction;
    private JTextField valueField;
    private JBLabel valueLabel;
    private JBLabel idLabel;

    private Map<String, JCheckBox> checkBoxMap = Collections.emptyMap();
    private String[] stringsFilesNames = ArrayUtil.EMPTY_STRING_ARRAY;

    private final CheckBoxList stringsFiles;

    public CreateArbResourcePanel(@NotNull Module module, String resId, String resValue) {
        this.module = module;
        this.valuesDir = FileHelpers.shouldActivateFor(module.getProject())
            ? FileHelpers.getValuesFolder(module.getProject())
            : null;

        setChangeNameVisible(false);
        setChangeValueVisible(false);

        ApplicationManager.getApplication().assertReadAccessAllowed();

        stringsFiles = new CheckBoxList();
        stringsFilesDescriptionAction.setLabelFor(stringsFiles);
        final ToolbarDecorator decorator = ToolbarDecorator.createDecorator(stringsFiles);

        decorator.setEditAction(null);
        decorator.disableUpDownActions();

        final AnActionButton selectAll = new AnActionButton("Select All", null, PlatformIcons.SELECT_ALL_ICON) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                doSelectAllStringFiles();
            }
        };
        final AnActionButton unselectAll = new AnActionButton("Unselect All", null, PlatformIcons.UNSELECT_ALL_ICON) {
            @Override
            public void actionPerformed(AnActionEvent e) {
                doUnselectAllStringFiles();
            }
        };

        decorator.setAddAction(button -> doAddNewStringsFile());
        decorator.setRemoveAction(button -> doDeleteStringFile());
        decorator.addExtraAction(selectAll);
        decorator.addExtraAction(unselectAll);

        stringsFilesPanel.add(decorator.createPanel());
        setChangeValueVisible(true);
        setChangeNameVisible(true);

        valueField.setText(resValue);
        idField.setText(resId);
        idField.requestFocusInWindow();

        updateStringFiles();
    }

    private void doDeleteStringFile() {
        final int selectedIndex = stringsFiles.getSelectedIndex();
        if (selectedIndex < 0) {
            return;
        }

        final String selectedName = stringsFilesNames[selectedIndex];
        final VirtualFile selectedLang = valuesDir == null ? null : valuesDir.findChild(selectedName);
        if (selectedLang == null) {
            return;
        }

        final VirtualFileDeleteProvider provider = new VirtualFileDeleteProvider();
        provider.deleteElement(dataId -> {
            if (CommonDataKeys.VIRTUAL_FILE_ARRAY.getName().equals(dataId)) {
                return new VirtualFile[]{selectedLang};
            } else {
                return null;
            }
        });

        updateStringFiles();
    }

    private void doSelectAllStringFiles() {
        for (JCheckBox checkBox : checkBoxMap.values()) {
            checkBox.setSelected(true);
        }
        stringsFiles.repaint();
    }

    private void doUnselectAllStringFiles() {
        for (JCheckBox checkBox : checkBoxMap.values()) {
            checkBox.setSelected(false);
        }
        stringsFiles.repaint();
    }

    private void doAddNewStringsFile() {
        if (module == null) {
            return;
        }

        final Project project = module.getProject();
        if (!FileHelpers.shouldActivateFor(project)) {
            return;
        }

        final DeviceConfiguratorPanel panel = new DeviceConfiguratorPanel();
        final DialogWrapper dialog = new DialogWrapper(project, panel);
        dialog.setTitle("Choose Language");
        dialog.showAndGet();

        if (dialog.isOK()) {
            final Locale locale = panel.getEditor().apply();
            final String suffix = locale.getLanguage() +
                (StringUtil.isNotEmpty(locale.getCountry()) ? "_" + locale.getCountry() : "");
            NewArbFileAction.Companion.createFile(suffix, project);
            updateStringFiles();
        }
    }

    @SuppressWarnings("unchecked")
    private void updateStringFiles() {
        if (valuesDir == null) {
            return;
        }

        final VirtualFile[] stringFiles = valuesDir.getChildren();

        final Map<String, JCheckBox> oldCheckBoxes = checkBoxMap;
        final int selectedIndex = stringsFiles.getSelectedIndex();
        final String selectedStringsFileName = selectedIndex >= 0 ? stringsFilesNames[selectedIndex] : null;

        final List<JCheckBox> checkBoxList = new ArrayList<>();
        checkBoxMap = new HashMap<>();
        stringsFilesNames = new String[stringFiles.length];

        int newSelectedIndex = -1;

        for (int i = 0; i < stringFiles.length; i++) {
            final String dirName = stringFiles[i].getName();
            final JCheckBox oldCheckBox = oldCheckBoxes.get(dirName);
            final boolean selected = oldCheckBox != null && oldCheckBox.isSelected();
            final JCheckBox checkBox = new JCheckBox(dirName, selected);
            checkBoxList.add(checkBox);
            checkBoxMap.put(dirName, checkBox);
            stringsFilesNames[i] = dirName;

            if (dirName.equals(selectedStringsFileName)) {
                newSelectedIndex = i;
            }
        }

        String defaultFileName = "strings_en.arb";
        JCheckBox noQualifierCheckBox = checkBoxMap.get(defaultFileName);
        if (noQualifierCheckBox == null) {
            noQualifierCheckBox = new JCheckBox(defaultFileName);

            checkBoxList.add(0, noQualifierCheckBox);
            checkBoxMap.put(defaultFileName, noQualifierCheckBox);

            String[] newDirNames = new String[stringsFilesNames.length + 1];
            newDirNames[0] = defaultFileName;
            System.arraycopy(stringsFilesNames, 0, newDirNames, 1, stringsFilesNames.length);
            stringsFilesNames = newDirNames;
        }
        noQualifierCheckBox.setSelected(true);

        stringsFiles.setModel(new CollectionListModel<>(checkBoxList));

        if (newSelectedIndex >= 0) {
            stringsFiles.setSelectedIndex(newSelectedIndex);
        }
    }

    @NotNull
    public String getResId() {
        return idField.getText().trim();
    }

    @NotNull
    public String getResValue() {
        return valueField.getText().trim();
    }

    @NotNull
    public List<String> getSelected() {
        final List<String> selectedDirs = new ArrayList<>();

        for (Map.Entry<String, JCheckBox> entry : checkBoxMap.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedDirs.add(entry.getKey());
            }
        }
        return selectedDirs;
    }

    public JComponent getPanel() {
        return panel;
    }

    private void setChangeValueVisible(boolean isVisible) {
        valueField.setVisible(isVisible);
        valueLabel.setVisible(isVisible);
    }

    private void setChangeNameVisible(boolean isVisible) {
        idField.setVisible(isVisible);
        idLabel.setVisible(isVisible);
    }
}

