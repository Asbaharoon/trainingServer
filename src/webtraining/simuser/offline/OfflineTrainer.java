package webtraining.simuser.offline;

import behavior.training.taskinduction.TaskProb;
import behavior.training.taskinduction.sabl.SABLAgent;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.common.StateJSONParser;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.ActionObserver;
import burlap.oomdp.singleagent.GroundedAction;
import burlap.oomdp.singleagent.SADomain;
import webtraining.TrainingProblemRequest;
import webtraining.simuser.CommandsTrainingSimUserInterface;
import webtraining.simuser.SimHumanEnv;

import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * numExplicit and numSteps hold the different "score" criteria
 * Created by James MacGlashan on 8/4/16.
 */
public class OfflineTrainer implements ActionObserver{

    protected Domain domain;
    protected StateJSONParser sp;

    protected CommandsTrainingSimUserInterface cti;

    public int numExplicit = 0;
    public int numSteps = 0;

    Semaphore semaphore = new Semaphore(1);

    public OfflineTrainer(TrainingProblemRequest request){

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

        this.cti.setActionDelay(0);

    }


    /**
     * This code receives what happens each step of the episode and include some code for
     * determining whether the agent is performing the true desired task and generating a
     * "score". You might want to do something with that...
     * @param s the previous state of the environment
     * @param ga the (ground) action taken
     * @param sp the next state of the environment
     */
    @Override
    public void actionEvent(State s, GroundedAction ga, State sp) {

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



        if(terminated){
            semaphore.release();
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




    /**
     * Give a command to train against in give command
     * @param command the command to execute
     * @param goalString the string description of the true goal
     * @param stateJson JSON string representation of the state
     */
    public void giveCommand(String command, String goalString, String stateJson){
        this.giveCommand(command, goalString, this.sp.stringToState(stateJson));
    }


    /**
     * Give a command to train against in give command
     * @param command the command to execute
     * @param goalString the string description of the true goal
     * @param s the state in which the command is given
     */
    public void giveCommand(String command, String goalString, State s){

        this.numExplicit = 0;
        this.numSteps = 0;
        this.cti.giveCommandInInitialState(s, command, goalString);

        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //hacky... acquire to block, then release after blocking completes
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        semaphore.release();


    }
}
