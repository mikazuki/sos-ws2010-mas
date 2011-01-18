package sos.mas;

import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.AchieveREInitiator;
import jade.proto.SubscriptionResponder;
import jade.proto.SubscriptionResponder.Subscription;

import java.util.Date;
import java.util.Vector;

public class GamemasterAgent extends Agent {

    private void out(String text, Object... args) {
        System.out.print("[" + getLocalName() + "] ");
        System.out.println(String.format(text, args));
    }

    class GMSubscriptionResponder extends SubscriptionResponder {
        GMSubscriptionResponder(Agent a) {
            super(a, MessageTemplate.and(
                                       
MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE),
                                                                       
MessageTemplate.MatchPerformative(ACLMessage.CANCEL)),
                                       
MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE)));
        }
       
        protected ACLMessage handleSubscription(ACLMessage subscription_msg) 
        {
            // handle a subscription request
            // if subscription is ok, create it        	
            try {
				createSubscription(subscription_msg);
			} catch (Exception e) {
	            ACLMessage answerMsg = new ACLMessage(ACLMessage.REFUSE);
	            answerMsg.addReceiver(subscription_msg.getSender());
	            answerMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
			}
			// if successful, should answer (return) with AGREE; otherwise with REFUSE or NOT_UNDERSTOOD
            ACLMessage answerMsg = new ACLMessage(ACLMessage.AGREE);
            answerMsg.addReceiver(subscription_msg.getSender());
            answerMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
            
            return answerMsg;
        }
        
       
        protected void notify(ACLMessage inform) 
        {
            // this is the method you invoke ("call-back") for creating a new inform message;
            // it is not part of the SubscriptionResponder API, so rename it as you like         
            // go through every subscription
            Vector subs = getSubscriptions();
            
            for(int i=0; i<subs.size(); i++)
                ((SubscriptionResponder.Subscription)	subs.elementAt(i)).notify(inform);
        }
    } 

    private GMSubscriptionResponder subscriptionResponder;
    private AID prisoner1;
    private AID prisoner2;
    private int iterations;

    @Override
    protected void setup() {
        try {
            out("Starting");

            handleArguments();
            registerService();
            

            subscriptionResponder = new GMSubscriptionResponder(this);
            addBehaviour(subscriptionResponder);

            startQueryProtocol();
        } catch (FIPAException fe) {
            fe.printStackTrace();

            takeDown();
        }
    }

    private void startQueryProtocol() {
        ACLMessage msg = new ACLMessage(ACLMessage.QUERY_IF);
        msg.addReceiver(prisoner1);
        msg.addReceiver(prisoner2);
        msg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);

        msg.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
        // TODO replace by FIPA SL
        msg.setContent("guilty");

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
                        // TODO replace with FIPA SL parsing
                        boolean complied = inform.getContent().equals("(true)");

                        // TODO store result

                        out("Agent %s %s", inform.getSender().getName(), (complied ? "complied" : "defected"));
                    }
                }
            });
        }
    }

    private void handleArguments() {
        Object[] args = getArguments();

        if (args == null || args.length < 3 || args.length > 3) {
            out("Need to supply the names of the two prisoner agents and the number of iterations.");

            takeDown();
        }

        prisoner1 = new AID((String) args[0], AID.ISLOCALNAME);
        prisoner2 = new AID((String) args[1], AID.ISLOCALNAME);
        iterations = Integer.parseInt((String) args[2]);
    }

    private void registerService() throws FIPAException {
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
    }

    @Override
    protected void takeDown() {
        out("Stopping");
    }
}
