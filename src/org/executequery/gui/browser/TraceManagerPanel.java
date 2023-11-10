package org.executequery.gui.browser;

import biz.redsoft.IFBTraceManager;
import org.executequery.EventMediator;
import org.executequery.GUIUtilities;
import org.executequery.base.TabView;
import org.executequery.components.FileChooserDialog;
import org.executequery.databasemediators.DatabaseConnection;
import org.executequery.datasource.DefaultDriverLoader;
import org.executequery.event.ConnectionRepositoryEvent;
import org.executequery.event.DefaultConnectionRepositoryEvent;
import org.executequery.gui.browser.managment.tracemanager.*;
import org.executequery.gui.browser.managment.tracemanager.net.LogMessage;
import org.executequery.gui.browser.managment.tracemanager.net.SessionInfo;
import org.executequery.localization.Bundles;
import org.executequery.repository.DatabaseConnectionRepository;
import org.executequery.repository.DatabaseDriverRepository;
import org.executequery.repository.RepositoryCache;
import org.executequery.util.UserProperties;
import org.underworldlabs.swing.DynamicComboBoxModel;
import org.underworldlabs.swing.ListSelectionPanel;
import org.underworldlabs.swing.NumberTextField;
import org.underworldlabs.swing.layouts.GridBagHelper;
import org.underworldlabs.swing.util.SwingWorker;
import org.underworldlabs.util.DynamicLibraryLoader;
import org.underworldlabs.util.FileUtils;
import org.underworldlabs.util.MiscUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;

public class TraceManagerPanel extends JPanel implements TabView {

    public static final String TITLE = Bundles.get(TraceManagerPanel.class, "title");
    private IFBTraceManager traceManager;
    private TablePanel loggerPanel;

    private AnalisePanel analisePanel;
    private FileOutputStream fileLog;
    private PipedOutputStream outputStream;

    private PipedInputStream inputStream;

    private BufferedReader bufferedReader;
    private JButton fileLogButton;
    private JButton fileDatabaseButton;
    private JButton fileConfButton;
    private JButton startStopSessionButton;
    private JButton clearTableButton;
    private JButton openFileLog;
    private JTextField fileLogField;
    private JTextField fileDatabaseField;
    private JTextField fileConfField;
    private JTextField openFileLogField;
    private JTextField userField;
    private JPasswordField passwordField;
    private JCheckBox logToFileBox;
    LogMessage constMsg = new LogMessage();
    private JCheckBox useBuildConfBox;
    private JTextField hostField;
    private NumberTextField portField;
    private JTextField sessionField;
    private JComboBox<DatabaseConnection> databaseBox;
    private int idLogMessage = 0;
    private List<String> charsets;
    private JComboBox charsetCombo;
    private JTabbedPane tabPane;
    private JButton hideShowTabPaneButton;
    private Message message;
    private List<SessionInfo> sessions;
    private SessionManagerPanel sessionManagerPanel;
    private BuildConfigurationPanel confPanel;
    private JPanel connectionPanel;
    private int currentSessionId;

    public static String bundleString(String key) {
        return Bundles.get(TraceManagerPanel.class, key);
    }
    private JCheckBox parseBox;

    public TraceManagerPanel() {
        init();
    }

