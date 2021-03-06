package cmpt370.fbms.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.nio.file.Path;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;

import org.apache.log4j.Logger;

import cmpt370.fbms.DataRetriever;
import cmpt370.fbms.FileOp;
import cmpt370.fbms.GuiController;
import cmpt370.fbms.Main;

/**
 * Extends a JDialog to make up the revision window, which consists of a JTable and some buttons.
 */
@SuppressWarnings("serial")
public class RevisionDialog extends JDialog
{
	// Logger instance
	private static Logger logger = Logger.getLogger(Main.class);

	public JTable table;
	public JButton viewRevisionButton, revertRevisionButton;
	public long selectedTimestamp = -1;
	public Vector<String> columns;
	public Path fileDisplayed;

	private Vector<Vector<String>> rows;

	/**
	 * Create the frame.
	 */
	public RevisionDialog(Path file)
	{
		super(MainFrame.getInstance(), true);

		// Put the relative path in the title
		String relativePath = MainFrame.getInstance().toolbar.locationBar.getText();
		if(!relativePath.endsWith(File.separator))
		{
			relativePath += File.separator;
		}
		relativePath += file.getFileName();
		setTitle("Revision Log: " + relativePath);

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setIconImage(new ImageIcon("res/icon.png").getImage());
		setSize(450, 250);

		fileDisplayed = file;

		// Create main panel
		JPanel contentPane = new JPanel(new BorderLayout());

		// Create table columns
		columns = new Vector<>();
		columns.add("Date");
		columns.add("File size");
		columns.add("Delta");

		// Create table
		table = new JTable();
		table.setShowGrid(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getTableHeader().setReorderingAllowed(false);
		table.addMouseListener(new RevisionTableSelectionListener(this));
		table.addKeyListener(new RevisionTableSelectionListener(this));

		// Create the data model
		DataRetriever revisionRetriever = new DataRetriever(fileDisplayed);
		rows = revisionRetriever.getRevisionInfoTable();
		table.setModel(new DefaultTableModel(rows, columns)
		{
			@Override
			public boolean isCellEditable(int row, int column)
			{
				return false;
			}
		});

		// Set the column widths
		table.getColumnModel().getColumn(0).setMinWidth(115);

		// Create scrollpane for the table
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		table.setFillsViewportHeight(true);

		// Create buttons at the bottom
		JPanel buttonPanel = new JPanel(new FlowLayout());
		viewRevisionButton = new JButton("View revision");
		revertRevisionButton = new JButton("Revert revision");
		viewRevisionButton.setEnabled(false);
		revertRevisionButton.setEnabled(false);
		buttonPanel.add(viewRevisionButton);
		buttonPanel.add(revertRevisionButton);

		// And add to the main panel
		contentPane.add(scrollPane, BorderLayout.CENTER);
		contentPane.add(buttonPanel, BorderLayout.SOUTH);
		add(contentPane);

		// Make the enter key active the cell instead of going to the next cell. Basically, we
		// create a new keyboard mapping for the enter key which does nothing, allowing the listener
		// to pick up the enter key later.
		final String solve = "Solve";
		KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
		table.getInputMap(JTable.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(enter, solve);
		table.getActionMap().put(solve, null);

		// Necessary to revalidate the frame so that we can see the table
		revalidate();

		// Handler for view revision button
		viewRevisionButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(selectedTimestamp != -1)
				{
					new GuiController().displayRevision(MainFrame.getInstance().selectedFile,
							selectedTimestamp);

					logger.debug("Viewed revision of "
							+ FileOp.convertPath(MainFrame.getInstance().selectedFile).toString()
							+ " @ T = " + selectedTimestamp);
				}
			}
		});

		// Handler for revert revision button
		revertRevisionButton.addActionListener(new ActionListener()
		{
			@Override
			public void actionPerformed(ActionEvent arg0)
			{
				if(selectedTimestamp != -1)
				{
					new GuiController().revertRevision(MainFrame.getInstance().selectedFile,
							selectedTimestamp);
					redrawTable();

					logger.debug("Reverted revision of "
							+ MainFrame.getInstance().selectedFile.toString() + " @ T = "
							+ selectedTimestamp);
				}
			}
		});

