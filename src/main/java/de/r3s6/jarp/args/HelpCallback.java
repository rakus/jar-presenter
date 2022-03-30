package de.r3s6.jarp.args;

/**
 * Callback to show help when '--help' was found.
 *
 * @author rks
 */
@FunctionalInterface
public interface HelpCallback {
    /** Execute it. */
    void accept();
}
