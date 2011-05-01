
package jip.monome.serialosc;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

/**
 * Monome discovering service
 */
public class MonomeZeroconf {
	public static final Logger logger = Logger.getLogger(MonomeZeroconf.class.getName());
	public final static String MONOME_TYPE = "_monome-osc._udp.local.";
	
	JmDNS jmdns;
	HashMap<String, ServiceInfo> monomesMap = new HashMap<String, ServiceInfo>();
	
	public MonomeZeroconf() throws IOException{
		jmdns = JmDNS.create();

        jmdns.addServiceListener(MONOME_TYPE, new MonomeServiceListener());
        
        Runtime.getRuntime().addShutdownHook(new Thread(){
        	public void run() {
        		logger.info("Closing bonjour ...");
                try {
					jmdns.close();
				} catch (IOException e) {
					// do nothing
				}
            }
        });
	}

	/**
	 * get available monomes
	 * @return a map<name, info> with the monomes attached to this computer
	 */
	public String[] getDevices(){
		return (String[]) monomesMap.keySet().toArray();
	}
	
	/**
	 * connects no a named monome 
	 * @param name
	 * @param prefix
	 * @param portNumber
	 * @return the object proxying the real device
	 */
	public MonomeDevice connect(String name, String prefix, int portNumber){
		ServiceInfo info = monomesMap.get(name);
		if (info != null)
			return new MonomeDevice(info, prefix, portNumber);
		else 
			return null;
	}
	
	@Override
	protected void finalize() throws Throwable {
		jmdns.close();
		super.finalize();
	}

	/**
	 * Monome service listener
	 *
	 */
	public class MonomeServiceListener implements ServiceListener {

		@Override
		public void serviceAdded(ServiceEvent event) {	        
			logger.info("Service added: " + event.getName() + "." + event.getType());			
	    }

		@Override
	    public void serviceRemoved(ServiceEvent event) {
			String name = event.getName();
	        
	        if (monomesMap.containsKey(name))
	        	monomesMap.remove(name);
	        
	        logger.info("Service removed : " + name + "." + event.getType());
	    }

		@Override
	    public void serviceResolved(ServiceEvent event) {
			monomesMap.put(event.getName(), event.getInfo());
			logger.info("Monome resolved: " + event.getInfo());
	    }
		
	}
	
	public static void main(String[] args) throws IOException, InterruptedException{
		MonomeZeroconf s = new MonomeZeroconf();
		String[] monomes;
		
		// wait for devices available
		do{
			Thread.sleep(20);
			monomes = s.getDevices();
		}while (monomes.length == 0);
		
		for (String name: monomes){
			System.out.println("Monome found: " + name);
		}
		
		// connect to the first found device
		final MonomeDevice m = s.connect(monomes[0], "/myapp", 8000);
		// listen for grid events
		m.addListener(new GridListener() {
			
			@Override
			public void press(int x, int y, int state) {
				m.grid.set(x, y, state);				
			}
		});
		
		for(int i = 0; i<5; i++){
			m.grid.all(1);
			Thread.sleep(100);
			m.grid.all(0);
			Thread.sleep(100);
		}
		m.grid.all(1);
		for(int i = 15; i >= 0; i--){
			m.grid.intensity(i);
			Thread.sleep(100);
		}
		m.grid.all(0);
		m.grid.intensity(10);
		
		System.out.println("Press q and Enter, to quit");
        int b;
        while ((b = System.in.read()) != -1 && (char) b != 'q') {
            /* Stub */
        }       
        System.exit(0);
	}
}
