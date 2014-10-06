package jettytests;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author James MacGlashan.
 */
public class HelloWebSocket implements WebSocket, WebSocket.OnFrame, WebSocket.OnBinaryMessage, WebSocket.OnTextMessage, WebSocket.OnControl{

	Connection myConnection;

	@Override
	public void onMessage(byte[] bytes, int i, int i2) {
		System.out.println("in binary on message.");
	}

	@Override
	public boolean onControl(byte b, byte[] bytes, int i, int i2) {
		return false;
	}

	@Override
	public boolean onFrame(byte b, byte b2, byte[] bytes, int i, int i2) {
		return false;
	}

	@Override
	public void onHandshake(FrameConnection frameConnection) {

	}

	@Override
	public void onMessage(String s) {
		System.out.println("Received: " + s);
		String myMsg = "back at ya";
		System.out.println("Sending: " + myMsg);
		try {
			this.myConnection.sendMessage(myMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onOpen(Connection connection) {
		System.err.printf("%s#onOpen %s\n",this.getClass().getSimpleName(),connection);
		this.myConnection = connection;
	}

	@Override
	public void onClose(int code, String message) {
		System.err.printf("%s#onDisonnect %d %s\n",this.getClass().getSimpleName(),code,message);
	}


	public static void main(String [] args){

		Server webSocketServer = new Server(8080);
		System.out.println("Starting server at ...");

		WebSocketHandler handler = new WebSocketHandler() {
			@Override
			public WebSocket doWebSocketConnect(HttpServletRequest httpServletRequest, String s) {
				return new HelloWebSocket();
			}
		};

		webSocketServer.setHandler(handler);
		try {
			webSocketServer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			webSocketServer.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
