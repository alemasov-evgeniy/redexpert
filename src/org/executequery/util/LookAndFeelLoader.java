/*
 * LookAndFeelLoader.java
 *
 * Copyright (C) 2002-2017 Takis Diakoumis
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.executequery.util;

import org.apache.commons.lang.math.NumberUtils;
import org.executequery.ApplicationException;
import org.executequery.ApplicationLauncher;
import org.executequery.plaf.ExecuteQueryTheme;
import org.executequery.plaf.LookAndFeelType;
import org.underworldlabs.swing.plaf.RedExpertFlatDarkLookAndFeel;
import org.underworldlabs.swing.plaf.RedExpertFlatlookAndFeel;
import org.underworldlabs.swing.plaf.UIUtils;
import org.underworldlabs.swing.plaf.base.CustomTextAreaUI;
import org.underworldlabs.swing.plaf.base.CustomTextPaneUI;

import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.DefaultEditorKit;
import java.awt.event.KeyEvent;

public final class LookAndFeelLoader {

    public LookAndFeelType loadLookAndFeel(String lookAndFeelType) {

        if (NumberUtils.isDigits(lookAndFeelType)) {

            ApplicationLauncher launcher = new ApplicationLauncher();
            launcher.startup();

            // legacy numeric setting - default to EQ L&F

            return loadLookAndFeel(LookAndFeelType.EXECUTE_QUERY);
        }
        return loadLookAndFeel(LookAndFeelType.valueOf(lookAndFeelType));
    }

    public LookAndFeelType loadLookAndFeel(LookAndFeelType lookAndFeelType) {

        switch (lookAndFeelType) {
            case EXECUTE_QUERY:
                loadDefaultLookAndFeel();
                break;
            case EXECUTE_QUERY_DARK:
                loadDarkEQLookAndFeel();
                break;
            case PLUGIN:
                loadCustomLookAndFeel();
                break;
            case OLD_THEME:
                  loadOldTheme();
                  break;
//                case SMOOTH_GRADIENT:
//                    UIManager.setLookAndFeel(new SmoothGradientLookAndFeel());
//                    break;
//                case BUMPY_GRADIENT:
//                    BumpyGradientLookAndFeel.setCurrentTheme(new ExecuteQueryTheme());
//                    UIManager.setLookAndFeel(new BumpyGradientLookAndFeel());
//                    break;
//                case EXECUTE_QUERY_THEME:
//                    loadDefaultLookAndFeelTheme();
//                    break;
//                case METAL:
//                    loadDefaultMetalLookAndFeelTheme();
//                    break;
//                case OCEAN:
//                    UIManager.setLookAndFeel(
//                            "javax.swing.plaf.metal.MetalLookAndFeel");
//                    break;
//                case MOTIF:
//                    UIManager.setLookAndFeel(
//                            "com.sun.java.swing.plaf.motif.MotifLookAndFeel");
//                    break;
//                case WINDOWS:
//                    UIManager.setLookAndFeel(
//                            "com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
//                    break;
//                case GTK:
//                    UIManager.setLookAndFeel(
//                            "com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
//                    break;
//                case PLUGIN:
//                    loadCustomLookAndFeel();
//                    break;
//                case NATIVE:
//                    loadNativeLookAndFeel();
//                    break;
//                case EXECUTE_QUERY_GRADIENT:
//                    loadDefault3DLookAndFeel();
//                    break;
//                default:
//                    loadDefaultLookAndFeel();
//                    break;
        }

        if (!UIUtils.isNativeMacLookAndFeel()) {

            CustomTextAreaUI.initialize();
            CustomTextPaneUI.initialize();
        }

        applyMacSettings();
        return lookAndFeelType;
    }

    private void applyMacSettings() {

        if (UIUtils.isMac()) {

            String[] textComponents = {"TextField", "TextPane", "TextArea", "EditorPane", "PasswordField"};
            for (String textComponent : textComponents) {

                InputMap im = (InputMap) UIManager.get(textComponent + ".focusInputMap");
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
                im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
            }

            if (UIUtils.isNativeMacLookAndFeel()) {

                UIManager.put("Table.gridColor", UIUtils.getDefaultBorderColour());
            }

        }

    }

    private void loadCustomLookAndFeel() {

        PluginLookAndFeelManager pluginManager = new PluginLookAndFeelManager();
        try {

            pluginManager.loadLookAndFeel();

        } catch (Exception e) {

            throw new ApplicationException(e);
        }

        if (!pluginManager.isInstalled()) {

            loadDefaultLookAndFeel();
        }

    }

    /**
     * Sets the default metal look and feel theme on Metal.
     */
    private void loadDefaultMetalLookAndFeelTheme() {

        try {

            MetalLookAndFeel.setCurrentTheme(new javax.swing.plaf.metal.DefaultMetalTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());

        } catch (UnsupportedLookAndFeelException e) {

            throw new ApplicationException(e);
        }

    }

    /**
     * Sets the default look and feel theme on Metal.
     */
    private void loadDefaultLookAndFeelTheme() {

        try {

            MetalLookAndFeel.setCurrentTheme(new ExecuteQueryTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());

        } catch (UnsupportedLookAndFeelException e) {

            throw new ApplicationException(e);
        }

    }

    /**
     * Sets the default 'Execute Query' look and feel.
     */
    private void loadDefaultLookAndFeel() {

        try {

            LookAndFeel laf = new RedExpertFlatlookAndFeel(); //FlatIntelliJLaf();
            UIManager.setLookAndFeel(laf);

/*
            List<String> values = new ArrayList<String>();
            UIDefaults defaults = (UIDefaults) laf.getDefaults();
            Enumeration<Object> i = defaults.keys();
            while (i.hasMoreElements()) { 
                
                Object key = i.nextElement();
                Object value = defaults.get(key);
                
                values.add(key + " :: " + value);
                
                /*
                if (value instanceof ColorUIResource) {

                    ColorUIResource color = (ColorUIResource) value;                    
                    values.add("\"" + key + "\", new ColorUIResource(" + 
                            color.getRed() + "," +
                            color.getGreen() + "," +
                            color.getBlue() +
                            "),");
                }
                * /
            }

            Collections.sort(values);
            for (String value : values) {
                System.out.println(value);
            }
            */

        } catch (UnsupportedLookAndFeelException e) {

            throw new ApplicationException(e);
        }

    }

    private void loadDarkEQLookAndFeel() {

        try {

            LookAndFeel laf = new RedExpertFlatDarkLookAndFeel();//FlatDarculaLaf();
            UIManager.setLookAndFeel(laf);

        } catch (UnsupportedLookAndFeelException e) {

            throw new ApplicationException(e);
        }

    }

    private void loadOldTheme() {

        try {

//            LookAndFeel laf = new FlatArcDarkIJTheme();
//            UIManager.setLookAndFeel(laf);

//            LookAndFeel laf = new FlatIntelliJLaf();
////            LookAndFeel laf = new FlatArcIJTheme();
//            UIManager.setLookAndFeel(laf);

            LookAndFeel laf = new org.underworldlabs.swing.plaf.UnderworldLabsFlatLookAndFeel();
            UIManager.setLookAndFeel(laf);

/*
            List<String> values = new ArrayList<String>();
            UIDefaults defaults = (UIDefaults) laf.getDefaults();
            Enumeration<Object> i = defaults.keys();
            while (i.hasMoreElements()) {

                Object key = i.nextElement();
                Object value = defaults.get(key);

                values.add(key + " :: " + value);

                /*
                if (value instanceof ColorUIResource) {

                    ColorUIResource color = (ColorUIResource) value;
                    values.add("\"" + key + "\", new ColorUIResource(" +
                            color.getRed() + "," +
                            color.getGreen() + "," +
                            color.getBlue() +
                            "),");
                }
                * /
            }

            Collections.sort(values);
            for (String value : values) {
                System.out.println(value);
            }
            */

        } catch (UnsupportedLookAndFeelException e) {

            throw new ApplicationException(e);
        }

    }
    /**
     * Sets the default OLD 'Execute Query' look and feel.
     */
    public void loadDefault3DLookAndFeel() {

        try {
            org.underworldlabs.swing.plaf.UnderworldLabsLookAndFeel metal =
                    new org.underworldlabs.swing.plaf.UnderworldLabsLookAndFeel();

            UIManager.setLookAndFeel(metal);

        } catch (UnsupportedLookAndFeelException e) {

            throw new ApplicationException(e);
        }

    }

    public void loadNativeLookAndFeel() {
        try {

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        } catch (Exception e) {

            throw new ApplicationException(e);
        }
    }

    public void loadCrossPlatformLookAndFeel() {
        try {

            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());

        } catch (Exception e) {

            throw new ApplicationException(e);
        }
    }

    public void decorateDialogsAndFrames(boolean decorateDialogs, boolean decorateFrames) {

        JDialog.setDefaultLookAndFeelDecorated(decorateDialogs);
        JFrame.setDefaultLookAndFeelDecorated(decorateFrames);
    }

}


