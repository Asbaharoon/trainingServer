package webtraining.problemservers;

import behavior.training.taskinduction.strataware.FeedbackStrategy;
import burlap.behavior.statehashing.DiscreteMaskHashingFactory;
import burlap.behavior.statehashing.StateHashFactory;
import burlap.oomdp.auxiliary.DomainGenerator;
import burlap.oomdp.core.Domain;
import burlap.oomdp.core.GroundedProp;
import commands.model3.GPConjunction;
import commands.model3.mt.Tokenizer;
import domain.singleagent.sokoban2.Sokoban2Domain;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.websocket.WebSocket;
import org.eclipse.jetty.websocket.WebSocketHandler;
import webtraining.TrainingProblemRequest;
import webtraining.TrainingServer;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * @author James MacGlashan.
 */
public class SokoServer extends TrainingServer{

	@Override
	public TrainingProblemRequest getTrainingProblemRequest() {
		return new TrainingProblemRequest() {

			@Override
			public Tokenizer getTokenizer() {
				return new Tokenizer(true, true);
			}

			@Override
			public StateHashFactory getStateHashingFactory(Domain operatingDomain) {

				DiscreteMaskHashingFactory hashingFactory = new DiscreteMaskHashingFactory();
				hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSAGENT, operatingDomain.getAttribute(Sokoban2Domain.ATTX));
				hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSAGENT, operatingDomain.getAttribute(Sokoban2Domain.ATTY));
				hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, operatingDomain.getAttribute(Sokoban2Domain.ATTX));
				hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, operatingDomain.getAttribute(Sokoban2Domain.ATTY));
				hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, operatingDomain.getAttribute(Sokoban2Domain.ATTCOLOR));
				hashingFactory.addAttributeForClass(Sokoban2Domain.CLASSBLOCK, operatingDomain.getAttribute(Sokoban2Domain.ATTSHAPE));

				return hashingFactory;
			}

			@Override
			public int getMaxBindingConstraints() {
				return 22;
			}

			@Override
			public List<GPConjunction> getLiftedTasks(Domain operatingDomain) {
				List<GPConjunction> liftedTaskDescriptions = new ArrayList<GPConjunction>(2);

				GPConjunction atr = new GPConjunction();
				atr.addGP(new GroundedProp(operatingDomain.getPropFunction(Sokoban2Domain.PFAGENTINROOM), new String[]{"a", "r"}));
				liftedTaskDescriptions.add(atr);

				GPConjunction btr = new GPConjunction();
				btr.addGP(new GroundedProp(operatingDomain.getPropFunction(Sokoban2Domain.PFBLOCKINROOM), new String[]{"b", "r"}));
				liftedTaskDescriptions.add(btr);
				return liftedTaskDescriptions;
			}

			@Override
			public List<FeedbackStrategy> getFeedbackStrategies() {
				List<FeedbackStrategy> feedbackStrategies = new ArrayList<FeedbackStrategy>();
				feedbackStrategies.add(new FeedbackStrategy(0.7, 0.7, 0.1));
				return feedbackStrategies;
			}

			@Override
			public DomainGenerator getDomainGenerator() {
				Sokoban2Domain dgen = new Sokoban2Domain();
				dgen.includeDirectionAttribute(true);
				dgen.includePullAction(true);
				return dgen;
			}
		};
	}


	public static void main(String [] args){

		int port = 8080;
		if(args.length == 1){
			port = Integer.parseInt(args[0]);
		}

		Server webSocketServer = new Server(port);
		System.out.println("Starting server at ...");

		WebSocketHandler handler = new WebSocketHandler() {
			@Override
			public WebSocket doWebSocketConnect(HttpServletRequest httpServletRequest, String s) {
				return new SokoServer();
			}
		};

		handler.getWebSocketFactory().setMinVersion(-1);

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
