import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class HeyNet {

	public static void main(String[] args) {

		Socket socket = null;
		InfoExtractor ex = null;
		try {
			socket = new Socket("127.0.0.1", 10101);
			ex = new InfoExtractor("src/words.txt", "src/hosts.txt");
			System.out.println("Initializing and loading CoreNLP tools.\nThis might take a few seconds ..!");
			ex.getAbstractTask("allow h1 to talk to h2");
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println(e.getMessage());
		}
		

		AbstractTask task = new AbstractTask();
		Scanner scanner = new Scanner(System.in);
		String line = "";
		PrintWriter writer = null;
		BufferedReader in = null;
		do{
			System.out.println("How can I help you?");
			line = scanner.nextLine();
			
			try {
				
				task = ex.getAbstractTask(line);
				writer = new PrintWriter(socket.getOutputStream(), true);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println(e.getMessage());
			} catch (NullPointerException e){
				e.printStackTrace();
				System.err.println(e.getMessage());
			}
			
			String fromServer = null;
			try {
				writer.println(task.serialize());
				fromServer = in.readLine();
				System.out.println("Network says:"+fromServer);
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println(e.getMessage());
			}catch (NullPointerException e){
				e.printStackTrace();
				System.err.println(e.getMessage());
			}
			
		}while(line!=null && !line.equalsIgnoreCase("exit"));
	}

}
