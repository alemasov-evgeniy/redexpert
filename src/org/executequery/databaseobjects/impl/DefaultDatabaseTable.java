/*
 * DefaultDatabaseTable.java
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

import org.apache.commons.lang.StringUtils;
import org.executequery.databasemediators.QueryTypes;
import org.executequery.databasemediators.spi.DefaultStatementExecutor;
import org.executequery.databaseobjects.*;
import org.executequery.gui.browser.ColumnData;
import org.executequery.gui.browser.comparer.Comparer;
import org.executequery.gui.browser.tree.TreePanel;
import org.executequery.gui.resultset.RecordDataItem;
import org.executequery.log.Log;
import org.executequery.sql.SQLFormatter;
import org.executequery.sql.SqlStatementResult;
import org.executequery.sql.TokenizingFormatter;
import org.underworldlabs.jdbc.DataSourceException;
import org.underworldlabs.util.MiscUtils;
import org.underworldlabs.util.SQLUtils;

import java.sql.*;
import java.util.*;

/**
 * @author Takis Diakoumis
 */
public class DefaultDatabaseTable extends AbstractTableObject implements DatabaseTable {

    /**
     * the table columns exported
     */
    private List<DatabaseColumn> exportedColumns;

    /**
     * the table indexed columns
     */
    private List<DefaultDatabaseIndex> indexes;

    /**
     * the user modified SQL text for changes
     */
    private String modifiedSQLText;

    private transient TableDataChangeWorker tableDataChangeExecutor;

    /**
     * Creates a new instance of DatabaseTable
     */


    private String externalFile;

    private String tablespace;

    protected List<ColumnData> listCD;
    protected List<org.executequery.gui.browser.ColumnConstraint> listCC;

    public DefaultDatabaseTable(DatabaseObject object, String metaDataKey) {

        this(object.getHost(), metaDataKey);

        setCatalogName(object.getCatalogName());
        setSchemaName(object.getSchemaName());
        setName(object.getName());
        setRemarks(object.getRemarks());
        if (object instanceof DefaultDatabaseObject) {
            DefaultDatabaseObject ddo = ((DefaultDatabaseObject) object);
            setTypeTree(ddo.getTypeTree());
            setDependObject(ddo.getDependObject());
        } else {
            typeTree = TreePanel.DEFAULT;
            setDependObject(null);
        }
    }

    /**
     * Creates a new instance of DatabaseTable
     */
    public DefaultDatabaseTable(DatabaseHost host) {
        super(host, "TABLE");
    }

    public DefaultDatabaseTable(DatabaseHost host, String metaDataKey) {
        super(host, metaDataKey);
    }

    @Override
    public boolean allowsChildren() {
        return true;
    }

    public List<String> getColumnNames() {
        List<String> names = new ArrayList<>();
        for (DatabaseColumn column : getColumns()) {
            names.add(column.getName());
        }
        return names;
    }

    public List<DatabaseColumn> getExportedKeys() throws DataSourceException {

        if (!isMarkedForReload() && exportedColumns != null) {

            return exportedColumns;
        }

        if (exportedColumns != null) {

            exportedColumns.clear();
            exportedColumns = null;
        }

        DatabaseHost host = getHost();
        if (host != null) {

            exportedColumns = host.getExportedKeys(getCatalogName(),
                    getSchemaName(),
                    getName());
        }

        return exportedColumns;
    }

    public boolean hasReferenceTo(DatabaseTable anotherTable) {


        List<ColumnConstraint> constraints = getConstraints();
        String anotherTableName = anotherTable.getName();

        for (ColumnConstraint constraint : constraints)
            if (constraint.isForeignKey())
                if (constraint.getReferencedTable().equals(anotherTableName))
                    return true;

        return false;

    }

    /**
     * Returns the column count of this table.
     *
     * @return the column count
     */
    public int getColumnCount() throws DataSourceException {

        return getColumns().size();
    }

    private List<ColumnConstraint> databaseConstraintsListWithSize(int size) {

        return Collections.synchronizedList(new ArrayList<>(size));
    }

    private List<DefaultDatabaseIndex> databaseIndexListWithSize(int size) {

        return Collections.synchronizedList(new ArrayList<>(size));
    }

    List<ColumnConstraint> constraints;

