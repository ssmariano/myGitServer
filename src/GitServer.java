//package gitserver;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class GitServer {
	private String BasePath = System.getProperty("user.home") + "/gitRepoServer/";
	
	private void createServerDirectory(){
		
		File serverBaseDirectory = new File(BasePath);
		System.out.println(serverBaseDirectory);
		if (!serverBaseDirectory.exists()){
			
			serverBaseDirectory.mkdir();
		}
		
			
	}
	public void startServer(int portNumber) {
		try {
			ServerSocket sSoc = new ServerSocket(portNumber);
			while (true) {
				Socket inSoc = sSoc.accept();
				ServerThread newServerThread = new ServerThread(inSoc);
				newServerThread.start();
				this.createServerDirectory();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	
		
	}

	class ServerThread extends Thread {

		private Socket socket = null;
		private String BasePath = System.getProperty("user.home") + "/gitRepoServer/";
		private ObjectOutputStream outStream;
		private ObjectInputStream inStream;
		
		
		//este fileName e para sair daqui
		File fileName;
		SCMessage scmIn;
		String nameToPass;
		String nameToPass2;
		String pathToClientDirectory;
		
		
		
	
		
		public ServerThread(Socket inSoc) {
			socket = inSoc;

			
			
			
						
		}

		public void run() {
			try {

				this.outStream = new ObjectOutputStream(socket.getOutputStream());
				this.inStream = new ObjectInputStream(socket.getInputStream());

				
				
				scmIn = (SCMessage) this.inStream.readObject();
				System.out.println(scmIn.toString());
				
				
				
				
				
				
				
				

				if (this.scmIn.getResource()!=null){
					String pathToClientDirectory = BasePath + scmIn.getRep();
					
					fileName = new File(pathToClientDirectory);
					
				}
				
				
				
				
				//isto vem do SCMessage, so esta aqui para teste
				//fileName = new File(BasePath + "myGit2");
				
				//scmIn=new SCMessage(40,"jose","jose",fileName,"maria","myGit");
				//scmIn = new SCMessage(20, "jose", "jose", fileName, null, null);
				
				if ((this.scmIn.getResource()!=null) && (fileName.isDirectory())){
					nameToPass = fileName.getName();
					nameToPass2 = nameToPass;
				} else if ((this.scmIn.getResource()!=null) && (!fileName.isDirectory())) {
					
					nameToPass = fileName.getParentFile().getName() + "/" + fileName.getName();
					
					nameToPass2 = fileName.getParent();
				}
				
				
				
				
				
				
				
				
				//isto e para que?
				//File f = new File("bla");
				//f.createNewFile();
				
				
				
				ServerLogic serverLogic = new ServerLogic();
				
				File file = new File(BasePath + "/" + "passwords.txt");
				
				File file1 = new File(BasePath + "/" + "permissoesRepositorios.txt");
		
				ArrayList<String> utilizadores = new ArrayList<String>();
				
				
				//Escrita no ficheiro passwords
				FileOutputStream fop=new FileOutputStream(file, true);
				
				//Escrita no ficheiro de permissoes de repositorios
				FileOutputStream fop1=new FileOutputStream(file1,true);
				
				
				if (!file.exists()) {
					file.createNewFile();
				}
				
				if (!file1.exists()) {
					file1.createNewFile();
				}
				
				try (FileInputStream fis = new FileInputStream(file)) {
					
					utilizadores= ServerLogic.readFromFile(fis,file);
					if(utilizadores.size()==0){
						ServerLogic.writeToFile(fop,file,"A:bola\n");
						utilizadores= ServerLogic.readFromFile(fis,file);
						
					}
					
						
					fis.close();
					
					
					
				}
				ArrayList<String>repositorios = new ArrayList<String>();
				try (FileInputStream fis1 = new FileInputStream(file1)) {
					
					repositorios= ServerLogic.readFromFile(fis1,file1);
					if(repositorios.size()==0){
						ServerLogic.writeToFile(fop1,file1,"jose:jose:manuel\n");
						repositorios= ServerLogic.readFromFile(fis1,file1);
						
					}
					fis1.close();
					
				}			
				
				
				
				
				
				Boolean verification = serverLogic.allTests(repositorios, utilizadores , scmIn.getRep() , scmIn.getUsername(), scmIn.getPassword(), outStream, inStream, fop1);
				// TODO: integrar metodo de verificacao de password e user name
				
				
				//Boolean verification = true;
				
				
				 
				if ((!verification) && (this.scmIn.getOpCode()==10) ){
					int op;
					op=ServerLogic.existeUser(utilizadores, scmIn.getUsername(),scmIn.getPassword(), fop, outStream, inStream);
					
					if(op==1)
						this.outStream.writeObject(new Boolean(false));
					if(op==2)
						this.outStream.writeObject(new String("A passaword esta errada"));
					
					this.outStream.writeObject(new Boolean(true));
			
			
			
			
				}else if (verification) {

					

					// send success - envia um boolean para o cliente
					this.outStream.writeObject(new Boolean(true));
					//String action = (String) this.inStream.readObject();
					//what action from client
					
					if( this.scmIn.getOpCode()==20) {
						
						this.receiveFromClient();
						this.deleteFilesFromServer();
						
						
						
					
					}else if(this.scmIn.getOpCode()==30) {
						
						if(serverLogic.temAcesso(repositorios, scmIn.getRep(), scmIn.getUsername(), outStream, inStream,fop1)){
							this.outStream.writeObject(new Boolean(true));
							
							this.sendFilesToClient();
							this.lastCheckPull(this.nameToPass2);
						}
						else
							this.outStream.writeObject(new Boolean(false));
						
						/* 0-dados de login errados
						 * 1-utilizador a quem se pretende dar acesso nao está registado
						 * 2-utilizador a quem se pretende dar acesso ja tem acesso
						 * 3-Foi concedida permissao ao utilizador a quem se pretende dar acesso
						 * 4- O utilizador que pretende dar acesso nao eh dono do repositorio*/
					} else if (this.scmIn.getOpCode()==40) {
						
						int success;;
					
						success=ServerLogic.givePermission(utilizadores, repositorios, scmIn.getRep(),
								scmIn.getUsername(), scmIn.getPassword(), scmIn.getShareUser(), fop1,outStream,inStream);
						System.out.println("permission= "+success);
						if(success==3){
							this.outStream.writeObject(new String("O user " + scmIn.getShareUser() + 
								" tem agora permissao no repositorio " + scmIn.getRep()));
						}
						else if(success==0){
							this.outStream.writeObject(new String("O user " + scmIn.getUsername() + 
									" deu o login errado "));
						}else if(success==2){
							System.out.println("Entrou permission=2 ");
							this.outStream.writeObject(new String("O user " + scmIn.getShareUser() + 
									" ja tem acesso ao repositorio "+scmIn.getRep()));
							
						}else if(success==1){
							this.outStream.writeObject(new String("O user " + scmIn.getShareUser() + 
									" nao esta registado "));
							
						}else if(success==4){
							this.outStream.writeObject(new String("O user " + scmIn.getUsername() + 
									" nao eh dono do repositorio "+scmIn.getRep()));
							
						}
					
					} else if (this.scmIn.getOpCode()==41){
						ServerLogic.removePermission(utilizadores, repositorios, scmIn.getRep(),
								scmIn.getUsername(), scmIn.getPassword(), scmIn.getShareUser(),fop1, outStream,inStream,fop1);
								
						this.outStream.writeObject(new String("O user " + scmIn.getShareUser() + 
								" ja nao tem permissao no repositorio " + scmIn.getRep()));
					/**	
					}else if(this.scmIn.getOpCode()==30){
						serverLogic.givePermission(utilizadores, repositorios, scmIn.getRep(),
								scmIn.getUsername(), scmIn.getPassword(), scmIn.getShareUser(), fop1,outStream,inStream);
					}else if(action.equals("remove")){
						serverLogic.removePermission(utilizadores, repositorios, scmIn.getRep(),
								scmIn.getUsername(), scmIn.getPassword(), scmIn.getShareUser(), fop1,outStream,inStream);
						
						*/
					} else {
						outStream.writeObject(new Boolean(false));
					}
					
					

				} else {
					outStream.writeObject(new Boolean(false));
				}

			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
		
		
			
			//close socket
			try {
				socket.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
		}
		
		
		

		
		public void lastCheckPull(String path) throws IOException{
			File myFile = new File(this.BasePath + path);
			
			this.outStream.writeObject((File) myFile);
		}
		
		private void sendFilesToClient() throws ClassNotFoundException, IOException{
			String path = (String)this.inStream.readObject();
			//File myFile = new File("/home/mbarros/gitRepoServer/myGit/dfsfsdf.txt");
			File myFile = new File(BasePath+path);
			
			if(!myFile.exists()) {
				System.out.println("ficheiro nao encontrado");
				this.outStream.writeObject("ficheiro nao encontrado");
				return;
			} else if(myFile.isDirectory()) { 
				this.outStream.writeObject("dir");	
			} else {
				this.outStream.writeObject("file");		
			}
			
			if (myFile.isDirectory()) {
				
				int countFiles = 0;
				
				String files[] = myFile.list();
				for (String file : files) {
					File srcFile = new File(myFile, file);
					if (srcFile.getName().endsWith("*_")){
						
						countFiles++;
					}
				}
				
				this.outStream.writeObject(myFile.list().length - countFiles);
				
				//String files[] = myFile.list();

				for (String file : files) {
					File srcFile = new File(myFile, file);
					if (srcFile.getName().endsWith("*_")){
						
						continue;
					}
					this.outStream.writeObject(srcFile.getName());
					this.sendOneFile(srcFile);
					Boolean success = (Boolean) this.inStream.readObject();
				}
			} else {
				//this.outStream.writeObject(myFile.getParentFile().getName());
				this.sendOneFile(myFile);
			}	
			
			
		}

		private void sendOneFile(File myFile) throws IOException, ClassNotFoundException {
			// send size of file
			
			if (myFile.getName().endsWith("*_")){
				
				return;
			}
			this.outStream.writeObject((int) myFile.length());
			// send last modified
			this.outStream.writeObject((long) myFile.lastModified());

			int newest = (int) this.inStream.readObject();
			if (newest == 0) {
				System.out.println("os ficheiros sao iguais");
				return;
			}

			
			byte[] mybytearray = new byte[1024];
			InputStream fileIn = new FileInputStream(myFile);
			int count;
			int totalSend = 0;
			OutputStream outStreamFile = this.socket.getOutputStream();

			// send file
			while ((count = fileIn.read(mybytearray)) > 0) {
				outStreamFile.write(mybytearray, 0, count);
				totalSend += count;
			}
			outStreamFile.flush();
			fileIn.close();
			System.out.print("\rtotal Sent:" + totalSend);
		}
		
		private void receiveFromClient() throws ClassNotFoundException, IOException {
			// file name
			String fileName = (String) this.inStream.readObject();
			// read file size
			Integer fileSize = (Integer) this.inStream.readObject();

			boolean isDirectory = (boolean) this.inStream.readObject();
			int numberOfFiles = 0;
			
			
			
			if (isDirectory) {
				
				
				
				numberOfFiles = (int) this.inStream.readObject();
				

				// create directory
				File serverDirectoryName = new File(BasePath + fileName);

				// if directory not exists, create it
				if (!serverDirectoryName.exists()) {
					serverDirectoryName.mkdir();
				}

				while (numberOfFiles > 0) {
					// file name
					String tmpName = (String) this.inStream.readObject();

					this.receiveFile(tmpName, fileName);

					outStream.writeObject(new Boolean(true));
					numberOfFiles--;
				}

				
				
				
				
			} else if (!isDirectory) {
				// parent folder
				String parentFolder = (String) inStream.readObject();
				File checkDirectory = new File(BasePath + parentFolder);
				if (!checkDirectory.exists()) {
					checkDirectory.mkdir();
				}

				this.receiveFile(fileName, parentFolder);
				
			}
			
		}
		
		private void deleteFilesFromServer () throws ClassNotFoundException, IOException{
			File testFileClient = (File) this.inStream.readObject();
			
			
			String directory = testFileClient.getName();
			File serverDirectoryName = new File(BasePath + testFileClient.getName());
			
			ArrayList<String>  filesFromClient =new ArrayList<String>(Arrays.asList(testFileClient.list()));
			ArrayList<String> filesFromServer = new ArrayList<String>( Arrays.asList(serverDirectoryName.list()));
			
			//remove duplicates 
			filesFromServer.removeAll(filesFromClient);
			
			
			ArrayList<String> filesToRemove = new ArrayList<String>();
					
			for (String tmpFile : filesFromServer) {
				if(!tmpFile.endsWith("*_")) {
					filesToRemove.add(tmpFile);
				}
			}
			
			System.out.println(filesToRemove);
			
			for (String tmpFile : filesToRemove) {
				int numberOfFiles = this.numberOfFilesInFolder(BasePath + directory, tmpFile);
				
				File newFile = new File(BasePath + directory +"/" +  tmpFile);

				if (newFile.exists()) {
					
					/////////////////////////////////////////////
					File newFileName = new File(newFile + "." + numberOfFiles + "*_");
					newFile.renameTo(newFileName);
					
				}
				
				}
			
			
			
					
			
			
			
		}
		
		private long getLastModifiedFileInServer(String directory, String name) {
			File tmpFile = new File(BasePath + directory + "/" + name);
			return tmpFile.lastModified();
		}

		// receber terceiro agumento que vai ser push ou pull
		private int checkLastModifier(long timeStampFileServer, long timeStampeFileClient) {
			

			if (timeStampFileServer < timeStampeFileClient) {
				System.out.println("file in server is older");
				// write file from client to server
				return 1;
			} else if (timeStampFileServer > timeStampeFileClient) {
				System.out.println("file in client is older");
				return 0;
				// write file from server to client
			} else {
				System.out.println("are equal");
				// inform both directories are updated
				return 0;
			}

		}

		private void receiveFile(String fileName, String directoryName) throws IOException, ClassNotFoundException {

			String newFilePath = BasePath + directoryName + "/" + fileName;

			Integer fileSize = (Integer) this.inStream.readObject();
			// last modified
			long lastModClientV = (long) this.inStream.readObject();
			long lastModServerV = getLastModifiedFileInServer(directoryName, fileName);
			int newest = this.checkLastModifier(lastModServerV, lastModClientV);
			// send to clien whitch are new
			this.outStream.writeObject((int) newest);
			if (newest == 0) {
				return;
			}
			int numberOfFiles = this.numberOfFilesInFolder(BasePath + directoryName, newFilePath);

			// verificar se newFilePathExiste
			// se existe verificar os ficheiros

			File newFile = new File(newFilePath);

			if (newFile.exists()) {
				
				/////////////////////////////////////////////
				File newFileName = new File(newFilePath + "." + numberOfFiles + "*_");
				newFile.renameTo(newFileName);
			}

			OutputStream fos = new FileOutputStream(newFilePath);

			byte[] mybytearray = new byte[1024];
			int count;
			int totalRead = 0;
			InputStream inStreamFile = this.socket.getInputStream();
			while ((count = inStreamFile.read(mybytearray)) > 0) {
				fos.write(mybytearray, 0, count);
				totalRead += count;
				if (totalRead == fileSize) {
					break;
				}
			}
			System.out.println("\rTotal Read: " + totalRead);
			fos.close();
			newFile.setLastModified(lastModClientV);
		}

		private int numberOfFilesInFolder(String folderName, String fileName) {
			
			File myFolder = new File(folderName);
			
			
			File myFile = new File(fileName);
			

			int count = 0;
			for (File f : myFolder.listFiles()) {
				if (f.getName().startsWith(myFile.getName())) {
					count++;
					System.out.println(count);
				}
			}

			return count;

	
		}
		
		
	}
	

}
