/*
 * This file is part of muCommander, http://www.mucommander.com
 * Copyright (C) 2002-2012 Maxence Bernard
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

package com.mucommander.ui.main.tabs;

import com.mucommander.commons.file.AbstractFile;
import com.mucommander.ui.event.LocationEvent;
import com.mucommander.ui.event.LocationListener;
import com.mucommander.ui.main.FolderPanel;
import com.mucommander.ui.main.FolderPanel.ChangeFolderThread;
import com.mucommander.ui.main.MainFrame;
import com.mucommander.ui.tabs.HideableTabbedPane;
import com.mucommander.ui.tabs.TabFactory;
import com.mucommander.ui.tabs.TabUpdater;

/**
* HideableTabbedPane of {@link com.mucommander.ui.main.tabs.FileTableTab} instances.
* 
* @author Arik Hadas
*/
public class FileTableTabs extends HideableTabbedPane<FileTableTab> implements LocationListener {

	/** FolderPanel containing those tabs */
	private FolderPanel folderPanel;
	
	/** Factory of instances of FileTableTab */
	private TabFactory<FileTableTab, AbstractFile> tabsFactory;
	
	public FileTableTabs(MainFrame mainFrame, FolderPanel folderPanel, AbstractFile[] initialFolders, int indexOfSelectedTab) {
		super(new FileTableTabsDisplayFactory(mainFrame, folderPanel));
		
		this.folderPanel = folderPanel;
		
		tabsFactory = new FileTableTabFactory(folderPanel);
		
		// Register to location change events
		folderPanel.getLocationManager().addLocationListener(this);
		
		// Add the initial folders
		for (AbstractFile folder : initialFolders)
			addTab(tabsFactory.createTab(folder));

		// Select the tab that was previously selected on last run
		selectTab(indexOfSelectedTab);
	}
	
	@Override
	protected void selectTab(int index) {
		super.selectTab(index);

		ChangeFolderThread changeFolderThread = folderPanel.tryChangeCurrentFolder(getTab(index).getLocation(), null, true);
		try {
			if (changeFolderThread != null)
				changeFolderThread.join();
		} catch (InterruptedException e) {
			// We're screwed - no valid location to display
			throw new RuntimeException("Unable to read any drive");
		}
	}
	
	/**
	 * Return the currently selected tab
	 * 
	 * @return currently selected tab
	 */
	public FileTableTab getCurrentTab() {
		return getTab(getSelectedIndex());
	}
	
	private void updateTabLocation(final AbstractFile location) {
		updateCurrentTab(new TabUpdater<FileTableTab>() {
			
			public void update(FileTableTab tab) {
				tab.setLocation(location);
			}
		});
	}
	
	private void updateTabLocking(final boolean lock) {
		updateCurrentTab(new TabUpdater<FileTableTab>() {
			
			public void update(FileTableTab tab) {
				tab.setLocked(lock);
			}
		});
	}
	
	/********************
	 * MuActions support
	 ********************/
	
	public void add(AbstractFile file) {
		addTab(tabsFactory.createTab(file));
	}
	
	public void add(FileTableTab tab) {
		addAndSelectTab(tab);
	}
	
	public FileTableTab closeCurrentTab() {
		return removeTab();
	}
	
	public void closeDuplicateTabs() {
		removeDuplicateTabs();
	}
	
	public void closeOtherTabs() {
		removeOtherTabs();
	}
	
	public void duplicate() {
		add(tabsFactory.createTab(folderPanel.getCurrentFolder()));
	}
	
	public void lock() {
		updateTabLocking(true);
	}
	
	public void unlock() {
		updateTabLocking(false);
	}

	/****************
	 * Other Actions
	 ****************/
	
	public void close(FileTableTabHeader fileTableTabHeader) {
		removeTab(fileTableTabHeader);
	}
	
	/**********************************
	 * LocationListener Implementation
	 **********************************/
	
	public void locationChanged(LocationEvent locationEvent) {
		updateTabLocation(folderPanel.getCurrentFolder());
	}

	public void locationCancelled(LocationEvent locationEvent) {
		updateTabLocation(folderPanel.getCurrentFolder());
	}

	public void locationFailed(LocationEvent locationEvent) {
		updateTabLocation(folderPanel.getCurrentFolder());
	}
	
	public void locationChanging(LocationEvent locationEvent) { }
}
