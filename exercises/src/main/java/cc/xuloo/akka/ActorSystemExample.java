package cc.xuloo.akka;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class ActorSystemExample {

    public static void main(String[] args) throws Exception {
        ActorSystem system = ActorSystem.create("FlightBookingSystem");
        ActorRef theActor = system.actorOf(Props.create(TheActor.class, TheActor::new));

        theActor.tell(new TheMessage(), ActorRef.noSender());
    }

    public static class TheActor extends AbstractActor {

        @Override
        public Receive createReceive() {
            return receiveBuilder()
                    .match(TheMessage.class, msg -> {
                        System.out.println("I just received a message");
                    })
                    .build();
        }
    }

    public static class TheMessage {}
}