    private void initTraceManager(DatabaseConnection dc) {
        try {

            Driver driver = null;
            Map<String, Driver> drivers = DefaultDriverLoader.getLoadedDrivers();
            if (dc != null) {
                for (String driverName : drivers.keySet()) {
                    if (driverName.startsWith(String.valueOf(dc.getDriverId()))) {
                        driver = drivers.get(driverName);
                        break;
                    }
                }
            }
            if (driver == null)
                driver = DefaultDriverLoader.getDefaultDriver();

            traceManager = (IFBTraceManager) DynamicLibraryLoader.loadingObjectFromClassLoader(
                    driver.getMajorVersion(), driver, "FBTraceManagerImpl");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readFromBufferedReader(BufferedReader reader, boolean fromFile) {
        String s = "";
        boolean finded = false;
        String line;
        while (true) {
            try {
                if ((line = reader.readLine()) == null)
                    break;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String str = line.replace("\\r", "");
            if (str.toLowerCase().startsWith("trace session id")) {
                if (finded) {
                    parseMessage(s, message, fromFile);
                }
                message = Message.LOG_MESSAGE;
                s = str;
                String temp = str.replace("Trace session ID ", "");
                if (temp.contains("started")) {
                    temp = temp.replace("started", "");
                    temp = temp.replace(" ", "");
                    currentSessionId = Integer.parseInt(temp);
                } else if (temp.contains("stopped")) {
                    temp = temp.replace("stopped", "");
                    temp = temp.replace(" ", "");
                    int sessionId = Integer.parseInt(temp);
                    if (sessionId == currentSessionId && !fromFile)
                        stopSession();
                }
                finded = true;
            } else if (str.matches(".?\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d+.*")) {
                if (finded) {
                    parseMessage(s, message, fromFile);
                }
                message = Message.LOG_MESSAGE;
                finded = true;
                s = str + "\n";
            } else if (str.matches("^Session ID:.*")) {
                if (finded) {
                    parseMessage(s, message, fromFile);
                }
                message = Message.SESSION_INFO;
                finded = true;
                s = str + "\n";
            } else {
                s += str + "\n";
            }
        }
        parseMessage(s, message, fromFile);
    }

    @Override
    public boolean tabViewClosing() {
        if (startStopSessionButton.getText().contentEquals(bundleString("Stop")))
            try {
                traceManager.stopTraceSession(traceManager.getSessionID(sessionField.getText()));
            } catch (SQLException e) {
                GUIUtilities.displayExceptionErrorDialog("Error stop session", e);
            } finally {
                stopSession();
            }
        inputStream = null;
        outputStream = null;
        fileLog = null;
        confPanel = null;
        openFileLog = null;
        startStopSessionButton = null;
        loggerPanel.cleanup();
        loggerPanel = null;
        tabPane.removeAll();
        tabPane = null;
        bufferedReader = null;
        return true;
    }

    @Override
    public boolean tabViewSelected() {
        return true;
    }

    @Override
    public boolean tabViewDeselected() {
        return true;
    }

    private void init() {
        message = Message.LOG_MESSAGE;
        sessions = new ArrayList<>();
        initTraceManager(null);
        sessionField = new JTextField();
        sessionField.setText("Session");
        sessionManagerPanel = new SessionManagerPanel(traceManager, sessionField);
        loadCharsets();
        ListSelectionPanel columnsCheckPanel = new ListSelectionPanel(new Vector<>(Arrays.asList(LogConstants.COLUMNS)));
        columnsCheckPanel.selectAllAction();
        loggerPanel = new TablePanel(columnsCheckPanel);
        analisePanel = new AnalisePanel(loggerPanel.getTableRows());
        fileLogButton = new JButton("...");
        fileDatabaseButton = new JButton("...");
        fileConfButton = new JButton("...");
        openFileLog = new JButton("...");
        fileLogField = new JTextField();
        fileDatabaseField = new JTextField();
        fileConfField = new JTextField();
        openFileLogField = new JTextField();
        userField = new JTextField();
        passwordField = new JPasswordField();
        parseBox = new JCheckBox(bundleString("parseTraceToGrid"));
        parseBox.setSelected(true);
        logToFileBox = new JCheckBox(bundleString("LogToFile"));
        logToFileBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                setEnableElements();
                if (fileLog != null) {
                    try {
                        fileLog.close();
                        fileLog = null;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });
        hostField = new JTextField("127.0.0.1");
        portField = new NumberTextField();
        portField.setText("3050");
        useBuildConfBox = new JCheckBox(bundleString("UseConfigFile"));
        useBuildConfBox.setSelected(true);
        useBuildConfBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setEnableElements();
            }
        });
        charsetCombo = new JComboBox<>(charsets.toArray());
        DynamicComboBoxModel model = new DynamicComboBoxModel();
        List<DatabaseConnection> databaseConnectionList = new ArrayList<>();
        databaseConnectionList.add(null);
        databaseConnectionList.addAll(((DatabaseConnectionRepository) RepositoryCache.load(DatabaseConnectionRepository.REPOSITORY_ID)).findAll());
        model.setElements(databaseConnectionList);
        databaseBox = new JComboBox<>(model);
        databaseBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (databaseBox.getSelectedItem() != null) {
                    DatabaseConnection dc = (DatabaseConnection) databaseBox.getSelectedItem();
                    fileDatabaseField.setText(dc.getSourceName());
                    userField.setText(dc.getUserName());
                    passwordField.setText(dc.getUnencryptedPassword());
                    hostField.setText(dc.getHost());
                    portField.setText(dc.getPort());
                    sessionField.setText(dc.getName() + "_trace_session");
                    charsetCombo.setSelectedItem(dc.getCharset());
                    if (dc.getPathToTraceConfig() != null)
                        fileConfField.setText(dc.getPathToTraceConfig());
                    if (dc.getMajorServerVersion() >= 3) {
                        confPanel.getAppropriationBox().setSelectedIndex(1);
                    } else {
                        confPanel.getAppropriationBox().setSelectedIndex(0);
                    }
                    initTraceManager(dc);
                    sessionManagerPanel.setFbTraceManager(traceManager);
                }
            }
        });
        startStopSessionButton = new JButton(bundleString("Start"));
        clearTableButton = new JButton(bundleString("ClearTable"));
        clearTableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });
        fileLogButton.addActionListener(new ActionListener() {
            final FileChooserDialog fileChooser = new FileChooserDialog();

            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser.showSaveDialog(fileLogButton);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    if (fileLog != null) {
                        try {
                            fileLog.close();
                            fileLog = null;
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    File file = fileChooser.getSelectedFile();
                    fileLogField.setText(file.getAbsolutePath());
                    try {
                        fileLog = new FileOutputStream(file, false);
                        fileLog.close();
                        fileLog = null;
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        });


        fileDatabaseButton.addActionListener(new ActionListener() {
            final FileChooserDialog fileChooser = new FileChooserDialog();

            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser.showOpenDialog(fileDatabaseButton);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    fileDatabaseField.setText(file.getAbsolutePath());
                }
            }
        });

        fileConfButton.addActionListener(new ActionListener() {
            final FileChooserDialog fileChooser = new FileChooserDialog();

            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser.showOpenDialog(fileConfButton);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    fileConfField.setText(file.getAbsolutePath());
                }
            }
        });

        openFileLog.addActionListener(new ActionListener() {
            final FileChooserDialog fileChooser = new FileChooserDialog();

            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser.showOpenDialog(openFileLog);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    SwingWorker sw = new SwingWorker("loadTraceFromFile") {
                        @Override
                        public Object construct() {
                            openFileLogField.setText(fileChooser.getSelectedFile().getAbsolutePath());
                            clearAll();
                            idLogMessage = 0;
                            BufferedReader reader = null;
                            try {
                                reader = new BufferedReader(
                                        new InputStreamReader(
                                                Files.newInputStream(Paths.get(openFileLogField.getText())), UserProperties.getInstance().getStringProperty("system.file.encoding")));
                                readFromBufferedReader(reader, true);
                            } catch (Exception e1) {
                                GUIUtilities.displayExceptionErrorDialog("file opening error", e1);
                            } finally {
                                if (reader != null) {
                                    try {
                                        reader.close();
                                    } catch (IOException e1) {
                                    }
                                }
                            }
                            return null;
                        }

                        @Override
                        public void finished() {
                            GUIUtilities.showNormalCursor();
                            tabPane.setEnabled(true);
                            loggerPanel.setEnableElements(true);
                            SwingWorker sw = new SwingWorker("buildAnalise") {
                                @Override
                                public Object construct() {
                                    analisePanel.setMessages(loggerPanel.getTableRows());
                                    analisePanel.rebuildRows();
                                    return null;
                                }
                            };
                            sw.start();

                        }
                    };
                    GUIUtilities.showWaitCursor();
                    tabPane.setEnabled(false);
                    loggerPanel.setEnableElements(false);
                    sw.start();

                }
                tabPane.setSelectedComponent(loggerPanel);
            }
        });

        startStopSessionButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (startStopSessionButton.getText().toUpperCase().contentEquals(bundleString("Start").toUpperCase())) {
                    if (logToFileBox.isSelected()) {
                        if (logToFileBox.isSelected()) {
                            if (!fileLogField.getText().isEmpty()) {
                                File file = new File(fileLogField.getText());
                                try {
                                    fileLog = new FileOutputStream(file, true);
                                } catch (FileNotFoundException e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                        if (fileLog != null) {
                            outputStream = new TraceOutputStream();
                        } else {
                            GUIUtilities.displayErrorMessage("File is empty");
                            return;
                        }
                    } else
                        outputStream = new PipedOutputStream();
                    try {
                        inputStream = new PipedInputStream(outputStream);
                        String charset = null;
                        if (charsetCombo.getSelectedIndex() > 0)
                            charset = MiscUtils.getJavaCharsetFromSqlCharset((String) charsetCombo.getSelectedItem());
                        bufferedReader = new BufferedReader(new InputStreamReader(inputStream, charset));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    traceManager.setUser(userField.getText());
                    traceManager.setPassword(new String(passwordField.getPassword()));
                    traceManager.setLogger(outputStream);
                    if (charsetCombo.getSelectedIndex() == 0)
                        traceManager.setCharSet(null);
                    else
                        traceManager.setCharSet(MiscUtils.getJavaCharsetFromSqlCharset((String) charsetCombo.getSelectedItem()));
                    traceManager.setDatabase(fileDatabaseField.getText());
                    traceManager.setHost(hostField.getText());
                    traceManager.setPort(portField.getValue());
                    try {
                        String conf;
                        if (useBuildConfBox.isSelected()) {
                            if (fileConfField.getText().isEmpty()) {
                                GUIUtilities.displayErrorMessage("Please select configuration file");
                                return;
                            }
                            conf = FileUtils.loadFile(fileConfField.getText());
                        } else conf = confPanel.getConfig();
                        traceManager.startTraceSession(sessionField.getText(), conf);
                        startStopSessionButton.setText(bundleString("Stop"));
                        tabPane.add(bundleString("SessionManager"), sessionManagerPanel);
                        for (int i = 0; i < connectionPanel.getComponents().length; i++) {
                            connectionPanel.getComponents()[i].setEnabled(false);
                        }
                        logToFileBox.setEnabled(false);
                        SwingWorker sw = new SwingWorker("TraceSession") {
                            @Override
                            public Object construct() {
                                try {
                                    readFromBufferedReader(bufferedReader, false);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                while (startStopSessionButton.getText().contentEquals(bundleString("Stop"))) {
                                    try {
                                        Thread.sleep(1000);
                                        traceManager.startTraceSession(sessionField.getText(), conf);
                                        readFromBufferedReader(bufferedReader, false);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                                return null;
                            }
                        };
                        sw.start();
                        tabPane.setSelectedComponent(loggerPanel);
                        DatabaseConnection dc = (DatabaseConnection) databaseBox.getSelectedItem();
                        if (dc != null) {
                            dc.setPathToTraceConfig(fileConfField.getText());
                        }
                        EventMediator.fireEvent(new DefaultConnectionRepositoryEvent(this,
                                ConnectionRepositoryEvent.CONNECTION_MODIFIED, (DatabaseConnection) databaseBox.getSelectedItem()
                        ));
                    } catch (Exception e1) {
                        GUIUtilities.displayExceptionErrorDialog("Error start Trace Manager", e1);
                    }
                } else try {
                    traceManager.stopTraceSession(currentSessionId);
                } catch (SQLException e1) {
                    GUIUtilities.displayExceptionErrorDialog("Error stop Trace Manager", e1);
                } finally {
                    stopSession();
                }
            }
        });

        hideShowTabPaneButton = new JButton(bundleString("HideTopPanel"));
        hideShowTabPaneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tabPane.setVisible(!tabPane.isVisible());
                if (hideShowTabPaneButton.getText().contentEquals(bundleString("HideTopPanel")))
                    hideShowTabPaneButton.setText(bundleString("ShowTopPanel"));
                else hideShowTabPaneButton.setText(bundleString("HideTopPanel"));
            }
        });

        tabPane = new JTabbedPane();
        connectionPanel = new JPanel();
        confPanel = new BuildConfigurationPanel();

        setLayout(new GridBagLayout());
        // JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new GridBagLayout());
        JLabel label = new JLabel(bundleString("OpenFileLog"));
        GridBagHelper gbh = new GridBagHelper();
        gbh.setDefaultsStatic().defaults();
        topPanel.add(label, gbh.setLabelDefault().get());
        gbh.nextCol();
        gbh.addLabelFieldPair(topPanel, openFileLog, openFileLogField, null, false, false);

        topPanel.add(tabPane, gbh.nextRowFirstCol().fillBoth().spanX().setMaxWeightY().get());

        gbh.fullDefaults();

        add(topPanel, gbh.fillBoth().spanX().setMaxWeightY().topGap(5).get());


        add(startStopSessionButton, gbh.nextRowFirstCol().setLabelDefault().get());

        add(clearTableButton, gbh.nextCol().setLabelDefault().get());


        tabPane.add(bundleString("Connection"), connectionPanel);
        tabPane.add(bundleString("BuildConfigurationFile"), new JScrollPane(confPanel));
        tabPane.add(bundleString("VisibleColumns"), columnsCheckPanel);
        tabPane.add(bundleString("Logger"), loggerPanel);
        tabPane.add(bundleString("Analise"), analisePanel);
        connectionPanel.setLayout(new GridBagLayout());
        gbh.fullDefaults();
        gbh.addLabelFieldPair(connectionPanel, bundleString("Connections"), databaseBox, null, true, true);
        gbh.nextRowFirstCol();
        label = new JLabel(bundleString("Database"));
        connectionPanel.add(label, gbh.setLabelDefault().get());
        gbh.addLabelFieldPair(connectionPanel, fileDatabaseButton, fileDatabaseField, null, false, true);
        gbh.addLabelFieldPair(connectionPanel, bundleString("SessionName"), sessionField, null, true, false, 2);
        gbh.addLabelFieldPair(connectionPanel, bundleString("Host"), hostField, null, false, false);
        gbh.addLabelFieldPair(connectionPanel, bundleString("Port"), portField, null, false, true);
        gbh.addLabelFieldPair(connectionPanel, bundleString("Username"), userField, null, true, false, 2);
        gbh.addLabelFieldPair(connectionPanel, bundleString("Password"), passwordField, null, false, true);
        gbh.addLabelFieldPair(connectionPanel, bundleString("Charset"), charsetCombo, null, true, true);
        gbh.nextRowFirstCol();
        connectionPanel.add(useBuildConfBox, gbh.setLabelDefault().get());
        gbh.addLabelFieldPair(connectionPanel, fileConfButton, fileConfField, null, false, true);
        gbh.nextRowFirstCol();
        connectionPanel.add(logToFileBox, gbh.setLabelDefault().get());
        gbh.addLabelFieldPair(connectionPanel, fileLogButton, fileLogField, null, false, true);
        connectionPanel.add(parseBox, gbh.nextRowFirstCol().setLabelDefault().get());
        connectionPanel.add(new JPanel(), gbh.anchorSouth().nextRowFirstCol().fillBoth().spanX().spanY().get());
        setEnableElements();

    }

    private void parseMessage(String msg, Message message, boolean fromFile) {
        if (message == Message.LOG_MESSAGE && (parseBox.isSelected() || fromFile)) {
            LogMessage logMessage = new LogMessage(msg);
            idLogMessage++;
            logMessage.setId(idLogMessage);
            loggerPanel.addRow(logMessage);
            if (!fromFile)
                analisePanel.addMessage(logMessage, !fromFile);
        } else {
            if (fromFile)
                return;
            if (sessionManagerPanel.isRefreshFlag()) {
                sessions.clear();
                sessionManagerPanel.setRefreshFlag(false);
            }
            SessionInfo sessionInfo = new SessionInfo(msg);
            sessions.add(sessionInfo);
            sessionManagerPanel.setSessions(sessions);
        }
        if (!parseBox.isSelected() && !fromFile) {
            if (loggerPanel.countRows() < 1)
                loggerPanel.addRow(constMsg);
            constMsg.setTimestamp(Timestamp.valueOf(LocalDateTime.now()));
            constMsg.setTypeEvent("WRITE_TO_FILE");
            loggerPanel.repaint();
        }
    }

    public void clearAll() {
        loggerPanel.clearAll();
        analisePanel.setMessages(loggerPanel.getTableRows());
        analisePanel.rebuildRows();
        idLogMessage = 0;
    }

    private void loadCharsets() {
        try {
            if (charsets == null)
                charsets = new ArrayList<String>();
            else
                charsets.clear();

            String resource = FileUtils.loadResource("org/executequery/charsets.properties");
            String[] strings = resource.split("\n");
            for (String s : strings) {
                if (!s.startsWith("#") && !s.isEmpty())
                    charsets.add(s);
            }
            java.util.Collections.sort(charsets);
            charsets.add(0, "NONE");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private DatabaseDriverRepository driverRepository() {
        return (DatabaseDriverRepository) RepositoryCache.load(
                DatabaseDriverRepository.REPOSITORY_ID);
    }

    private void setEnableElements() {
        fileConfButton.setEnabled(useBuildConfBox.isSelected());
        fileConfField.setEnabled(useBuildConfBox.isSelected());
        fileLogButton.setEnabled(logToFileBox.isSelected());
        fileLogField.setEnabled(logToFileBox.isSelected());
    }

    private void stopSession() {
        startStopSessionButton.setText(bundleString("Start"));
        tabPane.remove(sessionManagerPanel);
        for (int i = 0; i < connectionPanel.getComponents().length; i++) {
            connectionPanel.getComponents()[i].setEnabled(true);
        }
        setEnableElements();
        logToFileBox.setEnabled(true);
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
        outputStream = null;
    }

    enum Message {
        LOG_MESSAGE,
        SESSION_INFO
    }

    class TraceOutputStream extends PipedOutputStream {
        @Override
        public void write(int b) throws IOException {
            fileLog.write(b);
            super.write(b);
        }

        public void close() throws IOException {
            fileLog.close();
            super.close();
        }
    }
}
