package reportNameCheck;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.SwingUtilities;

/**
 * A field to show on {@link Main#nameDialog}
 * 
 * Consists of a button that copies its text to a specified index of {@link Main#activeNamePortions}
 * 
 * @author Jaden
 * @version 0.0.1
 * @since 0.0.1
 *
 */
@SuppressWarnings("serial")
public class FieldPortion extends JButton {
	
	/**
	 * Constructs a FieldPortion with the specified text that will copy to the specified index of {@link Main#activeNamePortions}
	 * @param thisString the button text
	 * @param activeNum the index
	 */
	FieldPortion(String thisString, int activeNum) {
		super(thisString);
		
		this.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(() -> {
					Main.activeNamePortions[activeNum] = thisString;
					Main.rewriteTextNameDialog();
					Main.rewritePreviewNameDialog();
				});
			}
		});
	}
}