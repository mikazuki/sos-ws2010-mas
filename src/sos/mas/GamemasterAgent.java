package sos.mas;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.AchieveREInitiator;

import java.util.Date;
import java.util.Vector;

public class GamemasterAgent extends Agent {

    private void out(String text, Object... args) {
        System.out.print("[" + getLocalName() + "] ");
        System.out.println(String.format(text, args));
    }

    @Override
    protected void setup() {
        try {
            out("Starting");

            Object[] args = getArguments();

            if (args == null || args.length < 3 || args.length > 3) {
                out("Need to supply the names of the two prisoner agents and the number of iterations.");

                takeDown();
            }

            String prisoner1 = (String) args[0];
            String prisoner2 = (String) args[1];
            int iterations = Integer.parseInt((String) args[2]);

            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setName(getLocalName());
            sd.setType("prisoners-dilemma-gamemaster");
            // Agents that want to use this service need to "know" the prisoners-dilemma-ontology
            sd.addOntologies("prisoners-dilemma-ontology");
            // Agents that want to use this service need to "speak" the FIPA-SL language
            sd.addLanguages(FIPANames.ContentLanguage.FIPA_SL);
            dfd.addServices(sd);

            DFService.register(this, dfd);

            ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
            msg.addReceiver(new AID(prisoner1, AID.ISLOCALNAME));
            msg.addReceiver(new AID(prisoner2, AID.ISLOCALNAME));
            msg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);

            msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
            msg.setContent("guilty"); // TODO replace by ontology?

            for (int i = 0; i < iterations; i++) {
                addBehaviour(new AchieveREInitiator(this, msg) {
                    protected void handleFailure(ACLMessage failure) {
                        if (failure.getSender().equals(myAgent.getAMS()))
                            // FAILURE notification from the JADE runtime: the receiver does not exist
                            out("Responder does not exist");
                        else
                            out("Agent %s failed to perform the requested action", failure.getSender().getName());
                    }

                    protected void handleAllResultNotifications(Vector notifications) {
                        for (Object notification : notifications) {
                            ACLMessage inform = (ACLMessage) notification;

                            // TODO more comprehensive parsing (i.e. handle errors)
                            boolean complied = inform.getContent().equals("(true)");

                            // TODO store result

                            out("Agent %s %s", inform.getSender().getName(), (complied ? "complied" : "defected"));
                        }
                    }
                });
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();

            takeDown();
        }
    }

    @Override
    protected void takeDown() {
        out("Stopping");
    }
}
