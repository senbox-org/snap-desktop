package org.esa.snap.graphbuilder.gpf.core;

/**
 * Enum representing the different status of a node.
 */
public enum NodeStatus {
    INCOMPLETE, // if missing inputs or the configuration is incomplete
    VALIDATED, // if it passed the test
    ERROR, // if something went wrong...
}