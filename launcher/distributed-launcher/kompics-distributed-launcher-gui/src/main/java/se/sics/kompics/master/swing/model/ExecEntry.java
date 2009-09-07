package se.sics.kompics.master.swing.model;

import java.io.Serializable;

public class ExecEntry extends AbstractEntry implements Serializable {

    private static final long serialVersionUID = 5974374756113421L;

    public static final String COMMAND = "command";
    public static final String RESULT = "result";

    private String command;

    private String result;

    public ExecEntry() {
    }


   
    public ExecEntry clone() {
        ExecEntry entry = new ExecEntry();
        entry.command = command;
        entry.result = result;
        return entry;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        String oldCommand = command;
        this.command = command;

        firePropertyChange(RESULT, oldCommand, command);
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        String oldResult = result;
        this.result = result;

        firePropertyChange(RESULT, oldResult, result);
    }


    
}
