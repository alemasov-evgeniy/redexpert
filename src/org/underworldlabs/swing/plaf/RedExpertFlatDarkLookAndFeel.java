package org.underworldlabs.swing.plaf;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.awt.*;

public class RedExpertFlatDarkLookAndFeel extends FlatDarculaLaf {
    public UIDefaults getDefaults() {
        UIDefaults defaults = super.getDefaults();

        defaults.put("Table.intercellSpacing", new Dimension(1, 1));
        defaults.put("Table.showVerticalLines", true);
        defaults.put("Table.showHorizontalLines", true);
        return defaults;
    }
}
