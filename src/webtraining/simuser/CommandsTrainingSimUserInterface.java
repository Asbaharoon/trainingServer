package webtraining.simuser;

import behavior.training.taskinduction.commands.version2.CommandsTrainingInterface2;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.common.NullAction;
import burlap.oomdp.singleagent.environment.DomainEnvironmentWrapper;
import burlap.oomdp.singleagent.environment.Environment;

/**
 * @author James MacGlashan.
 */
public class CommandsTrainingSimUserInterface extends CommandsTrainingInterface2 {

	public CommandsTrainingSimUserInterface(DomainGenerator dgen) {
		super(dgen);
		this.domain = dgen.generateDomain();
		this.noopAction = new NullAction("noop", domain, ""); //add noop to the operating domain



		DomainEnvironmentWrapper dEnvWrapper = new DomainEnvironmentWrapper(this.domain, this.env);
		this.domainEnvWrapper = dEnvWrapper.generateDomain();

		//generate a domain without the noop for planning
		this.planningDomain = dgen.generateDomain();

		this.env = new SimHumanEnv(this.domain, this.planningDomain);

		this.envRF = this.env.getEnvironmentRewardRFWrapper();
		this.envTF = this.env.getEnvironmentTerminalStateTFWrapper();
	}

	public Environment getEnv(){
		return this.env;
	}


	@Override
	public void giveCommandInInitialState(State s, String command) {
		throw new UnsupportedOperationException("CommandsTrainingSimUserInterface does not giving a command with the known true goal.");
	}

	public void giveCommandInInitialState(State s, String command, String trueGoal){

		if(this.agentIsRunning) {
			this.giveTerminateAndLearnSignal();
		}

		System.out.println("Received command: " + command);

		GroundedProp goal = this.parseStringIntoGP(trueGoal);

		//remember this state and command for learning completion
		this.initialState = s.copy();
		this.lastCommand = command;

		//first set our environment to this state
		this.env.setCurStateTo(s);
		((SimHumanEnv)env).setCurrentGoal(s, goal);
		this.env.receiveIsTerminalSignal(false);

		this.commandInterface.setRFDistribution(s, command);


		this.commandHistory.add(command);

		//then let learning start in a separate thread so that the user can interact with it
		this.agentThread = new Thread(new Runnable() {

			@Override
			public void run() {
				agent.runLearningEpisodeFrom(CommandsTrainingSimUserInterface.this.initialState);
			}
		});

		this.agentIsRunning = true;
		this.agentThread.start();

	}

}
