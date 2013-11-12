package P2PSystem;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;

import java.util.Queue;
import java.util.LinkedList;

public class Client {

	public static Peer self;
	public static Peer[] neighbors;
	public static int neighborsNo;
	public static String[] fileList;
	public DataOutputStream dos;
	public DataInputStream dis;
	public static ServerSocket serversocket;
	public static Socket socket[];
	public Gson gson;
	// message queue
	// public static Queue<Message> messageQueue;
	public static Message[] messageArray;
	public static Peer[] upstreamArray;
	public static int messageNumber;

	public Client() {
		initializeClient();
	}

	public String getPort() {
		return self.peerPort;
	}

	public String getIP() {
		return self.peerIP;
	}

	public void initializeClient() {
		// messageQueue = new LinkedList<Message>();

		messageArray = new Message[500];
		upstreamArray = new Peer[500];
		messageNumber = 0;

		gson = new Gson();
		String configurePath = "/Users/yangkklt/Documents/Courses/CS550/P2PSystem/configure";
		readConfigure(configurePath);
		String filePath = "/Users/yangkklt/cs550demo/" + self.peerName;
		getFileList(filePath);
		new Thread(new Listener()).start();
		Client.socket = new Socket[Client.neighborsNo];

	}

	public boolean hasConnected() {
		for (int i = 0; i < Client.neighborsNo; i++) {
			if (Client.neighbors[i] == null)
				return false;
		}
		return true;
	}

	public static void readConfigure(String filename) {

		BufferedReader reader = null;
		try {
			neighbors = new Peer[10];
			System.out.println("Please input the name of the peer : ");
			Scanner input = new Scanner(System.in);

			String inputString = input.nextLine();

			File file = new File(filename);
			reader = new BufferedReader(new FileReader(file));

			String line = null;
			String[] str = null;
			while ((line = reader.readLine()) != null) {
				str = line.split("\t");
				if (str[0].equals(inputString)) {

					// set this peer's id
					self = new Peer(str[0], str[1], str[2]);
					neighborsNo = str.length - 3;
					// System.out.println("# of neighbors: " + neighborsNo);
					reader = new BufferedReader(new FileReader(file));
					int neighbourIndex = 0;
					while ((line = reader.readLine()) != null) {
						String[] neighAttr = line.split("\t");
						for (int i = 3; i < neighborsNo + 3; i++) {
							if (str[i].equals(neighAttr[0])) {
								neighbors[neighbourIndex++] = new Peer(
										neighAttr[0], neighAttr[1],
										neighAttr[2]);
							}
						}
					}
				}
			}
			System.out.println("I have " + Client.neighborsNo + " neighbours");
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException el) {

				}
			}
		}
	}

	// read each peer's file list.
	public void getFileList(String peerPath) {

		File file = new File(peerPath);
		if (file.exists()) {
			// System.out.println("");
		} else {
			file.mkdirs();
		}

		fileList = new String[file.list().length];
		fileList = file.list();

		// for (int i = 0; i < fileList.length; i++)
		// System.out.println(fileList[i]);

	}

	public void connect() {

		try {
			for (int i = 0; i < Client.neighborsNo; i++) {
				if (Client.socket[i] == null) {
					Client.socket[i] = new Socket(Client.neighbors[i].peerIP,
							Integer.parseInt(Client.neighbors[i].peerPort));
					dos = new DataOutputStream(
							Client.socket[i].getOutputStream());
					dos.writeUTF(Client.self.peerName);
				} else {
					// System.out.println(" Notice : "
					// + Client.neighbors[i].peerName + "has occupied.");
				}
			}

			while (this.hasConnected() == false) {
			}

			for (int i = 0; i < Client.neighborsNo; i++) {
				new Thread(new Handler(i)).start();
			}
			// System.out.println("Network established");

		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void disconnect(Socket[] socket) {
		for (int i = 0; i < neighborsNo; i++) {
			try {
				socket[i].close();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// check if the client already has this message
	// true for already has
	// false for hasn't yet.
	public static boolean checkMessageArray(Message m) {
		for (int i = 0; i < Client.messageNumber; i++) {
			if (Client.messageArray[i].isEqual(m))
				return true;
		}
		return false;
	}

	public void obtain(String fn) {
		System.out
				.println("Please select the peer name you want to download from :");
		Scanner obtainFile = new Scanner(System.in);
		String pn = obtainFile.nextLine();

		// 1. peek the socket belonging to the target peer.
		int socketIndex = -1;
		for (int i = 0; i < Client.neighborsNo; i++) {
			if (Client.neighbors[i].peerName.equals(pn))
				socketIndex = i;
		}
		try {
			// System.out.println("socket index is " + socketIndex);
			// 2. send command index
			DataInputStream diss = new DataInputStream(
					Client.socket[socketIndex].getInputStream());
			DataOutputStream doss = new DataOutputStream(
					Client.socket[socketIndex].getOutputStream());
			doss.writeUTF("4");

			// 3. send file name
			doss.writeUTF(fn);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void query(MessageID mid, int TTL, String searchFileName) {
		Message m = new Message(mid, TTL, searchFileName);

		// send this message
		try {
			for (int i = 0; i < Client.neighborsNo; i++) {
				DataOutputStream dos = new DataOutputStream(
						Client.socket[i].getOutputStream());
				dos.writeUTF("2");
				// send message
				Gson gson = new Gson();
				String sendBuffer = gson.toJson(m);
				dos.writeUTF(sendBuffer);
				// send upstream information
				sendBuffer = gson.toJson(Client.self);
				dos.writeUTF(sendBuffer);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		Client client = new Client();

		Scanner input = null;
		int commandIndex;

		do {
			// user interface
			System.out.println("Please input operation index:");
			System.out.println("1: Create \t 2: Query \t 3: Obtain \t 4: Quit");
			input = new Scanner(System.in);
			commandIndex = input.nextInt();
			switch (commandIndex) {
			case 1:
				client.connect();
				break;
			case 2:

				Scanner searchResult = new Scanner(System.in);
				System.out
						.println("Please input the file name you are looking for: ");
				String searchFileName = searchResult.nextLine();

				System.out.println("Please input the TTL: ");
				int ttl = searchResult.nextInt();

				//long startTime = System.currentTimeMillis();
					MessageID mid = new MessageID(client.self);
					client.query(mid, ttl, searchFileName);

				//long endTime = System.currentTimeMillis();
				//long time = endTime - startTime;
				//System.out.println("Time " + time);
				break;
			case 3:
				Scanner obtainFile = new Scanner(System.in);
				System.out
						.println("Please input the file name you want to download :");
				String fn = obtainFile.nextLine();

				client.obtain(fn);

				System.out.println("Download file " + fn + " successfully");
				break;

			}
		} while (commandIndex != 4);
		disconnect(socket);
	}
}
