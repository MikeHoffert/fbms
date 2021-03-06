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

/**
 * Largely a plain old data class for grouping together information on files and folders.
 */
public class FileInfo implements Comparable<FileInfo>
{
	public String fileName = "";
	public boolean folder = false;
	public long fileSize = 0;
	public long createdDate = 0;
	public long lastAccessedDate = 0;
	public long lastModifiedDate = 0;
	public int numberOfRevisions = 0;
	public long revisionSizes = 0;

	@Override
	public int compareTo(FileInfo o)
	{
		return this.fileName.compareTo(o.fileName);
	}
}
