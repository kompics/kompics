/*
 * Copyright (C) 2006 Sun Microsystems, Inc. All rights reserved. Use is
 * subject to license terms.
 */
package passwordstore.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.KeyboardFocusManager;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ProgressBarUI;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoableEdit;

import passwordstore.dndx.DefaultTransferable;
import passwordstore.model.HostEntry;
import passwordstore.model.HostModel;
import passwordstore.swingx.CutCopyPasteHelper;
import passwordstore.swingx.DynamicAction;
import passwordstore.swingx.JImagePanel;
import passwordstore.swingx.LookAndFeelMenu;
import passwordstore.swingx.MnemonicHelper;
import passwordstore.swingx.app.AboutBox;
import passwordstore.swingx.app.Application;
import passwordstore.swingx.binding.JListListControllerAdapter;
import passwordstore.swingx.binding.ListController;
import passwordstore.swingx.border.DropShadowBorder;
import passwordstore.swingx.text.RegExStyler;
import passwordstore.swingx.text.TextUndoableEditGenerator;
import passwordstore.swingx.undo.ExtendedCompoundEdit;
import passwordstore.swingx.undo.ExtendedUndoManager;
import passwordstore.swingx.undo.PropertyUndoableEdit;

/**
 * The Controller for the PasswordStore application. Controller gets its name
 * from the model view controller (MVC) pattern. Controller is responsible
 * for creating the UI, listening for changes to both the model and view
 * and keeping everything in sync.
 *
 * @version $Revision$
 */
public class Controller {
    private static final DataFlavor PASSWORD_ENTRY_DATA_FLAVOR;

    // Model for the app.
    private HostModel model;
    
    private ListController<HostEntry> listController;
    
    // JList used to show the entries.
    private JList entryList;
    
    private JListListControllerAdapter<HostEntry> entryListAdapter;
    
    private JTable entryTable;
    
    private PasswordTableListControllerAdapter entryTableAdapter;

    // JTextField for the host name
    private JTextField hostTF;
    
    // JTextfield for the user name
    private JTextField userTF;
    
    // JTextField for the password
    JTextField passwordTF;
    
    // JTextArea for notes
    private JTextPane notesTP;
    
    // PropertyChangeListener attached to each HostEntry
    private PropertyChangeListener selectedEntryChangeListener;
    
    // The selected entry
    private HostEntry selectedEntry;
    
    // Set to true while changing the text of the userTF or invoking
    // setHost. This is used to avoid updating the text field or
    // selected HostEntry when the text or seleted HostEntry
    // has changed.
    private boolean changingHost;
    
    private boolean changingUser;
    
    private boolean changingPassword;
    
    private boolean changingNotes;
    
    private boolean changingImage;

    private JMenuItem viewTableMI;
    private JMenuItem viewListMI;
    
    private JImagePanel imagePanel;

    // Set to true if the user has edited the notesTP.
    private boolean editedNotes;
    
    private JTextField filterTF;

    private PasswordVisualizer visualizer;
    
    private ExtendedUndoManager undoManager;

    private boolean changingFilterText;

    private boolean inSandbox;

    
    private JProgressBar progressBar;
    
    
    static {
        DataFlavor flavor = null;
        try {
            flavor = new DataFlavor(
                DataFlavor.javaJVMLocalObjectMimeType +
                    "; class=java.util.ArrayList; x=password");        
        } catch (ClassNotFoundException ex) {
            assert false;
        }
        PASSWORD_ENTRY_DATA_FLAVOR = flavor;
    }
    

