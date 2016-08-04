package webtraining.simuser.offline;

import webtraining.TrainingProblemRequest;
import webtraining.problemservers.SokoServer2;

import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan
 */
public class OfflineDriver {

    public SokoServer2 server = new SokoServer2();


    public OfflineDriver(){
    }

    public List<ScorePair> simulateCurriculum(List<String> commandSequence, List<String> goalSequence, List<String> jsonStateSequence){

        if(commandSequence.size() != goalSequence.size() || goalSequence.size() != jsonStateSequence.size()){
            throw new RuntimeException("Error, command sequence, goal sequence, and state sequences are not the same size. Received: "
                    + commandSequence.size() + " " + goalSequence.size() + " " + jsonStateSequence.size());
        }

        OfflineTrainer trainer = new OfflineTrainer(server.getTrainingProblemRequest());

        List<ScorePair> scores = new ArrayList<ScorePair>();
        for(int i = 0; i < commandSequence.size(); i++){
            trainer.giveCommand(commandSequence.get(i), goalSequence.get(i), jsonStateSequence.get(i));
            scores.add(new ScorePair(trainer.numExplicit, trainer.numSteps));
        }


        return scores;

    }


    public static class ScorePair{
        public double explicit;
        public double steps;

        public ScorePair(double explicit, double steps) {
            this.explicit = explicit;
            this.steps = steps;
        }
    }


    public static void main(String[] args) {
        OfflineDriver driver = new OfflineDriver();


        //generate curriculum list

        //then run driver.simulateCurriculum(...) to get list of scores on each stage of the curriculum
        //you can run simulateCurriculum multiple times to get multiple samples of the scores the agent would get
        //for the same input curriculum

    }

}
