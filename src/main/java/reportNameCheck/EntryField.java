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

@SuppressWarnings("serial")
public class EntryField extends JTextField implements DocumentListener {

	Pattern regex;
	
	boolean isValid = false;
	
	EntryField(String regex, String defaultText) {
		super();
		TextPrompt prompt = new TextPrompt(defaultText, this);
		prompt.changeAlpha(150);
		this.setPreferredSize(new Dimension(prompt.getPreferredSize().width + 10, this.getPreferredSize().height));
		this.regex = Pattern.compile(regex);
		
		getDocument().addDocumentListener(this);
	}
	
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
	public void changedUpdate(DocumentEvent arg0) {}

	@Override
	public void insertUpdate(DocumentEvent arg0) {
		checkText();
		Main.rewritePreviewNameDialog();
	}

	@Override
	public void removeUpdate(DocumentEvent arg0) {
		checkText();
		Main.rewritePreviewNameDialog();
	}
}

@SuppressWarnings("serial")
class TextPrompt extends JLabel implements FocusListener, DocumentListener {
	
	private JTextComponent component;
	
	private Document document;
	
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
	
	void changeAlpha(int alpha) {
		alpha = alpha > 255 ? 255 : alpha < 0 ? 0 : alpha;
		
		Color foreground = getForeground();
		int red = foreground.getRed(), green = foreground.getGreen(), blue = foreground.getBlue();
		
		Color withAlpha = new Color(red, green, blue, alpha);
		super.setForeground(withAlpha);
	}
	
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
