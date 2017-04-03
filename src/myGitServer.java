//package gitserver;



public class myGitServer {

	public static void main(String[] args) {
		System.out.println("Servidor iniciado.");
		GitServer server = new GitServer();
		server.startServer(Integer.parseInt(args[0]));
		
		
	}
	
}