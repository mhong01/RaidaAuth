package adv.main;

import advclient.common.core.AppCore;
import advclient.common.core.GLogger;
import advclient.common.core.ServantManager;

public class MainAuthenticator {

	private static CheckingRAIDA c ;
	
	public static void main(String[] args) {
		c = new CheckingRAIDA();
		System.out.println(c.checkingRaida());
	}   

	
}
