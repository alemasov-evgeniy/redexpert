package org.executequery.actions.toolscommands;

import org.executequery.GUIUtilities;
import org.executequery.actions.OpenFrameCommand;
import org.underworldlabs.swing.actions.BaseCommand;
import org.underworldlabs.util.SystemProperties;

import javax.swing.*;
import java.awt.event.ActionEvent;

public class ExitFromAccountCommand extends OpenFrameCommand implements BaseCommand {
    @Override
    public void execute(ActionEvent e) {
        if (GUIUtilities.displayConfirmDialog("Do yo want exit from this account?") == JOptionPane.YES_OPTION) {
            SystemProperties.setStringProperty("user", "reddatabase.token", "");
            GUIUtilities.loadAuthorisationInfo();
        }
    }
}