package cc.xuloo.akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * The simplest ActorSystem. A single Actor.
 */
public class ActorSystemExample {

    /**
     * Creating the ActorSystem.
     *
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // Call the static create() method and give the system a name.
        // naming an ActorSystem is useful (and sometimes required) for configuration.
        // of some of the more advanced features of Akka.
        ActorSystem system = ActorSystem.create("FlightBookingSystem");

        // Creating an Actor via actorOf().
        // Notice we get back an ActorRef rather than an Actor.
        // This is an example of location transparency in Akka - the Actor implementation may be on another JVM in another datacentre.
        ActorRef theActor = system.actorOf(Props.create(TheActor.class, TheActor::new));

        // Sending the Actor a message.
        theActor.tell("I just received a message", ActorRef.noSender());
    }

    /**
     * A simple Actor.
     * Handles a single type of message.
     */
    public static class TheActor extends AbstractActor {

        /**
         * The Receive object defines how this Actor will respond
         * to the different types of messages it receives.
         *
         * @return
         */
        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(String.class, System.out::println)
                    .build();
        }
    }
}