		// Auto refresh the revision table every 2 seconds
		new Timer(2000, new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				long selectedTimestampBackup = selectedTimestamp;

				// Note that this will blank selectedTimestamp
				redrawTable();

				// Reselect the row (if any)
				if(selectedTimestampBackup != -1)
				{
					// Figure out which row is supposed to be selected (by name)
					int selectedRowNumber = -1;
					for(Vector<String> row : rows)
					{
						selectedRowNumber++;
						String fileName = row.get(0);
						if(GuiUtility.unformatDate(fileName) == selectedTimestampBackup)
						{
							// Found the right row
							selectedTimestamp = selectedTimestampBackup;
							revertRevisionButton.setEnabled(true);
							viewRevisionButton.setEnabled(true);
							break;
						}
					}
					table.setRowSelectionInterval(selectedRowNumber, selectedRowNumber);
				}
			}
		}).start();
	}

	public void redrawTable()
	{
		// Create the data model
		DataRetriever revisionRetriever = new DataRetriever(fileDisplayed);
		rows = revisionRetriever.getRevisionInfoTable();
		((DefaultTableModel) table.getModel()).setDataVector(rows, columns);

		// Set the column widths
		table.getColumnModel().getColumn(0).setMinWidth(115);

		selectedTimestamp = -1;
		revertRevisionButton.setEnabled(false);
		viewRevisionButton.setEnabled(false);
	}
}

/**
 * An event listener for finding changes to the currently selected row in the table (via either
 * clicking a row with the mouse or navigating via the keyboard).
 */
class RevisionTableSelectionListener extends MouseAdapter implements KeyListener
{
	// Logger instance
	private static Logger logger = Logger.getLogger(Main.class);

	private RevisionDialog dialog;

	RevisionTableSelectionListener(RevisionDialog dialogRef)
	{
		dialog = dialogRef;
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		selectRow();

		// Get double clicks
		if(e.getClickCount() == 2 && dialog.table.rowAtPoint(e.getPoint()) != -1)
		{
			activateRow();
		}
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		selectRow();

		// If the enter key was pressed, "enter" that row (go into folders and open revision window
		// for files)
		if(e.getKeyCode() == KeyEvent.VK_ENTER)
		{
			activateRow();
		}
	}

	/**
	 * Called when a row is selected. Sets that file as the current file and enables
	 * context-specific menu options.
	 */
	private void selectRow()
	{
		// If we have selected a valid row
		if(dialog.table.getSelectedRow() != -1)
		{
			// Get the selected timestamp
			dialog.selectedTimestamp = GuiUtility.unformatDate((String) dialog.table.getValueAt(
					dialog.table.getSelectedRow(), 0));

			dialog.revertRevisionButton.setEnabled(true);
			dialog.viewRevisionButton.setEnabled(true);

			logger.debug("Selected revision: " + MainFrame.getInstance().selectedFile.toString()
					+ " (timestamp: " + dialog.selectedTimestamp + ")");
		}
		else
		{
			dialog.selectedTimestamp = -1;
			dialog.revertRevisionButton.setEnabled(false);
			dialog.viewRevisionButton.setEnabled(false);
		}
	}

	/**
	 * Catches a double click or enter key on a row, which goes into folders and opens the revision
	 * info window for files.
	 */
	private void activateRow()
	{
		if(dialog.selectedTimestamp != -1)
		{
			new GuiController().displayRevision(MainFrame.getInstance().selectedFile,
					dialog.selectedTimestamp);

			logger.debug("Viewed revision of "
					+ FileOp.convertPath(MainFrame.getInstance().selectedFile).toString()
					+ " @ T = " + dialog.selectedTimestamp);
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{}

	@Override
	public void keyTyped(KeyEvent e)
	{}
}
