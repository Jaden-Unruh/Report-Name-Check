package reportNameCheck;

import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * Entry class for {@link reportNameCheck}
 * 
 * @author Jaden
 * @version 1.0.0
 * @since 0.0.1
 */
public class Main {

	// TODO more automatic?
	/*
	 * TODO a couple known flaws: 
	 * - If a file with the same name in the same directory as the target for a
	 * report rename, the report will not be renamed 
	 * - Some exceptions (null pointer exception when referencing `year` from
	 * `rewritePreviewNameDialog` from `EntryField.insertUpdate`...) don't throw to
	 * user to get fst
	 */

	/**
	 * The hierarchy spreadsheet and the parent directory for the reports, once
	 * selected
	 */
	static File[] selectedFiles = new File[2];

	/**
	 * The current state of {@link #info}
	 * 
	 * @see #getInfoText()
	 */
	static InfoText infoText;

	/**
	 * The primary application window
	 */
	static JFrame window;

	/**
	 * Information label at the bottom of {@link #window}
	 * 
	 * @see Main#infoText
	 */
	static JLabel info = new JLabel();

	/**
	 * Buttons on {@link #window} to open the selected directory
	 * ({@link #selectedFiles} index 1), and to start the program
	 */
	static JButton open, run;

	/**
	 * A preview of the folders in the selected directory ({@link #selectedFiles}
	 * index 1), shown on {@link #window}
	 */
	static JTextArea preview;

	/**
	 * Writes to the output information file (Report Name Check Information %s.txt)
	 */
	static FileWriter writeToInfo;

	/**
	 * The time the program started running, in nanoseconds from an arbitrary origin
	 * time (used relatively, from {@link System#nanoTime()}
	 */
	static long startTime;

	/**
	 * High-level representation of the location hierarchy spreadsheet (from
	 * {@link #selectedFiles} index 0)
	 */
	static XSSFSheet hierarchySheet;

	/**
	 * All reports found within the selected directory ({@link #selectedFiles} index
	 * 1), defined in {@link #findReports()}.
	 * 
	 * @see Main#incorrectReports
	 */
	static HashSet<File> reports;
	/**
	 * A subset of {@link #reports} containing only those files whose names are
	 * incorrect. Defined in {@link #checkNaming()}.
	 */
	static HashSet<File> incorrectReports;

	/**
	 * Regular expression representing a correctly named Sub-site report
	 * 
	 * 20##_IE###_A##-##_AB######_FCA Sub-Site Report_asdf
	 */
	static final String correctSubSiteRegex = "(20\\d{2})_(IE\\d{3}|IA\\d{3}|JS\\d{3})_([A-Z]\\d{2}-\\d{2})_(AB\\d{6})_FCA Sub-Site Report_(.+)\\.pdf$";

	/**
	 * Regular expression representing a correctly named Appendix B report
	 * 
	 * 20##_IE###_A##-##_AB######_FCA Sub-Site Report App B_asdf
	 */
	static final String correctAppBRegex = "(20\\d{2})_(IE\\d{3}|IA\\d{3}|JS\\d{3})_([A-Z]\\d{2}-\\d{2})_(AB\\d{6})_FCA Sub-Site Report App B_(.+)\\.pdf$";

	/**
	 * All characters that are invalid for a site description within a report name
	 */
	static final String[] INVALID_CHARACTERS = { "-", "(", ")", ".", "'", "\\", "/", ":", "_", "\"", "," };

	/**
	 * Used when prompting users for information. If true, it's a description
	 * prompt, if false, it's a full-name prompt
	 */
	static boolean isDescPopup = false;

	/**
	 * Entry method for Report Name Check. Opens main program window.
	 * 
	 * @param args unused
	 */
	public static void main(String[] args) {
		openWindow();
	}

