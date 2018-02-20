# REACTIVE LUNCH

_Reactive Lunch_ is designed to give the complete novice and introduction to the Akka toolkit; what it is and why it's a good thing.

I'll walk through a little background, some light theory and try to explain at a high level why it leaves traditional concurrency models in the dust. I'll then introduce some simple code examples for getting up and running with Akka and Actors.

If you read all the way through and concentrate a bit you'll have a good understanding of what Akka is and how it's going to make you better at doing concurrent programming on the JVM. It'll hopefully also whet you're appetite for the upcoming posts that dive deeper into the range of functionality avaiable in the Akka toolkit.

## What is Akka

The Akka toolkit is an actor model implementation for the JVM. It's similar to Erlang/OTP[^1] in its approach to how you develop your applications and manage failure. If you've never heard of Erlang let me explain...

### What is the Actor Model?

The Actor Model[^1] is a really nifty way of letting you, the developer, write highly concurrent applications without having to wrestle with the insanity of handling threads and locks and the rest of the esoteric world of concurrency that you've probably used in the past.

It makes concurrent applications easy to reason about, which means they're less likely to go wrong at runtime.

> With Akka you're more likely to be successful.

_Why?_ 

Because, with Akka, you take the state of your application (the variables or properties of the objects in your application) and you lock it away inside an Actor where it can only be accessed by sending messages to that Actor.

Traditionally properties of an object can be accessed by multiple threads. If that's not handled correctly you get the weird results that come out of unexpected order of execution; You get race conditions. You get `ConcurrentModificationException` and, most importantly, you get woken up in the middle of the night[^2] when things go wrong and they have to be fixed IMMEDIATELY! (Only you can't because the bug was caused by some weird and random concatenation of circumstances that only works when the light from Venus is refracted in just the right way...)

> You can only communicate with an Actor via messages sent to the Actor and messages received from an Actor.

This is the really important part so let me reiterate.

**An Actor is an object but you don't call methods on it - you send it messages.**

The Actor receives a message and does some work. It might change it's state (increment a counter, for instance, or change a User's address), for and/or send other messages to other Actors.

It can then reply to the sender of the message with another message in response or not.

_So how does that help us avoid having to deal with the wonderful world of threads and locks?_

Well, each Actor has it's own mailbox. The mailbox receives the messages that get sent to the Actor and it hands the messages to the Actor in the order in which they're received by the mailbox. The Actor handles each message, one at a time. When it's done handling a message it takes the next message from the mailbox, if there is one, if there isn't it waits for the next one to arrive.

_But there must be threads somewhere, though, right?_

Yes, there are threads, but you don't have to manage them. Akka does that for you. Akka uses something called a Dispatcher to manage a pool of threads. Actors, when they receive a message, grab a thread from the dispatcher, use it to do the work of handling the message and when they're done they give the thread back to the Dispatcher.

This is another important point. Because everything is working with a shared thread pool if you block a thread then the rest of your application's Actors have one less thread to work with. This can easily start to eat into the resource saving you're experiencing by using Akka in the first place.

The solution is to make sure that everything you do in an Actor is _**asynchronous**_.

> If you make a HTTP call to a 3rd party API it should be asynchronous.
> If you make a database query it should be asynchronous.
> If you do anything at all it should be asynchronous.

_I'm using library X and it only has a blocking API, what can i do?_

Remember the Dispatcher i mentioned? Well you can have as many dispatchers as you like in your application. That means you can create a new dispatcher, give it some threads to work with and then tell Akka to use your new Dispatcher whenever it needs to handle your blocking Actor.

_Ok, shut up now and show me how to do all this_

## Creating Actor Systems & Actors

OK, let's jump straight in and look at how to actually use Akka in code. Here's an Actor System and an Actor being created and a message being sent to the Actor. This is about the simplest possible thing you could do.

```java

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

public class FlightBooking {

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
```

Most of that should be self-explanatory but i think it's worth me taking a moment to explain about `ActorRef` and `Props`.

### ActorRef

I've been whittering on about Actors and then i've got an `ActorRef` type in the code above. This is because when you're interacting with Actors you're not actually interacting with Actors - you're interacting with `ActorRef`s.

Because at this point our code is running in a single JVM so it doesn't make a huge amount of difference. A message sent to an ActorRef is just sent straight to the underlying Actor. 

Later, though, when we talk about Clustering Akka applications you may well be sending a message to an Actor located on a different node in the cluster. It may well be in a different JVM in a different Container on a different machine in a different datacentre. But your code will look the same.

That's one of the really killer features of Akka. You can completely disregard any concerns about handling local/remote calls. Location Transparency is built in at such a fundamental level that you can just get on with writing your application and solving your business problem rather than worrying about transport problems[^3].

### Props

