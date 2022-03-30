package de.r3s6.jarp.args;

/**
 * Reports a problem while parsing the command line parameter.
 *
 * @author rks
 */
public class CmdLineArgExcpetion extends Exception {

    private static final long serialVersionUID = 1L;

    CmdLineArgExcpetion(final String message) {
        super(message);
    }

}