    Controller(JFrame host) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                sm.checkSystemClipboardAccess();
            } catch (SecurityException se) {
                inSandbox = true;
            }
        }
        undoManager = new ExtendedUndoManager();
        selectedEntryChangeListener = new SelectedEntryPropertyChangeHandler();
        createModel();
        listController = new EntryListController();
        listController.setEntries(model.getHostEntries());
        listController.addPropertyChangeListener(
                new ListControllerPropertyChangeListener());
        createComponents(host);
        createUI(host);
        createMenu(host);
        changingImage = true;
        disableControls();
        changingImage = false;
        if (model.getHostEntries().size() > 0) {
            visualizer.setAnimatesTransitions(false);
            listController.setSelection(Arrays.asList(
                    model.getHostEntries().get(0)));
            visualizer.setAnimatesTransitions(true);
        }
        host.pack();
    }
    
    public void viewList() {
        if (!viewingList()) {
            boolean tableHasFocus = (KeyboardFocusManager.
                    getCurrentKeyboardFocusManager().getPermanentFocusOwner() == entryTable);
            createEntryList();
            swap(entryTable, entryList);
            entryTable = null;
            entryTableAdapter.dispose();
            entryTableAdapter = null;
            int selectedIndex = entryList.getMinSelectionIndex();
            if (selectedIndex != -1) {
                entryList.scrollRectToVisible(
                        entryList.getCellBounds(selectedIndex, selectedIndex));
            }
            updateCutCopyPasteForEntryView();
            if (tableHasFocus) {
                entryList.requestFocus();
            }
        }
    }
    
    public void viewTable() {
        if (viewingList()) {
            boolean listHasFocus = (KeyboardFocusManager.
                    getCurrentKeyboardFocusManager().getPermanentFocusOwner() == entryList);
            createEntryTable();
            swap(entryList, entryTable);
            entryList = null;
            entryListAdapter.dispose();
            entryListAdapter = null;
            int selectedIndex = entryTable.getSelectedRow();
            if (selectedIndex != -1) {
                entryTable.scrollRectToVisible(
                        entryTable.getCellRect(selectedIndex, 0, true));
            }
            updateCutCopyPasteForEntryView();
            if (listHasFocus) {
                entryTable.requestFocus();
            }
        }
    }
    
    private boolean viewingList() {
        return (entryList != null);
    }

    private void swap(JComponent existing, JComponent toAdd) {
        JScrollPane sp = (JScrollPane)existing.getParent().getParent();
        sp.setViewportView(toAdd);
    }
    
    
    // Returns true if the app can exit, false otherwise.
    boolean canExit() {
//        try {
//            PersistenceHandler handler = PersistenceHandler.getHandler();
//            model.save(handler.getOutputStream());
//        } catch (IOException ex) {
//            // There was an error saving, prompt the user
//            ResourceBundle resources = Application.getInstance().
//                    getResourceBundle();
//            int alertResult = JOptionPane.showOptionDialog(entryList,
//                    resources.getString("passwordList.errorSaving"), 
//                    resources.getString("passwordList.errorSavingTitle"), 
//                    JOptionPane.YES_NO_OPTION,
//                    JOptionPane.ERROR_MESSAGE, null, null, null);
//            // User wants to exit anyway.
//            return (alertResult == JOptionPane.YES_OPTION);
//        }
        return true;
    }
    
    // Adds a new HostEntry
    public void addAccount() {
        // Create the new entry, adding some default values.
        HostEntry entry = new HostEntry();
        ResourceBundle resources = Application.getInstance().getResourceBundle();
        entry.setHost(resources.getString("newEntry.host"));
        entry.setUser(resources.getString("newEntry.user"));
        
        // Add the entry to the end of the list.
        int index = listController.getEntries().size();
        add(Arrays.asList(entry), index);
        
        // And give the text field focus.
        hostTF.selectAll();
        hostTF.requestFocus();
    }
    
    public void showAbout() {
        AboutBox.getInstance().show(SwingUtilities.getWindowAncestor(filterTF));
    }

    // Invoked when the selection in the list has changed
    private void selectionChanged(List<HostEntry> oldSelection) {
        undoManager.addEdit(new SelectionChangeUndo(oldSelection));
        if (selectedEntry != null) {
            if (editedNotes) {
                changingNotes = true;
                selectedEntry.setNotes(notesTP.getText());
                changingNotes = false;
                editedNotes = false;
            }
            selectedEntry.removePropertyChangeListener(
                    selectedEntryChangeListener);
        }
        List<HostEntry> selection = listController.getSelection();
        
        // We're about to change the host/user/password textfields. Set a
        // boolean indicating we should ignore any change events originating
        // from the text field.
        changingImage = changingHost = changingUser = changingPassword =
                changingNotes = true;
        
        if (selection.size() != 1) {
            // Only allow editing one value.
            disableControls();
            selectedEntry = null;
        } else {
            undoManager.setIgnoreEdits(true);
            // Only one value is selected, update the fields appropriately
            selectedEntry = selection.get(0);
            hostTF.setEditable(true);
            hostTF.setText(selectedEntry.getHost());
            userTF.setEditable(true);
            userTF.setText(selectedEntry.getUser());
            passwordTF.setEditable(true);
            passwordTF.setText(selectedEntry.getPassword());
            if (notesTP != null) {
                notesTP.setEditable(true);
                notesTP.setText(selectedEntry.getNotes());
            }
            selectedEntry.addPropertyChangeListener(
                    selectedEntryChangeListener);
            imagePanel.setImage(getImage(selectedEntry));
            imagePanel.setImagePath(selectedEntry.getImagePath());
            imagePanel.setEditable(!inSandbox);
            imagePanel.setBackground(Color.WHITE);
            visualizer.setPassword(selectedEntry.getPassword());
            undoManager.setIgnoreEdits(false);
        }
        
        // textfields are now in sync with selection, any changes from the UI
        // should be applied from the model, and similarly any changes to
        // the model should be applied to the UI.
        changingImage = changingHost = changingUser = changingPassword =
                changingNotes = false;
        
        updateCutCopyPasteForEntryView();
    }
    
    private void updateCutCopyPasteForEntryView() {
        List<HostEntry> selection = listController.getSelection();
        JComponent view = (entryTable == null) ? entryList : entryTable;
        CutCopyPasteHelper.setCopyEnabled(view, selection.size() > 0);
        CutCopyPasteHelper.setCutEnabled(view, selection.size() > 0);
    }
    
    // Adds an item at the specified index.
    // Notice that we do this here as List does not provide notification.
    private void add(List<HostEntry> entries, int index) {
        // Add the entries to the model
        undoManager.addEdit(new MutateUndo(index, entries.size(), false));
        listController.getEntries().addAll(index, entries);
        listController.setSelection(entries);
        if (entryList != null) {
            entryList.scrollRectToVisible(entryList.getCellBounds(index, index));
        } else {
            entryTable.scrollRectToVisible(entryTable.getCellRect(index,
                    0, true));
        }
    }
    
    // Removes an item at the specified index.
    // Notice that we do this here as List does not provide change notification.
    private void remove(int index, int count) {
        undoManager.addEdit(new MutateUndo(index, count, true));
        // Remove the PasswordEntries from the model.
        List<HostEntry> entries = listController.getEntries();
        for (int i = 0; i < count; i++) {
            entries.remove(index);
        }
    }
    
    // Invoked when a propety on a password has changed.
    // This may have triggered in one of two ways:
    // 1. From the textfields we're displaying
    // 2. From some other portion of the app
    //
    // Case 1 can be identified by one of the changingXXX fields. If it's true,
    // we know the edit originated from us and there is no need to reset the 
    // text in the textfield.
    private void entryChanged(HostEntry passwordEntry,
            String propertyChanged, Object lastValue) {
        assert (selectedEntry == passwordEntry);
        boolean addEdit = false;
        // A value in the selected entry has changed, update the UI.
        if (propertyChanged == "host" && !changingHost) {
            changingHost = true;
            hostTF.setText(passwordEntry.getHost());
            changingHost = false;
            addEdit = true;
        } else if (propertyChanged == "user" && !changingUser) {
            changingUser = true;
            userTF.setText(passwordEntry.getUser());
            changingUser = false;
            addEdit = true;
        } else if (propertyChanged == "password" && !changingPassword) {
            changingPassword = true;
            passwordTF.setText(passwordEntry.getPassword());
            visualizer.setPassword(passwordEntry.getPassword());
            changingPassword = false;
            addEdit = true;
        } else if (propertyChanged == "notes" && !changingNotes) {
            changingNotes = true;
            if (notesTP != null) {
                notesTP.setText(passwordEntry.getNotes());
            }
            changingNotes = false;
            addEdit = true;
        } else if (propertyChanged == "imagePath" && !changingImage) {
            changingImage = true;
            imagePanel.setImagePath(passwordEntry.getImagePath());
            changingImage = false;
            addEdit = true;
        }
        if (addEdit) {
            undoManager.addEdit(new PropertyUndoableEdit(passwordEntry, propertyChanged, lastValue));
        }
    }
    
    // Invoked when the user types in the host text field
    private void hostChanged() {
        // Only propagate the change if we aren't the one responsible for
        // the change
        if (!changingHost) {
            changingHost = true;
            selectedEntry.setHost(hostTF.getText());
            selectedEntry.setLastModified(System.currentTimeMillis());
            changingHost = false;
        }
    }
    
    // Invoked when the user types in the user text field
    private void userChanged() {
        // Only propagate the change if we aren't the one responsible for
        // the change
        if (!changingUser) {
            changingUser = true;
            selectedEntry.setLastModified(System.currentTimeMillis());
            selectedEntry.setUser(userTF.getText());
            changingUser = false;
        }
    }

    // Invoked when the user types in the password text field
    private void passwordChanged() {
        // Only propagate the change if we aren't the one responsible for
        // the change
        if (!changingPassword) {
            changingPassword = true;
            selectedEntry.setLastModified(System.currentTimeMillis());
            selectedEntry.setPassword(passwordTF.getText());
            visualizer.setPassword(passwordTF.getText());
            changingPassword = false;
        }
    }
    
    private void notesChanged() {
        if (!changingNotes) {
            editedNotes = true;
        }
    }
    
    private void imageChanged() {
        if (!changingImage) {
            undoManager.addEdit(new PropertyUndoableEdit(
                    selectedEntry, "imagePath", selectedEntry.getImagePath()));
            changingImage = true;
            selectedEntry.setLastModified(System.currentTimeMillis());
            selectedEntry.setImagePath(imagePanel.getImagePath());
            changingImage = false;
        }
    }
    
    // Disables the controls. This is used if an invalid entry has been
    // selected
    private void disableControls() {
        undoManager.setIgnoreEdits(true);
        hostTF.setEditable(false);
        hostTF.setText("");
        userTF.setEditable(false);
        userTF.setText("");
        passwordTF.setEditable(false);
        passwordTF.setText("");
        if (notesTP != null) {
            notesTP.setText("");
            notesTP.setEditable(false);
        }
        imagePanel.setImage(null);
        imagePanel.setEditable(false);
        imagePanel.setBackground(passwordTF.getBackground());
        visualizer.setPassword(null);
        undoManager.setIgnoreEdits(false);
    }
    
    // Tries to load the model. If that fails a new model is returned
    private void createModel() {
        model = new HostModel();
        model.load(getClass().getResourceAsStream(
                "/passwordstore/ui/default/default.xml"));
        for (HostEntry entry : model.getHostEntries()) {
            URI imageURI = entry.getImagePath();
            if ("default".equals(imageURI.getScheme())) {
                String path = imageURI.getPath();
                try {
                    URL url = getClass().getResource(
                            "/passwordstore/ui/default" + path);
                    entry.setImagePath(url.toURI());
                } catch (URISyntaxException ex) {
                }
            }
        }
//        PersistenceHandler handler = PersistenceHandler.getHandler();
//        if (handler.exists()) {
//            try {
//                model.load(handler.getInputStream());
//            } catch (IOException ex) {
//                // For demo purposes an error loading the file is ignored.
//                model = new HostModel();
//            }
//        }
    }
    
    private void createProgressBar(JFrame frame, int lengthOfTask) {
    	
    	progressBar = new JProgressBar(0, lengthOfTask);
    	progressBar.setValue(0);
    	progressBar.setStringPainted(true);

    }
    
    
    // Creates and populates the menu for the app
    private void createMenu(JFrame frame) {
        // NOTE: notice that many of the actions in here are null, they
        // will be filled in later on.
        JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);


        ResourceBundle bundle = Application.getInstance().getResourceBundle();
        if (!Application.isOSX()) {
            JMenu fileMenu = MnemonicHelper.createMenu(
                    bundle.getString("menu.file"));
            menuBar.add(fileMenu);
            JMenuItem exitMI = MnemonicHelper.createMenuItem(fileMenu,
                    bundle.getString("menu.exit"));
            exitMI.addActionListener(new DynamicAction(
                    Application.getInstance(), "exit"));
        }
        
        JMenu editMenu = MnemonicHelper.createMenu(
                bundle.getString("menu.edit"));
        menuBar.add(editMenu);
        JMenuItem undoMI = createMenuItem(editMenu, "menu.undo",
                undoManager.getUndoAction());
        undoMI.setAccelerator(KeyStroke.getKeyStroke("ctrl Z"));
        undoMI.setEnabled(false);
        editMenu.add(undoMI);
        JMenuItem redoMI = createMenuItem(editMenu, "menu.redo",
                undoManager.getRedoAction());
        redoMI.setAccelerator(KeyStroke.getKeyStroke("ctrl Y"));
        redoMI.setEnabled(false);
        editMenu.add(redoMI);
        editMenu.addSeparator();
        JMenuItem cutMI = createMenuItem(editMenu, "menu.cut",
                CutCopyPasteHelper.getCutAction());
        cutMI.setAccelerator(KeyStroke.getKeyStroke("ctrl X"));
        editMenu.add(cutMI);
        JMenuItem copyMI = createMenuItem(editMenu, "menu.copy",
                CutCopyPasteHelper.getCopyAction());
        copyMI.setAccelerator(KeyStroke.getKeyStroke("ctrl C"));
        editMenu.add(copyMI);
        JMenuItem pasteMI = createMenuItem(editMenu, "menu.paste",
                CutCopyPasteHelper.getPasteAction());
        pasteMI.setAccelerator(KeyStroke.getKeyStroke("ctrl V"));
        editMenu.add(pasteMI);
        
        JMenu viewMenu = MnemonicHelper.createMenu(
                bundle.getString("menu.view"));
        ButtonGroup viewButtonGroup = new ButtonGroup();
        menuBar.add(viewMenu);

        viewMenu.add(new LookAndFeelMenu());
        viewMenu.addSeparator();

        viewListMI = new JRadioButtonMenuItem();
        MnemonicHelper.configureTextAndMnemonic(viewListMI,
                bundle.getString("menu.viewList"));
        viewMenu.add(viewListMI);
        viewListMI.setSelected(true);
        viewButtonGroup.add(viewListMI);
        viewListMI.addActionListener(new DynamicAction(this, "viewList"));
        
        viewTableMI = new JRadioButtonMenuItem();
        MnemonicHelper.configureTextAndMnemonic(viewTableMI,
                bundle.getString("menu.viewTable"));
        viewMenu.add(viewTableMI);
        viewButtonGroup.add(viewTableMI);
        viewTableMI.addActionListener(new DynamicAction(this, "viewTable"));
        viewMenu.add(viewTableMI);
        
        JMenu accountMenu = MnemonicHelper.createMenu(
                bundle.getString("menu.account"));
        menuBar.add(accountMenu);
        JMenuItem addMenuItem = MnemonicHelper.createMenuItem(accountMenu,
                bundle.getString("menu.newAccount"));
        addMenuItem.addActionListener(new DynamicAction(this, "addAccount"));
        addMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl N"));

        JMenu genPasswordMenu = MnemonicHelper.createMenu(
                bundle.getString("menu.generatePassword"));
        accountMenu.add(genPasswordMenu);
        MnemonicHelper.createMenuItem(genPasswordMenu,
                bundle.getString("menu.generateNumeric"),
                new PasswordGeneratingAction(listController, passwordTF, false, false, true, false));
        MnemonicHelper.createMenuItem(genPasswordMenu,
                bundle.getString("menu.generateAlphabetic"),
                new PasswordGeneratingAction(listController, passwordTF, true, true, false, false));
        MnemonicHelper.createMenuItem(genPasswordMenu,
                bundle.getString("menu.generateAlphabeticNumeric"),
                new PasswordGeneratingAction(listController, passwordTF, true, true, true, false));
        MnemonicHelper.createMenuItem(genPasswordMenu,
                bundle.getString("menu.generateAlphabeticNumbericPunctuation"),
                new PasswordGeneratingAction(listController, passwordTF, true, true, true, true));
        
        JMenu helpMenu = MnemonicHelper.createMenu(
                bundle.getString("menu.help"));
        menuBar.add(helpMenu);
        JMenuItem aboutMenuItem = createMenuItem(helpMenu, "menu.about",
                new DynamicAction(this, "showAbout"));
        
    }

    // Conveniance to create a configure a JMenuItem.
    private JMenuItem createMenuItem(JMenu menu, String key, Action action) {
        JMenuItem mi;
        if (action != null) {
            mi = new JMenuItem(action);
        } else {
            mi = new JMenuItem();
        }
        MnemonicHelper.configureTextAndMnemonic(mi, Application.getResourceAsString(key));
        menu.add(mi);
        return mi;
    }
    
    private void createUI(JFrame frame) {
        progressBar = new javax.swing.JProgressBar();
//        progressBar.setName("progressBar");
//        progressBar.setVisible(true);
//        addComponent(progressBar, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE).

        LoginDialog loginDialog = new LoginDialog(frame, "Planetlab login");
        loginDialog.pack();
        loginDialog.setVisible(true);
        
    	
    	JTabbedPane tp = new JTabbedPane();
        JScrollPane entrySP = new JScrollPane(entryList);
        entrySP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        // PENDING: localize
        tp.addTab(Application.getResourceAsString("tab.details"), createDetailsPanel());
        tp.addTab(Application.getResourceAsString("tab.notes"), null);
        tp.addTab(Application.getResourceAsString("tab.status"), createStatusPanel());
        tp.addChangeListener(new TabbedPaneChangeHandler(tp));

        JLabel filterLabel = new JLabel(Application.getResourceAsString(
                "label.filter"));
        GroupLayout frameLayout = new GroupLayout(frame.getContentPane());
        frame.setLayout(frameLayout);
        frameLayout.setAutoCreateContainerGaps(true);
        frameLayout.setAutoCreateGaps(true);
        GroupLayout.ParallelGroup hGroup = frameLayout.createParallelGroup(
                GroupLayout.Alignment.LEADING);
        hGroup.
          addGroup(GroupLayout.Alignment.TRAILING, frameLayout.createSequentialGroup().
            addComponent(filterLabel).
            addComponent(filterTF, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)).
          addComponent(entrySP, 100, 400, Integer.MAX_VALUE).
          addComponent(tp);
        frameLayout.setHorizontalGroup(hGroup);

        
        GroupLayout.SequentialGroup vGroup = frameLayout.createSequentialGroup();
        vGroup.
          addGroup(frameLayout.createParallelGroup(GroupLayout.Alignment.BASELINE).
            addComponent(filterLabel).
            addComponent(filterTF)).
          addComponent(entrySP, 100, 200, Integer.MAX_VALUE).
          addComponent(tp, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
        frameLayout.setVerticalGroup(vGroup);
        
        

        
    }
    
    private Component createNotesPanel() {
        createNotesTextPane();
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        panel.setLayout(layout);
        JScrollPane notesSP = new JScrollPane(notesTP);
        notesSP.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        GroupLayout.ParallelGroup hg = layout.createParallelGroup();
        layout.setHorizontalGroup(hg);
        hg.
          addComponent(notesSP, 1, 1, Integer.MAX_VALUE);
        
        GroupLayout.ParallelGroup vg = layout.createParallelGroup();
        layout.setVerticalGroup(vg);
        vg.
          addComponent(notesSP, 1, 1, Integer.MAX_VALUE);
        return panel;
    }
    
    private Component createDetailsPanel() {
        JPanel imageWrapper = new JPanel(new BorderLayout());
        imageWrapper.setOpaque(false);
        imageWrapper.add(imagePanel);
        imageWrapper.setBorder(new DropShadowBorder(Color.BLACK, 0, 5, .5f, 12, false, true, true, true));
        // PENDING: localize
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        JLabel hostLabel = new JLabel(Application.getResourceAsString("label.host"));
        JLabel accountLabel = new JLabel(Application.getResourceAsString("label.account"));
        JLabel passwordLabel = new JLabel(Application.getResourceAsString("label.password"));
        GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        panel.setLayout(layout);
        GroupLayout.SequentialGroup hg = layout.createSequentialGroup();
        layout.setHorizontalGroup(hg);
        hg.
          addComponent(imageWrapper, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).
          addGroup(layout.createParallelGroup().
            addComponent(hostLabel).
            addComponent(accountLabel).
            addComponent(passwordLabel)).
          addGroup(layout.createParallelGroup().
            addComponent(hostTF).
            addComponent(userTF).
            addComponent(passwordTF).
            addComponent(visualizer));
        
        GroupLayout.ParallelGroup vg = layout.createParallelGroup();
        layout.setVerticalGroup(vg);
        vg.
          addComponent(imageWrapper, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).
          addGroup(layout.createSequentialGroup().
            addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
              addComponent(hostLabel).
              addComponent(hostTF)).
            addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
              addComponent(accountLabel).
              addComponent(userTF)).
            addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
              addComponent(passwordLabel).
              addComponent(passwordTF).
              addComponent(visualizer)));
        return panel;
    }
    
    private Component createStatusPanel() {
        JPanel buttonWrapper = new JPanel(new BorderLayout());
        buttonWrapper.setOpaque(false);
        JButton connect = new JButton("Connect");
        buttonWrapper.add(connect);
//        buttonWrapper.setBorder(new DropShadowBorder(Color.BLACK, 0, 5, .5f, 12, false, true, true, true));
        // PENDING: localize
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        JLabel hostLabel = new JLabel(Application.getResourceAsString("label.host"));
        JLabel accountLabel = new JLabel(Application.getResourceAsString("label.account"));
        JLabel passwordLabel = new JLabel(Application.getResourceAsString("label.password"));
        GroupLayout layout = new GroupLayout(panel);
        layout.setAutoCreateContainerGaps(true);
        layout.setAutoCreateGaps(true);
        panel.setLayout(layout);
        GroupLayout.SequentialGroup hg = layout.createSequentialGroup();
        layout.setHorizontalGroup(hg);
        hg.
          addComponent(buttonWrapper, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).
          addGroup(layout.createParallelGroup().
            addComponent(hostLabel).
            addComponent(accountLabel).
            addComponent(passwordLabel)).
          addGroup(layout.createParallelGroup().
            addComponent(hostTF).
            addComponent(userTF).
            addComponent(passwordTF).
            addComponent(visualizer));
        
        GroupLayout.ParallelGroup vg = layout.createParallelGroup();
        layout.setVerticalGroup(vg);
        vg.
          addComponent(buttonWrapper, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE).
          addGroup(layout.createSequentialGroup().
            addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
              addComponent(hostLabel).
              addComponent(hostTF)).
            addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
              addComponent(accountLabel).
              addComponent(userTF)).
            addGroup(layout.createParallelGroup(GroupLayout.Alignment.BASELINE).
              addComponent(passwordLabel).
              addComponent(passwordTF).
              addComponent(visualizer)));
        return panel;
    }
    
    
    private void createEntryList() {
        // Model set later on.
        entryList = new JList();
        entryList.setCellRenderer(new EntryListCellRenderer());
        entryListAdapter = new JListListControllerAdapter<HostEntry>(
                listController, entryList);
        entryList.setPrototypeCellValue(new HostEntry());
        entryList.setTransferHandler(new ListTableTransferHandler());
        CutCopyPasteHelper.registerCutCopyPasteBindings(entryList);
        CutCopyPasteHelper.setPasteEnabled(entryList, true);
        CutCopyPasteHelper.registerDataFlavors(entryList,
                new DataFlavor[] { PASSWORD_ENTRY_DATA_FLAVOR } );
    }
    
    private void createEntryTable() {
        entryTable = new JTable();
        entryTable.setFillsViewportHeight(true);
        entryTable.setAutoCreateRowSorter(true);
        entryTableAdapter = new PasswordTableListControllerAdapter(
                listController, entryTable);
        entryTable.setTransferHandler(new ListTableTransferHandler());
        CutCopyPasteHelper.registerCutCopyPasteBindings(entryTable);
        CutCopyPasteHelper.setPasteEnabled(entryTable, true);
        CutCopyPasteHelper.registerDataFlavors(entryTable,
                new DataFlavor[] { PASSWORD_ENTRY_DATA_FLAVOR } );
    }
    
    private void createComponents(JFrame frame) {
        DocumentListener documentListener = new DocumentHandler();
        hostTF = createTF(documentListener);
        userTF = createTF(documentListener);
        passwordTF = createTF(documentListener);
        imagePanel = new JImagePanel();
        imagePanel.setBorder(new CompoundBorder(new LineBorder(Color.DARK_GRAY, 1),
                new EmptyBorder(2, 2, 2, 2)));
        imagePanel.setPreferredSize(new Dimension(128, 128));
        imagePanel.setBackground(Color.WHITE);
        imagePanel.setOpaque(true);
        imagePanel.addPropertyChangeListener(
                new ImagePanelPropertyChangeHandler());
        filterTF = new JTextField(15);
        filterTF.getDocument().addDocumentListener(new FilterDocumentHandler());
        visualizer = new PasswordVisualizer();
        visualizer.setOpaque(false);
        // Makes it align with image
        visualizer.setBorder(new EmptyBorder(0, 0, 5, 0));
        createEntryList();
    }
    
    private JTextField createTF(DocumentListener documentListener) {
        JTextField tf = new JTextField(10);
        TextUndoableEditGenerator gen = new TextUndoableEditGenerator(tf);
        gen.addUndoableEditListener(undoManager);
        tf.getDocument().addDocumentListener(documentListener);
        return tf;
    }
    
    private void createNotesTextPane() {
        notesTP = new JTextPane();
	SimpleAttributeSet urlAttributes = new SimpleAttributeSet();
	StyleConstants.setForeground(urlAttributes, Color.blue);
	StyleConstants.setBackground(urlAttributes, Color.white);
	StyleConstants.setUnderline(urlAttributes, true);
        RegExStyler styler = new RegExStyler(notesTP);
        styler.addStyle("(http|ftp)://[_a-zA-Z0-9./~\\-]+",
                urlAttributes, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        if (selectedEntry == null) {
            notesTP.setEditable(false);
        } else {
            notesTP.setText(selectedEntry.getNotes());
        }
        notesTP.getDocument().addDocumentListener(new DocumentHandler());
        notesTP.addMouseListener(new NotesMouseHandler(styler));
        TextUndoableEditGenerator gen = new TextUndoableEditGenerator(notesTP);
        gen.addUndoableEditListener(undoManager);
    }

    HostModel getModel() {
        return model;
    }

    private Image getImage(HostEntry selectedEntry) {
        if (selectedEntry.getImagePath() != null) {
            try {
                return new ImageIcon(selectedEntry.getImagePath().toURL()).getImage();
            } catch (MalformedURLException ex) {
            }
            return null;
        }
        return null;
    }

    private void filterChanged() {
        if (!changingFilterText) {
            CompoundEdit edit = new FilterUndo(undoManager,
                    listController.getFilter());
            undoManager.addEdit(edit);
            listController.setFilter(filterTF.getText());
            edit.end();
        }
    }
    

    // Listener attached to JTextField's model. Will call back to
    // host/user/passwordChanged as approriate
    private class DocumentHandler implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            edited(e);
        }

        public void removeUpdate(DocumentEvent e) {
            edited(e);
        }

        public void changedUpdate(DocumentEvent e) {
            // TextFields can ignore this one.
        }
        
        private void edited(DocumentEvent e) {
            Document source = e.getDocument();
            if (hostTF.getDocument() == source) {
                hostChanged();
            } else if (userTF.getDocument() == source) {
                userChanged();
            } else if (passwordTF.getDocument() == source) {
                passwordChanged();
            } else if (notesTP != null && notesTP.getDocument() == source) {
                notesChanged();
            }
        }
    }
    
    
    // PropertyChangeListener attached to the selected entry to track changes
    // made to it.
    private class SelectedEntryPropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            entryChanged((HostEntry)e.getSource(), e.getPropertyName(),
                    e.getOldValue());
        }
    }
    
    // PropertyChangeListener attached to the ListController. Invokes
    // selectionChanged when the 'selection' property changes.
    private class ListControllerPropertyChangeListener implements
            PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent evt) {
            if (evt.getPropertyName() == "selection") {
                selectionChanged((List<HostEntry>)evt.getOldValue());
            }
        }
    }
    

    private final class ImagePanelPropertyChangeHandler implements PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName() == "imagePath") {
                imageChanged();
            }
        }
    }
    
    
    private final class FilterDocumentHandler implements DocumentListener {
        public void insertUpdate(DocumentEvent e) {
            filterChanged();
        }

        public void removeUpdate(DocumentEvent e) {
            filterChanged();
        }

        public void changedUpdate(DocumentEvent e) {
        }
    }
    
    
    private final static class EntryListController extends
            ListController<HostEntry> {
        protected boolean includeEntry(HostEntry entry, String filter) {
            String host = entry.getHost();
            if (host != null && host.toLowerCase().contains(filter)) {
                return true;
            }
            String user = entry.getUser();
            if (user != null && user.toLowerCase().contains(filter)) {
                return true;
            }
            return false;
        }
    }
    
    
    private final class TabbedPaneChangeHandler implements ChangeListener {
        private final JTabbedPane tp;
        private int selectedIndex;
        
        TabbedPaneChangeHandler(JTabbedPane tp) {
            this.tp = tp;
        }
        
        public void stateChanged(ChangeEvent e) {
            if (tp.getSelectedIndex() == 1 && tp.getComponentAt(1) == null) {
                tp.setComponentAt(1, createNotesPanel());
            }
            selectedIndex = tp.getSelectedIndex();
        }
    }
    
    
    private static final class NotesMouseHandler extends MouseAdapter {
        private final RegExStyler styler;
        
        NotesMouseHandler(RegExStyler styler) {
            this.styler = styler;
        }
        
        public void mouseClicked(MouseEvent e) {
            String text = styler.getMatchingText(e, styler.getStyles().get(0));
            // PENDING: make sure this works in webstart!
            if (text != null && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(new URI(text));
                } catch (IOException ex) {
                } catch (URISyntaxException ex) {
                } catch (UnsupportedOperationException ex) {
                }
            }
        }
    }


    private final class ListTableTransferHandler extends TransferHandler {
        public boolean importData(JComponent comp, Transferable t) {
            try {
                List entries = (List)t.getTransferData(PASSWORD_ENTRY_DATA_FLAVOR);
                int index;
                if (entryList != null) {
                    index = entryList.getLeadSelectionIndex();
                } else {
                    index = entryTable.getSelectionModel().
                            getLeadSelectionIndex();
                }
                if (index == -1) {
                    index = 0;
                } else {
                    index++;
                }
                index = Math.min(index, listController.getEntries().size());
                List<HostEntry> copy = new ArrayList<HostEntry>(
                        entries.size());
                for (Object entry : entries) {
                    copy.add(((HostEntry)entry).clone());
                }
                add(copy, index);
                return true;
            } catch (IOException ex) {
            } catch (UnsupportedFlavorException ex) {
            }
            return false;
        }

        public void exportToClipboard(JComponent comp, Clipboard clip,
                int action) throws IllegalStateException {
            action = getSourceActions(comp) & action;
            List<HostEntry> selection = listController.getSelection();
            List<HostEntry> copy = new ArrayList<HostEntry>(
                    selection.size());
            for (HostEntry entry : selection) {
                copy.add(entry.clone());
            }
            Transferable trans = new DefaultTransferable(
                    PASSWORD_ENTRY_DATA_FLAVOR, copy);
            clip.setContents(trans, null);
            if (action == MOVE) {
                List<HostEntry> entries = listController.getEntries();
                List<HostEntry> selectionCopy = new ArrayList<HostEntry>(
                        listController.getSelection());
                listController.setSelection(null);
                ExtendedCompoundEdit moveUndo = new ExtendedCompoundEdit(undoManager);
                undoManager.addEdit(moveUndo);
                for (HostEntry entry : selection) {
                    int index = entries.indexOf(entry);
                    remove(index, 1);
                }
                moveUndo.end();
            }
        }

        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
            for (DataFlavor flavor : transferFlavors) {
                if (flavor.equals(PASSWORD_ENTRY_DATA_FLAVOR)) {
                    return true;
                }
            }
            return false;
        }
    }
    
    
    private final class FilterUndo extends ExtendedCompoundEdit {
        private boolean subsumeNextSelection;
        private String filterString;
        
        public FilterUndo(ExtendedUndoManager unodManager, String filterString) {
            super(undoManager);
            this.filterString = filterString;
            subsumeNextSelection = true;
        }
        
        public boolean addEdit(UndoableEdit anEdit) {
            if (anEdit instanceof FilterUndo) {
                subsumeNextSelection = true;
                return true;
            }
            if (subsumeNextSelection && anEdit instanceof SelectionChangeUndo) {
                subsumeNextSelection = false;
                return true;
            }
            return super.addEdit(anEdit);
        }

        public boolean isSignificant() {
            return true;
        }

        public void undo() throws CannotUndoException {
            super.undo();
            swapFilter();
        }

        public void redo() throws CannotRedoException {
            super.redo();
            swapFilter();
        }
        
        private void swapFilter() {
            changingFilterText = true;
            String currentFilter = listController.getFilter();
            listController.setFilter(filterString);
            filterTF.setText(filterString);
            filterTF.requestFocus();
            filterString = currentFilter;
            changingFilterText = false;
        }
    }
    
    
    private final class SelectionChangeUndo extends AbstractUndoableEdit {
        private List<HostEntry> lastSelection;
        
        SelectionChangeUndo(List<HostEntry> lastSelection) {
            this.lastSelection = lastSelection;
        }
        
        public void undo() throws CannotUndoException {
            super.undo();
            swapSelection();
        }
        
        public void redo() throws CannotRedoException {
            super.redo();
            swapSelection();
        }

        private void swapSelection() {
            List<HostEntry> selection = listController.getSelection();
            listController.setSelection(lastSelection);
            lastSelection = selection;
        }

        public boolean isSignificant() {
            return true;
        }

        public boolean addEdit(UndoableEdit anEdit) {
            if (anEdit instanceof SelectionChangeUndo) {
                anEdit.die();
                lastSelection = ((SelectionChangeUndo)anEdit).lastSelection;
                return true;
            }
            return false;
        }
        
        public String toString() {
            return getClass().getName() + "@" + hashCode() +
                    "[selection=" + lastSelection + 
                    "]";
        }
    }
    
    
