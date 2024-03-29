package org.underworldlabs.swing.plaf;

import com.formdev.flatlaf.FlatIntelliJLaf;

import javax.swing.*;
import java.awt.*;

public class RedExpertFlatlookAndFeel extends FlatIntelliJLaf {
    public UIDefaults getDefaults() {
        UIDefaults defaults = super.getDefaults();

        defaults.put("Table.intercellSpacing", new Dimension(1, 1));
        defaults.put("Table.showVerticalLines", true);
        defaults.put("Table.showHorizontalLines", true);
        return defaults;
    }
}
