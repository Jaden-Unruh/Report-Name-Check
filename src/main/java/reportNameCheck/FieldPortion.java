package reportNameCheck;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;

@SuppressWarnings("serial")
public class FieldPortion extends JButton {
	
	FieldPortion(String thisString, int activeNum) {
		super(thisString);
		
		this.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Main.activeNamePortions[activeNum] = thisString;
				Main.rewritePreviewNameDialog();
			}
		});
	}
}
