/*
 * DefaultDatabaseView.java
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

package org.executequery.databaseobjects.impl;

import org.executequery.databaseobjects.DatabaseColumn;
import org.executequery.databaseobjects.DatabaseHost;
import org.executequery.databaseobjects.DatabaseObject;
import org.executequery.databaseobjects.DatabaseView;
import org.underworldlabs.jdbc.DataSourceException;
import org.underworldlabs.util.SQLUtils;

import java.util.List;

public class DefaultDatabaseView extends AbstractTableObject implements DatabaseView {

    public DefaultDatabaseView(DatabaseObject object) {

        this(object.getHost());

        setCatalogName(object.getCatalogName());
        setSchemaName(object.getSchemaName());
        setName(object.getName());
        setRemarks(object.getRemarks());
        setSource(object.getSource());
    }

    public DefaultDatabaseView(DatabaseHost host) {

        super(host, "VIEW");
    }

    public String getCreateFullSQLText() throws DataSourceException {

        String sql = getSource();

        StringBuilder sb = new StringBuilder();

        List<DatabaseColumn> columns = this.getColumns();

        sb.append("CREATE OR ALTER VIEW ");
        sb.append("\"");
        sb.append(getName());
        sb.append("\"");
        sb.append("(\n");

        for (int i = 0; i < columns.size(); i++) {
            sb.append("\t");
            sb.append("\"");
            sb.append(columns.get(i).getName());
            sb.append("\"");
            if (i != columns.size() - 1)
                sb.append(",\n");
        }
        sb.append(")\n");
        sb.append("AS\n");
        sb.append(sql);

        sb.append("\n");
        return sb.toString();
    }

    @Override
    public String getDropSQL() throws DataSourceException {
        return SQLUtils.generateDefaultDropRequest("VIEW", getName());
    }

    @Override
    public String getAlterSQL(AbstractDatabaseObject databaseObject) throws DataSourceException {
        return null;
    }

    @Override
    public boolean hasSQLDefinition() {

        return true;
    }

    public int getType() {
        if (isSystem()) {
            return SYSTEM_VIEW;
        } else {
            return VIEW;
        }
    }

    public String getMetaDataKey() {
        return META_TYPES[getType()];
    }

    public boolean allowsChildren() {
        return true;
    }

}




