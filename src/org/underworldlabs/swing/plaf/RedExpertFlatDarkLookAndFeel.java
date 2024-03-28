package org.underworldlabs.swing.plaf;

import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.*;
import java.awt.*;

public class RedExpertFlatDarkLookAndFeel extends FlatDarculaLaf {
    public UIDefaults getDefaults() {
        UIDefaults defaults = super.getDefaults();
//        defaults.entrySet();
//        for (Map.Entry key: defaults.entrySet())
//        {
//            try {
//                FileUtils.writeFile("D:\\defaults.properties",key.getKey() + "="+key.getValue(),true);
//            } catch (IOException e) {
//                throw new RuntimeException(e);
//            }
//        }
//        defaults.put("Table.showGrid", true);
//        defaults.put("Table.gridColor", new ColorUIResource(0, 0, 0));
        defaults.put("Table.intercellSpacing", new Dimension(1, 1));
        defaults.put("Table.showVerticalLines", true);
        defaults.put("Table.showHorizontalLines", true);
        return defaults;
    }
}
