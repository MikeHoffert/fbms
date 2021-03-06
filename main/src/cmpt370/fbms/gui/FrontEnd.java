/*
 * FBMS: File Backup and Management System Copyright (C) 2013 Group 06
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If
 * not, see <http://www.gnu.org/licenses/>.
 */

package cmpt370.fbms.gui;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import cmpt370.fbms.Errors;
import cmpt370.fbms.Main;

/**
 * The bridge from Main to the GUI. Creates the system tray icon that allows the GUI to be accessed.
 */
public class FrontEnd
{
	// Logger instance
	private static Logger logger = Logger.getLogger(Main.class);

	private MainFrame frame;

	public FrontEnd()
	{
		// Check the SystemTray support
		if(!SystemTray.isSupported())
		{
			System.out.println("SystemTray is not supported");
			return;
		}

		// Create the icon for the tray
		BufferedImage trayIconImage = null;
		try
		{
			trayIconImage = ImageIO.read(Paths.get("res/icon.png").toFile());
		}
		catch(IOException e)
		{
			logger.error("Could not load program icon", e);
		}

		// And scale it to the appropriate size using smooth scaling
		int trayIconWidth = new TrayIcon(trayIconImage).getSize().width;
		final TrayIcon trayIcon = new TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1,
				Image.SCALE_SMOOTH));
		trayIcon.setToolTip("FBMS: Running");

		final SystemTray tray = SystemTray.getSystemTray();

		// Create a popup menu components (when right clicking on icon)
		final PopupMenu popup = new PopupMenu();
		MenuItem displayItem = new MenuItem("Display");
		MenuItem exitItem = new MenuItem("Exit");

		// Add components to popup menu
		popup.add(displayItem);
		popup.add(exitItem);

		// And set that menu to the tray icon
		trayIcon.setPopupMenu(popup);

		try
		{
			// Add to the tray
			tray.add(trayIcon);
		}
		catch(AWTException e)
		{
			Errors.fatalError("Cannot create system tray icon", e);
		}

		// Listener for when the tray icon is clicked
		trayIcon.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				// If the window doesn't exist, create it. If it already exists, bring it to the
				// front (on some OSes, this will just make it blink in the taskbar)
				frame = MainFrame.getInstance();
				frame.setVisible(true);
			}
		});

		// Listener for when the display item in the right click menu is clicked
		displayItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				frame = MainFrame.getInstance();
				frame.setVisible(true);
			}
		});

		// Listener for when the exit item in the right click menu is clicked
		exitItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				tray.remove(trayIcon);
				System.exit(0);
			}
		});
	}
}
