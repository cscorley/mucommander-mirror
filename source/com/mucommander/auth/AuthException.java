
package com.mucommander.auth;

import com.mucommander.file.FileURL;

import java.io.IOException;


/**
 * AuthException should be thrown whenever access to a remote file system was denied due to false or missing credentials.
 *
 * <p>AuthException is caught in different places of the application 
 * to provide a way for the user to authenticate (a dialog pops up).
 *
 * @author Maxence Bernard
 */
public class AuthException extends IOException {

    private FileURL fileURL;
    private String msg;

	
    /**
     * Creates a new AuthException instance, without any associated exception.
     *
     * @param fileURL file URL for which authentication failed.
     */
    public AuthException(FileURL fileURL) {
        this(fileURL, null);
    }
	
    /**
     * Creates a new AuthException instance that was caused by the given exception.
     *
     * @param fileURL file URL for which authentication failed.
     * @param msg a reason why the IOException was thrown if not <code>null</code>, in understandable terms.
     */
    public AuthException(FileURL fileURL, String msg) {
        this.fileURL = fileURL;
        if(msg!=null)
            this.msg = msg.trim();
    }
	

    /**
     * Returns the URL of the file for which authentication failed.
     */
    public FileURL getFileURL() {
        return fileURL;
    }

	
    /**
     * Returns a message describing the exception.
     */
    public String getMessage() {
        return msg;
    }

}
