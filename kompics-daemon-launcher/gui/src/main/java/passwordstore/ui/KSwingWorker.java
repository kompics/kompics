package passwordstore.ui;

import javax.swing.SwingWorker;

import se.sics.kompics.Request;
import se.sics.kompics.Response;
import se.sics.kompics.wan.plab.CoMonStats;
import se.sics.kompics.wan.plab.PlanetLabCredentials;
import se.sics.kompics.wan.plab.events.RankHostsUsingCoMonRequest;

public class KSwingWorker extends SwingWorker<Response, Void> {

	private Response response=null;
	private PlanetLabCredentials cred;
	private Request request;
	
	public KSwingWorker(PlanetLabCredentials cred, Request request)
	{
		this.cred = cred;
		this.request =  request;
	}
	
    @Override
    public Response doInBackground() {
        Response response = null;
        RankHostsUsingCoMonRequest request = new 
        RankHostsUsingCoMonRequest(cred, 
        		true, CoMonStats.BOOT_STATE);
        
        // wait on return value from handler
        // synchronize on the handler object
        synchronized(this)
        {
        	try {
				wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        return response;
    }
    
    public void setResponse(Response response)
    {
    	this.response = response;
    }

    @Override
    public void done() {

        try {
            Response response = get();
            
            // fire off as a property change to listening components??
            
        } catch (InterruptedException ignore) {}
        catch (java.util.concurrent.ExecutionException e) {
            String why = null;
            Throwable cause = e.getCause();
            if (cause != null) {
                why = cause.getMessage();
            } else {
                why = e.getMessage();
            }
            System.err.println("Error retrieving file: " + why);
        }
    }
}
