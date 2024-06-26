/*
 * JMenuItemFactory.java
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

package org.executequery.gui.menu;

import org.apache.commons.lang.StringUtils;
import org.executequery.localization.Bundles;
import org.executequery.log.Log;
import org.underworldlabs.swing.actions.ActionBuilder;
import org.underworldlabs.swing.menu.MenuItemFactory;
import org.underworldlabs.swing.plaf.UIUtils;
import org.underworldlabs.util.MiscUtils;

import javax.swing.*;

public class JMenuItemFactory {

    public JMenuItem createJMenuItem(JMenuItem parent, MenuItem menuItem) {

        JMenuItem jMenuItem = createMenuItemForImpl(menuItem.getImplementingClass());

        try {

            if (jMenuItem instanceof JMenu) {

                jMenuItem.setText(nameOrBundleValue(menuItem));
                jMenuItem.setMnemonic(menuItem.getMnemonicChar());

                if (parent != null) {

                    addMenuItemToParent(parent, menuItem, jMenuItem);
                }

            } else {

                if (menuItem.hasId()) {
                    Action action = actionForMenuItem(menuItem);
                    jMenuItem.setAction(action);
                    //Log.info(jMenuItem.getAction());
                    if (action.getValue(Action.SMALL_ICON) != null){
                        jMenuItem.setIcon((Icon) action.getValue(Action.SMALL_ICON));
                    }
                }

                if (menuItem.hasName()) {

                    jMenuItem.setText(nameOrBundleValue(menuItem));
                }

                if (menuItem.hasActionCommand()) {

                    jMenuItem.setActionCommand(menuItem.getActionCommand());
                }

                if (menuItem.isAcceleratorKeyNull() && menuItem.getId() == null) {

                    jMenuItem.setAccelerator(null);

                } else {

                    jMenuItem.setAccelerator(keyStrokeForMenuItem(menuItem));
                }

                if (menuItem.hasMnemonic()) {

                    jMenuItem.setMnemonic(menuItem.getMnemonicChar());
                }

                if (menuItem.hasToolTip()) {

                    jMenuItem.setToolTipText(toolTipOrBundleValue(menuItem));
                }

                addMenuItemToParent(parent, menuItem, jMenuItem);

            }

        } catch (Exception e) {
            Log.error("Error occurred by object with id '" + menuItem.getId() + "'", e);
        }

        //jMenuItem.setIcon(null);

        return jMenuItem;
    }

    private String nameOrBundleValue(MenuItem menuItem) {

        if (menuItem.hasPropertyKey()) {

            String key = menuItem.getPropertyKey();
            String value = Bundles.get(key);
            if (StringUtils.isNotBlank(value)) {

                return value;
            }

        }

        return menuItem.getName();
    }

    private String toolTipOrBundleValue(MenuItem menuItem) {

        if (menuItem.hasPropertyKey()) {

            String key = menuItem.getPropertyKey();
            String value = Bundles.get(key + ".description");
            if (StringUtils.isNotBlank(value)) {

                return value;
            }

        }

        return menuItem.getToolTip();
    }

    private void addMenuItemToParent(JMenuItem parent, MenuItem menuItem, JMenuItem jMenuItem) {

        if (menuItem.hasIndex()) {

            parent.add(jMenuItem, menuItem.getIndex());

        } else {

            parent.add(jMenuItem);
        }
    }

    private Action actionForMenuItem(MenuItem menuItem) {

        return ActionBuilder.get(menuItem.getId());
    }


    private KeyStroke keyStrokeForMenuItem(MenuItem menuItem) {

        String accelKey = menuItem.getAcceleratorKey();
        if (accelKey == null && menuItem.getId() != null) {

            return (KeyStroke) ActionBuilder.get(
                    menuItem.getId()).getValue(Action.ACCELERATOR_KEY);
        }

        if (UIUtils.isMac() && accelKey.contains("control")) {

            accelKey = accelKey.replaceAll("control", "meta");
        }

        return KeyStroke.getKeyStroke(accelKey);
    }


    private JMenuItem createMenuItemForImpl(String implClass) {

        if (MiscUtils.isNull(implClass)) {

            return MenuItemFactory.createMenuItem();
        }

        try {

            Class<?> _class = Class.forName(implClass, true, ClassLoader.getSystemClassLoader());
            Object object = _class.newInstance();

            return (JMenuItem) object;

        } catch (ClassNotFoundException e) {

            handleMenuCreationError(e);

        } catch (InstantiationException e) {

            handleMenuCreationError(e);

        } catch (IllegalAccessException e) {

            handleMenuCreationError(e);
        }

        return null;
    }

    private void handleMenuCreationError(Throwable e) {

        Log.warning("Error creating menu item: " + e.getMessage());
    }

}






