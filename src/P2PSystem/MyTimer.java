package P2PSystem;


import java.util.TimerTask;
import java.util.Vector;

public class MyTimer extends TimerTask{

	private static Vector<Message> msg;
	public MyTimer(Vector<Message> m){
		msg = m;
	}
	public void run() {  
	   flushoutMsg();  
	}

	private void flushoutMsg() {
		msg.clear();
		System.out.println("Flush out the message table!");
	}  
}
