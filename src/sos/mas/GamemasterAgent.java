package sos.mas;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import java.util.Date;
import java.util.Vector;

public class GamemasterAgent extends Agent {
    private static final int PointsBothComplied = 3; 
    private static final int PointsWinner = 5;
    private static final int PointsLoser = 0;
    private static final int PointsBothDefected = 1;	
	
    @Override
    protected void setup() {
        System.out.println("Starting gamemaster agent \"" + getAID().getName());

        Object[] args = getArguments();

        if (args == null || args.length < 3 || args.length > 3) {
            System.out.println("Need to supply the names of the two prisoner agents and the number of iterations.");

            takeDown();
        }

        String prisoner1 = (String) args[0];
        String prisoner2 = (String) args[1];
        int iterations = Integer.parseInt((String) args[2]);

        ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
        msg.addReceiver(new AID(prisoner1, AID.ISLOCALNAME));
        msg.addReceiver(new AID(prisoner2, AID.ISLOCALNAME));
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);

        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        msg.setContent("guilty"); // TODO replace by ontology?

        for (int i = 0; i < iterations; i++) {
            addBehaviour(new AchieveREInitiator(this, msg) {
                protected void handleFailure(ACLMessage failure) {
                    if (failure.getSender().equals(myAgent.getAMS())) {
                        // FAILURE notification from the JADE runtime: the receiver
                        // does not exist
                        System.out.println("Responder does not exist");
                    } else {
                        System.out.println(
                                "Agent " + failure.getSender().getName() + " failed to perform the requested action");
                    }
                }

                protected void handleAllResultNotifications(Vector notifications) {
                    for (Object notification : notifications) {
                        ACLMessage inform = (ACLMessage) notification;

                        // TODO more comprehensive parsing (i.e. handle errors)
                        boolean complied = inform.getContent().equals("inform-t");

                        // TODO store result

                        System.out.println(
                                "Agent " + inform.getSender().getName() + " " + (complied ? "complied" : "defected"));
                    }
                }
            });
        }
    }

    @Override
    protected void takeDown() {
        System.out.println("Stopping gamemaster agent \"" + getAID().getName());
    }
}
