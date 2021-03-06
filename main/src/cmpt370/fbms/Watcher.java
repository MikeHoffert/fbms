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

package cmpt370.fbms;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import net.contentobjects.jnotify.JNotifyListener;

import org.apache.log4j.Logger;

/**
 * Interfaces with the JNotify library to catch changes to the file system as they happen.
 */
public class Watcher implements JNotifyListener
{
	// Logger instance
	private static Logger logger = Logger.getLogger(Main.class);

	private Set<Path> createdFiles;
	private Set<Path> modifiedFiles;
	private Set<RenamedFile> renamedFiles;
	private Set<Path> deletedFiles;

	/**
	 * Creates a file watcher for the supplied lists.
	 * 
	 * @param createdFiles
	 *            List of created files.
	 * @param modifiedFiles
	 *            List of modified files.
	 * @param renamedFiles
	 *            List of renamed files.
	 * @param deletedFiles
	 *            List of deleted files.
	 */
	public Watcher(Set<Path> createdFiles, Set<Path> modifiedFiles, Set<RenamedFile> renamedFiles,
			Set<Path> deletedFiles)
	{
		this.createdFiles = createdFiles;
		this.modifiedFiles = modifiedFiles;
		this.renamedFiles = renamedFiles;
		this.deletedFiles = deletedFiles;
	}

	/**
	 * Event handler for files that are renamed. The JNotify watcher will call this method if it
	 * detects a file rename operation. Such files will be added to the appropriate list in Control.
	 * Since rename operations must store both the old name and the new name, a PoD object is used
	 * to combine both Paths into one object.
	 * 
	 * @param wd
	 *            Unused
	 * @param rootPath
	 *            The path the file is located in
	 * @param oldName
	 *            The file's previous name
	 * @param newName
	 *            The file's new name
	 */
	public void fileRenamed(int wd, String rootPath, String oldName, String newName)
	{
		// Strings must be converted into Path objects
		Path oldPath = Paths.get(rootPath + File.separator + oldName);
		Path newPath = Paths.get(rootPath + File.separator + newName);

		RenamedFile listObject = new RenamedFile();
		listObject.oldName = oldPath;
		listObject.newName = newPath;

		synchronized(renamedFiles)
		{
			renamedFiles.add(listObject);
		}

		// Use the logger in Control to issue messages
		logger.info("Renamed file " + oldName + " to " + newName + " in " + rootPath);
	}

	/**
	 * Event handler for files that are modified. The JNotify watcher will call this method if it
	 * detects a file rename operation. Such files will be added to the appropriate list in Control.
	 * 
	 * @param wd
	 *            Unused
	 * @param rootPath
	 *            The path the file is located in
	 * @param name
	 *            The file's name
	 */
	public void fileModified(int wd, String rootPath, String name)
	{
		Path path = Paths.get(rootPath + File.separator + name);

		synchronized(modifiedFiles)
		{
			modifiedFiles.add(path);
		}

		logger.info("Modified file " + path);
	}

	/**
	 * Event handler for files that are deleted. The JNotify watcher will call this method if it
	 * detects a file rename operation. Such files will be added to the appropriate list in Control.
	 * 
	 * @param wd
	 *            Unused
	 * @param rootPath
	 *            The path the file is located in
	 * @param name
	 *            The file's name
	 */
	public void fileDeleted(int wd, String rootPath, String name)
	{
		Path path = Paths.get(rootPath + File.separator + name);

		synchronized(deletedFiles)
		{
			deletedFiles.add(path);
		}

		logger.info("Deleted file " + path);
	}

	/**
	 * Event handler for files that are created. The JNotify watcher will call this method if it
	 * detects a file rename operation. Such files will be added to the appropriate list in Control.
	 * 
	 * @param wd
	 *            Unused
	 * @param rootPath
	 *            The path the file is located in
	 * @param name
	 *            The file's name
	 */
	public void fileCreated(int wd, String rootPath, String name)
	{
		Path path = Paths.get(rootPath + File.separator + name);

		synchronized(createdFiles)
		{
			createdFiles.add(path);
		}

		logger.info("Created file " + path);
	}
}
