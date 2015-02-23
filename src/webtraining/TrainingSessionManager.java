package webtraining;

import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.commands.CommandsTrainingInterface;
import burlap.behavior.singleagent.EpisodeAnalysis;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.GroundedAction;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author James MacGlashan.
 */
public class TrainingSessionManager {

	public static final String						MSGFIELD_MSGTYPE = "msgType";

	public static final String						MSGTYPE_COMMAND = "giveCommand_msg";
	public static final String						MSGTYPE_TERMLEARN = "terminateAndLearn_msg";
	public static final String						MSGTYPE_TERMNOLEARN = "terminateWithoutLearning_msg";
	public static final String						MSGTYPE_FEEDBACK = "feedback_msg";
	public static final String						MSGTYPE_OBSERVE = "observe";
	public static final String						MSGTYPE_ENDEXP = "end_exp";


	public static final String						MSGFIELD_COMMAND = "command";
	public static final String						MSGFIELD_STATE = "state";
	public static final String						MSGFIELD_FEEDBACK = "feedback";
	public static final String						MSGFIELD_ACTION = "action";
	public static final String						MSGFIELD_ENDSTATE = "end_state";
	public static final String						MSGFIELD_LOGID = "log_id";
	public static final String						MSGFIELD_EXPINFO = "exp_info";
	public static final String						MSGFIELD_DELAY = "delay";


	protected WebSocket.Connection connection;
	protected Domain domain;
	protected StateJSONParser sp;

	protected CommandsTrainingInterface cti;

	protected boolean								passIntendedEndState = true;




	public TrainingSessionManager(WebSocket.Connection connection, TrainingProblemRequest request){

		this.connection = connection;

		this.cti = new CommandsTrainingInterface(request.getDomainGenerator());
		this.domain = this.cti.getOperatingDomain();

		StateHashFactory hashingFactory = request.getStateHashingFactory(this.domain);

		this.cti.intantiateDefaultAgent(hashingFactory, request.getFeedbackStrategies());
		this.cti.instatiateCommandsLearning(hashingFactory, request.getTokenizer(), request.getLiftedTasks(this.domain), request.getMaxBindingConstraints());

		this.sp = new StateJSONParser(this.domain);

		this.cti.addActionObserverToOperatingDomain(new ClientActionObserver(this));
		this.cti.setAlwaysResetPriorsWithCommand(false);
	}



	public void observeTransition(State s, GroundedAction a, State nextState){

		//compute end state for hallucinate function
		State endState = null;
		if(passIntendedEndState){
			TaskProb tp = this.cti.getMostLikelyTask();
			EpisodeAnalysis ea = tp.getPolicy().evaluateBehavior(s, tp.getRf(), tp.getTf());
			endState = ea.getState(ea.numTimeSteps()-1);
		}

		//set up message
		List<Map<String, Object>> jsonState = sp.getJSONPrepared(nextState);
		Map<String, Object> javaMessage = new HashMap<String,Object>();
		javaMessage.put(MSGFIELD_MSGTYPE, MSGTYPE_OBSERVE);
		javaMessage.put(MSGFIELD_STATE, jsonState);
		javaMessage.put(MSGFIELD_ACTION, a.toString());
		if(endState != null){
			List<Map<String, Object>> jsonEndState = sp.getJSONPrepared(endState);
			javaMessage.put(MSGFIELD_ENDSTATE, jsonEndState);
		}

		JsonFactory jsonFactory = new JsonFactory();
		StringWriter writer = new StringWriter();
		JsonGenerator jsonGenerator;
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			jsonGenerator = jsonFactory.createGenerator(writer);
			objectMapper.writeValue(jsonGenerator, javaMessage);
		} catch(Exception e){
			System.out.println("Error");
		}

		String jsonMsgString = writer.toString();


		try {
			this.connection.sendMessage(jsonMsgString);
		} catch (IOException e) {
			System.out.println("Tried to send observation message, but connection is dead. Terminating behavior.");
			this.cti.giveTerminateSignal();
		}


	}

	public void receiveClientMessage(Map<String, Object> message){
		String msgType = (String)message.get(MSGFIELD_MSGTYPE);
		if(msgType != null){
			if(msgType.equals(MSGTYPE_COMMAND)){
				this.receiveGiveCommandMessage(message);
			}
			else if(msgType.equals(MSGTYPE_TERMLEARN)){
				this.receiveTerminateAndLearnMessage(message);
			}
			else if(msgType.equals(MSGTYPE_TERMNOLEARN)){
				this.receiveTerminateWithoutLearningMessage(message);
			}
			else if(msgType.equals(MSGTYPE_FEEDBACK)){
				this.receiveGiveFeedbackMessage(message);
			}
			else if(msgType.equals(MSGTYPE_ENDEXP)){
				this.receiveEndExpMessage(message);
			}
		}
	}



	protected void receiveGiveCommandMessage(Map<String, Object> message){
		String command = (String)message.get(MSGFIELD_COMMAND);
		List<Map<String, Object>> jsonState = (List<Map<String, Object>>)message.get(MSGFIELD_STATE);
		State s = sp.JSONPreparedToState(jsonState);

		Object delayOb = message.get(MSGFIELD_DELAY);
		if(delayOb != null){
			int delay;
			if(delayOb instanceof Integer){
				delay = (Integer)delayOb;
			}
			else{
				delay = Integer.parseInt((String)delayOb);
			}
			this.cti.setActionDelay(delay);
		}

		this.cti.giveCommandInInitialState(s, command);
	}

	protected void receiveTerminateAndLearnMessage(Map<String, Object> message){
		this.cti.giveTerminateAndLearnSignal();
	}

	protected void receiveTerminateWithoutLearningMessage(Map<String, Object> message){
		this.cti.giveTerminateSignal();
	}

	protected void receiveGiveFeedbackMessage(Map<String, Object> message){
		Double Val = (Double)message.get(MSGFIELD_FEEDBACK);
		double val = Val;
		if(val > 0.){
			this.cti.giveReward();
		}
		else{
			this.cti.givePunishment();
		}
	}


	protected void receiveEndExpMessage(Map<String, Object> message){

		String logId = (String)message.get(MSGFIELD_LOGID);

		this.cti.writeAllEpisodesToFiles("logs/" + logId);

		try {
			String expInfo = (String) message.get(MSGFIELD_EXPINFO);

			if(expInfo != null){
				BufferedWriter writer = new BufferedWriter(new FileWriter("logs/" + logId + "/expInfo.txt"));
				writer.write(expInfo);
				writer.close();
			}

		}catch (Exception e){
			System.out.println("Error in recording exp information for ." + logId + "\n" + e.getMessage());
		}


	}






}
