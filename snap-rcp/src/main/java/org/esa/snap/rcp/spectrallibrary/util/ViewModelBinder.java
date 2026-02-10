package org.esa.snap.rcp.spectrallibrary.util;

import org.esa.snap.rcp.spectrallibrary.model.SpectralLibraryViewModel;
import org.esa.snap.rcp.spectrallibrary.model.UiStatus;
import org.esa.snap.rcp.spectrallibrary.ui.SpectralLibraryPanel;
import org.esa.snap.speclib.model.SpectralAxis;
import org.esa.snap.speclib.model.SpectralLibrary;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


public class ViewModelBinder {

    private final SpectralLibraryViewModel vm;
    private final SpectralLibraryPanel panel;


    public ViewModelBinder(SpectralLibraryViewModel vm, SpectralLibraryPanel panel) {
        this.vm = vm;
        this.panel = panel;
    }


    public void bind() {
        vm.addPropertyChangeListener(new VmListener());

        panel.getPreviewPanel().setSelectionListener(vm::setSelectedPreviewProfileId);

        panel.getLibraryTable().getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            int viewRow = panel.getLibraryTable().getSelectedRow();
            if (viewRow < 0) {
                vm.setSelectedLibraryProfileId(null);
                return;
            }
            int modelRow = panel.getLibraryTable().convertRowIndexToModel(viewRow);
            UUID id = panel.getLibraryTableModel().getIdAt(modelRow);
            vm.setSelectedLibraryProfileId(id);
        });

        updateLibraries(vm.getLibraries());
        updateActiveLibrarySelection(vm.getActiveLibraryId());
        panel.getLibraryTableModel().setProfiles(vm.getLibraryProfiles());
        panel.getPreviewPanel().setProfiles(vm.getPreviewProfiles());
        panel.getPreviewPanel().setSelectedProfileId(vm.getSelectedPreviewProfileId().orElse(null));
        updatePreviewAxisFromActiveLibrary();
        updateStatus(vm.getStatus());
    }


    private class VmListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            Runnable ui = () -> {
                switch (evt.getPropertyName()) {
                    case SpectralLibraryViewModel.PROP_LIBRARIES -> {
                        updateLibraries(vm.getLibraries());
                        updateActiveLibrarySelection(vm.getActiveLibraryId());
                        updatePreviewAxisFromActiveLibrary();
                    }
                    case SpectralLibraryViewModel.PROP_ACTIVE_LIBRARY_ID -> {
                        updateActiveLibrarySelection(vm.getActiveLibraryId());
                        updatePreviewAxisFromActiveLibrary();
                    }
                    case SpectralLibraryViewModel.PROP_LIBRARY_PROFILES ->
                            panel.getLibraryTableModel().setProfiles(vm.getLibraryProfiles());
                    case SpectralLibraryViewModel.PROP_PREVIEW_PROFILES ->
                            panel.getPreviewPanel().setProfiles(vm.getPreviewProfiles());
                    case SpectralLibraryViewModel.PROP_SELECTED_PREVIEW_PROFILE_ID ->
                            panel.getPreviewPanel().setSelectedProfileId(vm.getSelectedPreviewProfileId().orElse(null));
                    case SpectralLibraryViewModel.PROP_STATUS ->
                            updateStatus(vm.getStatus());
                    default -> {}
                }
            };
            if (SwingUtilities.isEventDispatchThread()) {
                ui.run();
            } else {
                SwingUtilities.invokeLater(ui);
            }
        }
    }

    private void updateLibraries(List<SpectralLibrary> libs) {
        DefaultComboBoxModel<Object> m = new DefaultComboBoxModel<>();
        for (SpectralLibrary l : libs) m.addElement(l);
        panel.getLibraryCombo().setModel(m);
    }

    private void updateActiveLibrarySelection(Optional<UUID> idOpt) {
        ComboBoxModel<Object> m = panel.getLibraryCombo().getModel();
        if (idOpt.isEmpty()) {
            if (m.getSize() > 0) panel.getLibraryCombo().setSelectedIndex(0);
            return;
        }
        UUID id = idOpt.get();
        for (int i = 0; i < m.getSize(); i++) {
            Object o = m.getElementAt(i);
            if (o instanceof SpectralLibrary lib && id.equals(lib.getId())) {
                panel.getLibraryCombo().setSelectedItem(o);
                return;
            }
        }
    }

    private void updatePreviewAxisFromActiveLibrary() {
        SpectralLibrary lib = getSelectedLibraryFromCombo();
        if (lib == null) {
            panel.getPreviewPanel().clearXAxis();
            return;
        }

        SpectralAxis axis = lib.getAxis();
        double[] x = axis != null ? axis.getWavelengths() : null;
        if (x == null || x.length == 0) {
            panel.getPreviewPanel().clearXAxis();
        } else {
            panel.getPreviewPanel().setXAxis(x);
        }
    }

    private SpectralLibrary getSelectedLibraryFromCombo() {
        Object sel = panel.getLibraryCombo().getSelectedItem();
        return (sel instanceof SpectralLibrary lib) ? lib : null;
    }

    private void updateStatus(UiStatus status) {
        if (status == null) {
            panel.getStatusLabel().setText(" ");
            return;
        }
        String prefix = switch (status.severity()) {
            case IDLE -> "";
            case INFO -> "Info: ";
            case WARN -> "Warn: ";
            case ERROR -> "Error: ";
        };
        panel.getStatusLabel().setText(prefix + status.message());
    }
}
