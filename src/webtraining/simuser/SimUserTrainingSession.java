package webtraining.simuser;

import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.sabl.SABLAgent;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.ActionObserver;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.SADomain;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.jetty.websocket.WebSocket;
import webtraining.TrainingProblemRequest;

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
public class SimUserTrainingSession implements ActionObserver {

	public static final String						MSGFIELD_MSGTYPE = "msgType";

	public static final String						MSGTYPE_COMMAND = "giveCommand_msg";
	public static final String						MSGTYPE_OBSERVE = "observe";
	public static final String						MSGTYPE_ENDEXP = "end_exp";

	public static final String						MSGFIELD_COMMAND = "command";
	public static final String						MSGFIELD_STATE = "state";
	public static final String						MSGFIELD_GOAL = "goal";
	public static final String						MSGFIELD_FEEDBACK = "feedback";
	public static final String						MSGFIELD_TERM = "terminated";
	public static final String						MSGFIELD_ACTION = "action";
	public static final String						MSGFIELD_SCORE = "score";
	public static final String						MSGFIELD_SCORE2 = "score2";
	public static final String						MSGFIELD_LOGID = "log_id";
	public static final String						MSGFIELD_EXPINFO = "exp_info";
	public static final String						MSGFIELD_DELAY = "delay";
	public static final String						MSGFIELD_SPEEDMODE = "speed_mode";

	protected WebSocket.Connection connection;
	protected Domain domain;
	protected StateJSONParser sp;

	protected CommandsTrainingSimUserInterface cti;


	protected int numExplicit = 0;
	protected int numSteps = 0;



	public SimUserTrainingSession(WebSocket.Connection connection, TrainingProblemRequest request){

		System.out.println("Instantiated Sim user manager.");

		this.connection = connection;

		this.cti = new CommandsTrainingSimUserInterface(request.getDomainGenerator());
		this.domain = this.cti.getOperatingDomain();

		StateHashFactory hashingFactory = request.getStateHashingFactory(this.domain);

		this.cti.intantiateDefaultAgent(hashingFactory, request.getFeedbackStrategies());
		this.cti.instatiateCommandsLearning(hashingFactory, request.getTokenizer(), request.getLiftedTasks(this.domain), request.getMaxBindingConstraints());

		this.sp = new StateJSONParser(this.domain);

		//this.cti.addActionObserverToOperatingDomain(this);
		((SADomain)this.cti.getEnvDomain()).addActionObserverForAllAction(this);
		this.cti.setAlwaysResetPriorsWithCommand(false);
		this.cti.setRemoveRPPMWhenTrueSatisfied(true);

	}

	@Override
	public void actionEvent(State s, GroundedAction a, State nextState) {

		double r = ((SimHumanEnv)this.cti.getEnv()).getRealLastReward();
		boolean terminated = this.cti.getEnv().curStateIsTerminal();

		if(r != 0.){
			TaskProb ml = this.getMostLikelyTask();
			SimHumanEnv env = (SimHumanEnv)this.cti.getEnv();
			if(ml == null){
				this.numExplicit++;
				this.numSteps++;
			}
			else if(!ml.getTask().toString().equals(env.goalTF.toString())){
				this.numExplicit++;
				this.numSteps++;
			}
		}
		else{
			TaskProb ml = this.getMostLikelyTask();
			SimHumanEnv env = (SimHumanEnv)this.cti.getEnv();
			if(ml == null){
				this.numSteps++;
			}
			else if(!ml.getTask().toString().equals(env.goalTF.toString())){
				this.numSteps++;
			}
		}

		//System.out.println("Score: " + this.numExplicit);

		//set up message
		List<Map<String, Object>> jsonState = sp.getJSONPrepared(nextState);
		Map<String, Object> javaMessage = new HashMap<String,Object>();
		javaMessage.put(MSGFIELD_MSGTYPE, MSGTYPE_OBSERVE);
		javaMessage.put(MSGFIELD_STATE, jsonState);
		javaMessage.put(MSGFIELD_ACTION, a.toString());
		javaMessage.put(MSGFIELD_FEEDBACK, r);
		javaMessage.put(MSGFIELD_TERM, terminated);
		javaMessage.put(MSGFIELD_SCORE, this.numExplicit);
		javaMessage.put(MSGFIELD_SCORE2, this.numSteps);

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
		Object speedOb = message.get(MSGFIELD_SPEEDMODE);
		if(speedOb != null){
			int speed;
			if(speedOb instanceof Integer){
				speed = (Integer)speedOb;
			}
			else{
				speed = Integer.parseInt((String)speedOb);
			}
			this.cti.setSpeedMode(speed);
		}

		String goalString = (String)message.get(MSGFIELD_GOAL);

		this.numExplicit = 0;
		this.numSteps = 0;
		this.cti.giveCommandInInitialState(s, command, goalString);
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

	protected TaskProb getMostLikelyTask(){
		SABLAgent agent = this.cti.getAgent();

		List<TaskProb> distro = agent.getTaskProbabilityDistribution();
		TaskProb ml = null;
		double mlp = -1.;
		for(TaskProb tp : distro){
			if(mlp == -1){
				ml = tp;
				mlp = tp.getProb();
			}
			else if(tp.getProb() > mlp){
				ml = tp;
				mlp = tp.getProb();
			}
			else if(tp.getProb() == mlp){
				ml = null;
			}

		}

		return ml;
	}
}
