package org.executequery.gui.databaseobjects;

import org.executequery.databasemediators.DatabaseConnection;
import org.executequery.databaseobjects.NamedObject;
import org.executequery.databaseobjects.impl.DefaultDatabasePackage;
import org.executequery.gui.ActionContainer;
import org.executequery.gui.text.SimpleSqlTextPanel;
import org.underworldlabs.util.SQLUtils;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class CreatePackagePanel extends AbstractCreateObjectPanel {

    public static final String CREATE_TITLE = getCreateTitle(NamedObject.PACKAGE);
    public static final String ALTER_TITLE = getEditTitle(NamedObject.PACKAGE);
    private SimpleSqlTextPanel headerPanel;
    private SimpleSqlTextPanel bodyPanel;
    private DefaultDatabasePackage databasePackage;

    public CreatePackagePanel(DatabaseConnection dc, ActionContainer dialog) {
        this(dc, dialog, null);
    }

    public CreatePackagePanel(DatabaseConnection dc, ActionContainer dialog, DefaultDatabasePackage databaseObject) {
        super(dc, dialog, databaseObject);
    }

    @Override
    protected void initEdited() {
        reset();
        if (parent == null)
            addPrivilegesTab(tabbedPane, databasePackage);
        addDependenciesTab(databasePackage);
        addCreateSqlTab(databasePackage);
    }

    protected String generateQuery() {
        String sb = headerPanel.getSQLText() + "^\n" +
                bodyPanel.getSQLText() + "^\n" +
                SQLUtils.generateComment(getFormattedName(), "PACKAGE",
                        simpleCommentPanel.getComment(), "^", true, getDatabaseConnection());
        return sb;
    }

    @Override
    protected void reset() {
        nameField.setText(databasePackage.getName().trim());
        simpleCommentPanel.setDatabaseObject(databasePackage);
        headerPanel.setSQLText(databasePackage.getHeaderSource());
        bodyPanel.setSQLText(databasePackage.getBodySource());
    }

    @Override
    public void createObject() {
        displayExecuteQueryDialog(generateQuery(), "^");
    }

    @Override
    public String getCreateTitle() {
        return CREATE_TITLE;
    }

    @Override
    public String getEditTitle() {
        return ALTER_TITLE;
    }

    @Override
    public String getTypeObject() {
        return NamedObject.META_TYPES[NamedObject.PACKAGE];
    }

    @Override
    public void setDatabaseObject(Object databaseObject) {
        databasePackage = (DefaultDatabasePackage) databaseObject;
    }

    @Override
    public void setParameters(Object[] params) {}

    @Override
    protected void init() {
        headerPanel = new SimpleSqlTextPanel();
        bodyPanel = new SimpleSqlTextPanel();
        nameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                changeName();
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                changeName();
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                changeName();
            }
        });

        tabbedPane.add(bundleString("Header"), headerPanel);
        tabbedPane.add(bundleString("Body"), bodyPanel);
        addCommentTab(null);

        String sqlTemplate = getFormattedName() + "\nAS" + "\nBEGIN" + "\n\nEND";
        headerPanel.setSQLText("CREATE OR ALTER PACKAGE " + sqlTemplate);
        bodyPanel.setSQLText("RECREATE PACKAGE BODY " + sqlTemplate);
    }

    private void changeName() {

        String sqlText = headerPanel.getSQLText().trim()
                .replaceAll("PACKAGE\\s+((\".*\")|(\\w*\\$?\\w*\\b)|)",
                        "PACKAGE " + getFormattedName().replace("$", "\\$"))
                .replace("PACKAGE " + getFormattedName().replace("$", "\\$"), "PACKAGE " + getFormattedName());
        headerPanel.setSQLText(sqlText);

        sqlText = bodyPanel.getSQLText().trim()
                .replaceAll("PACKAGE BODY\\s+((\".*\")|(\\w*\\$?\\w*\\b)|)",
                        "PACKAGE BODY " + getFormattedName().replace("$", "\\$"))
                .replace("PACKAGE BODY " + getFormattedName().replace("$", "\\$"), "PACKAGE BODY " + getFormattedName());
        bodyPanel.setSQLText(sqlText);
    }

}
