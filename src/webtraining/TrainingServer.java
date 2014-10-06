package webtraining;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public abstract class TrainingServer implements WebSocket, WebSocket.OnFrame, WebSocket.OnBinaryMessage, WebSocket.OnTextMessage, WebSocket.OnControl{

	protected TrainingSessionManager trainingSessionManager;
	protected TrainingProblemRequest problemRequest;

	public TrainingServer(){
		this.problemRequest = this.getTrainingProblemRequest();
	}

	public abstract TrainingProblemRequest getTrainingProblemRequest();

	@Override
	public void onMessage(byte[] bytes, int i, int i2) {
		System.out.println("Receiving message in byte array: I don't know how to handle this.");
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
	public void onMessage(String message) {
		System.out.println("receiving: " + message);
		JsonFactory jsonFactory = new JsonFactory();
		Map<String, Object> messageData = new HashMap<String, Object>();
		try {
			ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
			TypeReference<Map<String, Object>> listTypeRef =
					new TypeReference<Map<String, Object>>() {};
			messageData = objectMapper.readValue(message, listTypeRef);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.trainingSessionManager.receiveClientMessage(messageData);
	}

	@Override
	public void onOpen(Connection connection) {
		System.err.printf("%s#onOpen %s\n",this.getClass().getSimpleName(),connection);
		this.trainingSessionManager = new TrainingSessionManager(connection, this.problemRequest);
	}

	@Override
	public void onClose(int code, String message) {
		System.err.printf("%s#onDisonnect %d %s\n",this.getClass().getSimpleName(),code,message);
	}

}
