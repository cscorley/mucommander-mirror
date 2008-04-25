/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2008 Maxence Bernard
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

package com.mucommander.ui.main;

import com.mucommander.bonjour.BonjourMenu;
import com.mucommander.bonjour.BonjourService;
import com.mucommander.bookmark.Bookmark;
import com.mucommander.bookmark.BookmarkListener;
import com.mucommander.bookmark.BookmarkManager;
import com.mucommander.conf.ConfigurationEvent;
import com.mucommander.conf.ConfigurationListener;
import com.mucommander.conf.impl.MuConfiguration;
import com.mucommander.file.AbstractFile;
import com.mucommander.file.FileProtocols;
import com.mucommander.file.FileURL;
import com.mucommander.file.RootFolders;
import com.mucommander.runtime.JavaVersions;
import com.mucommander.runtime.OsFamilies;
import com.mucommander.runtime.OsVersions;
import com.mucommander.text.Translator;
import com.mucommander.ui.action.MuAction;
import com.mucommander.ui.action.OpenLocationAction;
import com.mucommander.ui.button.PopupButton;
import com.mucommander.ui.dialog.server.*;
import com.mucommander.ui.event.LocationEvent;
import com.mucommander.ui.event.LocationListener;
import com.mucommander.ui.helper.MnemonicHelper;
import com.mucommander.ui.icon.FileIcons;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Hashtable;
import java.util.Vector;


/**
 * DrivePopupButton is a button which when clicked pops up a menu with a list of drives and shortcuts which can be used
 * to change the current folder.
 *
 * @author Maxence Bernard
 */
public class DrivePopupButton extends PopupButton implements LocationListener, BookmarkListener, ConfigurationListener {

    /** FolderPanel instance that contains this button */
    private FolderPanel folderPanel;
	
    /** Root folders array */
    private static AbstractFile rootFolders[] = RootFolders.getRootFolders();

    /** static FileSystemView instance, has a (non-null) value only under Windows */
    private static FileSystemView fileSystemView;

    /** Caches extended drive names, has a (non-null) value only under Windows */
    private static Hashtable extendedNameCache;


    static {
        if(OsFamilies.WINDOWS.isCurrent()) {
            fileSystemView = FileSystemView.getFileSystemView();
            extendedNameCache = new Hashtable();
        }
    }


    /**
     * Creates a new drive button which is to be added to the given FolderPanel.
     *
     * @param folderPanel the FolderPanel instance this button will be added to
     */
    public DrivePopupButton(FolderPanel folderPanel) {
        this.folderPanel = folderPanel;
		
        // Listen to location events to update drive button when folder changes
        folderPanel.getLocationManager().addLocationListener(this);

        // Listen to bookmark changes to update the drive button if a bookmark corresponding
        // to the current folder has been added/edited/removed
        BookmarkManager.addBookmarkListener(this);

        // Listen to configuration changes to update the drive if the system file icons policy has changed 
        MuConfiguration.addConfigurationListener(this);

        // Use new JButton decorations introduced in Mac OS X 10.5 (Leopard) with Java 1.5 and up
        if(OsFamilies.MAC_OS_X.isCurrent() && OsVersions.MAC_OS_X_10_5.isCurrentOrHigher() && JavaVersions.JAVA_1_5.isCurrentOrHigher()) {
            setMargin(new Insets(6,8,6,8));
            putClientProperty("JComponent.sizeVariant", "small");
            putClientProperty("JButton.buttonType", "textured");
        }
    }

    public Dimension getPreferredSize() {
        // Limit button's maximum width to something reasonable and leave enough space for location field, 
        // as bookmarks name can be as long as users want them to be.
        // Note: would be better to use JButton.setMaximumSize() but it doesn't seem to work
        Dimension d = super.getPreferredSize();
        if(d.width > 160)
            d.width = 160;
        return d;
    }


