/*
 * Copyright 2025 Kaur Palang
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openintegrationengine.tlsmanager.client.dialog;

import com.mirth.connect.client.ui.Mirth;
import com.mirth.connect.client.ui.MirthDialog;
import com.mirth.connect.client.ui.RefreshTableModel;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthTable;
import com.mirth.connect.client.ui.components.MirthTriStateCheckBox;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EventObject;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.prefs.Preferences;

public class ItemPickerDialog extends MirthDialog {

    private JPanel containerPanel;

    private JLabel certificateFilterLabel;
    private JTextField certificateFilterField;
    private JLabel selectAllLabel;
    private JLabel certificateSelectSeparator;
    private JLabel deselectAllLabel;

    private JScrollPane certificateScrollPane;
    private MirthTable certificateTable;

    private JButton okButton;
    private JButton cancelButton;

    private static final int CERTIFICATES_SELECTED_COLUMN = 0;
    private static final int CERTIFICATES_NAME_COLUMN = 1;
    private static final int CERTIFICATES_ID_COLUMN = 2;

    private final List<String> allOptions;
    private List<String> selectedOptions;
    private boolean isDefaultSelected;
    private String defaultValue;

    private BiConsumer<Boolean, List<String>> onSaveConsumer;

    public ItemPickerDialog(
        Window owner,
        String windowTitle,
        List<String> allOptions,
        List<String> selectedOptions,
        boolean isDefaultSelected,
        String defaultValue,
        BiConsumer<Boolean, List<String>> onSaveConsumer
    ) {
        super(owner, windowTitle, true);
        this.allOptions = allOptions;
        this.selectedOptions = selectedOptions;
        this.isDefaultSelected = isDefaultSelected;
        this.defaultValue = defaultValue;
        this.onSaveConsumer = onSaveConsumer;

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        initComponents();
        initLayout();
        setProperties();
        pack();
        setLocationRelativeTo(getOwner());
        setVisible(true);
    }

    private void initComponents() {
        setBackground(UIConstants.BACKGROUND_COLOR);
        getContentPane().setBackground(getBackground());

        containerPanel = new JPanel();
        containerPanel.setBackground(getBackground());
        containerPanel.setBorder(
            BorderFactory.createTitledBorder(
                BorderFactory.createMatteBorder(
                    1, 1, 1, 1,
                    new Color(204, 204, 204)
                ),
                "Destination TLS settings",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font("Tahoma", Font.BOLD, 11)
            )
        );

        certificateFilterLabel = new JLabel("Filter:");
        certificateFilterField = new JTextField();
        certificateFilterField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void removeUpdate(DocumentEvent evt) {
                filterChanged();
            }

            @Override
            public void insertUpdate(DocumentEvent evt) {
                filterChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent evt) {
                filterChanged();
            }

            private void filterChanged() {
                certificateTable.getRowSorter().allRowsChanged();
            }
        });

        selectAllLabel = new JLabel("<html><u>Select All</u></html>");
        selectAllLabel.setForeground(Color.BLUE);
        selectAllLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        selectAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                if (evt.getComponent().isEnabled()) {
                    for (int row = 0; row < certificateTable.getRowCount(); row++) {
                        certificateTable.setValueAt(MirthTriStateCheckBox.CHECKED, row, CERTIFICATES_SELECTED_COLUMN);
                    }
                }
            }
        });

        certificateSelectSeparator = new JLabel("|");

        deselectAllLabel = new JLabel("<html><u>Deselect All</u></html>");
        deselectAllLabel.setForeground(Color.BLUE);
        deselectAllLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deselectAllLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                if (evt.getComponent().isEnabled()) {
                    for (int row = 0; row < certificateTable.getRowCount(); row++) {
                        certificateTable.setValueAt(MirthTriStateCheckBox.UNCHECKED, row, CERTIFICATES_SELECTED_COLUMN);
                    }
                }
            }
        });

        certificateTable = new MirthTable();
        certificateTable.setModel(new RefreshTableModel(new String[] { "", "Alias" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == CERTIFICATES_SELECTED_COLUMN;
            }
        });
        certificateTable.setDragEnabled(false);
        certificateTable.setRowSelectionAllowed(false);
        certificateTable.setRowHeight(UIConstants.ROW_HEIGHT);
        certificateTable.setFocusable(false);
        certificateTable.setOpaque(true);
        certificateTable.getTableHeader().setReorderingAllowed(false);
        certificateTable.setEditable(true);
        certificateTable.setSortable(true);

        if (Preferences.userNodeForPackage(Mirth.class).getBoolean("highlightRows", true)) {
            certificateTable.setHighlighters(HighlighterFactory.createAlternateStriping(UIConstants.HIGHLIGHTER_COLOR, UIConstants.BACKGROUND_COLOR));
        }

        var rowSorter = new TableRowSorter<>(certificateTable.getModel());
        rowSorter.setComparator(0, (Comparator<Integer>) (o1, o2) -> {
            // 0, 2, 1
            if (Objects.equals(o1, o2)) {
                return 0;
            } else if (o1 == 0 || (o1 == 2 && o2 == 1)) {
                return -1;
            } else {
                return 1;
            }
        });
        certificateTable.setRowSorter(rowSorter);

        var rowFilter = new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(RowFilter.Entry<? extends TableModel, ? extends Integer> entry) {
                String name = entry.getStringValue(1);
                return StringUtils.containsIgnoreCase(name, certificateFilterField.getText());
            }
        };
        rowSorter.setRowFilter(rowFilter);
        certificateTable.setRowFilter(rowFilter);

        certificateTable.getColumnExt(CERTIFICATES_SELECTED_COLUMN).setMinWidth(20);
        certificateTable.getColumnExt(CERTIFICATES_SELECTED_COLUMN).setMaxWidth(20);
        certificateTable.getColumn(CERTIFICATES_SELECTED_COLUMN).setCellEditor(new TagSelectionCellEditor());
        certificateTable.getColumn(CERTIFICATES_SELECTED_COLUMN).setCellRenderer(new TagSelectionCellRenderer());

        certificateScrollPane = new JScrollPane(certificateTable);

        okButton = new JButton("OK");
        okButton.addActionListener(evt -> {
            processTableState();
            onSaveConsumer.accept(isDefaultSelected, selectedOptions);
            dispose();
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(evt -> dispose());
    }

    private void initLayout() {
        setLayout(new MigLayout("insets 8, novisualpadding, hidemode 3, fill", "", "[grow][][]"));

        containerPanel.setLayout(new MigLayout("insets 8, novisualpadding, hidemode 3, fill", "[]13[grow]", "[][][][][][][][][grow]"));

        containerPanel.add(certificateFilterLabel, "right, split 5");
        containerPanel.add(certificateFilterField, "w 100:350");
        containerPanel.add(selectAllLabel, "gapbefore 12");
        containerPanel.add(certificateSelectSeparator);
        containerPanel.add(deselectAllLabel);
        containerPanel.add(certificateScrollPane, "newline, grow 25, sx");

        add(containerPanel, "grow, push");

        add(new JSeparator(), "newline, growx, sx");

        add(okButton, "newline, w 50!, sx, right, split");
        add(cancelButton, "w 50!");
    }

    private void setProperties() {

        var data = new Object[allOptions.size() + 1][2];

        // 0 is CHECKED and 1 is UNCHECKED
        data[0][CERTIFICATES_SELECTED_COLUMN] = isDefaultSelected ? 0 : 1;
        data[0][CERTIFICATES_NAME_COLUMN] = defaultValue;

        int i = 1;
        for (String item : allOptions) {
            data[i][CERTIFICATES_SELECTED_COLUMN] = selectedOptions.contains(item) ? 0 : 1;
            data[i][CERTIFICATES_NAME_COLUMN] = item;
            i++;
        }
        ((RefreshTableModel) certificateTable.getModel()).refreshDataVector(data);
    }

    private void processTableState() {
        var localSelectedOptions = new ArrayList<String>();

        for (int row = 0; row < certificateTable.getModel().getRowCount(); row++) {
            var state = (int) certificateTable.getModel().getValueAt(row, CERTIFICATES_SELECTED_COLUMN);
            var certificateAlias = (String) certificateTable.getModel().getValueAt(row, CERTIFICATES_NAME_COLUMN);

            if (certificateAlias.equals(defaultValue)) {
                // State 0 is CHECKED
                isDefaultSelected = state == 0;
            } else if (state == 0) {
                localSelectedOptions.add(certificateAlias);
            }
        }

        selectedOptions = localSelectedOptions;
    }

    // Proper copyright
    private class TagSelectionCellEditor extends DefaultCellEditor {

        private MirthTriStateCheckBox checkBox;
        private JPanel panel;

        public TagSelectionCellEditor() {
            super(new MirthTriStateCheckBox());
            checkBox = (MirthTriStateCheckBox) editorComponent;
            panel = new JPanel(new MigLayout("insets 0, novisualpadding, hidemode 3, fill"));
            panel.add(checkBox, "center");
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.getState();
        }

        @Override
        public boolean isCellEditable(EventObject anEvent) {
            return true;
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            super.getTableCellEditorComponent(table, value, isSelected, row, column);
            if (value != null) {
                checkBox.setState((int) value);
            }
            panel.setBackground(row % 2 == 0 ? UIConstants.HIGHLIGHTER_COLOR : UIConstants.BACKGROUND_COLOR);
            checkBox.setBackground(panel.getBackground());
            return panel;
        }
    }

    // Proper copyright
    private class TagSelectionCellRenderer implements TableCellRenderer {

        private MirthTriStateCheckBox checkBox;
        private JPanel panel;

        public TagSelectionCellRenderer() {
            panel = new JPanel(new MigLayout("insets 0, novisualpadding, hidemode 3, fill"));
            checkBox = new MirthTriStateCheckBox();
            panel.add(checkBox, "center");
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (value != null) {
                checkBox.setState((int) value);
            }
            panel.setBackground(row % 2 == 0 ? UIConstants.HIGHLIGHTER_COLOR : UIConstants.BACKGROUND_COLOR);
            checkBox.setBackground(panel.getBackground());
            return panel;
        }
    }
}
