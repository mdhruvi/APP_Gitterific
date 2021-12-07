package actors;

import actors.Messages.UnwatchSearchResults;
import actors.Messages.WatchSearchResults;
import akka.Done;
import akka.NotUsed;
import akka.actor.AbstractActor;
import akka.actor.Actor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import akka.stream.KillSwitches;
import akka.stream.Materializer;
import akka.stream.UniqueKillSwitch;
import akka.stream.javadsl.*;
import models.searchResult.SearchResultItem;
import models.searchResult.SearchResults;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Injector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.Json;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;


public class UserActor extends AbstractActor {

    private final Logger logger = LoggerFactory.getLogger("play");

    private Map<String, UniqueKillSwitch> searchResultsMap = new HashMap<>();

    private Map<String, ActorRef> searchResultsActors;

    private Materializer mat;

    private Sink<JsonNode, NotUsed> hubSink;

    private Sink<JsonNode, CompletionStage<Done>> jsonSink;

    private Flow<JsonNode, JsonNode, NotUsed> websocketFlow;

    private Injector injector;


    public UserActor() {
        searchResultsActors = null;
        mat = null;
        hubSink = null;
        websocketFlow = null;
        injector = null;
    }


    @Inject
    public UserActor(Injector injector, Materializer mat) {
        this.searchResultsActors = new HashMap<>();
        this.mat = mat;
        this.injector = injector;
        createSink();
    }


    public void createSink() {
        Pair<Sink<JsonNode, NotUsed>, Source<JsonNode, NotUsed>> sinkSourcePair =
                MergeHub.of(JsonNode.class, 16)
                        .toMat(BroadcastHub.of(JsonNode.class, 256), Keep.both())
                        .run(mat);

        hubSink = sinkSourcePair.first();
        Source<JsonNode, NotUsed> hubSource = sinkSourcePair.second();

        jsonSink = Sink.foreach((JsonNode json) -> {
            // When the user types in a stock in the upper right corner, this is triggered,
            String queryRequest = json.findPath("query").asText();
            askForItems(queryRequest);
        });

        // Put the source and sink together to make a flow of hub source as output (aggregating all
        // searchResults as JSON to the browser) and the actor as the sink (receiving any JSON messages
        // from the browser), using a coupled sink and source.
        this.websocketFlow = Flow.fromSinkAndSourceCoupled(jsonSink, hubSource)
                .watchTermination((n, stage) -> {
                    // Stop the searchResultsActors
                    searchResultsActors.forEach((query, actor) -> stage.thenAccept(f -> context().stop(actor)));

                    // When the flow shuts down, make sure this actor also stops.
                    stage.thenAccept(f -> context().stop(self()));

                    return NotUsed.getInstance();
                });
    }


    private void askForItems(String query) {
        ActorRef actorForQuery = searchResultsActors.get(query);
        if (actorForQuery != null) {
            actorForQuery.tell(new WatchSearchResults(query), self());
        } else {
            actorForQuery = getContext().actorOf(Props.create(GuiceInjectedActor.class, injector,
                    SearchResultsActor.class));
            searchResultsActors.put(query, actorForQuery);
            actorForQuery.tell(new Messages.RegisterActor(), self());
            actorForQuery.tell(new WatchSearchResults(query), self());
        }
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(WatchSearchResults.class, watchSearchResults -> {
                    logger.info("Received message WatchSearchResults {}", watchSearchResults);
                    if (watchSearchResults != null) {
                        // Ask the searchResultsActors for a stream containing these searchResults
                        askForItems(watchSearchResults.query);
                        sender().tell(websocketFlow, self());
                    }
                })
                .match(UnwatchSearchResults.class, unwatchSearchResults -> {
                    logger.info("Received message UnwatchSearchResults {}", unwatchSearchResults);
                    if (unwatchSearchResults != null) {
                        searchResultsMap.get(unwatchSearchResults.query).shutdown();
                        searchResultsMap.remove(unwatchSearchResults.query);
                    }
                })
                .match(Messages.RepoItem.class, message -> {
                    logger.info("Received message RepoItem {}", message);
                    if (message != null) {
                        addRepoItems(message);
                        sender().tell(websocketFlow, self());
                    }
                })
                .build();
    }


    public void addRepoItems(Messages.RepoItem repoItem) {
        Set<SearchResults> searchResults = repoItem.searchResults;
        String query = repoItem.query;

        logger.info("Adding statuses {} for query {}", searchResults, query);

        Source<JsonNode, NotUsed> getSource = Source.from(searchResults)
                .map(Json::toJson);

        // Set up a flow that will let us pull out a killswitch for this specific stock,
        // and automatic cleanup for very slow subscribers (where the browser has crashed, etc).
        final Flow<JsonNode, JsonNode, UniqueKillSwitch> killswitchFlow = Flow.of(JsonNode.class)
                .joinMat(KillSwitches.singleBidi(), Keep.right());
        // Set up a complete runnable graph from the stock source to the hub's sink
        String name = "searchresult-" + query;
        final RunnableGraph<UniqueKillSwitch> graph = getSource
                .viaMat(killswitchFlow, Keep.right())
                .to(hubSink)
                .named(name);

        // Start it up!
        UniqueKillSwitch killSwitch = graph.run(mat);

        // Pull out the kill switch so we can stop it when we want to unwatch a stock.
        searchResultsMap.put(query, killSwitch);
    }


    public interface Factory {
        Actor create(String id);
    }


    public void setMat(Materializer mat) {
        this.mat = mat;
    }


    public Map<String, UniqueKillSwitch> getSearchResultsMap() {
        return searchResultsMap;
    }


    public void setSearchResultsMap(Map<String, UniqueKillSwitch> searchResultsMap) {
        this.searchResultsMap = searchResultsMap;
    }


    public Materializer getMat() {
        return mat;
    }


    public Sink<JsonNode, CompletionStage<Done>> getJsonSink() {
        return jsonSink;
    }


    public void setJsonSink(Sink<JsonNode, CompletionStage<Done>> jsonSink) {
        this.jsonSink = jsonSink;
    }

 
    public Map<String, ActorRef> getSearchResultsActors() {
        return searchResultsActors;
    }

 
    public void setSearchResultsActors(Map<String, ActorRef> searchResultsActors) {
        this.searchResultsActors = searchResultsActors;
    }
}