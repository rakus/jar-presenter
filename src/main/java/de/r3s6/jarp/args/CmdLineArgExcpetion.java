/*
 * Copyright 2022 Ralf Schandl
 *
 * Distributed under MIT license.
 * See file LICENSE for detail or visit https://opensource.org/licenses/MIT
 */
package de.r3s6.jarp.args;

/**
 * Reports a problem while parsing the command line arguments.
 *
 * @author Ralf Schandl
 */
public class CmdLineArgExcpetion extends Exception {

    private static final long serialVersionUID = 1L;

    CmdLineArgExcpetion(final String message) {
        super(message);
    }

}
