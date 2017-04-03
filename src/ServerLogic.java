import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.StringTokenizer;

public class ServerLogic {
	
	
	/*
	 * Funçao que escreve para um ficheiro
	 * out - fileoutputstream a usar
	 * file - ficheiro onde escreve
	 * towrite - string a ser escrita
	 */
	public static void writeToFile (FileOutputStream out, File file, String toWrite) throws IOException{
		
		byte[] contentInBytes = toWrite.getBytes();
		out.write(contentInBytes);
		out.flush();	
	}
	
	/*
	 * Funçao que le todos os utilizadores do ficheiro
	 * e retorna cada um numa posiçao de um arraylist<String>
	 */
	public static ArrayList<String> readFromFile(FileInputStream in, File file) throws IOException{

		ArrayList<Byte> contentInBytes=new ArrayList<Byte>();
		int content;
		while ((content = in.read()) != -1)
			contentInBytes.add((byte) content);
		
		byte[] bytes=new byte[contentInBytes.size()];
		
		int k=0;
		while(contentInBytes.size()>k){
			bytes[k]=contentInBytes.get(k);
			k++;
		}
			
		String read;
		read = new String(bytes, StandardCharsets.UTF_8);
		ArrayList<String> utilizadores = new ArrayList<String> ();
		utilizadores.clear();
		
		StringTokenizer st = new StringTokenizer(read,"\n");
		
		while(st.hasMoreTokens()){
			utilizadores.add(st.nextToken());
		}
		
		return utilizadores;
	}
	
	/*Retorna:
	 * 0- Se registou um novo
	 * 1- Se o utilizador existe retorna 1
	 * 2- Password erradas
	 * 
	 */
	
	public static int existeUser(ArrayList<String> utilizadores,
			String utilizador, String pwd, FileOutputStream out,
			ObjectOutputStream outStream, ObjectInputStream inStream) throws IOException, ClassNotFoundException{
		int existe = 0;
		int nUser=0;
		while(utilizadores.size()>nUser){
			
			//Existe utilizador?
			if(separaUserPwd(utilizadores.get(nUser))[0].equals(utilizador)){
				
				//Password correta
				if(separaUserPwd(utilizadores.get(nUser))[1].equals(pwd)){
					return existe=1;
				}
				else{
					outStream.writeObject("Passwords diferentes");
					return existe=2;
				}
			}
			nUser++;
		}
		//Utilizador nao esta registao
		
		System.out.println("Registo");
		
		outStream.writeObject(new Boolean(true));
		String password = (String) inStream.readObject();
		
		
		
		if(pwd.equals(password)){
			writeToFile(out,null,utilizador+":"+password+"\n");
			existe=0;
			outStream.writeObject(new String("Utilizador "+ utilizador + " registado com sucesso."));
			
		}
		else{
			return existe=2;
			
		}
		return existe;
	}
	
	private static String[] separaUserPwd(String utilizador){
		String[] user_pwd=new String[2];
		StringTokenizer st = new StringTokenizer(utilizador, ":");
		int i=0;
		while(i<2){
			user_pwd[i]=st.nextToken();
			i++;
		}
		
		return user_pwd;
	}
	
	/*
	 * Funçao retorna um inteiro de acordo com a operaçao efetuada
	 * 0-dados de login errados
	 * 1-utilizador a quem se pretende dar acesso nao está registado
	 * 2-utilizador a quem se pretende dar acesso ja tem acesso
	 * 3-Foi concedida permissao ao utilizador a quem se pretende dar acesso
	 * 4- O utilizador que pretende dar acesso nao eh dono do repositorio
	 * 
	 */
	public static int givePermission (ArrayList<String> utilizadores,ArrayList<String> reps,
			String rep, String dono,String pwd, String giveToUser,FileOutputStream out,
			ObjectOutputStream outStream, ObjectInputStream inStream) throws IOException, ClassNotFoundException{
		int permission=0;
		
		//Verifica login
		if(existeUser(utilizadores,dono,pwd,out,outStream,inStream)==0){
			return permission;
			
		}
		//Verifica login de que se pretende dar permissao
		if(!existeUserSemReceberPwd(utilizadores,giveToUser)){
			permission =1;
			return permission;
					
		}
		
		int nRep=0;
		ArrayList<String> repositorioN=new ArrayList<String>();
		
		while(reps.size()>nRep){
			//Existe repositorio?
			repositorioN=separaRepDonoPermissions(reps.get(nRep));
			if(repositorioN.get(0).equals(rep)){
				//O dono é o dono?
				if(repositorioN.get(1).equals(dono)){
					//Comeca a 2 porque nao quero comparar com o nome
					//do rep nem com o dono
					int i=2;
					//O giveTouser ja tem permissao, ou seja, já estah na lista?
					while(i<repositorioN.size()){
						if(repositorioN.get(i).equals(giveToUser)){
							permission=2;
							return permission;
						}
						i++;
					}
					
					//Se chegou aqui eh porque o giveToUser ainda nao tem permissao, ha que dar-lha
					//File file1 = new File("permissoesRepositorios.txt");
					
					//FileOutputStream fop1=new FileOutputStream(file1,true);
					FileOutputStream fop2=new FileOutputStream(out.getFD());
					
					String toWrite="";
					reps.remove(nRep);
					if(reps.size()!=0){
						//toWrite=reps.get(0)+"\n";
						writeToFile(fop2,null,toWrite);
						//Escreve o novo ficheiro
						for(String s : reps){
							toWrite=s+="\n";
							writeToFile(out,null,toWrite);
						}
					}
					toWrite="";
					//Escreve a nova linha
					for(String s : repositorioN){
						toWrite+=s+=":";
					}
					toWrite+=giveToUser;
					
					writeToFile(out,null,toWrite+"\n");
					fop2.close();

					//////////////////////////////////////////
					permission =3;
					return permission;
				}
				else{
					permission = 4;
					return permission;
				}
			}
			nRep++;
			
		}
	
		return permission;
		
	}
	