In order to enable the Location Transparency to work we need a way of creating Actors indirectly. This is where the `Props` object comes in. What we're doing is creating a factory, really, which the `ActorSystem` can use to create the concrete Actor if it needs to, or get the reference to the remote Actor.

## An Example

Now let's look at modelling something trivial but not quite as trivial as above.

You're the architect for an airline and you've been tasked with designing a new flight booking system. 

You have to be able to create a flight and then handle reservations, cancellations etc. etc for that flight.

Let's have a think about how to model this with Akka. 

Well, first of all as i mentioned above Actors are used to manage state. This makes them great at representing 'things'. A 'thing' in the world maps directly to an Actor in your system which holds the state of the thing in the world[^4]. There are many 'things'; flights, passengers, reservations... but where's the state in our flight booking app? 

It's the Flight itself, right? It needs to know the destination and arrival airports, the time it's due to take off, the type of aeroplane being used and, of course, the list of passenger bookings.

What's more if we define a Flight as the state held by a Flight Actor then we have everything we need to know about a Flight in one place. If we add a passenger reservation or change a reservation or interact with the Flight in any way we don't need to interact with any other Actors to do so. We receive a message to AddPassenger and we add the passenger by simply changing the state in the Actor. If you want to impress your loved ones you can say that the Flight Service is 'consistent across transactional boundaries'. 

> This is really important when designing microservices and can often cause problems because there's often a focus on the _micro_ part of the phrase and services are designed too small. If the _Bounded Context_ is too small and doesn't encompass everything required to change state within an entity in the service then you're no longer 'consistent across transactional boundaries' and you're going to have problems because now you're going to have to couple your service too tightly with another service when they should, really, both be the same service.

So, we want a Flight Actor which receives messages telling it to add/edit/remove reservations. The state is held in-memory as a property of the actor so it's extremely quick to query and respond (it doesn't need to round-trip to the database).

### Some code

Open the 'reactive-lunches' project in your IDE of choice. If you don't have a preference i'd suggest IntelliJ, but that's just me.

Whatever, you'll need an SBT shell, in IntelliJ you have one built into the interface. Otherwise you can open a terminal window, cd to the root of the project and execute the sbt command.

Your terminal prompt will be something like this:

[insert terminal prompt from first excercise]

This means you're in the tutorial correctly (nice) and you're all set. You can now just execute runMain and the above code will be executed for you and you'll see some output to confirm that a message has, indeed, been processed.

### The Actor Lifecycle

So far we've covered the basics of what Akka is and how to get something running. Let's now look a little deeper at Akka Actors and how they work. Everything from this point on is going to be really useful.

The first example created an Actor and sent it a message. Once created the Actor is available in memory whether we continue to use it or not. So, at some point, we're probably going to want to shut it down. 

There are several ways to do this:

stop the Actor
kill the Actor
Send a 'Poison Pill' to the Actor
stop the whole ActorSystem

Which one you use depends on exactly what you're trying to achieve and also how quickly you want the actor shut down.

stop

if you want the Actor stopped NOW without continuing to process any more messages that may be waiting in its mailbox then this is the one for you.

kill



Poison Pill

If you want to continue processing all the messages that were in the Actor's mailbox before the PoisonPill arrived in the mailbox then this is the one you want. The PoisonPill is handled just like any other message, so it waits in the mailbox until its turn to be processed. When the Actor realises the message it's received is a PoisonPill it starts the shutdown procedure.






## Summary

It's almost impossible to make highly concurrent applications in a traditional way without introducing bugs because it doesn't take long for the number of possible interactions to increase to the point where it's just not possible for a developer to consider all possible cases as the threads start to access shared mutable state.

So to use Akka we have to think slightly differently when writing an application. We need to think in terms of messages being sent back and forth.

This can be a bit frustrating at first. It's not something we're used to. But, if you decompose your problem into Actors (which maintain state) and Messages (which tell Actors to do something and provide the results of questions you've asked an Actor) then you can build an application that's much closer to the real world. This means it's more natural to reason about the problem and you're more likely to build something solid.

This is because Actors are great for representing 'things in the world'; users, cats, thermometers, vampires... whatever you're modelling in your software application.

These Actors then send messages to each other. These messages might be replied to or they might not. 

This is how we interact with things (including other people) in the real world. So, rather than getting lost in the technology and the implementation we can concentrate on the business problem (the state and interactions) we're trying to solve.

[^1]:http://learnyousomeerlang.com/introduction#what-is-erlang
[^2]:https://pdfs.semanticscholar.org/7626/93415b205b075639fad6670b16e9f72d14cb.pdf
[^3]:That's if you ever get any sleep at all...
[^4]:You can, of course, customize the [transport mechanism](https://doc.akka.io/docs/akka/current/remoting.html) (and [serialisation](https://doc.akka.io/docs/akka/current/remoting.html)) via configuration.
[^5]:You may have heard this referred to as a '[Digital Twin](https://en.wikipedia.org/wiki/Digital_twin)'