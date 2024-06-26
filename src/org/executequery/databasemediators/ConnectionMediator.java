/*
 * ConnectionMediator.java
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

package org.executequery.databasemediators;

import org.executequery.ApplicationException;
import org.executequery.EventMediator;
import org.executequery.databasemediators.spi.DefaultConnectionBuilder;
import org.executequery.datasource.ConnectionManager;
import org.executequery.event.ConnectionEvent;
import org.executequery.event.DefaultConnectionEvent;
import org.underworldlabs.jdbc.DataSourceException;
import org.underworldlabs.swing.GUIUtils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * @author Takis Diakoumis
 */
public final class ConnectionMediator {

    private static final ConnectionMediator connectionMediator = new ConnectionMediator();

    private ConnectionMediator() {
    }

    public static synchronized ConnectionMediator getInstance() {

        return connectionMediator;
    }

    public void disconnect(DatabaseConnection dc) throws DataSourceException {

        ConnectionManager.closeConnection(dc);
        fireConnectionClosed(dc);
    }

    public void connect(List<DatabaseConnection> databaseConnections) {


    }

    public boolean connect(DatabaseConnection dc) throws DataSourceException {

        if (!establish(dc)) {

            return false;
        }
        try {
            Connection connection = ConnectionManager.getTemporaryConnection(dc);
            DatabaseMetaData dmd = connection.getMetaData();
            dc.setMajorServerVersion(dmd.getDatabaseMajorVersion());
            dc.setMinorServerVersion(dmd.getDatabaseMinorVersion());
            dc.setServerName(dmd.getDatabaseProductName());
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        fireConnectionOpened(dc);
        GUIUtils.scheduleGC();

        return true;
    }

    private boolean establish(DatabaseConnection dc) {

        DefaultConnectionBuilder builder = new DefaultConnectionBuilder(dc);
        builder.connect();

        if (builder.isCancelled()) {

            return false;
        }

        boolean connected = builder.isConnected();

        if (!connected) {

            DataSourceException exception = builder.getException();
            if (exception != null) {

                throw exception;

            } else {

                throw new ApplicationException("Unknown error establishing connection.");
            }

        }

        return connected;
    }

    public boolean test(DatabaseConnection dc) throws DataSourceException {
        
        if (!establish(dc)) {
            
            return false;
        }
        disconnect(dc);
        return true;
    }
    
    private void fireConnectionOpened(DatabaseConnection dc) {

        fireConnectionEvent(new DefaultConnectionEvent(dc, DefaultConnectionEvent.CONNECTED));
    }

    private void fireConnectionClosed(DatabaseConnection dc) {

        fireConnectionEvent(new DefaultConnectionEvent(dc, DefaultConnectionEvent.DISCONNECTED));
    }

    private void fireConnectionEvent(ConnectionEvent event) {

        EventMediator.fireEvent(event);
    }

}





