package adv.main;

public class MainAuthenticator {

	private static CheckingRAIDA c ;
	
	public static void main(String[] args) {
		c = new CheckingRAIDA();
		System.out.println(c.checkingRaida());
		c.startServices();
	}

	
}
