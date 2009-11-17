/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2009 Maxence Bernard
 *
 * muCommander is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * muCommander is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mucommander.file;

import java.lang.reflect.Method;

/**
 * @author Maxence Bernard
 * @see UnsupportedFileOperationException
 * @see AbstractFile
 */
public enum FileOperation {

    /**
     * Represents a 'read' operation, as provided by {@link AbstractFile#getInputStream()}.
     *
     * @see {@link AbstractFile#getInputStream()}.
     **/
    READ_FILE,

    /**
     * Represents a 'random read' operation, as provided by {@link AbstractFile#getRandomAccessInputStream()}.
     *
     * @see {@link AbstractFile#getRandomAccessInputStream()}.
     **/
    RANDOM_READ_FILE,

    /**
     * Represents a 'write' operation, as provided by {@link AbstractFile#getOutputStream()}.
     *
     * @see {@link AbstractFile#getOutputStream()}.
     **/
    WRITE_FILE,

    /**
     * Represents an 'append' operation, as provided by {@link AbstractFile#getAppendOutputStream()}.
     *
     * @see {@link AbstractFile#getAppendOutputStream()}.
     **/
    APPEND_FILE,

    /**
     * Represents a 'random write' operation, as provided by {@link AbstractFile#getRandomAccessOutputStream()}.
     *
     * @see {@link AbstractFile#getRandomAccessOutputStream()}.
     **/
    RANDOM_WRITE_FILE,

    /**
     * Represents an 'mkdir' operation, as provided by {@link AbstractFile#mkdir()}.
     *
     * @see {@link AbstractFile#mkdir()}.
     **/
    CREATE_DIRECTORY,

    /**
     * Represents an 'ls' operation, as provided by {@link AbstractFile#ls()}.
     *
     * @see {@link AbstractFile#ls()}.
     **/
    LIST_CHILDREN,

    /**
     * Represents a 'delete' operation, as provided by {@link AbstractFile#delete()}.
     *
     * @see {@link AbstractFile#delete()}.
     **/
    DELETE,

    /**
     * Represents a 'change date' operation, as provided by {@link AbstractFile#changeDate(long)}.
     *
     * @see {@link AbstractFile#changeDate(long)}.
     **/
    CHANGE_DATE;

    // TODO
//    CHANGE_PERMISSION


    /**
     * Returns the {@link AbstractFile} method corresponding to this file operation.
     *
     * @param c the AbstractFile class for which to return a <code>Method</code> object.
     * @return the {@link AbstractFile} method corresponding to this file operation.
     */
    public Method getCorrespondingMethod(Class<? extends AbstractFile> c) {
        try {
            switch(this) {
                case READ_FILE:
                    return c.getMethod("getInputStream");

                case RANDOM_READ_FILE:
                    return c.getMethod("getRandomAccessInputStream");

                case WRITE_FILE:
                    return c.getMethod("getOutputStream");

                case APPEND_FILE:
                    return c.getMethod("getAppendOutputStream");

                case RANDOM_WRITE_FILE:
                    return c.getMethod("getRandomAccessOutputStream");

                case CREATE_DIRECTORY:
                    return c.getMethod("mkdir");

                case LIST_CHILDREN:
                    return c.getMethod("ls");

                case CHANGE_DATE:
                    return c.getMethod("changeDate", Long.TYPE);

                case DELETE:
                    return c.getMethod("delete");

                default:
                    // This should never be reached, unless method signatures have changed and this method hasn't been updated.
                    FileLogger.warning("this line should not have been executed");
                    return null;
            }
        }
        catch(Exception e) {
            // Should never happen, unless method signatures have changed and this method hasn't been updated.
            FileLogger.warning("this line should not have been executed", e);
            return null;
        }
    }
}
