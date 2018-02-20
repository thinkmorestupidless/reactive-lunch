package cc.xuloo.akka;

import akka.actor.*;
import akka.pattern.PatternsCS;
import scala.concurrent.duration.Duration;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Demonstrates the 'lifecycle' of an Actor.
 * Uses a Supervisor and a Child Actor (McCauley Culkin) to emit a trace statement when preStart and postStop methods
 * are invoked during the different ways of stopping an actor.
 */
public class ActorLifecycleExample {

    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("FlightBookingSystem");

        // Uncomment each of the following to see how the different lifecycle methods are invoked.
        stopWithStop(system);
//        stopWithPoisonPill(system);
//        stopWithKill(system);
//        stopWithGracefulStop(system);
    }

    /**
     * Calling stop(ActorRef) on the ActorSystem stops the Actor immediately.
     * No further messages are processed.
     *
     * @param system
     */
    public static void stopWithStop(ActorSystem system) {
        ActorRef theActor = system.actorOf(Props.create(Supervisor.class, Supervisor::new));

        theActor.tell(new TheMessage(), ActorRef.noSender());
        system.stop(theActor);
    }

    /**
     * Sending a PoisonPill to an Actor is just like sending any other message.
     * The Actor continues processing messages that arrived in the mailbox before the PoisonPill
     * and when it processes the PoisonPill it goes through the shutdown procedure.
     *
     * @param system
     */
    public static void stopWithPoisonPill(ActorSystem system) {
        ActorRef theActor = system.actorOf(Props.create(Supervisor.class, Supervisor::new));

        theActor.tell(new TheMessage(), ActorRef.noSender());
        theActor.tell(PoisonPill.getInstance(), ActorRef.noSender());
    }

    /**
     * Sending a Kill message to an Actor
     *
     * @param system
     */
    public static void stopWithKill(ActorSystem system) {
        ActorRef theActor = system.actorOf(Props.create(Supervisor.class, Supervisor::new));

        theActor.tell(new TheMessage(), ActorRef.noSender());
        theActor.tell(Kill.getInstance(), ActorRef.noSender());
    }

    /**
     * Gracefully stopping an ActorSystem will stop all the Actors in the system
     * from the tip of the tree back to the root.
     *
     * @param system
     */
    public static void stopWithGracefulStop(ActorSystem system) {
        ActorRef theActor = system.actorOf(Props.create(Supervisor.class, Supervisor::new));

        theActor.tell(new TheMessage(), ActorRef.noSender());

        try {
            CompletionStage<Boolean> stopped = PatternsCS.gracefulStop(theActor, Duration.create(5, TimeUnit.SECONDS), PoisonPill.getInstance());
            stopped.toCompletableFuture().get(6, TimeUnit.SECONDS);
            // the actor has been stopped
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            // the actor wasn't stopped within 5 seconds
        }
    }

    /**
     * Supervisor is used to demonstrate the different methods of stopping an Actor
     *
     */
    public static class Supervisor extends AbstractActor {

        public static Props props() {
            return Props.create(Supervisor.class, Supervisor::new);
        }

        private ActorRef listener;

        private int acks;

        private ActorRef a;

        private ActorRef b;

        @Override
        public void preStart() {
            System.out.println("Supervisor Starting");

            acks = 0;
            a = getContext().actorOf(McCauleyCulkin.props("First Child"));
            b = getContext().actorOf(McCauleyCulkin.props("Second Child"));
        }

        @Override
        public void postStop() {
            System.out.println("Supervisor Stopped");
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(TheMessage.class, msg -> {
                        System.out.println("Supervisor received a message");

                        listener = getSender();

                        a.tell(msg, getSelf());
                        b.tell(msg, getSelf());
                    })
                    .match(TheAck.class, msg -> {
                        System.out.println("Supervisor received an Ack");

                        if (++acks == 2) {
                            System.out.println("Supervisor received all Acks");

                            listener.tell(new TheAck(), getSelf());
                        }
                    })
                    .build();
        }
    }

    /**
     * Child Actor ;)
     *
     * Used to show when the lifecycle methods of a child actor are invoked as it's stopped by its supervisor.
     */
    public static class McCauleyCulkin extends AbstractActor {

        public static Props props(String key) {
            return Props.create(McCauleyCulkin.class, () -> new McCauleyCulkin(key));
        }

        private final String key;

        public McCauleyCulkin(String key) {
            this.key = key;
        }

        @Override
        public void preStart() {
            System.out.println(key + " Started");
        }

        @Override
        public void postStop() {
            System.out.println(key + " Stopped");
        }

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(TheMessage.class, msg -> {
                        System.out.println(key + " received a message");

                        getSender().tell(new TheAck(), getSelf());
                    })
                    .build();
        }
    }

    public static class TheMessage {}

    public static class TheAck {}
}
