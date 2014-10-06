package webtraining;

import burlap.oomdp.core.State;
import burlap.oomdp.singleagent.ActionObserver;
import burlap.oomdp.singleagent.GroundedAction;

/**
 * @author James MacGlashan.
 */
public class ClientActionObserver implements ActionObserver {

	protected TrainingSessionManager sessionManager;

	public ClientActionObserver(TrainingSessionManager sessionManager){
		this.sessionManager = sessionManager;
	}

	@Override
	public void actionEvent(State s, GroundedAction ga, State sp) {
		this.sessionManager.observeTransition(sp, ga, sp);
	}

}
