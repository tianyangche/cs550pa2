package P2PSystem;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Queue;
import java.util.LinkedList;

import com.google.gson.Gson;

public class Handler implements Runnable {

	private Client client;
	private Socket socket;
	public Gson gson;
	public DataInputStream dis;
	public DataOutputStream dos;

	public int threadIndex;

	public Handler(int i) {
		gson = new Gson();
		threadIndex = i;
	}

	public void run() {
		try {
			socket = Client.socket[threadIndex];
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			int commandIndex;
			do {
				commandIndex = Integer.parseInt(dis.readUTF());
				switch (commandIndex) {
				case 2:
					Message temp = new Message();
					temp = gson.fromJson(dis.readUTF(), temp.getClass());

					temp.TTLdecrease();
					// temp.printMessage();
					Peer p = new Peer();
					p = gson.fromJson(dis.readUTF(), p.getClass());
					// p.printPeer();

					// if no, store the message in the array
					if (Client.checkMessageArray(temp) == false) {
						Client.messageArray[Client.messageNumber] = temp;
						Client.upstreamArray[Client.messageNumber] = p;
						Client.messageNumber++;
					}
					Gson g = new Gson();
					// forward it to it's neighbors
					if (temp.currentTTL != 0) {
						for (int i = 0; i < Client.neighborsNo; i++) {
							// not send to its upstream
							if (p.peerName.equals(Client.neighbors[i].peerName) == false) {

								DataOutputStream dos = new DataOutputStream(
										Client.socket[i].getOutputStream());
								dos.writeUTF("2");
								// send message
								String sendBuffer = g.toJson(temp);
								dos.writeUTF(sendBuffer);
								// send upstream information
								sendBuffer = g.toJson(Client.self);
								dos.writeUTF(sendBuffer);
							}
						}
					}

					// send hit query back
					// 1. find if the current peer has the target file.
					String searchFile = temp.FileName;
					boolean searchResult = false;
					for (int i = 0; i < Client.fileList.length; i++) {
						if (Client.fileList[i].equals(searchFile))
							searchResult = true;
					}

					if (searchResult)
						System.out.println("I have " + searchFile);
					

					int connectionIndex = 0;
					// 2. find the upstream connection
					for (int i = 0; i < Client.neighborsNo; i++) {
						if (Client.neighbors[i].peerName.equals(p.peerName)) {
							connectionIndex = i;
						}
					}

					dos = new DataOutputStream(
							Client.socket[connectionIndex].getOutputStream());
					// System.out.println("from 2 3");
					dos.writeUTF("3");

					// 3. send back to the upstream peer.
					// public HitMessage(MessageID me, boolean fl, Peer ta)
					HitMessage h = new HitMessage(temp.messageID, searchResult,
							Client.self);

					String sendBuffer = g.toJson(h);
					dos.writeUTF(sendBuffer);

					break;
				case 3:

					// out.println("in 3 3");
					HitMessage receivedHitMessage = new HitMessage();
					receivedHitMessage = gson.fromJson(dis.readUTF(),
							receivedHitMessage.getClass());
					// receivedHitMessage.printHitMessage();

					// if current peer is not the sender, we continue sending
					// this hit message to upstream.
					int previousIndex = -1;
					// 1. decide whether the current peer is the source.
					if (receivedHitMessage.m.peerID.peerName
							.equals(Client.self.peerName) == false) {
						// 2. find upstream peer index
						for (int i = 0; i < Client.messageNumber; i++) {
							if (Client.messageArray[i].messageID
									.isEqual(receivedHitMessage.m)) {
								previousIndex = i;
							}
						}
					}

					int chooseSocket = -1;

					// find the upstream client's socket index
					if (previousIndex != -1) {
						for (int i = 0; i < Client.neighborsNo; i++) {
							if (Client.neighbors[i].peerName
									.equals(Client.upstreamArray[previousIndex].peerName))
								chooseSocket = i;
						}
					}

					// System.out.println("I get hit messge "
					// + receivedHitMessage.m.peerID.peerName + "-"
					// + receivedHitMessage.m.sequenceNumber + "-"
					// + receivedHitMessage.target.peerName);

					// if current peer is the sender and the hit message has the
					// file
					if (Client.self.peerName
							.equals(receivedHitMessage.m.peerID.peerName)
							&& receivedHitMessage.flag) {
						System.out.println(receivedHitMessage.target.peerName
								+ " has the file.");
						// previousIndex = -1;
					}

					// System.out.println("previous index is " + previousIndex);

					if (chooseSocket != -1) {
						dos = new DataOutputStream(
								Client.socket[chooseSocket].getOutputStream());

						dos.writeUTF("3");

						// 3. send back to the upstream peer.
						// public HitMessage(MessageID me, boolean fl, Peer
						// ta)
						// receivedHitMessage.target = Client.self;

						sendBuffer = gson.toJson(receivedHitMessage);
						dos.writeUTF(sendBuffer);
					}

					break;

				case 4:
					// System.out.println("4");
					// 1. get file name
					String fn = dis.readUTF();
					// 2. search the name
					System.out.println("Download file is " + fn);
					String filePath = "/Users/yangkklt/cs550demo/"
							+ Client.self.peerName + "/" + fn;
					// System.out.println(filePath);
					FileInputStream in = new FileInputStream(filePath);
					File fileTemp = new File("/Users/yangkklt/tempfile");
					FileOutputStream out = new FileOutputStream(fileTemp);
					int c;
					byte buffer[] = new byte[10240];
					// read the file to a buffer
					int textLength = 0;
					while ((c = in.read(buffer)) != -1) {
						for (int i = 0; i < c; i++) {
							out.write(buffer[i]);
							textLength = i;
						}
					}
					
					
					String str = new String(buffer, "UTF-8");
					str = str.substring(0, textLength);
					// System.out.println(str);
					dos.writeUTF("5");
					dos.writeUTF(fn);
					dos.writeUTF(str);
					break;

				case 5:

					System.out.println("command 5");
					String fname = dis.readUTF();
					String result = dis.readUTF();
					result = result + "\0";
					System.out.println("file content is :");
					System.out.println(result);
					// write to local file
					FileWriter fw = new FileWriter("/Users/yangkklt/cs550demo/"
							+ Client.self.peerName + "/" + fname);
					fw.write(result, 0, result.length());
					fw.flush();
					fw.close();

					break;
				}
			} while (commandIndex != 0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}