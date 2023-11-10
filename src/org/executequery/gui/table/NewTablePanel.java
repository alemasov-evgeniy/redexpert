/*
 * NewTablePanel.java
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

package org.executequery.gui.table;

import org.executequery.gui.browser.ColumnData;
import org.underworldlabs.util.MiscUtils;

import java.util.List;

/**
 * @author Takis Diakoumis
 */
public class NewTablePanel extends TableDefinitionPanel
        implements CreateTableSQLSyntax {

    /**
     * The table creator object - parent to this
     */
    private final TableModifier creator;

    /**
     * The buffer for the current line
     */
    private final StringBuffer line;

    /**
     * The buffer off all SQL generated
     */
    private final StringBuffer sqlText;

    private final StringBuffer primaryText;

    public List<String> descriptions;

    boolean primary;

    public NewTablePanel(TableModifier creator) {
        super();
        this.creator = creator;

        line = new StringBuffer(50);
        sqlText = new StringBuffer(100);
        primaryText = new StringBuffer(50);
    }

    /**
     * Returns the SQL scriptlet text.
     *
     * @return the SQL text
     */
    public String getSQLText() {
        return sqlText.toString();
    }

    /**
     * Resets the SQL text.
     */
    public void resetSQLText() {
        addColumnLines(-1);
    }

    /**
     * Indicates that the table value for the specified row and
     * column has changed to the value specified.
     *
     * @param col   - the last updated col
     * @param row   - the last updated row
     * @param value - the new value
     */
    @Override
    public void tableChanged(int col, int row, String value) {

        //Log.debug("tableChanged [row: "+row+" col: "+col+" value: "+value + "]");

        if (value == null) {
            updateScript(row, col);
            return;
        }

        //if (row == -1 || (col == 1 && value == null)) {
        if (row == -1) {// || (col == 1 && value == null)) {
            return;
        }

        ColumnData cd = tableVector.get(row);
        switch (col) {
            case NAME_COLUMN:
                cd.setColumnName(value);
                break;

            case TYPE_COLUMN:
                cd.setColumnType(value);
                break;

            case DOMAIN_COLUMN:
                cd.setDomain(value);
                break;

            case COLLATE_COLUMN:
                cd.setCollate(value);
                break;

            case SIZE_COLUMN:
                if (!MiscUtils.isNull(value)) {
                    int _value = Integer.parseInt(value);
                    cd.setColumnSize(_value);
                }
                break;

            case SCALE_COLUMN:
                if (!MiscUtils.isNull(value)) {
                    int _value = Integer.parseInt(value);
                    cd.setColumnScale(_value);
                }
                break;

            case SUBTYPE_COLUMN:
                if (!MiscUtils.isNull(value)) {
                    int _value = Integer.parseInt(value);
                    cd.setColumnSubtype(_value);
                }
                break;

        }

        updateScript(row, col);
    }

    /**
     * Updates the generated scriptlet using the specified
     * row and col as the last upfdaed/modified value.
     *
     * @param row - the last updated row
     * @param col - the last updated col
     */
    private void updateScript(int row, int col) {
        line.setLength(0);
        ColumnData cd = tableVector.get(row);
        if (creator.getSelectedConnection().isNamesToUpperCase() && !MiscUtils.isNull(cd.getColumnName()))
            cd.setColumnName(cd.getColumnName().toUpperCase());
        line.setLength(0);
        line.append(NEW_LINE_2).
                append(cd.getColumnName() == null ? CreateTableSQLSyntax.EMPTY : cd.getFormattedColumnName()).
                append(SPACE);
        if (MiscUtils.isNull(cd.getComputedBy())) {
            if (MiscUtils.isNull(cd.getDomain())) {
                if (cd.getColumnType() != null) {
                    line.append(cd.getFormattedDataType());
                }
            } else {
                line.append(cd.getFormattedDomain());
            }
            if (cd.isAutoincrement() && cd.getAutoincrement().isIdentity()) {
                line.append("\nGENERATED BY DEFAULT AS IDENTITY (START WITH ")
                        .append(cd.getAutoincrement().getStartValue()).append(")");
            } else {
                if (!MiscUtils.isNull(cd.getDefaultValue().getValue())) {
                    line.append(MiscUtils.formattedDefaultValue(cd.getDefaultValue(), cd.getSQLType(), cd.getDatabaseConnection()));
                }
                line.append(cd.isRequired() ? NOT_NULL : CreateTableSQLSyntax.EMPTY);
                if (!MiscUtils.isNull(cd.getCheck())) {
                    line.append(" CHECK ( ").append(cd.getCheck()).append(")");
                }
            }
        } else {
            line.append("COMPUTED BY ( ").append(cd.getComputedBy()).append(")");
        }
        if (row < tableVector.size() - 1) {
            line.append(COMMA);
        }

        if (cd.isNewColumn()) {
            cd.setNewColumn(false);
        }

        addColumnLines(row);
    }

    /**
     * Adds all the column definition lines to
     * the SQL text buffer for display.
     *
     * @param row current row being edited
     */
    public void addColumnLines(int row) {
        creator.setSQLText();

    }

    public String getPrimaryText() {
        if (primary)
            return primaryText.toString();
        else return "";
    }
}












