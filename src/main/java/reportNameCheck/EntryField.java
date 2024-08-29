package reportNameCheck;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.regex.Pattern;

import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

/**
 * An entry field that takes a regular expression and highlights red when the
 * inputted text does not match the regex
 * 
 * @author Jaden
 * @version 0.0.1
 * @since 0.0.1
 */
@SuppressWarnings("serial")
public class EntryField extends JTextField implements DocumentListener {

	/**
	 * The regular expression that the user's input to the text field should match
	 */
	Pattern regex;

	/**
	 * Whether the text currently matches {@link #regex}
	 */
	boolean isValid = false;

	/**
	 * Which index of {@link Main#activeNamePortions} this edits
	 * 
	 * Note that these can be used when we're not constructing the name
	 * ({@link Main#doDescPopup(String, String, String, boolean)} uses one), but we
	 * don't read {@link Main#activeNamePortions} at that point, so it doesn't
	 * matter - use a placeholder 0.
	 */
	int nameNum = -1;

	/**
	 * Constructs an EntryField with the specified properties
	 * @param regex the regular expression to match
	 * @param defaultText the "ghost text" to feature before user edits
	 * @param nameNum which index of {@link Main#activeNamePortions} to edit
	 */
	EntryField(String regex, String defaultText, int nameNum) {
		super();
		TextPrompt prompt = new TextPrompt(defaultText, this);
		prompt.changeAlpha(150);
		this.setPreferredSize(new Dimension(prompt.getPreferredSize().width + 10, this.getPreferredSize().height));
		this.regex = Pattern.compile(regex);
		this.nameNum = nameNum;

		getDocument().addDocumentListener(this);
	}

	/**
	 * Checks if the text currently matches {@link #regex}
	 */
	private void checkText() {
		String text = this.getText();
		if (regex.matcher(text).matches()) {
			this.setForeground(Color.BLACK);
			isValid = true;
		} else {
			this.setForeground(Color.RED);
			isValid = false;
		}
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {
	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		checkText();
		Main.activeNamePortions[nameNum] = getText();
		Main.rewritePreviewNameDialog();
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		checkText();
		Main.activeNamePortions[nameNum] = getText();
		Main.rewritePreviewNameDialog();
	}
}

/**
 * The 'ghost text' in the back of a {@link EntryField}
 * @author Jaden
 * @version 0.0.1
 * @since 0.0.1
 */
@SuppressWarnings("serial")
class TextPrompt extends JLabel implements FocusListener, DocumentListener {

	/**
	 * The component the label is attached to (an instance of {@link EntryField}
	 */
	private JTextComponent component;

	/**
	 * The document of the {@link #component}
	 */
	private Document document;

	/**
	 * Constructs a TextPrompt with the given text attached to the given component
	 * @param text the text to show
	 * @param component the component to show text on
	 */
	public TextPrompt(String text, JTextComponent component) {
		this.component = component;
		document = component.getDocument();

		setText(text);
		setFont(component.getFont());
		setForeground(component.getForeground());
		setBorder(new EmptyBorder(component.getInsets()));
		setHorizontalAlignment(JLabel.LEADING);

		component.addFocusListener(this);
		document.addDocumentListener(this);

		component.setLayout(new BorderLayout());
		component.add(this);
		checkForPrompt();
	}

	/**
	 * Sets the alpha (opacity) of the text
	 * @param alpha the alpha to set to
	 */
	void changeAlpha(int alpha) {
		alpha = alpha > 255 ? 255 : alpha < 0 ? 0 : alpha;

		Color foreground = getForeground();
		int red = foreground.getRed(), green = foreground.getGreen(), blue = foreground.getBlue();

		Color withAlpha = new Color(red, green, blue, alpha);
		super.setForeground(withAlpha);
	}

	/**
	 * Checks if the prompt should be shown
	 */
	private void checkForPrompt() {
		if (document.getLength() > 0) {
			setVisible(false);
			return;
		}

		if (component.hasFocus())
			setVisible(false);
		else
			setVisible(true);
	}

	@Override
	public void changedUpdate(DocumentEvent arg0) {
		checkForPrompt();
	}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		checkForPrompt();
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		checkForPrompt();
	}

	@Override
	public void focusGained(FocusEvent arg0) {
		checkForPrompt();
	}

	@Override
	public void focusLost(FocusEvent arg0) {
		checkForPrompt();
	}

}
