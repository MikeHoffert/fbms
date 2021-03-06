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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

import javax.swing.ImageIcon;

import org.apache.log4j.Logger;

import cmpt370.fbms.gui.GuiUtility;

/**
 * DataRetriever fetches information for the GUI, bridging the gap between the GUI and data such as
 * folder and revision contents.
 */
public class DataRetriever
{
	// Logger instance
	private static Logger logger = Logger.getLogger(Main.class);

	private Path path;

	public DataRetriever(Path inPath)
	{
		path = inPath;
	}

	/**
	 * Loops through all the files in a specified folder and returns a list of information about
	 * these files (and folders).
	 * 
	 * @param path
	 *            The folder to search in
	 * @return A list of FileInfo objects that detail
	 */
	public List<FileInfo> getFolderContents()
	{
		File[] folderContents = path.toFile().listFiles();

		if(folderContents == null)
		{
			Errors.fatalError("Attempted to retreive folder contents of non-folder.");
		}

		List<FileInfo> list = new LinkedList<>();

		for(File file : folderContents)
		{
			if(path.equals(Main.backupDirectory)
					&& file.toPath().getFileName().toString().equals(".revisions.db"))
			{
				continue;
			}

			FileInfo currentFile = new FileInfo();

			// String fileName
			currentFile.fileName = file.getName();

			// boolean folder
			currentFile.folder = file.isDirectory();

			try
			{
				BasicFileAttributes attributes = Files.readAttributes(file.toPath(),
						BasicFileAttributes.class);

				// long lastAccessedDate
				currentFile.lastAccessedDate = attributes.lastAccessTime().to(TimeUnit.SECONDS);

				// long lastModifiedDate
				currentFile.lastModifiedDate = attributes.lastModifiedTime().to(TimeUnit.SECONDS);

				// long createdDate
				currentFile.createdDate = attributes.creationTime().to(TimeUnit.SECONDS);
			}
			catch(IOException e)
			{
				Errors.nonfatalError("Could not read file attributes of " + file.getName(), e);
			}

			// Leave the number of revisions and file sizes out if it's a folder
			if(!currentFile.folder)
			{
				// long fileSize
				try
				{
					currentFile.fileSize = Files.size(file.toPath());
				}
				catch(IOException e)
				{
					Errors.nonfatalError("Could not read file size of " + file.getName(), e);
				}

				DbConnection db = DbConnection.getInstance();
				List<RevisionInfo> revisionInfoList = db.getFileRevisions(FileOp.convertPath(file.toPath()));
				int totalRevisions = 0;
				long totalSizes = 0;

				if(revisionInfoList != null)
				{
					for(RevisionInfo revisionInfo : revisionInfoList)
					{
						totalRevisions++;
						if(revisionInfo.diff != null)
						{
							totalSizes += revisionInfo.diff.length() * 2; // characters are 2 bytes
						}
						else if(revisionInfo.binary != null) // Watch out for empty revisions!
						{
							totalSizes += revisionInfo.binary.length;
						}
					}

					// int numberOfRevisions
					currentFile.numberOfRevisions = totalRevisions;

					// long revisionSizes
					currentFile.revisionSizes = totalSizes;
				}
			}

			list.add(currentFile);
		}
		Collections.sort(list);

		return list;
	}

	/**
	 * Just a wrapper so that the FrontEnd can access the revision info for a file easily.
	 * 
	 * @param path
	 *            The path of the file revision info is required for.
	 * @return A list of RevisionInfo objects containing ALL the revision information about the
	 *         file.
	 */
	public List<RevisionInfo> getRevisionInfo()
	{
		DbConnection db = DbConnection.getInstance();
		return db.getFileRevisions(path);
	}

	/**
	 * Takes in a path to a folder and outputs a Vector of Vectors. The parent vector is the rows.
	 * The vector inside this is the columns.
	 * 
	 * @param path
	 *            The folder to create a table of vectors for.
	 * @return A vector of vectors of strings (a 2D vector of Strings) that the JTable can be
	 *         created from.
	 */
	public Vector<Vector<Object>> getFolderContentsTable()
	{
		Vector<Vector<Object>> tableData = new Vector<>();

		// Get the contents of the folder
		List<FileInfo> files = getFolderContents();

		logger.debug("Found " + files.size() + " entries for " + path.toString());

		// Add folders
		for(FileInfo file : files)
		{
			Vector<Object> row = null;

			if(file.folder)
			{
				row = new Vector<>();

				if(FileOp.convertPath(path.resolve(file.fileName)).toFile().exists())
				{
					row.add(new ImageIcon("res/folder.png"));
				}
				else
				{
					row.add(new ImageIcon("res/folder_deleted.png"));
				}
				row.add(file.fileName);
				row.add(GuiUtility.humanReadableBytes(file.fileSize, false));
				row.add(GuiUtility.formatDate(file.createdDate));
				row.add(GuiUtility.formatDate(file.lastAccessedDate));
				row.add(GuiUtility.formatDate(file.lastModifiedDate));
				row.add(Integer.toString(file.numberOfRevisions));
				row.add(GuiUtility.humanReadableBytes(file.revisionSizes, false));

				tableData.add(row);
			}
		}

		// Add files
		for(FileInfo file : files)
		{
			Vector<Object> row = null;

			if(!file.folder)
			{
				row = new Vector<>();

				if(FileOp.convertPath(path.resolve(file.fileName)).toFile().exists())
				{
					row.add(new ImageIcon("res/file.png"));
				}
				else
				{
					row.add(new ImageIcon("res/file_deleted.png"));
				}
				row.add(file.fileName);
				row.add(GuiUtility.humanReadableBytes(file.fileSize, false));
				row.add(GuiUtility.formatDate(file.createdDate));
				row.add(GuiUtility.formatDate(file.lastAccessedDate));
				row.add(GuiUtility.formatDate(file.lastModifiedDate));
				row.add(Integer.toString(file.numberOfRevisions));
				row.add(GuiUtility.humanReadableBytes(file.revisionSizes, false));

				tableData.add(row);
			}
		}

		return tableData;
	}

	/**
	 * Taking in path of a file and returning a vector that can be used to populate a table.
	 * 
	 * @param path
	 *            path to a file for which all the data needs to be generated.
	 * @return A vector of vectors of strings (a 2D vector of Strings) that the JTable can be
	 *         created from.
	 */
	public Vector<Vector<String>> getRevisionInfoTable()
	{
		Vector<Vector<String>> revisionData = new Vector<>();

		List<RevisionInfo> revisions = getRevisionInfo();

		Collections.sort(revisions);

		logger.debug("Found " + revisions.size() + " entries for " + path.toString());

		for(RevisionInfo revision : revisions)
		{
			Vector<String> row = new Vector<String>();

			row.add(GuiUtility.formatDate(revision.time));
			row.add(GuiUtility.humanReadableBytes(revision.filesize, false));
			if(revision.delta < 0)
			{
				row.add("<html><span style=\"color: red;\">"
						+ GuiUtility.humanReadableBytes(revision.delta, false));
			}
			else if(revision.delta > 0)
			{
				row.add("<html><span style=\"color: green;\">+"
						+ GuiUtility.humanReadableBytes(revision.delta, false));
			}
			else
			{
				row.add("<html><span style=\"color: gray;\">"
						+ GuiUtility.humanReadableBytes(revision.delta, false));
			}

			revisionData.add(row);
		}

		return revisionData;
	}
}
