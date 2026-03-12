package org.esa.snap.rcp.spectrallibrary.model;


public record UiStatus(Severity severity, String message) {


    public enum Severity { IDLE, INFO, WARN, ERROR }


    public static UiStatus idle() {
        return new UiStatus(Severity.IDLE, "");
    }

    public static UiStatus info(String msg) {
        return new UiStatus(Severity.INFO, msg);
    }

    public static UiStatus warn(String msg) {
        return new UiStatus(Severity.WARN, msg);
    }

    public static UiStatus error(String msg) {
        return new UiStatus(Severity.ERROR, msg);
    }
}