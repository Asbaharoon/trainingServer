package webtraining.simuser;

import auxiliary.DynamicFeedbackEnvironment;
import behavior.training.experiments.interactive.soko.sokoamdp.SokoAMDPPlannerPolicyGen;
import burlap.behavior.singleagent.Policy;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.debugtools.RandomFactory;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import burlap.oomdp.core.State;
import burlap.oomdp.core.TerminalFunction;
import burlap.oomdp.singleagent.Action;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.common.UniformCostRF;
import commands.model3.TrajectoryModule;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SimHumanEnv extends DynamicFeedbackEnvironment {

	protected boolean hasSat = false;
	protected GroundedProp goalGp;
	public TerminalFunction goalTF;
	protected Policy goalPolicy;
	protected Domain planningDomain;

	protected double mu_p = 0.5;
	protected double mu_m = 0.5;
	protected double realLastReward = 0.;


	public SimHumanEnv(Domain operatingDomain, Domain planningDomain) {
		super(operatingDomain);
		this.planningDomain = planningDomain;
	}

	@Override
	public State executeAction(String aname, String[] params) {

		if(aname.equals("noop")){
			this.hasSat = true;
		}
		else{
			this.hasSat = false;
		}

		this.decayHumanFeedback();
		Action a = this.operatingDomain.getAction(aname);
		final State nextState = a.performAction(curState, params);

		this.lastReward = this.genReward(this.curState, new GroundedAction(a, params));
		this.lastStepRewardSequence.add(this.lastReward);

		this.curState = nextState;

		this.waitForUpdateDelay();
		this.lastRecordedRewardSequences.add(this.lastStepRewardSequence);
		this.lastStepRewardSequence = new LinkedList<Double>();
		this.realLastReward = lastReward;

		return nextState;
	}

	@Override
	public double getLastReward() {
		return this.lastReward;
	}

	public double getRealLastReward(){
		return this.realLastReward;
	}

	public TerminalFunction getGoalTF() {
		return goalTF;
	}

	@Override
	public boolean curStateIsTerminal() {
		boolean gp = this.goalGp.isTrue(this.curState);
		return this.hasSat && gp;
	}

	protected void setCurrentGoal(State s, GroundedProp gp){
		this.goalGp = gp;
		SokoAMDPPlannerPolicyGen pgen = new SokoAMDPPlannerPolicyGen();
		this.goalTF = new TrajectoryModule.ConjunctiveGroundedPropTF(Arrays.asList(gp));
		this.goalPolicy = pgen.getPolicy(this.planningDomain, s, new UniformCostRF(), this.goalTF, new DiscreteStateHashFactory());
	}

	protected double genReward(State s, GroundedAction ga){

		double roll = RandomFactory.getMapped(0).nextDouble();

		if(this.inPolicy(s, ga)){
			if(roll < this.mu_p){
				return 1.;
			}
			return 0.;
		}
		else{
			if(roll < this.mu_m){
				return -1.;
			}
			return 0.;
		}

	}

	protected boolean inPolicy(State s, GroundedAction ga){

		if(this.goalGp.isTrue(s)){
			return ga.actionName().equals("noop");
		}


		List<Policy.ActionProb> aps = this.goalPolicy.getActionDistributionForState(s);
		for(Policy.ActionProb ap : aps){
			if(ap.ga.equals(ga) && ap.pSelection > 0){
				return true;
			}
		}

		return false;
	}
}
