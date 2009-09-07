/*
 * DesktopApplication1.java
 */
package se.sics.kompics.master.swing;

import java.io.IOException;
import java.util.EventObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import org.apache.commons.configuration.ConfigurationException;
import org.jdesktop.application.Application;
import org.jdesktop.application.LocalStorage;
import org.jdesktop.application.SingleFrameApplication;
import se.sics.kompics.Kompics;
import se.sics.kompics.wan.config.Configuration;
import se.sics.kompics.wan.config.MasterConfiguration;
import se.sics.kompics.wan.plab.PlanetLabCredentials;

/**
 * The main class of the application.
 */
public class DesktopApplication1 extends SingleFrameApplication {

    private static final Logger logger = Logger.getLogger(DesktopApplication1View.class.getCanonicalName());
//    PLabClient client;
    PlanetLabCredentials cred = null;
    String statePersistFile = "state.xml";
    LocalStorage localStorage;

    Client sClient;

    class MaybeExit implements Application.ExitListener {

        public boolean canExit(EventObject e) {
            JFrame f = getMainFrame();
            String s = "Really Exit?"; // *ResourceMap
            int o = JOptionPane.showConfirmDialog(f, s);
            return o == JOptionPane.YES_OPTION;
        }

        public void willExit(EventObject e) {
        }
    }

    /**
     * At startup create and show the main frame of the application.
     */
    @Override
    protected void startup() {

//        client = (PLabClient) ComponentRegistry.getComponent(PLabClient.class, PLabClient.class.getCanonicalName());

        sClient = (Client) ComponentRegistry.getComponent(Client.class, Client.class.getCanonicalName());

        try {
            getContext().getSessionStorage().restore(getMainFrame(), statePersistFile);
        } catch (IOException e) {
            logger.log(Level.WARNING, "couldn't restore session", e);
        }


        localStorage = getContext().getLocalStorage();

        Application app = Application.getInstance();
        app.addExitListener(new MaybeExit());

        show(new DesktopApplication1View(this));
    }

    @Override
    protected void shutdown() {

        try {
            getContext().getSessionStorage().save(getMainFrame(), statePersistFile);
        } catch (IOException e) {
            logger.log(Level.WARNING, "couldn't save session", e);
        }

        getSClient().shutdownAllConnectedDaemons();

        super.shutdown();
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override
    protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of DesktopApplication1
     */
    public static DesktopApplication1 getApplication() {
        return Application.getInstance(DesktopApplication1.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {

        try {
//            Configuration.init(args, PlanetLabConfiguration.class);
              Configuration.init(args, MasterConfiguration.class);

            Kompics.createAndStart(Client.class, 2); //PLabClient
        } catch (ConfigurationException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        launch(DesktopApplication1.class, args);

    }

//    public void setClient(PLabClient client) {
//        this.client = client;
//    }

    public void setCred(PlanetLabCredentials cred) {
        this.cred = cred;
    }

//    public PLabClient getClient() {
//        return client;
//    }

    public PlanetLabCredentials getCred() {
        return cred;
    }

    public LocalStorage getLocalStorage() {
        return localStorage;
    }

    public String getSessionFile() {
        return statePersistFile;
    }

    public Client getSClient()
    {
        return sClient;
    }

}
