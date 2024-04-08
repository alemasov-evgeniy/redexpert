/*
 * LookAndFeelType.java
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

package org.executequery.plaf;


public enum LookAndFeelType {

    EXECUTE_QUERY("FlatIntelliJLaf"),
    EXECUTE_QUERY_DARK("FlatDarculaLaf"),
    OLD_THEME("Red Expert Old Theme"),
    PLUGIN("Plugin");

    private String description;

    private LookAndFeelType(String description) {

        this.description = description;
    }

    public String getDescription() {

        return description;
    }

    @Override
    public String toString() {

        return getDescription();
    }

    public boolean isDarkTheme() {

        return (this == LookAndFeelType.EXECUTE_QUERY_DARK);
    }

    public boolean isExecuteQueryLookCompatible() {

        return (this == EXECUTE_QUERY ||
                this == EXECUTE_QUERY_DARK ||
                this == OLD_THEME);
    }

}


