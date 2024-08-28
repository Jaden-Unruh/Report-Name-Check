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
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;

import org.apache.commons.io.FilenameUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class Main {

	static File[] selectedFiles = new File[2];

	static InfoText infoText;

	static JFrame window;
	static JLabel info = new JLabel();
	static JButton open, run;
	static JTextArea preview;

	static FileWriter writeToInfo;

	static long startTime;

	static XSSFSheet hierarchySheet;

	static HashSet<File> reports;
	static HashSet<File> incorrectReports;

	static final String correctSubSiteRegex = "(20\\d{2})_(IE\\d{3}|IA\\d{3}|JS\\d{3})_([A-Z]\\d{2}-\\d{2})_(AB\\d{6})_FCA Sub-Site Report_.*$";
	static final String correctAppBRegex = "(20\\d{2})_(IE\\d{3}|IA\\d{3}|JS\\d{3})_([A-Z]\\d{2}-\\d{2})_(AB\\d{6})_FCA Sub-Site Report App B_.*$";

	static final String[] INVALID_CHARACTERS = { "-", "(", ")", ".", "'", "\\", "/", ":", "_", "\"", "," };

	public static void main(String[] args) {
		openWindow();

		getNameDialog(new File("2024_IA000_A00-00_AB000000_FCA Sub-Site Report_Akana University"), "2024", "IA000",
				"JS123", "A00-00", "J12-34", "AB000000", "AB123456", " App B", "Akana University", "Akana-University");
	}

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

							// TODO: terminate

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

	private static void loadSheets() throws FileNotFoundException, IOException {
		updateInfo(InfoText.LOAD_SHEETS);
		XSSFWorkbook hierarchyBook = new XSSFWorkbook(new FileInputStream(selectedFiles[0]));
		hierarchyBook.setMissingCellPolicy(Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
		hierarchySheet = hierarchyBook.getSheetAt(0);
	}

	private static void findReports() {
		updateInfo(InfoText.FIND_REPORTS);
		reports = new HashSet<>();
		findReportsRecur(selectedFiles[1]);
	}

	private static void findReportsRecur(File directory) {
		File[] contents = directory.listFiles();
		for (File file : contents) {
			if (file.isDirectory())
				findReportsRecur(file);
			if (FilenameUtils.getExtension(file.getName()).equals("pdf"))
				reports.add(file);
		}
	}

	private static void checkNaming() throws IOException {
		updateInfo(InfoText.CHECK_NAMES);
		incorrectReports = new HashSet<>();
		int corrSubSite = 0, corrAppB = 0;
		Pattern subSiteRegex = Pattern.compile(correctSubSiteRegex);
		Pattern appBRegex = Pattern.compile(correctAppBRegex);
		for (File file : reports) {
			String name = file.getName();
			Matcher subSiteMatch = subSiteRegex.matcher(name), appBMatch = appBRegex.matcher(name);
			if (subSiteMatch.matches()) {
				String year = subSiteMatch.group(1), siteID = subSiteMatch.group(2), locID = subSiteMatch.group(3),
						maximo = subSiteMatch.group(4), desc = subSiteMatch.group(5);
				XSSFRow foundRow = findRowMID(maximo);
				if (foundRow == null)
					// can't find maximo ID, add to files with incorrect names
					incorrectReports.add(file);
				else {
					if (foundRow.getCell(2).toString().equals(siteID) && foundRow.getCell(4).equals(locID)) {
						String finalDescription = getDescription(desc, foundRow.getCell(15).toString(), name);
						if (finalDescription == null) {
							// user skipped/closed dialog, add to files with incorrect names
							incorrectReports.add(file);
							continue;
						}
						file.renameTo(new File(
								file.getParent() + String.format(Messages.getString("Main.function.subSiteFormat"),
										year, siteID, locID, maximo, finalDescription)));
						corrSubSite++;
					} else {
						// some data incorrect, add to files with incorrect names
						incorrectReports.add(file);
					}
				}
				continue;
			}
			if (appBMatch.matches()) {
				corrAppB++;
				continue;
			}
			// file name is incorrect (doesn't match regex for app B or sub-site)
			incorrectReports.add(file);
		}
		writeToInfo.write(String.format(Messages.getString("Main.infoFile.correct"), corrSubSite, corrAppB));
	}

	private static boolean doSkip;

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

	private static String doDescPopup(String currDesc, String sheetDesc, String reportName, boolean equal) {
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

		EntryField entry = new EntryField("[^._\\-\\/\\\\'\":,()]+", "description");
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

	final static String YEAR_REGEX = "20\\d{2}";
	final static String SITE_ID_REGEX = "IE\\d{3}|IA\\d{3}|JS\\d{3}";
	final static String LOCATION_ID_REGEX = "[A-Z]\\d{2}-\\d{2}";
	final static String MAXIMO_REGEX = "AB\\d{6}";
	final static String APP_B_REGEX = "App B";
	final static String DESC_REGEX = "FCA Sub-Site Report(?: App B)?_(.+)";

	private static void handleIncorrect() {
		updateInfo(InfoText.INCORRECT);
		for (File file : incorrectReports) {
			String foundName = file.getName();

			String foundYear = Pattern.compile(YEAR_REGEX).matcher(foundName).group(),
					foundSite = Pattern.compile(SITE_ID_REGEX).matcher(foundName).group(),
					foundLocation = Pattern.compile(LOCATION_ID_REGEX).matcher(foundName).group(),
					foundMaximo = Pattern.compile(MAXIMO_REGEX).matcher(foundName).group(),
					foundAppB = Pattern.compile(APP_B_REGEX).matcher(foundName).group(),
					foundDesc = Pattern.compile(DESC_REGEX).matcher(foundName).group(1);

			XSSFRow sheetRowMID = findRowMID(foundMaximo);
			XSSFRow sheetRowLOC = findRowLOC(foundLocation);

			String sheetSite = "", sheetLocation = "", sheetMaximo = "", sheetDesc = "";

			if (sheetRowMID.equals(sheetRowLOC)) {
				sheetSite = sheetRowMID.getCell(2).toString();
				sheetLocation = sheetRowMID.getCell(4).toString();
				sheetMaximo = sheetRowMID.getCell(7).toString();
				sheetDesc = sheetRowMID.getCell(15).toString();
			}

			String newName = getNameDialog(file, foundYear, foundSite, sheetSite, foundLocation, sheetLocation,
					foundMaximo, sheetMaximo, foundAppB, foundDesc, sheetDesc);
		}
	}

	static String[] activeNamePortions = new String[6];
	static JTextField previewNameDialog = new JTextField();
	static JCheckBox isAppB = new JCheckBox();
	static JPanel nameDialogPanel;
	static JDialog nameDialog;
	static EntryField year, site, loc, max, desc;

	private static String getNameDialog(File file, String foundYear, String foundSite, String sheetSite,
			String foundLocation, String sheetLocation, String foundMaximo, String sheetMaximo, String foundAppB,
			String foundDesc, String sheetDesc) {
		nameDialogPanel = new JPanel(new GridBagLayout());

		nameDialogPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

		nameDialogPanel.add(new JLabel(Messages.getString("Main.name.topText")), simpleConstraints(0, 0, 6, 1));
		nameDialogPanel.add(new JLabel(file.getName()), simpleConstraints(0, 1, 5, 1));

		JButton open = new JButton(Messages.getString("Main.name.open"));
		nameDialogPanel.add(open, simpleConstraints(9, 1, 1, 1));

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

		year = new EntryField(YEAR_REGEX, "20##");
		site = new EntryField(SITE_ID_REGEX, "IA000");
		loc = new EntryField(LOCATION_ID_REGEX, "A00-00");
		max = new EntryField(MAXIMO_REGEX, "AB000000");
		desc = new EntryField("[^._\\-\\/\\\\'\":,()]+", "Akana University");

		nameDialogPanel.add(year, simpleConstraints(0, 5, 1, 1));
		nameDialogPanel.add(site, simpleConstraints(1, 5, 1, 1));
		nameDialogPanel.add(loc, simpleConstraints(2, 5, 1, 1));
		nameDialogPanel.add(max, simpleConstraints(3, 5, 1, 1));
		nameDialogPanel.add(desc, simpleConstraints(5, 5, 1, 1));

		nameDialogPanel.add(previewNameDialog, simpleConstraints(0, 6, 6, 1));
		previewNameDialog.setEditable(false);
		previewNameDialog.setFocusable(false);

		JButton skip = new JButton(Messages.getString("Main.popup.skip")),
				done = new JButton(Messages.getString("Main.popup.done"));

		nameDialog = new JDialog(window, Messages.getString("Main.popup.title"), true);
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

		rewritePreviewNameDialog();
		nameDialog.pack();
		nameDialog.setVisible(true);

		if (doSkip)
			return null;
		else {
			rewritePreviewNameDialog();
			return previewNameDialog.getText();
		}
	}

	static void rewritePreviewNameDialog() {
		//Making not-invoke later shows up, but concurrency(?) error
		Runnable doRewrite = new Runnable() {
			public void run() {
				previewNameDialog.setText(String.format(Messages.getString("Main.name.nameFormat"), activeNamePortions[0],
						activeNamePortions[1], activeNamePortions[2], activeNamePortions[3],
						isAppB.isSelected() ? " App B" : "", activeNamePortions[5]));
				nameDialog.pack();
				year.setText(activeNamePortions[0]);
				site.setText(activeNamePortions[1]);
				loc.setText(activeNamePortions[2]);
				max.setText(activeNamePortions[3]);
				desc.setText(activeNamePortions[5]);
			}
		};
		SwingUtilities.invokeLater(doRewrite);
	}

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

	private static XSSFRow findRowLOC(String location) {
		int rows = hierarchySheet.getPhysicalNumberOfRows();
		for (int i = 0; i < rows; i++) {
			XSSFRow row = hierarchySheet.getRow(i);
			if (row == null)
				return null;
			if (row.getCell(4).toString().equals(location))
				return row;
		}
		return null;
	}

	private static boolean checkCorrectSelections() {
		return FilenameUtils.getExtension(selectedFiles[0].getName()).equals("xlsx") && selectedFiles[1].isDirectory();
	}

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

	static String getInfoText() {
		return "";
	}

	/**
	 * Updates info to the specified enum value
	 * 
	 * @param text the value to set to
	 */
	static void updateInfo(InfoText text) {
		infoText = text;
		info.setText(getInfoText());
		window.pack();
	}
}