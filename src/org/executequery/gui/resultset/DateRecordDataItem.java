/*
 * DateRecordDataItem.java
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

package org.executequery.gui.resultset;

public class DateRecordDataItem extends SimpleRecordDataItem {

    public DateRecordDataItem(String name, int dataType, String dataTypeName) {

        super(name, dataType, dataTypeName);
    }

    @Override
    public void setValue(Object value) {

        Object dateValue = value;
        if (value instanceof String) {

            dateValue = valueAsType(value);
        }

        super.setValue(dateValue);
    }

}