    /**
     * Updates this drive button's label and icon to reflect the current folder and match one of the drive button's
     * shortcuts:
     * <<ul>
     *	<li>If the specified folder corresponds to a bookmark, the bookmark's name will be displayed
     *	<li>If the specified folder corresponds to a local file, the enclosing volume's name will be displayed
     *	<li>If the specified folder corresponds to a remote file, the protocol's name will be displayed
     * </ul>
     * The button's icon will be the current folder's one.
     */
    private void updateButton() {
        AbstractFile currentFolder = folderPanel.getCurrentFolder();
        String currentPath = currentFolder.getAbsolutePath();
        FileURL currentURL = currentFolder.getURL();

        String newLabel = null;
//        String newToolTip = null;

        // First tries to find a bookmark matching the specified folder
        Vector bookmarks = BookmarkManager.getBookmarks();
        int nbBookmarks = bookmarks.size();
        Bookmark b;
        for(int i=0; i<nbBookmarks; i++) {
            b = (Bookmark)bookmarks.elementAt(i);
            if(currentPath.equals(b.getLocation())) {
                // Note: if several bookmarks match current folder, the first one will be used
                newLabel = b.getName();
                break;
            }
        }
		
        // If no bookmark matched current folder
        if(newLabel == null) {
            String protocol = currentURL.getProtocol();
            // Remote file, use protocol's name
            if(!protocol.equals(FileProtocols.FILE)) {
                newLabel = protocol.toUpperCase();
            }
            // Local file, use volume's name 
            else {
                // Patch for Windows UNC network paths (weakly characterized by having a host different from 'localhost'):
                // display 'SMB' which is the underlying protocol
                if(OsFamilies.WINDOWS.isCurrent() && !FileURL.LOCALHOST.equals(currentURL.getHost())) {
                    newLabel = "SMB";
                }
                else {
                    // getCanonicalPath() must be avoided under Windows for the following reasons:
                    // a) it is not necessary, Windows doesn't have symlinks
                    // b) it triggers the dreaded 'No disk in drive' error popup dialog.
                    // c) when network drives are present but not mounted (e.g. X:\ mapped onto an SMB share),
                    // getCanonicalPath which is I/O bound will take a looooong time to execute

                    if(OsFamilies.WINDOWS.isCurrent())
                        currentPath = currentFolder.getAbsolutePath(false).toLowerCase();
                    else
                        currentPath = currentFolder.getCanonicalPath(false).toLowerCase();

                    int bestLength = -1;
                    int bestIndex = 0;
                    String temp;
                    int len;
                    for(int i=0; i<rootFolders.length; i++) {
                        if(OsFamilies.WINDOWS.isCurrent())
                            temp = rootFolders[i].getAbsolutePath(false).toLowerCase();
                        else
                            temp = rootFolders[i].getCanonicalPath(false).toLowerCase();

                        len = temp.length();
                        if (currentPath.startsWith(temp) && len>bestLength) {
                            bestIndex = i;
                            bestLength = len;
                        }
                    }
                    newLabel = rootFolders[bestIndex].getName();

                    // Not used because the call to FileSystemView is slow
//                    if(fileSystemView!=null)
//                        newToolTip = getWindowsExtendedDriveName(rootFolders[bestIndex]);
                }
            }
        }
		
        setText(newLabel);
//        setToolTipText(newToolTip);
        // Set the folder icon based on the current system icons policy
        setIcon(FileIcons.getFileIcon(currentFolder));
    }


    /**
     * Returns the extended name of the given local file, e.g. "Local Disk (C:)" for C:\. The returned value is
     * interesting only under Windows. This method is I/O bound and very slow so it should not be called from the main
     * event thread.
     *
     * @param localFile the file for which to return the extended name
     * @return the extended name of the given local file
     */
    private static String getExtendedDriveName(AbstractFile localFile) {
        // Note: fileSystemView.getSystemDisplayName(java.io.File) is unfortunately very very slow
        String name = fileSystemView.getSystemDisplayName((java.io.File)localFile.getUnderlyingFileObject());

        if(name==null || name.equals(""))   // This happens for CD/DVD drives when they don't contain any disc
            return localFile.getName();

        return name;
    }


    ////////////////////////////////
    // PopupButton implementation //
    ////////////////////////////////