	private static ArrayList<String> separaRepDonoPermissions(String utilizador){
		ArrayList<String> permissions=new ArrayList<String>();
		StringTokenizer st = new StringTokenizer(utilizador, ":");
		while(st.hasMoreTokens())
			permissions.add(st.nextToken());

		
		return permissions;
	}

	
	/*
	 * funcao que verifica se o userAcesso tem realmente acesso ao repositorio rep
	 */
	public static boolean temAcesso(ArrayList<String> reps, String rep, String userAcesso,
			ObjectOutputStream outStream, ObjectInputStream inStream,FileOutputStream fop1) throws IOException{
		if(rep==null)
			return false;
		boolean temAcesso=false;
		int nRep=0;
		ArrayList<String> repositorioN=new ArrayList<String>();
		
		while(reps.size()>nRep){
			repositorioN=separaRepDonoPermissions(reps.get(nRep));
			
			int i=1;
			while(i<repositorioN.size()){
				if(repositorioN.get(i).equals(userAcesso)&&repositorioN.get(0).equals(rep)){
					
					temAcesso= true;
					return temAcesso;
				}
				i++;
			}	
			nRep++;
		}
		
		//Se chegou aqui é porque é um novo rep, ou porque nao tem acesso ao rep
		//Ha que verificar se o rep eh realmente novo
		System.out.println("nome do rep: "+rep);
		int nReps=0;
		while(reps.size()>nReps){
			repositorioN=separaRepDonoPermissions(reps.get(nReps));
			if(rep.equals
					(repositorioN.get(0))){
				return false;
			}
			nReps++;
		}
		//Se chegou aqui eh porque é um rep novo, ha que escreve-lo para o ficheiro 
		//das permissoes
		//registaNovoRepositorio(rep, userAcesso,fop1, reps, outStream, inStream);
		writeToFile(fop1,null,rep+":"+userAcesso+"\n");
		return temAcesso=true;
	}
	
	public static boolean removePermission (ArrayList<String> utilizadores,ArrayList<String> reps,
			String rep, String dono,String pwd, String removeUser,FileOutputStream out,
			ObjectOutputStream outStream, ObjectInputStream inStream, FileOutputStream fop1) throws IOException, ClassNotFoundException{
		boolean removePermission=false;
		//Verifica login
		if(existeUser(utilizadores,dono,pwd,out,outStream,inStream)==2){
			return removePermission;
			
		}
		
		int nRep=0;
		int repAlterado=0;
		ArrayList<String> repositorioN=new ArrayList<String>();
		
		while(reps.size()>nRep){
			//Existe repositorio?
			repositorioN=separaRepDonoPermissions(reps.get(nRep));
			if(repositorioN.get(0).equals(rep)){
				//O dono é o dono?
				if(repositorioN.get(1).equals(dono)){
					//Comeca a 2 porque nao quero comparar com o nome
					//do rep nem com o dono
					int i=2;
					//O giveTouser ja tem permissao, ou seja, já estah na lista?
					while(i<repositorioN.size()){
						if(repositorioN.get(i).equals(removeUser)){
							repositorioN.remove(i);
							removePermission= true;
							i=repositorioN.size();
							repAlterado=nRep;
							nRep=reps.size();
						}
						i++;
					}
					
					FileOutputStream fop2=new FileOutputStream(fop1.getFD());
					
					String toWrite="";
					reps.remove(repAlterado);
					if(reps.size()!=0){
						writeToFile(fop2,null,toWrite);
						//Escreve o novo ficheiro
						for(String s : reps){
							toWrite=s+="\n";
							writeToFile(fop1,null,toWrite);
						}
					}
					toWrite="";
					//Escreve a nova linha
					for(String s : repositorioN){
						toWrite+=s+=":"; 
					}
					writeToFile(fop1,null,toWrite+"\n");
					fop1.close();
					fop2.close();

					return removePermission;
				}
				else{
					return removePermission=false;
				}
			}
			nRep++;
			
		}
	
		return removePermission;
		
	}
	
	public static boolean login(ArrayList<String> utilizadores,
			String utilizador, String pwd, ObjectOutputStream outStream, ObjectInputStream inStream) throws IOException{
		boolean login=false;
		
		int nUser=0;
		while(utilizadores.size()>nUser){
			
			if(separaUserPwd(utilizadores.get(nUser))[0].equals(utilizador)){
				
				if(separaUserPwd(utilizadores.get(nUser))[1].equals(pwd)){
					return login=true;
				}
				else{
					return login;
				}
			}
			nUser++;
		}
		//Utilizador nao esta registao
		return login;
	}
	
	
	public boolean allTests(ArrayList<String> reps,ArrayList<String> utilizadores ,String rep, String userAcesso,String pwd,

		ObjectOutputStream outStream, ObjectInputStream inStream,FileOutputStream fop1) throws IOException{

		boolean retorna=false;

		if(ServerLogic.login(utilizadores, userAcesso, pwd, outStream, inStream)){

			if(ServerLogic.temAcesso(reps, rep, userAcesso, outStream, inStream,fop1)){

				return true;
			}
		}
		return retorna;


	}
	public static boolean existeUserSemReceberPwd(ArrayList<String> utilizadores,
			String utilizador) throws IOException, ClassNotFoundException{
		boolean existe = false;
	
		int nUser=0;
		while(utilizadores.size()>nUser){
			//Existe utilizador?
			if(separaUserPwd(utilizadores.get(nUser))[0].equals(utilizador)){
				return existe=true;
				}
			nUser++;
			}		
		return existe;
	}
	
}
