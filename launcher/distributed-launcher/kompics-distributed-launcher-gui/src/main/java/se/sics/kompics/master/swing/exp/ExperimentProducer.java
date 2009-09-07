/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package se.sics.kompics.master.swing.exp;

import java.util.Map;
import org.netbeans.spi.wizard.Summary;
import org.netbeans.spi.wizard.WizardException;
import org.netbeans.spi.wizard.WizardPage;

/**
 *
 * @author jdowling
 */
public class ExperimentProducer implements WizardPage.WizardResultProducer
{
    public static class ExpSetup {
        private final String artifactId;
        private final String groupId;
        private final String version;
        private final String repoId;
        private final String repoUrl;
        private final String mainClass;
        private final String args;

        private final String[] hostnames;

        public ExpSetup(String artifactId, String groupId, String version, String repoId, String repoUrl, String mainClass, String args, String[] hostnames) {
            this.artifactId = artifactId;
            this.groupId = groupId;
            this.version = version;
            this.repoId = repoId;
            this.repoUrl = repoUrl;
            this.mainClass = mainClass;
            this.args = args;
            this.hostnames = hostnames;
        }

        public String getArgs() {
            return args;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public String getGroupId() {
            return groupId;
        }

        public String[] getHostnames() {
            return hostnames;
        }

        public String getMainClass() {
            return mainClass;
        }

        public String getRepoId() {
            return repoId;
        }

        public String getRepoUrl() {
            return repoUrl;
        }

        public String getVersion() {
            return version;
        }
    }


   public Object finish (Map wizardData) throws WizardException
   {
      System.out.println (wizardData);

        String artifactId = (String) wizardData.get(ExperimentWizardPanel1.ARTIFACT_ID);
        String groupId = (String) wizardData.get(ExperimentWizardPanel1.GROUP_ID);
        String version = (String) wizardData.get(ExperimentWizardPanel1.VERSION);
        String repoId = (String) wizardData.get(ExperimentWizardPanel1.REPO_ID);
        String repoUrl = (String) wizardData.get(ExperimentWizardPanel1.REPO_URL);
        String mainClass = (String) wizardData.get(ExperimentWizardPanel1.MAIN_CLASS);
        String args = (String) wizardData.get(ExperimentWizardPanel1.ARGS);

      return null;
//      return Summary.create (items, null);
   }

   public boolean cancel (Map settings)
   {
      System.out.println ("cancel called");
      System.out.println (settings);

      return true; // Allow the user to cancel the wizard.
   }
}
