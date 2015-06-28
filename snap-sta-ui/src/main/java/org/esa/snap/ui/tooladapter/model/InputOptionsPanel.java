package org.esa.snap.ui.tooladapter.model;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Helper class to hold autocomplete suggestions.
 *
 */
public class InputOptionsPanel {
    private JList list;
    private JPopupMenu popupMenu;
    private String subWord;
    private int insertionPosition;
    private final JTextArea textArea;

    public InputOptionsPanel(JTextArea parent) {
        popupMenu = new JPopupMenu();
        popupMenu.setOpaque(false);
        popupMenu.setBorder(null);
        textArea = parent;
    }

    public void hide() {
        popupMenu.setVisible(false);
    }

    public void show(int position, Point location) {
        this.insertionPosition = position;
        popupMenu.show(textArea, location.x, textArea.getBaseline(0, 0) + location.y);
    }

    public void setSuggestionList(List<String> entries, String subWord) {
        popupMenu.removeAll();
        this.subWord = subWord;
        popupMenu.add(list = createSuggestionList(entries), BorderLayout.CENTER);
    }

    public boolean insertSelection() {
        if (list.getSelectedValue() != null) {
            String text = textArea.getText();
            try {
                final String selectedSuggestion = ((String) list.getSelectedValue());
                Document document = textArea.getDocument();
                int insertIndex = text.lastIndexOf(subWord, insertionPosition);
                document.remove(insertIndex, subWord.length());
                document.insertString(insertIndex, selectedSuggestion, null);
                return true;
            } catch (BadLocationException ignored) {
                textArea.setText(text);
            }
            hide();
        }
        return false;
    }

    public void moveUp() {
        int index = Math.max(list.getSelectedIndex() - 1, 0);
        selectIndex(index);
    }

    public void moveDown() {
        int index = Math.min(list.getSelectedIndex() + 1, list.getModel().getSize() - 1);
        selectIndex(index);
    }

    private JList createSuggestionList(List<String> entries) {
        List<String> filteredEntries = entries.stream().filter(e -> e.startsWith(subWord)).collect(Collectors.toList());
        JList<String> list = new JList<>(filteredEntries.toArray(new String[filteredEntries.size()]));
        list.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setSelectedIndex(0);
        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    insertSelection();
                }
            }
        });
        return list;
    }

    private void selectIndex(int index) {
        final int position = textArea.getCaretPosition();
        list.setSelectedIndex(index);
        SwingUtilities.invokeLater(() -> textArea.setCaretPosition(position));
    }
}