    /**
     * Returns the columns of this table.
     *
     * @return the columns
     */
    public List<ColumnConstraint> getConstraints() throws DataSourceException {

        if (constraints == null) {

            if (getColumns() != null) {

                constraints = new ArrayList<>();

                for (DatabaseColumn i : columns) {

                    DatabaseTableColumn column = (DatabaseTableColumn) i;
                    if (column.hasConstraints()) {

                        List<ColumnConstraint> columnConstraints = column.getConstraints();
                        for (ColumnConstraint constraint : columnConstraints) {

                            String name = constraint.getName();
                            if (isContainsTheSameObjectByName(name)) {
                                getConstraintByName(name).addColumnToDisplayList(constraint.getColumn());
                                if (Objects.equals(constraint.getTypeName(), "FOREIGN"))
                                    getConstraintByName(name).addReferenceColumnToDisplayList(constraint.getColumn());
                            } else
                                constraints.add(constraint);

                        }

                    }

                }
                DefaultStatementExecutor executor = new DefaultStatementExecutor(getHost().getDatabaseConnection(), true);
                SqlStatementResult result = null;
                try {
                    String query = "select A.RDB$CONSTRAINT_NAME,\n" +
                            "A.RDB$CONSTRAINT_TYPE,\n" +
                            "A.RDB$RELATION_NAME,\n" +
                            "C.RDB$TRIGGER_SOURCE\n" +
                            "from RDB$RELATION_CONSTRAINTS A, RDB$CHECK_CONSTRAINTS B, RDB$TRIGGERS C\n" +
                            "where (A.RDB$CONSTRAINT_TYPE = 'CHECK') and\n" +
                            "(A.RDB$CONSTRAINT_NAME = B.RDB$CONSTRAINT_NAME) and\n" +
                            "(B.RDB$TRIGGER_NAME = C.RDB$TRIGGER_NAME) and\n" +
                            "(C.RDB$TRIGGER_TYPE = 1)\n" +
                            "and (A.RDB$RELATION_NAME = ?)";
                    PreparedStatement st = executor.getPreparedStatement(query);
                    st.setString(1, getName());
                    result = executor.execute(QueryTypes.SELECT, st);
                    ResultSet rs = result.getResultSet();
                    List<String> names = new ArrayList<>();
                    if (rs != null) {
                        while (rs.next()) {
                            String name = rs.getString(1).trim();
                            if (!names.contains(name)) {
                                ColumnConstraint constraint = new TableColumnConstraint(rs.getString(4));
                                constraint.setName(name);
                                constraint.setTable(this);
                                constraints.add(constraint);
                                names.add(name);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.error("Error loading check-constraints:" + (result != null ? result.getErrorMessage() : e.getMessage()), e);
                } finally {
                    executor.releaseResources();
                }
                result = null;
                try {
                    String query = "SELECT C.RDB$CONSTRAINT_NAME,I.RDB$FIELD_NAME\n" +
                            "FROM RDB$RELATION_CONSTRAINTS AS C LEFT JOIN RDB$INDEX_SEGMENTS AS I\n" +
                            "ON C.RDB$INDEX_NAME=I.RDB$INDEX_NAME\n" +
                            "where C.RDB$RELATION_NAME=? AND C.RDB$CONSTRAINT_TYPE = 'UNIQUE'";
                    PreparedStatement st = executor.getPreparedStatement(query);
                    st.setString(1, getName());
                    result = executor.execute(QueryTypes.SELECT, st);
                    ResultSet rs = result.getResultSet();
                    if (rs != null) {
                        while (rs.next()) {
                            String name = rs.getString(1).trim();
                            ColumnConstraint constraint = new TableColumnConstraint(UNIQUE_KEY);
                            constraint.setName(name);
                            String columnName = rs.getString("RDB$FIELD_NAME").trim();
                            for (DatabaseColumn i : columns) {
                                if (i.getName().trim().contentEquals(columnName))
                                    constraint.setColumn((DatabaseTableColumn) i);
                            }
                            if (isContainsTheSameObjectByName(name))
                                getConstraintByName(name).addColumnToDisplayList(constraint.getColumn());
                            else
                                constraints.add(constraint);
                        }
                    }
                } catch (Exception e) {
                    Log.error("Error loading unique-constraints:" + result.getErrorMessage(), e);

                } finally {
                    executor.releaseResources();
                }

                constraints.removeAll(Collections.singleton(null));
                constraints.sort(new Comparator<ColumnConstraint>() {
                    @Override
                    public int compare(ColumnConstraint o1, ColumnConstraint o2) {
                        return o1.getName().compareTo(o2.getName());
                    }
                });
                return constraints;

            } else {

                return databaseConstraintsListWithSize(0);
            }
        } else return constraints;
    }

    private boolean isContainsTheSameObjectByName(String name) {

        if (constraints != null) {

            for (ColumnConstraint element : constraints) {

                if (Objects.equals(element.getName(), name))
                    return true;
            }

        }

        return false;
    }

    private ColumnConstraint getConstraintByName(String name) {

        ColumnConstraint constraint = null;
        if (constraints != null) {

            for (ColumnConstraint element : constraints) {

                if (Objects.equals(element.getName(), name))
                    constraint = element;
            }

        }

        return constraint;
    }

    /**
     * Returns the indexes of this table.
     *
     * @return the indexes
     */
    public List<DefaultDatabaseIndex> getIndexes() throws DataSourceException {

        if (!isMarkedForReload() && indexes != null) {

            return indexes;
        }

        ResultSet rs = null;
        try {

            DatabaseHost _host = getHost();
            rs = _host.getDatabaseMetaData().getIndexInfo(getCatalogName(), getSchemaName(), getName(), false, true);
            TableColumnIndex lastIndex = null;
            indexes = new ArrayList<>();
            List<TableColumnIndex> tindexes = new ArrayList<>();
            while (rs.next()) {
                String name = rs.getString(6);
                if (StringUtils.isBlank(name)) {

                    continue;
                }
                if (lastIndex == null || !lastIndex.getName().equals(name)) {
                    TableColumnIndex index = new TableColumnIndex(name);
                    index.setNonUnique(rs.getBoolean(4));
                    index.addIndexedColumn(rs.getString(9));
                    index.setMetaData(resultSetRowToMap(rs));
                    lastIndex = index;
                    tindexes.add(index);
                } else {
                    lastIndex.addIndexedColumn(rs.getString(9));
                }
            }
            releaseResources(rs, null);
            DefaultDatabaseMetaTag metaTag = new DefaultDatabaseMetaTag(getHost(), null, null, META_TYPES[INDEX]);
            for (TableColumnIndex index : tindexes) {
                DefaultDatabaseIndex index1 = metaTag.getIndexFromName(index.getName());
                index1.loadColumns();
                indexes.add(index1);
                if (index1.getExpression() != null) {
                    index.setIndexedColumns(null);
                    index.setExpression(index1.getExpression());
                }
                index.setConstraint_type(index1.getConstraint_type());
            }

            return indexes;

        } catch (DataSourceException e) {

            // catch and re-throw here to create
            // an empty index list, so we don't
            // keep hitting the same error
            indexes = databaseIndexListWithSize(0);
            throw e;

        } catch (SQLException e) {

            // catch and re-throw here to create
            // an empty index list, so we don't
            // keep hitting the same error
            indexes = databaseIndexListWithSize(0);
            throw new DataSourceException(e);

        } finally {

            releaseResources(rs, null);
            setMarkedForReload(false);
        }
    }

    private List<DefaultDatabaseTrigger> triggers;

    @Override
    public List<DefaultDatabaseTrigger> getTriggers() throws DataSourceException {

        if (!isMarkedForReload() && triggers != null)
            return triggers;

        triggers = new ArrayList<>();
        ResultSet rs;
        DefaultStatementExecutor querySender = new DefaultStatementExecutor(getHost().getDatabaseConnection());
        String query = "select T.RDB$TRIGGER_NAME,\n" +
                "T.RDB$RELATION_NAME\n" +
                "from RDB$TRIGGERS T\n" +
                "left join RDB$CHECK_CONSTRAINTS C ON C.RDB$TRIGGER_NAME = T.RDB$TRIGGER_NAME\n" +
                "where ((T.RDB$SYSTEM_FLAG = 0) or (T.RDB$SYSTEM_FLAG is null))\n" +
                "and (C.RDB$TRIGGER_NAME is NULL)\n" +
                "and (T.RDB$RELATION_NAME = ?)\n" +
                "order by  T.RDB$TRIGGER_SEQUENCE, T.RDB$TRIGGER_NAME";

        try {

            PreparedStatement st = querySender.getPreparedStatement(query);
            st.setString(1, getName());
            rs = querySender.getResultSet(-1, st).getResultSet();
            while (rs.next()) {
                String trigName = rs.getString(1);
                if (trigName != null) {
                    trigName = trigName.trim();
                    triggers.add((DefaultDatabaseTrigger) ((DefaultDatabaseHost) getHost()).getDatabaseObjectFromTypeAndName(NamedObject.TRIGGER, trigName));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();

        } finally {
            querySender.releaseResources();
        }

        return triggers;
    }

    /**
     * Returns this table's column meta data result set.
     *
     * @return the column meta data result set
     */
    public ResultSet getColumnMetaData() throws DataSourceException {

        return getMetaData();
    }

    /**
     * Returns the database object type.
     *
     * @return the object type
     */
    public int getType() {
        if (isSystem()) {
            return SYSTEM_TABLE;
        } else return TABLE;
    }

    /**
     * Returns the metadata key name of this object.
     *
     * @return the metadata key name.
     */
    public String getMetaDataKey() {
        return META_TYPES[getType()];
    }

  /**
   * Override to clear the columns.
   */
  public void reset() {
    super.reset();
    modifiedSQLText = null;
    clearColumns();
    clearIndexes();
    clearDataChanges();
    clearConstraints();
    clearTriggers();
  }

    public void clearDefinitionChanges() {
        modifiedSQLText = null;
        clearColumns();
        clearIndexes();
    }


  public void clearColumns() {
    if (columns != null) {
      columns.clear();
    }
    columns = null;
  }

  public void clearConstraints() {
    if (constraints != null) {
      constraints.clear();
    }
    constraints = null;
  }

  public void clearIndexes() {
    if (indexes != null) {
      indexes.clear();
    }
    indexes = null;
  }

  public void clearTriggers() {
    if (triggers != null) {
      triggers.clear();
    }
    triggers = null;
  }

  /**
   * Reverts any changes made to this table and associated elements.
   */
  public void revert() {

        List<DatabaseColumn> newColumns = new ArrayList<>();
        for (DatabaseColumn i : columns) {

            DatabaseTableColumn column = (DatabaseTableColumn) i;

            if (!column.isNewColumn()) {

                column.revert();

            } else {

                newColumns.add(column);
            }

        }

        for (DatabaseColumn column : newColumns) {

            columns.remove(column);
        }

        newColumns.clear();
        tableDataChanges().clear();
        modifiedSQLText = null;
    }

    /**
     * Applies any changes to the database.
     */
    public int applyChanges() throws DataSourceException {

        int result = applyTableDefinitionChanges();
        result += applyTableDataChanges();

        return result;
    }

    @Override
    public void cancelChanges() {

        if (tableDataChangeExecutor != null) {

            tableDataChangeExecutor.cancel();
        }
        tableDataChangeExecutor = null;
    }

    @Override
    public int applyTableDefinitionChanges() throws DataSourceException {

        Statement stmnt = null;

        try {

            String changes = getModifiedSQLText();
            if (StringUtils.isBlank(changes)) {

                // bail if we're empty here

                return 1;
            }

            int result = 0;
            String[] queries = changes.split(";");

            Connection connection = getHost().getConnection();
            stmnt = connection.createStatement();

            for (String s : queries) {

                String query = s.trim();
                if (StringUtils.isNotBlank(query)) {

                    result += stmnt.executeUpdate(query);
                }

            }

            if (!connection.getAutoCommit()) {

                connection.commit();
            }

            // set to reset for the next call
            reset();

            return result;

        } catch (SQLException e) {

            throw new DataSourceException(e);

        } finally {

            releaseResources(stmnt);
        }
    }

    @Override
    public boolean hasTableDefinitionChanges() {

        return StringUtils.isNotBlank(getModifiedSQLText());
    }

    private String adapter;

    public DefaultDatabaseTable(DatabaseObject object) {

        this(object.getHost());

        setCatalogName(object.getCatalogName());
        setSchemaName(object.getSchemaName());
        setName(object.getName());
        setRemarks(object.getRemarks());
        if (object instanceof DefaultDatabaseObject) {
            DefaultDatabaseObject ddo = ((DefaultDatabaseObject) object);
            setTypeTree(ddo.getTypeTree());
            setDependObject(ddo.getDependObject());
        } else {
            typeTree = TreePanel.DEFAULT;
            setDependObject(null);
        }
    }

    private boolean loadedInfoAboutExternalFile = false;
    private boolean loadedInfoAboutTablespace = false;

    private void loadInfoAboutExternalFile() {
        DefaultStatementExecutor querySender = new DefaultStatementExecutor(getHost().getDatabaseConnection());
        try {
            //querySender.setDatabaseConnection(getHost().getDatabaseConnection());
            String adapter = ", RDB$ADAPTER";
            if (!getHost().getDatabaseProductName().toLowerCase().contains("reddatabase"))
                adapter = "";
            PreparedStatement statement = querySender.getPreparedStatement("select rdb$external_file" + adapter + " from rdb$relations where rdb$relation_name = ?");
            statement.setString(1, getName());
            ResultSet rs = querySender.getResultSet(-1, statement).getResultSet();
            if (rs.next()) {
                setExternalFile(rs.getString(1));
                if (!adapter.isEmpty())
                    setAdapter(rs.getString(2));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            querySender.releaseResources();
            loadedInfoAboutExternalFile = true;
        }
    }

    private void loadInfoAboutTablespace() {
        DefaultStatementExecutor querySender = new DefaultStatementExecutor(getHost().getDatabaseConnection());
        try {
            if (getHost().getDatabaseProductName().toLowerCase().contains("reddatabase") && getHost().getDatabaseMajorVersion() >= 4) {
                PreparedStatement statement = querySender.getPreparedStatement("select rdb$tablespace_name from rdb$relations where rdb$relation_name = ?");
                statement.setString(1, getName());
                ResultSet rs = querySender.getResultSet(-1, statement).getResultSet();
                if (rs.next()) {
                    setTablespace(rs.getString(1));
                }

            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } finally {
            querySender.releaseResources();
            loadedInfoAboutTablespace = true;
        }
    }

    @Override
    public String getExternalFile() {
        if (!loadedInfoAboutExternalFile)
            loadInfoAboutExternalFile();
        return externalFile;
    }

    public void setExternalFile(String externalFile) {
        this.externalFile = externalFile;
    }

    @Override
    public String getAdapter() {
        if (!loadedInfoAboutExternalFile)
            loadInfoAboutExternalFile();
        return adapter;
    }

    public void setAdapter(String adapter) {
        this.adapter = adapter;
    }

    /**
     * Indicates whether this table or any of its columns
     * or constraints have pending modifications to be applied.
     *
     * @return true | false
     */
    public boolean isAltered() throws DataSourceException {

        if (hasTableDataChanges()) {

            return true;
        }

        List<DatabaseColumn> _columns = getColumns();
        if (_columns != null) {

            for (DatabaseColumn i : _columns) {

                DatabaseTableColumn column = (DatabaseTableColumn) i;

                if (column.hasChanges()) {

                    return true;
                }

            }
        }

        List<ColumnConstraint> constraints = getConstraints();
        if (constraints != null) {

            for (ColumnConstraint i : constraints) {

                if (i.isNewConstraint() || i.isAltered()) {

                    return true;
                }

            }

        }

        return false;
    }

    /**
     * Returns the ALTER TABLE statement to modify this constraint.
     */
    public String getAlteredSQLText() throws DataSourceException {

        StringBuilder sb = new StringBuilder();
        List<DatabaseColumn> _columns = getColumns();
        List<ColumnConstraint> _constraints = getConstraints();
        boolean first = true;
        sb.append("ALTER TABLE ").append(MiscUtils.getFormattedObject(getName()));
        if (_constraints != null) {
            for (ColumnConstraint constraint : _constraints) {
                if (constraint instanceof TableColumnConstraint) {
                    TableColumnConstraint dtc = (TableColumnConstraint) constraint;
                    if (dtc.isMarkedDeleted()) {
                        if (!first)
                            sb.append(",");
                        first = false;
                        sb.append("\nDROP CONSTRAINT ").append(MiscUtils.getFormattedObject(dtc.getName()));
                    }
                }
            }
        }
        if (_columns != null) {
            for (DatabaseColumn column : _columns) {
                if (column instanceof DatabaseTableColumn) {
                    DatabaseTableColumn dtc = (DatabaseTableColumn) column;
                    if (dtc.isMarkedDeleted()) {
                        if (!first)
                            sb.append(",");
                        first = false;
                        sb.append("\nDROP ").append(dtc.getNameEscaped());
                    }
                }
            }
        }
        if (first)
            return "";
        return sb.toString();
    }

    @Override
    public String getCreateSQLText() throws DataSourceException {

        updateListCD();
        updateListCC();

        return SQLUtils.generateCreateTable(
                getName(), listCD, listCC, true, false, true, true, true,
                null, getExternalFile(), getAdapter(), getSqlSecurity(), getTablespace(), getRemarks());
    }

    @Override
    public String getCompareCreateSQL() throws DataSourceException {

        updateListCD();
        updateListCC();

        if (Comparer.isComputedFieldsNeed())
            for (ColumnData cd : listCD)
                if(!MiscUtils.isNull(cd.getComputedBy()))
                    cd.setComputedBy(null);

        return SQLUtils.generateCreateTable(
                getName(), listCD, listCC, true, false, false, false,
                Comparer.isCommentsNeed(), null, getExternalFile(), getAdapter(), getSqlSecurity(), getTablespace(), getRemarks());
    }

    @Override
    public String getDropSQL() throws DataSourceException {
        return SQLUtils.generateDefaultDropRequest("TABLE", getName());
    }

    @Override
    public String getCompareAlterSQL(AbstractDatabaseObject databaseObject) {
        DefaultDatabaseTable comparingTable = (DefaultDatabaseTable) databaseObject;
        return SQLUtils.generateAlterTable(this, comparingTable, false,
                new boolean[]{false, false, false, false}, Comparer.isComputedFieldsNeed());
    }

    public String getDropSQLText(boolean cascadeConstraints) {

    /*StatementGenerator statementGenerator = null;
    String databaseProductName = databaseProductName();

    String dropStatement = null;
    if (cascadeConstraints) {

      dropStatement = statementGenerator.dropTableCascade(databaseProductName, this);

    } else {

      dropStatement = statementGenerator.dropTable(databaseProductName, this);
    }

    return dropStatement;*/
        return null;

    }

    public boolean hasForeignKey() {

        List<ColumnConstraint> keys = getForeignKeys();
        return keys != null && !keys.isEmpty();
    }

    public boolean hasPrimaryKey() {

        List<ColumnConstraint> keys = getPrimaryKeys();
        return keys != null && !keys.isEmpty();
    }

    public List<ColumnConstraint> getPrimaryKeys() {

        List<ColumnConstraint> primaryKeys = new ArrayList<>();
        List<ColumnConstraint> _constraints = getConstraints();
        for (ColumnConstraint columnConstraint : _constraints) {

            if (columnConstraint.isPrimaryKey()) {

                primaryKeys.add(columnConstraint);
            }

        }

        return primaryKeys;
    }

    @Override
    public List<ColumnConstraint> getForeignKeys() {

        List<ColumnConstraint> foreignKeys = new ArrayList<>();
        List<ColumnConstraint> _constraints = getConstraints();
        for (ColumnConstraint columnConstraint : _constraints) {

            if (columnConstraint.isForeignKey()) {

                foreignKeys.add(columnConstraint);
            }

        }

        return foreignKeys;
    }

    public List<ColumnConstraint> getUniqueKeys() {

        List<ColumnConstraint> uniqueKeys = new ArrayList<>();
        List<ColumnConstraint> _constraints = getConstraints();

        for (ColumnConstraint columnConstraint : _constraints) {

            if (columnConstraint.isUniqueKey()) {

                uniqueKeys.add(columnConstraint);
            }

        }

        return uniqueKeys;
    }

    @Override
    public String getAlterSQLTextForUniqueKeys() {

    /*StatementGenerator statementGenerator = null;

    return statementGenerator.createUniqueKeyChange(databaseProductName(), this);*/
        return null;
    }

    @Override
    public String getAlterSQLTextForForeignKeys() {

    /*StatementGenerator statementGenerator = null;
    return statementGenerator.createForeignKeyChange(databaseProductName(), this);*/
        return null;
    }

    @Override
    public String getAlterSQLTextForPrimaryKeys() {

    /*StatementGenerator statementGenerator = null;

    return statementGenerator.createPrimaryKeyChange(databaseProductName(), this);
     */
        return null;
    }

    public String getCreateConstraintsSQLText() throws DataSourceException {

    /*StatementGenerator statementGenerator = null;

    String databaseProductName = databaseProductName();

    return statementGenerator.tableConstraintsAsAlter(databaseProductName, this);*/
        return null;
    }

    /**
     * Returns the CREATE TABLE statement for this database table.
     * This will be table column (plus data type) definitions only,
     * this does not include constraint metadata.
     */
    public String getCreateSQLText(int style) throws DataSourceException {

        updateListCD();
        updateListCC();

        return SQLUtils.generateCreateTable(
                getName(), listCD, listCC, true, false, true, true, true, null,
                getExternalFile(), getAdapter(), getSqlSecurity(), getTablespace(), getRemarks());
    }

    protected void updateListCD() {
        listCD = new ArrayList<>();
        for (int i = 0; i < getColumnCount(); i++)
            listCD.add(new ColumnData(getHost().getDatabaseConnection(), getColumns().get(i)));
    }

    protected void updateListCC() {
        listCC = new ArrayList<>();
        for (int i = 0; i < getConstraints().size(); i++)
            listCC.add(new org.executequery.gui.browser.ColumnConstraint(false, getConstraints().get(i)));
    }

    /**
     * Returns the CREATE TABLE statement for this database table.
     * This will be table column (plus data type) definitions only,
     * this does not include constraint metadata.
     */
    public String getCreateSQLTextX(int style) throws DataSourceException {

        StringBuilder sb = new StringBuilder();

        sb.append("CREATE TABLE ");
        sb.append(getName());
        sb.append(" (");

        // determine the spaces from the left side to each column name
        String firstIndent = getSpacesForLength(sb.length());

        // determine the spaces from the column name to the data type
        int maxLength = 0;
        for (DatabaseColumn i : columns) {
            DatabaseTableColumn column = (DatabaseTableColumn) i;
            maxLength = Math.max(maxLength, column.getName().length());
        }
        // add another 5 spaces from the max
        maxLength += 5;

        int secondIndentLength;
        for (int i = 0, n = columns.size(); i < n; i++) {
            DatabaseTableColumn column = (DatabaseTableColumn) columns.get(i);

            if (i > 0) {
                sb.append(firstIndent);
            }

            String columnName = column.getName();
            sb.append(columnName.toUpperCase());

            secondIndentLength = maxLength - columnName.length();
            for (int j = 0; j < secondIndentLength; j++) {
                sb.append(" ");
            }

            sb.append(column.getFormattedDataType());

            if (StringUtils.isNotBlank(column.getDefaultValue())) {
                sb.append(" DEFAULT ");
                sb.append(column.getDefaultValue());
            }

            if (column.isRequired()) {
                sb.append(" NOT NULL");
            }

            if (i < (n - 1)) {
                sb.append(",\n");
            }

        }

        if (style == STYLE_CONSTRAINTS_DEFAULT) {
            sb.append(",\n");
            List<ColumnConstraint> constraints = getConstraints();
            for (int i = 0, n = constraints.size(); i < n; i++) {
                TableColumnConstraint constraint =
                        (TableColumnConstraint) constraints.get(i);
                sb.append(firstIndent);
                sb.append(constraint.getConstraintSQLText());

                if (i < (n - 1)) {
                    sb.append(",\n");
                }

            }
            sb.append(");\n");
        } else if (style == STYLE_CONSTRAINTS_ALTER) {

            sb.append(");\n\n");
            List<ColumnConstraint> constraints = getConstraints();

            for (ColumnConstraint i : constraints) {

                TableColumnConstraint constraint = (TableColumnConstraint) i;

                sb.append(constraint.getCreateSQLText());
                sb.append("\n");
            }

        } else {

            // finish off the statement as is
            sb.append(");\n");
        }

        return sb.toString();
    }

    /**
     * Returns the user modified SQL text to apply
     * any pending changes. If this has not been set (no
     * changes were made) then a call to getAlteredSQLText()
     * is made.
     *
     * @return the modified SQL
     */
    public String getModifiedSQLText() throws DataSourceException {
        if (modifiedSQLText == null) {
            return getAlteredSQLText();
        }
        return modifiedSQLText;
    }

    @Override
    public void setModifiedSQLText(String modifiedSQLText) {
        this.modifiedSQLText = modifiedSQLText;
    }

    @Override
    public String getInsertSQLText() {

        String fields = "";
        String values = "";

        try {

            List<DatabaseColumn> columns = getColumns();

            for (int i = 0, n = columns.size(); i < n; i++) {

                DatabaseTableColumn column = (DatabaseTableColumn) columns.get(i);

                fields += column.getNameForQuery();
                values += ":" + toCamelCase(column.getName());

                if (i < n - 1) {

                    fields += ", ";
                    values += ", ";
                }

            }

        } catch (DataSourceException e) {

            fields = "_fields_";
            values = "_values_";
            e.printStackTrace();
        }

        return getFormatter().format(SQLUtils.generateDefaultInsertStatement(getName(), fields, values));

    }

    @Override
    public String getUpdateSQLText() {

        String settings = "";

        try {

            List<DatabaseColumn> columns = getColumns();

            for (int i = 0, n = columns.size(); i < n; i++) {

                DatabaseTableColumn column = (DatabaseTableColumn) columns.get(i);

                settings += column.getNameForQuery() + " = :" +
                        toCamelCase(column.getName());
                if (i < n - 1)
                    settings += ", ";

            }

        } catch (DataSourceException e) {

            settings = "_oldValue_ = _newValue_";
            e.printStackTrace();
        }

        return getFormatter().format(SQLUtils.generateDefaultUpdateStatement(getName(), settings));

    }

    @Override
    public String getSelectSQLText() {

        String fields = "";

        try {

            List<DatabaseColumn> columns = getColumns();

            for (int i = 0, n = columns.size(); i < n; i++) {

                DatabaseTableColumn column = (DatabaseTableColumn) columns.get(i);

                fields += column.getNameForQuery();
                if (i < n - 1)
                    fields += ", ";

            }

        } catch (DataSourceException e) {

            fields = "*";
            e.printStackTrace();
        }

        return getFormatter().format(SQLUtils.generateDefaultSelectStatement(getName(), fields));

    }

    TokenizingFormatter formatter;

    protected TokenizingFormatter getFormatter() {
        if (formatter == null)
            formatter = new TokenizingFormatter();
        return formatter;
    }

    private String getSpacesForLength(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    @Override
    public DatabaseSource getDatabaseSource() {

        if (getParent() != null) {

            return (DatabaseSource) getParent().getParent();
        }

        return null;
    }

    @Override
    public String getParentNameForStatement() {

        if (getParent() != null && getParent().getParent() != null) {

            return getParent().getParent().getName();
        }

        return null;
    }

    @Override
    public boolean hasSQLDefinition() {

        return true;
    }

    @Override
    public String prepareStatement(List<String> columns, List<RecordDataItem> changes) {

        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(getNameWithPrefixForQuery()).append(" SET ");
        for (String column : columns) {
            sb.append(MiscUtils.getFormattedObject(column)).append(" = ?,");
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append(" WHERE ");

        boolean applied = false;
        List<DatabaseColumn> cols = getColumns();
        for (int i = 0; i < cols.size(); i++) {
            DatabaseColumn column = cols.get(i);
            String col = MiscUtils.getFormattedObject(cols.get(i).getName());
            RecordDataItem rdi = changes.get(i);
            if (column.isGenerated())
                rdi.setGenerated(true);
            else {
                if (applied) {
                    sb.append(" AND ");
                }
                if (rdi.isValueNull())
                    sb.append(col).append(" is NULL ");
                else
                    sb.append(col).append(" = ? ");
                applied = true;
            }
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append("\nORDER BY ").append(cols.get(0)).append(" \n");
        sb.append("ROWS 1");
        return sb.toString();
    }

    public String prepareStatementDeleting(List<RecordDataItem> changes) {

        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(getNameWithPrefixForQuery());
        sb.append(" WHERE ");

        boolean applied = false;
        List<DatabaseColumn> cols = getColumns();
        for (int i = 0; i < cols.size(); i++) {
            DatabaseColumn column = cols.get(i);
            String col = MiscUtils.getFormattedObject(cols.get(i).getName());
            RecordDataItem rdi = changes.get(i);
            if (column.isGenerated())
                rdi.setGenerated(true);
            else {
                if (applied) {

                    sb.append(" AND ");
                }
                if (rdi.isValueNull())
                    sb.append(col).append(" is NULL ");
                else
                    sb.append(col).append(" = ? ");
                applied = true;
            }
        }

        sb.deleteCharAt(sb.length() - 1);
        sb.append("\nORDER BY ").append(cols.get(0)).append(" \n");
        sb.append("ROWS 1");
        return sb.toString();
    }

    @Override
    public String prepareStatementAdding(List<String> columns, List<RecordDataItem> changes) {

        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(getNameWithPrefixForQuery());
        String columnsForQuery = " (";
        String values = " VALUES (";
        boolean applied = false;
        List<DatabaseColumn> cols = getColumns();
        for (int i = 0; i < cols.size(); i++) {
            DatabaseColumn column = cols.get(i);
            String col = MiscUtils.getFormattedObject(cols.get(i).getName());
            RecordDataItem rdi = changes.get(i);
            if (column.isGenerated() || column.isIdentity() && rdi.isNewValueNull() || column.getDefaultValue() != null && rdi.isNewValueNull())
                rdi.setGenerated(true);
            else {
                if (applied) {

                    columnsForQuery += " , ";
                    values += " , ";
                }
                columnsForQuery += col;
                values += "?";
                applied = true;
            }
        }
        columnsForQuery += ") ";
        values += ") ";
        sb.append(columnsForQuery).append(values);
        return sb.toString();
    }

    @Override
    public String prepareStatementWithPK(List<String> columns) {

        StringBuilder sb = new StringBuilder();
        sb.append("UPDATE ").append(getNameWithPrefixForQuery()).append(" SET ");
        for (String column : columns) {
            sb.append(MiscUtils.getFormattedObject(column)).append(" = ?,");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(" WHERE ");
        boolean applied = false;
        for (String primaryKey : getPrimaryKeyColumnNames()) {
            if (applied) {
                sb.append(" AND ");
            }
            sb.append(MiscUtils.getFormattedObject(primaryKey)).append(" = ? ");
            applied = true;
        }
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    @Override
    public String prepareStatementDeletingWithPK() {

        StringBuilder sb = new StringBuilder();
        sb.append("DELETE FROM ").append(getNameWithPrefixForQuery());
        sb.append(" WHERE ");

        boolean applied = false;
        for (String primaryKey : getPrimaryKeyColumnNames()) {

            if (applied) {

                sb.append(" AND ");
            }
            sb.append(MiscUtils.getFormattedObject(primaryKey)).append(" = ? ");
            applied = true;
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    @Override
    public List<String> getPrimaryKeyColumnNames() {
        return namesFromConstraints(getPrimaryKeys());
    }

    @Override
    public List<String> getForeignKeyColumnNames() {
        return namesFromConstraints(getForeignKeys());
    }

    private List<String> namesFromConstraints(List<ColumnConstraint> constraints) {

        List<String> names = new ArrayList<>();
        for (ColumnConstraint constraint : constraints)
            names.add(constraint.getColumnName());

        return names;
    }

    @Override
    protected String queryForInfo() {

        String query = "select r.rdb$description\n" +
                "from rdb$relations r\n" +
                "where r.rdb$relation_name = '" + getName() + "'";

        return query;
    }

    @Override
    protected void setInfoFromResultSet(ResultSet rs) {

        try {
            if (rs.next())
                setRemarks(rs.getString(1));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public String getTablespace() {
        if (!loadedInfoAboutTablespace)
            loadInfoAboutTablespace();
        return tablespace;
    }

    public void setTablespace(String tablespace) {
        this.tablespace = tablespace;
    }

    static final long serialVersionUID = -963831243178078154L;

}