//    private static final class ChangeTabUndo extends AbstractUndoableEdit {
//        private final JTabbedPane tp;
//        private int lastIndex;
//
//        public ChangeTabUndo(JTabbedPane tp, int lastIndex) {
//            this.tp = tp;
//            this.lastIndex = lastIndex;
//        }
//        
//        public void redo() throws CannotRedoException {
//            super.redo();
//            swap();
//        }
//
//        public void undo() throws CannotUndoException {
//            super.undo();
//            swap();
//        }
//
//        public boolean addEdit(UndoableEdit anEdit) {
//            if (anEdit instanceof ChangeTabUndo) {
//                return true;
//            }
//            return false;
//        }
//
//        public boolean isSignificant() {
//            return true;
//        }
//        
//        private void swap() {
//            int currentIndex = tp.getSelectedIndex();
//            tp.setSelectedIndex(lastIndex);
//            lastIndex = currentIndex;
//        }
//    }
    

    private final class MutateUndo extends AbstractUndoableEdit {
        private List<HostEntry> elements;
        private int index;
        private int count;
        
        MutateUndo(int index, int count, boolean isRemove) {
            this.index = index;
            this.count = count;
            if (isRemove) {
                fetchSubList();
            }
        }

        public void undo() throws CannotUndoException {
            super.undo();
            swap();
        }
        
        public void redo() throws CannotRedoException {
            super.redo();
            swap();
        }

        private void swap() {
            if (elements != null) {
                add(elements, index);
                elements = null;
            } else {
                fetchSubList();
                remove(index, count);
            }
        }

        public boolean isSignificant() {
            return true;
        }
        
        private void fetchSubList() {
            elements = new ArrayList<HostEntry>(
                    listController.getEntries().subList(index,
                    index + count));
        }
        
        public String toString() {
            return getClass().getName() + "@" + hashCode() + "[index=" + index + 
                    ", count=" + count +
                    ", elements=" + elements +
                    "]";
        }
    }
}
