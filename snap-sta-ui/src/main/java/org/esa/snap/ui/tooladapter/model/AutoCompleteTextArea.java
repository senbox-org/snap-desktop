package org.esa.snap.ui.tooladapter.model;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extension of JTextArea that allows autocompletion of entries, based on
 * operator's system variables and parameters.
 *
 * @author Cosmin Cara
 */
public class AutoCompleteTextArea extends JTextArea {
    private InputOptionsPanel suggestion;
    private List<String> autoCompleteEntries;
    private char triggerChar;

    public AutoCompleteTextArea(String text, int rows, int columns) {
        super(text, rows, columns);
        addKeyListener(new KeyListener() {
            private boolean triggerCharPressed;

            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER || e.getKeyChar() == KeyEvent.VK_TAB) {
                    if (suggestion != null && triggerCharPressed) {
                        if (suggestion.insertSelection()) {
                            e.consume();
                            final int position = getCaretPosition();
                            SwingUtilities.invokeLater(() -> {
                                try {
                                    getDocument().remove(position - 1, 1);
                                } catch (BadLocationException ex) {
                                    ex.printStackTrace();
                                }
                            });
                        }
                    }
                    triggerCharPressed = false;
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN && suggestion != null && triggerCharPressed) {
                    suggestion.moveDown();
                } else if (e.getKeyCode() == KeyEvent.VK_UP && suggestion != null && triggerCharPressed) {
                    suggestion.moveUp();
                } else if (e.getKeyChar() == triggerChar) {
                    triggerCharPressed = true;
                    SwingUtilities.invokeLater(AutoCompleteTextArea.this::showSuggestion);
                } else if (Character.isLetterOrDigit(e.getKeyChar()) && triggerCharPressed || e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
                    SwingUtilities.invokeLater(AutoCompleteTextArea.this::showSuggestion);
                } else if (Character.isWhitespace(e.getKeyChar()) || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    triggerCharPressed = false;
                    hideSuggestion();
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {

            }
        });
    }

    /**
     * Sets the character that will trigger autocompletion
     */
    public void setTriggerChar(char trigger) {
        triggerChar = trigger;
    }

    /**
     * Sets the list of autocompletion entries (suggestions)
     */
    public void setAutoCompleteEntries(List<String> entries) {
        autoCompleteEntries = entries;
    }

    protected void showSuggestion() {
        hideSuggestion();
        final int position = getCaretPosition();
        Point location;
        try {
            location = modelToView(position).getLocation();
        } catch (BadLocationException e) {
            return;
        }
        String text = getText();
        int start = Math.max(0, text.lastIndexOf(triggerChar, position));
        if (start + 1 > position) {
            return;
        }
        final String subWord = text.substring(start + 1, position);
        if (suggestion == null) {
            suggestion = new InputOptionsPanel(this);
        }
        List<String> filtered = autoCompleteEntries != null ? autoCompleteEntries.stream().filter(e -> e.startsWith(subWord)).collect(Collectors.toList()) :
                (autoCompleteEntries = new ArrayList<>());
        if (filtered.isEmpty()) {
            hideSuggestion();
        } else {
            suggestion.setSuggestionList(subWord.isEmpty() ? autoCompleteEntries : filtered, subWord);
            suggestion.show(position, location);
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        }
    }

    protected void hideSuggestion() {
        if (suggestion != null) {
            suggestion.hide();
        }
    }

    /**
     * This will visually hold the current suggestions (if any).
     */
    class InputOptionsPanel {
        private JList<String> list;
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
            createSuggestionList(entries);
            popupMenu.add(list, BorderLayout.CENTER);
        }

        public boolean insertSelection() {
            if (list.getSelectedValue() != null) {
                String text = textArea.getText();
                try {
                    final String selectedSuggestion = list.getSelectedValue();
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

        private void createSuggestionList(List<String> entries) {
            if (list == null) {
                list = new JList<>(entries.toArray(new String[entries.size()]));
                list.setDoubleBuffered(true);
                list.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 0));
                list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                list.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        if (e.getClickCount() == 2) {
                            insertSelection();
                        }
                    }
                });
            } else {
                list.removeAll();
                list.setListData(entries.toArray(new String[entries.size()]));
            }
            list.setSelectedIndex(0);
        }

        private void selectIndex(int index) {
            final int position = textArea.getCaretPosition();
            list.setSelectedIndex(index);
            SwingUtilities.invokeLater(() -> textArea.setCaretPosition(position));
        }
    }
}
