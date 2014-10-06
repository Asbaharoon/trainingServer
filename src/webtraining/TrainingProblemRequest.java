package webtraining;

import behavior.training.taskinduction.strataware.FeedbackStrategy;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Domain;
import commands.model3.GPConjunction;

import commands.model3.mt.Tokenizer;

import java.util.List;

/**
 * @author James MacGlashan.
 */
public interface TrainingProblemRequest {
	public DomainGenerator getDomainGenerator();
	public StateHashFactory getStateHashingFactory(Domain operatingDomain);
	public List<FeedbackStrategy> getFeedbackStrategies();
	public List<GPConjunction> getLiftedTasks(Domain operatingDomain);
	public Tokenizer getTokenizer();
	public int getMaxBindingConstraints();
}
