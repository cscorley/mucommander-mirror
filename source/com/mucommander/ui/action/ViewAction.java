package com.mucommander.ui.action;

import com.mucommander.ui.MainFrame;
import com.mucommander.ui.viewer.ViewerRegistrar;
import com.mucommander.file.AbstractFile;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * This action opens the currently selected file in an integrated viewer.
 *
 * @author Maxence Bernard
 */
public class ViewAction extends MucoAction {

    public ViewAction(MainFrame mainFrame) {
        super(mainFrame, "command_bar.view", KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
    }

    public void performAction(MainFrame mainFrame) {
        AbstractFile file = mainFrame.getLastActiveTable().getSelectedFile();
        if(file!=null && !(file.isDirectory() || file.isSymlink()))
            ViewerRegistrar.createViewerFrame(mainFrame, file);
    }
}
