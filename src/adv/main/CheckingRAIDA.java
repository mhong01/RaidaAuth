package adv.main;

import advclient.common.core.AppCore;
import advclient.common.core.GLogger;
import advclient.common.core.ServantManager;

public class CheckingRAIDA {
	
	private ServantManager sm;
	private ProgramState ps;
	
	private GLogger logger;
	
	public CheckingRAIDA() {
		initSystem();
	}

	public void initSystem() {
        logger = new WLogger();
        String home = System.getProperty("user.home");
        //home += File.separator + "CloudCoinWallet";
            
        sm = new ServantManager(logger, home);
        if (!sm.init()) {
            resetState();
            ps.errText = "Failed to init program. Make sure you have correct folder permissions (" + home + ")";
            return;
        }
    
        AppCore.readConfig();
        AppCore.copyTemplatesFromJar();
        resetState();
    }
	
    public void resetState() {
        ps = new ProgramState();
        if (sm == null) 
            return;

//        if (sm.getWallets().length != 0) {
//            setActiveWallet(sm.getWallets()[0]);
//            ps.currentScreen = ProgramState.SCREEN_SHOW_TRANSACTIONS;
//        }
    }
    
    public boolean checkingRaida() {
    	return sm.isRAIDAOK();
    }
}