    public JPopupMenu getPopupMenu() {
        final JPopupMenu popupMenu = new JPopupMenu();

        // Update root folders in case new volumes were mounted
        rootFolders = RootFolders.getRootFolders();

        // Add root volumes
        final int nbRoots = rootFolders.length;
        final MainFrame mainFrame = folderPanel.getMainFrame();

        MnemonicHelper mnemonicHelper = new MnemonicHelper();   // Provides mnemonics and ensures uniqueness
        JMenuItem item;

        boolean useExtendedDriveNames = fileSystemView!=null;
        final Vector itemsV = useExtendedDriveNames?new Vector():null;

        for(int i=0; i<nbRoots; i++) {
            item = popupMenu.add(new CustomOpenLocationAction(mainFrame, new Hashtable(), rootFolders[i]));
            setMnemonic(item, mnemonicHelper);

            // Set system icon for volumes, only if system icons are available on the current platform
            item.setIcon(FileIcons.hasProperSystemIcons()?FileIcons.getSystemFileIcon(rootFolders[i]):null);

            if(useExtendedDriveNames) {
                // Use the last known value (if any) while we update it in a separate thread
                String previousExtendedName = (String)extendedNameCache.get(rootFolders[i]);
                if(previousExtendedName!=null)
                    item.setText(previousExtendedName);

                itemsV.add(item);   // JMenu offers no way to retrieve a particular JMenuItem, so we have to keep them
            }
        }

        if(useExtendedDriveNames) {
            // Calls to getExtendedDriveName(String) are very slow, so they are performed in a separate thread so as
            // to not lock the main even thread. The popup menu gets first displayed with the short drive names, and
            // then refreshed with the extended names as they are retrieved.

            new Thread() {
                public void run() {
                    for(int i=0; i<nbRoots; i++) {
                        // Under Windows, show the extended drive name (e.g. "Local Disk (C:)" instead of just "C:") but use
                        // the simple drive name for the mnemonic (i.e. 'C' instead of 'L').
                        String extendedName = getExtendedDriveName(rootFolders[i]);
                        ((JMenuItem)itemsV.elementAt(i)).setText(extendedName);

                        // Keep the extended name for later (see above)
                        extendedNameCache.put(rootFolders[i], extendedName);
                    }

                    // Re-calculate the popup menu's dimensions
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                                popupMenu.pack();
                        }
                    });
                }

            }.start();
        }

        popupMenu.add(new JSeparator());

        // Add boookmarks
        Vector bookmarks = BookmarkManager.getBookmarks();
        int nbBookmarks = bookmarks.size();
        Bookmark b;

        if(nbBookmarks>0) {
            for(int i=0; i<nbBookmarks; i++) {
                b = (Bookmark)bookmarks.elementAt(i);
                item = popupMenu.add(new CustomOpenLocationAction(mainFrame, new Hashtable(), b));
                setMnemonic(item, mnemonicHelper);
            }
        }
        else {
            // No bookmark : add a disabled menu item saying there is no bookmark
            popupMenu.add(Translator.get("bookmarks_menu.no_bookmark")).setEnabled(false);
        }

        popupMenu.add(new JSeparator());

        // Add Bonjour services menu
        setMnemonic(popupMenu.add(new BonjourMenu() {
            public MuAction getMenuItemAction(BonjourService bs) {
                return new CustomOpenLocationAction(mainFrame, new Hashtable(), bs);
            }
        }) , mnemonicHelper);
        popupMenu.add(new JSeparator());

        // Add 'connect to server' shortcuts
        setMnemonic(popupMenu.add(new ServerConnectAction("SMB...", SMBPanel.class)), mnemonicHelper);
        setMnemonic(popupMenu.add(new ServerConnectAction("FTP...", FTPPanel.class)), mnemonicHelper);
        // SFTP support is not compatible with all version of the Java runtime
        if(com.mucommander.file.impl.sftp.SFTPProtocolProvider.isAvailable())
            setMnemonic(popupMenu.add(new ServerConnectAction("SFTP...", SFTPPanel.class)), mnemonicHelper);
        setMnemonic(popupMenu.add(new ServerConnectAction("HTTP...", HTTPPanel.class)), mnemonicHelper);
        setMnemonic(popupMenu.add(new ServerConnectAction("NFS...", NFSPanel.class)), mnemonicHelper);

        return popupMenu;
    }


    /**
     * Convenience method that sets a mnemonic to the given JMenuItem, using the specified MnemonicHelper.
     *
     * @param menuItem the menu item for which to set a mnemonic
     * @param mnemonicHelper the MnemonicHelper instance to be used to determine the mnemonic's character.
     */
    private void setMnemonic(JMenuItem menuItem, MnemonicHelper mnemonicHelper) {
        menuItem.setMnemonic(mnemonicHelper.getMnemonic(menuItem.getText()));
    }


    //////////////////////////////
    // LocationListener methods //
    //////////////////////////////
	
    public void locationChanged(LocationEvent e) {
        // Update the button's label to reflect the new current folder
        updateButton();
    }
	
    public void locationChanging(LocationEvent e) {
    }
	
    public void locationCancelled(LocationEvent e) {
    }

    public void locationFailed(LocationEvent e) {
    }

    
    //////////////////////////////
    // BookmarkListener methods //
    //////////////////////////////
	
    public void bookmarksChanged() {
        // Refresh label in case a bookmark with the current location was changed
        updateButton();
    }


    ///////////////////////////////////
    // ConfigurationListener methods //
    ///////////////////////////////////

    /**
     * Listens to certain configuration variables.
     */
    public void configurationChanged(ConfigurationEvent event) {
        String var = event.getVariable();

        // Update the button's icon if the system file icons policy has changed
        if (var.equals(MuConfiguration.USE_SYSTEM_FILE_ICONS))
            updateButton();
    }


    ///////////////////
    // Inner classes //
    ///////////////////

    /**
     * This action pops up {@link com.mucommander.ui.dialog.server.ServerConnectDialog} for a specified
     * protocol.
     */
    private class ServerConnectAction extends AbstractAction {
        private Class serverPanelClass;

        private ServerConnectAction(String label, Class serverPanelClass) {
            super(label);
            this.serverPanelClass = serverPanelClass;
        }

        public void actionPerformed(ActionEvent actionEvent) {
            new ServerConnectDialog(folderPanel, serverPanelClass).showDialog();
        }
    }


    /**
     * This modified {@link OpenLocationAction} changes the current folder on the {@link FolderPanel} that contains
     * this button, instead of the currently active {@link FolderPanel}.  
     */
    private class CustomOpenLocationAction extends OpenLocationAction {

        public CustomOpenLocationAction(MainFrame mainFrame, Hashtable properties, Bookmark bookmark) {
            super(mainFrame, properties, bookmark);
        }

        public CustomOpenLocationAction(MainFrame mainFrame, Hashtable properties, AbstractFile file) {
            super(mainFrame, properties, file);
        }

        public CustomOpenLocationAction(MainFrame mainFrame, Hashtable properties, BonjourService bs) {
            super(mainFrame, properties, bs);
        }

        ////////////////////////
        // Overridden methods //
        ////////////////////////

        protected FolderPanel getFolderPanel() {
            return folderPanel;
        }
    }
}
