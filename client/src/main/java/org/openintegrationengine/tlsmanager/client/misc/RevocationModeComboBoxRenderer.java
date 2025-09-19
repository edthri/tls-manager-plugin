package org.openintegrationengine.tlsmanager.client.misc;

import org.openintegrationengine.tlsmanager.shared.models.RevocationMode;

import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import java.awt.Component;

public class RevocationModeComboBoxRenderer extends BasicComboBoxRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof RevocationMode action) {
            setText(action.getDisplayText());
        }
        return this;
    }
}