	/**
	 * Populates and opens the main program window
	 */
	private static void openWindow() {
		window = new JFrame(Messages.getString("Main.window.title"));
		window.setLayout(new GridBagLayout());
		window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		window.add(new JLabel(Messages.getString("Main.window.hierarchyPrompt")), simpleConstraints(0, 0, 2, 1));
		JButton selectHierarchy = new SelectButton(0, false);
		window.add(selectHierarchy, simpleConstraints(2, 0, 2, 1));

		window.add(new JLabel(Messages.getString("Main.window.parentPrompt")), simpleConstraints(0, 1, 2, 1));
		JButton selectParent = new SelectButton(1, true);
		window.add(selectParent, simpleConstraints(2, 1, 2, 1));

		window.add(new JLabel(Messages.getString("Main.window.previewText")), simpleConstraints(0, 2, 4, 1));
		preview = new JTextArea();
		JScrollPane previewPane = new JScrollPane(preview);
		previewPane.setPreferredSize(new Dimension(300, 100));
		window.add(previewPane, simpleConstraints(0, 3, 4, 1));

		window.add(info, simpleConstraints(0, 4, 4, 1));

		JButton close = new JButton(Messages.getString("Main.window.close"));
		window.add(close, simpleConstraints(0, 5, 1, 1));

		JButton help = new JButton(Messages.getString("Main.window.help"));
		window.add(help, simpleConstraints(1, 5, 1, 1));

		open = new JButton(Messages.getString("Main.window.open"));
		window.add(open, simpleConstraints(2, 5, 1, 1));
		open.setEnabled(false);

		run = new JButton(Messages.getString("Main.window.run"));
		window.add(run, simpleConstraints(3, 5, 1, 1));
		run.setEnabled(false);

		close.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});

		help.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String[] choices = { Messages.getString("Main.window.help.close"),
						Messages.getString("Main.window.help.github") };
				if (JOptionPane.showOptionDialog(window, Messages.getString("Main.window.help.text"),
						Messages.getString("Main.window.help.title"), JOptionPane.DEFAULT_OPTION,
						JOptionPane.INFORMATION_MESSAGE, null, choices, choices[0]) == 1) {
					if (Desktop.isDesktopSupported())
						try {
							Desktop.getDesktop()
									.browse(new URL("https://github.com/Jaden-Unruh/Report-Name-Check").toURI());
						} catch (Exception e1) {
							updateInfo(InfoText.DESKTOP);
						}
					else
						updateInfo(InfoText.DESKTOP);
				}
			}
		});

		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().open(selectedFiles[1]);
				} catch (IOException e1) {
					try {
						showErrorMessage(e1);
					} catch (IOException e2) {
					}
				}
			}
		});

		run.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (checkCorrectSelections()) {
					SwingWorker<Boolean, Void> sw = new SwingWorker<Boolean, Void>() {
						protected Boolean doInBackground() throws Exception {

							init();
							loadSheets();
							findReports();
							checkNaming();
							handleIncorrect();
							terminate();

							updateInfo(InfoText.DONE);
							run.setEnabled(true);

							return true;
						}

						@Override
						protected void done() {
							try {
								get();
							} catch (InterruptedException | ExecutionException e) {
								run.setEnabled(true);
								try {
									showErrorMessage(e);
								} catch (IOException e1) {
								}
							}
						}
					};
					run.setEnabled(false);
					sw.execute();
				} else
					updateInfo(InfoText.SELECT_PROMPT);
			}
		});

		ImageIcon icon = new ImageIcon(Main.class.getClassLoader().getResource("reportNameCheck/Akana_Logo.png"));
		window.setIconImage(icon.getImage());

		window.pack();
		window.setVisible(true);
	}

	/**
	 * Creates and initializes the info file
	 * 
	 * @throws IOException if there's an error creating the file
	 * @see #writeToInfo
	 */
	private static void init() throws IOException {
		startTime = System.nanoTime();
		updateInfo(InfoText.INIT);

		String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss.SSS"));
		File infoFile = new File(
				String.format(Messages.getString("Main.infoFile.name"), selectedFiles[1].getParent(), dateTime));
		infoFile.createNewFile();
		writeToInfo = new FileWriter(infoFile);
		writeToInfo.append(String.format(Messages.getString("Main.infoFile.header"), dateTime));
	}

	/**
	 * Creates {@link #hierarchySheet} from the selected location hierarchy file
	 * (Main{@link #selectedFiles} index 0)
	 * 
	 * @throws FileNotFoundException if the file cannot be found
	 * @throws IOException           if there's an error reading the file
	 */
	private static void loadSheets() throws FileNotFoundException, IOException {
		updateInfo(InfoText.LOAD_SHEETS);
		XSSFWorkbook hierarchyBook = new XSSFWorkbook(new FileInputStream(selectedFiles[0]));
		hierarchyBook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
		hierarchySheet = hierarchyBook.getSheetAt(0);
	}

	/**
	 * Finds all reports within the selected parent directory -
	 * {@link #selectedFiles} index 1. Adds all found reports to {@link #reports}
	 * 
	 * @see #reports
	 * @see #findReportsRecur(File)
	 */
	private static void findReports() {
		updateInfo(InfoText.FIND_REPORTS);
		reports = new HashSet<>();
		findReportsRecur(selectedFiles[1]);
	}

	/**
	 * Finds all reports within the provided directory, used recursively. Adds all
	 * found reports to {@link #reports}
	 * 
	 * @param directory the directory to search
	 * @see #reports
	 * @see #findReports()
	 */
	private static void findReportsRecur(File directory) {
		File[] contents = directory.listFiles();
		for (File file : contents) {
			if (file.isDirectory())
				findReportsRecur(file);
			if (FilenameUtils.getExtension(file.getName()).equals("pdf"))
				reports.add(file);
		}
	}

	/**
	 * Checks the naming on all {@link #reports}.
	 * 
	 * Compares against {@link #correctSubSiteRegex} and {@link #correctAppBRegex}.
	 * If it matches, pull each component and confirm that the rows in
	 * {@link #hierarchySheet} corresponding to the location ID and maximo ID are
	 * the same and have all the same other components of the file name. If
	 * description varies or contains invalid characters, prompt the user with
	 * {@link #getDescription(String, String, String)}.
	 * 
	 * If anything doesn't match, or the user skips the description prompt, add to
	 * {@link #incorrectReports} and continue.
	 * 
	 * @throws IOException if {@link #writeToInfo} fails
	 */
	private static void checkNaming() throws IOException {
		updateInfo(InfoText.CHECK_NAMES);
		incorrectReports = new HashSet<>();
		int corrSubSite = 0, corrAppB = 0;
		Pattern subSiteRegex = Pattern.compile(correctSubSiteRegex);
		Pattern appBRegex = Pattern.compile(correctAppBRegex);
		for (File file : reports) {
			String name = file.getName();
			Matcher subSiteMatch = subSiteRegex.matcher(name), appBMatch = appBRegex.matcher(name);
			subSiteMatch.find();
			appBMatch.find();
			if (subSiteMatch.matches() || appBMatch.matches()) {
				boolean appB = appBMatch.matches();
				Matcher corrMatch = appB ? appBMatch : subSiteMatch;
				String year = corrMatch.group(1), siteID = corrMatch.group(2), locID = corrMatch.group(3),
						maximo = corrMatch.group(4), desc = corrMatch.group(5);
				XSSFRow foundRow = findRowMID(maximo);
				if (foundRow == null)
					// can't find maximo ID, add to files with incorrect names
					incorrectReports.add(file);
				else {
					if (foundRow.getCell(2).toString().equals(siteID) && foundRow.getCell(4).toString().equals(locID)) {
						String finalDescription = getDescription(desc, foundRow.getCell(3).toString(), name);
						if (finalDescription == null) {
							// user skipped/closed dialog, add to files with incorrect names
							incorrectReports.add(file);
							continue;
						}
						file.renameTo(
								new File(
										file.getParent() + "\\"
												+ String.format(Messages.getString("Main.name.nameFormat"), year,
														siteID, locID, maximo, appB ? " App B" : "", finalDescription)
												+ ".pdf"));
						if (appB)
							corrAppB++;
						else
							corrSubSite++;
					} else {
						// some data incorrect, add to files with incorrect names
						incorrectReports.add(file);
					}
				}
				continue;
			}
			// file name is incorrect (doesn't match regex for app B or sub-site)
			incorrectReports.add(file);
		}
		writeToInfo.write(String.format(Messages.getString("Main.infoFile.correct"), corrSubSite, corrAppB));
	}

	/**
	 * Whether the user is actively skipping a dialog
	 */
	private static boolean doSkip;

	/**
	 * Prompts the user for a site description for the file with the provided name
	 * 
	 * @param currDesc   the current site description on the file
	 * @param sheetDesc  the site description pulled from {@link #hierarchySheet}
	 * @param reportName the full name of the report
	 * @return the user-decided site description
	 * @see #doDescPopup(String, String, String, boolean)
	 */
	private static String getDescription(String currDesc, String sheetDesc, String reportName) {
		if (currDesc.equals(sheetDesc)) {
			if (Arrays.stream(INVALID_CHARACTERS).anyMatch(currDesc::contains)) {
				return doDescPopup(currDesc, sheetDesc, reportName, true);
			} else {
				return currDesc;
			}
		} else {
			return doDescPopup(currDesc, sheetDesc, reportName, false);
		}
	}

	/**
	 * Initiates the popup prompting a site description
	 * 
	 * @param currDesc   the current site description on the file
	 * @param sheetDesc  the site description pulled from {@link #hierarchySheet}
	 * @param reportName the full name of the report
	 * @param equal      whether currDesc and sheetDesc are the same
	 * @return the users input for site description
	 * @see #getDescription(String, String, String)
	 */
	private static String doDescPopup(String currDesc, String sheetDesc, String reportName, boolean equal) {

		isDescPopup = true;

		JPanel popupPanel = new JPanel(new GridBagLayout());

		popupPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

		if (equal)
			popupPanel.add(new JLabel(Messages.getString("Main.popup.topLabel")), simpleConstraints(0, 0, 3, 1));
		else
			popupPanel.add(new JLabel(Messages.getString("Main.popup.topLabelUnequal")), simpleConstraints(0, 0, 3, 1));
		popupPanel.add(new JLabel(reportName), simpleConstraints(0, 1, 3, 1));
		popupPanel.add(new JLabel(Messages.getString("Main.popup.oldDesc")), simpleConstraints(0, 2, 1, 1));

		JTextField currDescField = new JTextField(currDesc);
		popupPanel.add(currDescField, simpleConstraints(1, 2, 1, 1));
		currDescField.setHorizontalAlignment(JTextField.CENTER);
		currDescField.setEditable(false);
		currDescField.setFocusable(false);

		JButton copy = new JButton(Messages.getString("Main.popup.copy"));
		popupPanel.add(copy, simpleConstraints(2, 2, 1, 1));

		popupPanel.add(new JLabel(Messages.getString("Main.popup.midLabel")), simpleConstraints(0, 4, 3, 1));

		EntryField entry = new EntryField("[^._\\-\\/\\\\'\":,()]+", "description", 0);
		popupPanel.add(entry, simpleConstraints(0, 5, 3, 1));
		entry.setPreferredSize(new Dimension(100, 20));

		if (!equal) {
			popupPanel.add(new JLabel(Messages.getString("Main.popup.sheetDesc")), simpleConstraints(0, 3, 1, 1));

			JTextField sheetDescField = new JTextField(sheetDesc);
			popupPanel.add(sheetDescField, simpleConstraints(1, 3, 1, 1));
			sheetDescField.setHorizontalAlignment(JTextField.CENTER);
			sheetDescField.setEditable(false);
			sheetDescField.setFocusable(false);

			JButton copySheet = new JButton(Messages.getString("Main.popup.copy"));
			popupPanel.add(copySheet, simpleConstraints(2, 3, 1, 1));

			copySheet.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent arg0) {
					entry.setText(sheetDescField.getText());
				}
			});
		}

		JButton skip = new JButton(Messages.getString("Main.popup.skip")),
				done = new JButton(Messages.getString("Main.popup.done"));
		popupPanel.add(skip, simpleConstraints(0, 6, 1, 1));
		popupPanel.add(done, simpleConstraints(1, 6, 2, 1));

		JDialog popup = new JDialog(window, Messages.getString("Main.popup.title"), true);
		popup.add(popupPanel);
		popup.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		doSkip = true;

		copy.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				entry.setText(currDescField.getText());
			}
		});

		skip.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doSkip = true;
				popup.dispose();
			}
		});

		done.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				doSkip = false;
				popup.dispose();
			}
		});

		popup.pack();
		popup.setVisible(true);

		if (doSkip)
			return null;
		else
			return entry.getText();
	}

	/**
	 * Regular expressions representing each component of a correct report name
	 */
	final static String YEAR_REGEX = "20\\d{2}", SITE_ID_REGEX = "IE\\d{3}|IA\\d{3}|JS\\d{3}",
			LOCATION_ID_REGEX = "[A-Z]\\d{2}-\\d{2}", MAXIMO_REGEX = "AB\\d{6}", APP_B_REGEX = "App B",
			DESC_REGEX = "FCA Sub-Site Report(?: App B)?_(.+).pdf";

	/**
	 * Processes files in {@link #incorrectReports}
	 * 
	 * Attempts to find year, site, location, maximo id, description, and whether
	 * the file is an App B from the name. Also attempts to pull data from the
	 * location hierarchy spreadsheet.
	 * 
	 * Opens a dialog with
	 * {@link #getNameDialog(File, String, String, String, String, String, String, String, String, String, String)}
	 * with all the found information to get the name, then renames the file.
	 * 
	 * @throws IOException if {@link #writeToInfo} fails
	 */
	private static void handleIncorrect() throws IOException {
		updateInfo(InfoText.INCORRECT);
		for (File file : incorrectReports) {
			String foundName = file.getName();

			String foundYear = findMatch(Pattern.compile(YEAR_REGEX).matcher(foundName)),
					foundSite = findMatch(Pattern.compile(SITE_ID_REGEX).matcher(foundName)),
					foundLocation = findMatch(Pattern.compile(LOCATION_ID_REGEX).matcher(foundName)),
					foundMaximo = findMatch(Pattern.compile(MAXIMO_REGEX).matcher(foundName)),
					foundAppB = findMatch(Pattern.compile(APP_B_REGEX).matcher(foundName)),
					foundDesc = findMatch(Pattern.compile(DESC_REGEX).matcher(foundName), 1);

			XSSFRow sheetRowMID = findRowMID(foundMaximo);
			XSSFRow sheetRowLOC = findRowLOC(foundLocation);

			String sheetSite = "", sheetLocation = "", sheetMaximo = "", sheetDesc = "";

			if (sheetRowMID.equals(sheetRowLOC) && sheetRowMID != null) {
				sheetSite = sheetRowMID.getCell(2).toString();
				sheetLocation = sheetRowMID.getCell(4).toString();
				sheetMaximo = sheetRowMID.getCell(7).toString();
				sheetDesc = sheetRowMID.getCell(3).toString();
			}

			String newName = getNameDialog(file, foundYear, foundSite, sheetSite, foundLocation, sheetLocation,
					foundMaximo, sheetMaximo, foundAppB, foundDesc, sheetDesc);

			if (newName != null)
				file.renameTo(new File(file.getParent() + "\\" + newName + ".pdf"));
			else
				writeToInfo.append(String.format(Messages.getString("Main.infoFile.skipped"), file.getAbsolutePath()));
		}
	}

	/**
	 * Returns the match found by the given Matcher, unless there is no match, then
	 * return an empty String
	 * 
	 * @param match the Matcher
	 * @return the match found, or an empty String
	 * @see #findMatch(Matcher, int)
	 */
	static String findMatch(Matcher match) {
		try {
			match.find();
			return match.group();
		} catch (IllegalStateException e) {
			return "";
		}
	}

	/**
	 * Returns the specified group found by the given Matcher, unless there is no
	 * match, then return an empty String
	 * 
	 * @param match the Matcher
	 * @param group the 1-indexed group
	 * @return the group found, or an empty String
	 * @see #findMatch(Matcher)
	 */
	static String findMatch(Matcher match, int group) {
		try {
			match.find();
			return match.group(group);
		} catch (IllegalStateException e) {
			return "";
		}
	}

	/**
	 * Each part of the name of the file actively being renamed.
	 * 
	 * By index: {year, siteID, locationID, maximoID, ?appB, description}
	 */
	static String[] activeNamePortions = new String[6];

	/**
	 * The preview of what the file will be named.
	 * 
	 * @see #activeNamePortions
	 */
	static JTextField previewNameDialog = new JTextField();

	/**
	 * Checkbox on {@link #nameDialog} for the user to select whether the file is an
	 * appendix B report
	 */
	static JCheckBox isAppB = new JCheckBox();

	/**
	 * The main panel on {@link #nameDialog}
	 */
	static JPanel nameDialogPanel;

	/**
	 * A popup for the user to construct the name for a file
	 * 
	 * @see #nameDialogPanel
	 */
	static JDialog nameDialog;

	/**
	 * Entry fields on {@link #nameDialog} for each component of the report name
	 */
	static EntryField year, site, loc, max, desc;

	/**
	 * A button to close {@link #nameDialog} and save the input name
	 */
	static JButton done;

	/**
	 * Opens the dialog to prompt the user for a file name
	 * 
	 * @param file          the file to rename
	 * @param foundYear     the year found in the name of the file
	 * @param foundSite     the siteId found in the name of the file
	 * @param sheetSite     the siteID found in {@link #hierarchySheet}
	 * @param foundLocation the locationID found in the name of the file
	 * @param sheetLocation the locationID found in {@link #hierarchySheet}
	 * @param foundMaximo   the maximoID found in the name of the file
	 * @param sheetMaximo   the maximoID found in {@link #hierarchySheet}
	 * @param foundAppB     " App B" if the file is appendix B as determined by the
	 *                      file name, "" otherwise
	 * @param foundDesc     the site description found in the name of the file
	 * @param sheetDesc     the site description found in {@link #hierarchySheet}
	 * @return the name for the file
	 */
	private static String getNameDialog(File file, String foundYear, String foundSite, String sheetSite,
			String foundLocation, String sheetLocation, String foundMaximo, String sheetMaximo, String foundAppB,
			String foundDesc, String sheetDesc) {

		isDescPopup = false;

		nameDialogPanel = new JPanel(new GridBagLayout());

		nameDialogPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

		nameDialogPanel.add(new JLabel(Messages.getString("Main.name.topText")), simpleConstraints(0, 0, 6, 1));
		nameDialogPanel.add(new JLabel(file.getName()), simpleConstraints(0, 1, 5, 1));

		JButton open = new JButton(Messages.getString("Main.name.open"));
		nameDialogPanel.add(open, simpleConstraints(5, 1, 1, 1));

		nameDialogPanel.add(new JLabel(Messages.getString("Main.name.year")), simpleConstraints(0, 2, 1, 1));
		nameDialogPanel.add(new JLabel(Messages.getString("Main.name.site")), simpleConstraints(1, 2, 1, 1));
		nameDialogPanel.add(new JLabel(Messages.getString("Main.name.loc")), simpleConstraints(2, 2, 1, 1));
		nameDialogPanel.add(new JLabel(Messages.getString("Main.name.max")), simpleConstraints(3, 2, 1, 1));
		nameDialogPanel.add(new JLabel(Messages.getString("Main.name.isAB")), simpleConstraints(4, 2, 1, 1));
		nameDialogPanel.add(new JLabel(Messages.getString("Main.name.desc")), simpleConstraints(5, 2, 1, 1));

		nameDialogPanel.add(new FieldPortion(foundYear, 0), simpleConstraints(0, 3, 1, 1));
		nameDialogPanel.add(new FieldPortion(foundSite, 1), simpleConstraints(1, 3, 1, 1));
		nameDialogPanel.add(new FieldPortion(sheetSite, 1), simpleConstraints(1, 4, 1, 1));
		nameDialogPanel.add(new FieldPortion(foundLocation, 2), simpleConstraints(2, 3, 1, 1));
		nameDialogPanel.add(new FieldPortion(sheetLocation, 2), simpleConstraints(2, 4, 1, 1));
		nameDialogPanel.add(new FieldPortion(foundMaximo, 3), simpleConstraints(3, 3, 1, 1));
		nameDialogPanel.add(new FieldPortion(sheetMaximo, 3), simpleConstraints(3, 4, 1, 1));
		nameDialogPanel.add(new FieldPortion(foundDesc, 5), simpleConstraints(5, 3, 1, 1));
		nameDialogPanel.add(new FieldPortion(sheetDesc, 5), simpleConstraints(5, 4, 1, 1));

		nameDialogPanel.add(isAppB, simpleConstraints(4, 3, 1, 3));
		isAppB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				rewritePreviewNameDialog();
			}
		});
		isAppB.setSelected(foundAppB.equals("App B"));

		year = new EntryField(YEAR_REGEX, "20##", 0);
		site = new EntryField(SITE_ID_REGEX, "IA000", 1);
		loc = new EntryField(LOCATION_ID_REGEX, "A00-00", 2);
		max = new EntryField(MAXIMO_REGEX, "AB000000", 3);
		desc = new EntryField("[^._\\-\\/\\\\'\":,()]+", "Akana University", 5);

		nameDialogPanel.add(year, simpleConstraints(0, 5, 1, 1));
		nameDialogPanel.add(site, simpleConstraints(1, 5, 1, 1));
		nameDialogPanel.add(loc, simpleConstraints(2, 5, 1, 1));
		nameDialogPanel.add(max, simpleConstraints(3, 5, 1, 1));
		nameDialogPanel.add(desc, simpleConstraints(5, 5, 1, 1));

		nameDialogPanel.add(previewNameDialog, simpleConstraints(0, 6, 6, 1));
		previewNameDialog.setEditable(false);
		previewNameDialog.setFocusable(false);

		JButton skip = new JButton(Messages.getString("Main.popup.skip"));
		done = new JButton(Messages.getString("Main.popup.done"));

		nameDialogPanel.add(skip, simpleConstraints(1, 7, 2, 1));
		nameDialogPanel.add(done, simpleConstraints(4, 7, 2, 1));

		nameDialog = new JDialog(window, Messages.getString("Main.name.title"), true);
		nameDialog.add(nameDialogPanel);
		nameDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		skip.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doSkip = true;
				nameDialog.dispose();
			}
		});

		done.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				doSkip = false;
				nameDialog.dispose();
			}
		});

		open.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					Desktop.getDesktop().open(file);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});

		activeNamePortions = new String[6];
		doSkip = true;
		rewriteTextNameDialog();
		rewritePreviewNameDialog();
		nameDialog.pack();
		nameDialog.setVisible(true);

		if (doSkip)
			return null;
		else {
			rewriteTextNameDialog();
			rewritePreviewNameDialog();
			return previewNameDialog.getText();
		}
	}

	/**
	 * Updates and rewrites {@link #previewNameDialog} on {@link #nameDialog}
	 */
	static void rewritePreviewNameDialog() {
		// if (!isDescPopup) {
		previewNameDialog.setText(String.format(Messages.getString("Main.name.nameFormat"), activeNamePortions[0],
				activeNamePortions[1], activeNamePortions[2], activeNamePortions[3],
				isAppB.isSelected() ? " App B" : "", activeNamePortions[5]));
		done.setEnabled(year.isValid && site.isValid && loc.isValid && max.isValid && desc.isValid);
		nameDialog.pack();
		// }
	}

	/**
	 * Updates and rewrites the EntryFields on {@link #nameDialog}
	 */
	static void rewriteTextNameDialog() {
		year.setText(activeNamePortions[0]);
		site.setText(activeNamePortions[1]);
		loc.setText(activeNamePortions[2]);
		max.setText(activeNamePortions[3]);
		desc.setText(activeNamePortions[5]);
		nameDialog.pack();
	}

	/**
	 * Closes {@link #hierarchySheet} and writes and closes {@link #writeToInfo}
	 * 
	 * @throws IOException
	 */
	private static void terminate() throws IOException {
		updateInfo(InfoText.CLOSING);
		hierarchySheet.getWorkbook().close();

		String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS"));
		DecimalFormat secondFormatter = new DecimalFormat("#,###.########");

		double seconds = (double) (System.nanoTime() - startTime) / 1e9;

		writeToInfo.append(
				String.format(Messages.getString("Main.infoFile.footer"), dateTime, secondFormatter.format(seconds)));

		writeToInfo.close();
	}

	/**
	 * Finds a row in {@link #hierarchySheet} with the given Maximo ID
	 * 
	 * @param maximoID the maximo id to find
	 * @return the corresponding row
	 * @see #findRowLOC(String)
	 */
	private static XSSFRow findRowMID(String maximoID) {
		int rows = hierarchySheet.getPhysicalNumberOfRows();
		for (int i = 0; i < rows; i++) {
			XSSFRow row = hierarchySheet.getRow(i);
			if (row == null)
				return null;
			if (row.getCell(7).toString().equals(maximoID))
				return row;
		}
		return null;
	}

	/**
	 * Finds a row in {@link #hierarchySheet} with the given Location ID
	 * 
	 * @param location the location id to find
	 * @return the corresponding row
	 * @see #findRowMID(String)
	 */
	private static XSSFRow findRowLOC(String location) {
		int rows = hierarchySheet.getPhysicalNumberOfRows();
		for (int i = 0; i < rows; i++) {
			XSSFRow row = hierarchySheet.getRow(i);
			if (row == null)
				return null;
			if (row.getCell(4).toString().equals(location) && !row.getCell(6).toString().equals(""))
				return row;
		}
		return null;
	}

	/**
	 * Confirms the user has selected an xlsx file for the hierarchy spreadsheet and
	 * a directory for the parent directory
	 * 
	 * @return true if both selections are correct
	 */
	private static boolean checkCorrectSelections() {
		return FilenameUtils.getExtension(selectedFiles[0].getName()).equals("xlsx") && selectedFiles[1].isDirectory();
	}

	/**
	 * Shows an error message window for the given exception
	 * 
	 * @param e the exception to show a window for
	 * @throws IOException if {@link #writeToInfo} fails
	 */
	private static void showErrorMessage(Exception e) throws IOException {

		e.printStackTrace();
		String[] choices = { Messages.getString("Main.window.error.close"),
				Messages.getString("Main.window.error.more") };
		updateInfo(InfoText.ERROR);
		writeToInfo.append(String.format("Error encountered: %s\n", e.toString()));
		if (JOptionPane.showOptionDialog(window,
				String.format(Messages.getString("Main.window.error.header"), e.toString()), "Error",
				JOptionPane.DEFAULT_OPTION, JOptionPane.ERROR_MESSAGE, null, choices, choices[0]) == 1) {
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));
			JTextArea jta = new JTextArea(25, 50);
			jta.setText(String.format(Messages.getString("Main.window.error.fst"), sw.toString()));
			jta.setEditable(false);
			JOptionPane.showMessageDialog(window, new JScrollPane(jta), "Error", JOptionPane.ERROR_MESSAGE);
		}

		String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss.SSS"));
		DecimalFormat secondFormatter = new DecimalFormat("#,###.########");

		double seconds = (double) (System.nanoTime() - startTime) / 1e9;

		writeToInfo.append(
				String.format(Messages.getString("Main.infoFile.footer"), dateTime, secondFormatter.format(seconds)));

		writeToInfo.close();
	}

	/**
	 * Creates a GridBagConstraints object with the given attributes, and all other
	 * values set to defaults
	 * 
	 * @param x      horizontal location in grid bag
	 * @param y      vertical location in grid bag
	 * @param width  columns spanned in grid bag
	 * @param height rows spanned in grid bag
	 * @return the new GridBagConstraints object
	 */
	static GridBagConstraints simpleConstraints(int x, int y, int width, int height) {
		return new GridBagConstraints(x, y, width, height, 0, 0, GridBagConstraints.CENTER, 0, new Insets(0, 0, 0, 0),
				0, 0);
	}

	/**
	 * Gets the text that should currently be shown in {@link #info}, from
	 * {@link #infoText}
	 * 
	 * @return the String of text that should be shown
	 * @see #updateInfo(InfoText)
	 */
	static String getInfoText() {
		switch (infoText) {
		case CHECK_NAMES:
			return Messages.getString("Main.infoText.checkNames");
		case CLOSING:
			return Messages.getString("Main.infoText.closing");
		case DESKTOP:
			return Messages.getString("Main.infoText.desktop");
		case ERROR:
			return Messages.getString("Main.infoText.error");
		case FIND_REPORTS:
			return Messages.getString("Main.infoText.findReports");
		case INCORRECT:
			return Messages.getString("Main.infoText.incorrect");
		case INIT:
			return Messages.getString("Main.infoText.init");
		case LOAD_SHEETS:
			return Messages.getString("Main.infoText.loadSheets");
		case SELECT_PROMPT:
			return Messages.getString("Main.infoText.selectPrompt");
		case DONE:
			return Messages.getString("Main.infoText.done");
		}
		return null;
	}

	/**
	 * Updates info to the specified enum value
	 * 
	 * @param text the value to set to
	 * @see #info
	 * @see #infoText
	 * @see #getInfoText()
	 */
	static void updateInfo(InfoText text) {
		infoText = text;
		info.setText(getInfoText());
		window.pack();
	}
}