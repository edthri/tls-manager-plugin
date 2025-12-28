/*
 * SPDX-License-Identifier: MPL-2.0
 * Copyright (c) 2025 NovaMap Health Limited <https://novamap.health>
 */

package org.openintegrationengine.tlsmanager.client.dialog;

import com.mirth.connect.client.ui.Mirth;
import com.mirth.connect.client.ui.MirthDialog;
import com.mirth.connect.client.ui.RefreshTableModel;
import com.mirth.connect.client.ui.UIConstants;
import com.mirth.connect.client.ui.components.MirthTable;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.decorator.HighlighterFactory;

import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
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
import java.awt.Font;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

public class SingleSelectDialog extends MirthDialog {

    private JPanel containerPanel;

    private JLabel optionFilterLabel;
    private JTextField optionFilterField;

    private JScrollPane optionsScrollPane;
    private MirthTable optionsTable;

    private JButton okButton;
    private JButton cancelButton;

    private static final int SELECTED_COLUMN = 0;
    private static final int NAME_COLUMN = 1;

    private final Set<String> allOptions;
    private final String selectedOption;

    private final Consumer<String> onSaveConsumer;

    public SingleSelectDialog(
        Window owner,
        String windowTitle,
        Set<String> allOptions,
        String selectedOption,
        Consumer<String> onSaveConsumer
    ) {
        super(owner, windowTitle, true);

        if (allOptions == null) {
            throw new IllegalArgumentException("allOptions cannot be null");
        }

        this.allOptions = allOptions;
        this.selectedOption = selectedOption;

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
                "TLS settings",
                TitledBorder.DEFAULT_JUSTIFICATION,
                TitledBorder.DEFAULT_POSITION,
                new Font(Font.SANS_SERIF, Font.BOLD, 11)
            )
        );

        optionFilterLabel = new JLabel("Filter:");
        optionFilterField = new JTextField();
        optionFilterField.getDocument().addDocumentListener(new DocumentListener() {
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
                optionsTable.getRowSorter().allRowsChanged();
            }
        });

        optionsTable = new MirthTable();
        optionsTable.setModel(new RefreshTableModel(new String[]{"", "Alias"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }

            @Override
            public Class<?> getColumnClass(int column) {
                return column == SELECTED_COLUMN ? Boolean.class : String.class;
            }
        });
        optionsTable.setDragEnabled(false);
        optionsTable.setRowSelectionAllowed(false);
        optionsTable.setRowHeight(UIConstants.ROW_HEIGHT);
        optionsTable.setFocusable(false);
        optionsTable.setOpaque(true);
        optionsTable.getTableHeader().setReorderingAllowed(false);
        optionsTable.setEditable(true);
        optionsTable.setSortable(true);

        if (Preferences.userNodeForPackage(Mirth.class).getBoolean("highlightRows", true)) {
            optionsTable.setHighlighters(HighlighterFactory.createAlternateStriping(UIConstants.HIGHLIGHTER_COLOR, UIConstants.BACKGROUND_COLOR));
        }

        var rowSorter = new TableRowSorter<>(optionsTable.getModel());
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
        optionsTable.setRowSorter(rowSorter);

        var rowFilter = new RowFilter<TableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends TableModel, ? extends Integer> entry) {
                String name = entry.getStringValue(1);
                return StringUtils.containsIgnoreCase(name, optionFilterField.getText());
            }
        };
        rowSorter.setRowFilter(rowFilter);
        optionsTable.setRowFilter(rowFilter);

        optionsTable.getColumnExt(SELECTED_COLUMN).setMinWidth(20);
        optionsTable.getColumnExt(SELECTED_COLUMN).setMaxWidth(20);

        var radioDelegate = new RadioCellEditorRenderer();
        optionsTable.getColumnModel().getColumn(SELECTED_COLUMN).setCellRenderer(radioDelegate);

        optionsTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int column = optionsTable.columnAtPoint(e.getPoint());
                int row = optionsTable.rowAtPoint(e.getPoint());

                if (row != -1 && optionsTable.convertColumnIndexToModel(column) == SELECTED_COLUMN) {
                    int modelRow = optionsTable.convertRowIndexToModel(row);
                    TableModel model = optionsTable.getModel();
                    for (int i = 0; i < model.getRowCount(); i++) {
                        model.setValueAt(i == modelRow, i, SELECTED_COLUMN);
                    }
                }
            }
        });

        optionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        optionsScrollPane = new JScrollPane(optionsTable);

        okButton = new JButton("OK");
        okButton.addActionListener(evt -> {
            var selectedItem = getSelectedItem();
            onSaveConsumer.accept(selectedItem);
            dispose();
        });

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(evt -> dispose());
    }

    private void initLayout() {
        setLayout(new MigLayout("insets 8, novisualpadding, hidemode 3, fill", "", "[grow][][]"));

        containerPanel.setLayout(new MigLayout("insets 8, novisualpadding, hidemode 3, fill", "[]13[grow]", "[][][][][][][][][grow]"));

        containerPanel.add(optionFilterLabel, "right, split 5");
        containerPanel.add(optionFilterField, "w 100:350");
        containerPanel.add(optionsScrollPane, "newline, grow 25, sx");

        add(containerPanel, "grow, push");

        add(new JSeparator(), "newline, growx, sx");

        add(okButton, "newline, w 50!, sx, right, split");
        add(cancelButton, "w 50!");
    }

    private void setProperties() {
        var data = new Object[allOptions.size()][2];

        int i = 0;
        for (var option : allOptions) {
            data[i][SELECTED_COLUMN] = option.equals(selectedOption);
            data[i][NAME_COLUMN] = option;
            i++;
        }

        ((RefreshTableModel) optionsTable.getModel()).refreshDataVector(data);
    }

    private String getSelectedItem() {
        var selectedIndex = optionsTable.getSelectedModelIndex();
        return optionsTable.getModel().getValueAt(selectedIndex, NAME_COLUMN).toString();
    }

    private static class RadioCellEditorRenderer extends AbstractCellEditor implements TableCellRenderer {
        private final JRadioButton radioButton;
        private final JPanel panel;

        public RadioCellEditorRenderer() {
            panel = new JPanel(new MigLayout("insets 0, novisualpadding, hidemode 3, fill"));
            this.radioButton = new JRadioButton();

            radioButton.setOpaque(false);
            panel.add(radioButton, "center");
        }

        @Override
        public Object getCellEditorValue() {
            return true;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            radioButton.setSelected(Boolean.TRUE.equals(value));
            panel.setBackground(row % 2 == 0 ? UIConstants.HIGHLIGHTER_COLOR : UIConstants.BACKGROUND_COLOR);
            radioButton.setBackground(panel.getBackground());
            return panel;
        }
    }
}
