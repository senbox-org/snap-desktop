package org.esa.snap.ui.loading;

/**
 * Created by jcoravu on 7/1/2019.
 */
public interface IMessageDialog {

    public void close();

    public void showErrorDialog(String errorMessage);

    public void showErrorDialog(String message, String title);

    public void showInformationDialog(String infoMessage);

    public void showInformationDialog(String infoMessage, String title);
}
