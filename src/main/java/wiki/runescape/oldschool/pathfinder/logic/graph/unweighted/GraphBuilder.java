package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.util.*;
import java.util.function.Predicate;

import static wiki.runescape.oldschool.pathfinder.logic.graph.GraphBuilder.*;

public class GraphBuilder {

    private static final List<String> WALK_LATERAL = List.of(WALK_NORTH, WALK_EAST, WALK_SOUTH, WALK_WEST);
    private static final List<String> WALK_DIAGONAL = List.of(WALK_NORTH_EAST, WALK_SOUTH_EAST, WALK_SOUTH_WEST, WALK_NORTH_WEST);
    private final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

    public Graph buildUnweightedGraph(wiki.runescape.oldschool.pathfinder.logic.graph.Graph graph) {
        this.logger.info("Building unweighted graph");

        // Create vertices
        Map<Coordinate, GraphVertexReal> vertices = new HashMap<>();
        graph.vertices().forEach((coordinate, vertex) ->
                vertices.put(coordinate, new GraphVertexReal(coordinate, new ArrayList<>(), new ArrayList<>(), vertex.wildernessLevel())));

        // Link lateral walking steps
        linkWalkingEdges(graph, vertices, edge -> WALK_LATERAL.contains(edge.title()));

        // Link diagonal walking steps
        linkWalkingEdges(graph, vertices, edge -> WALK_DIAGONAL.contains(edge.title()));

        // Link transports
        linkTransportEdges(graph, vertices);

        // Create teleports
        Collection<Teleport> teleports = graph.teleports().stream().map(tp -> this.buildUnweightedTeleport(tp, vertices.get(tp.to().coordinate()))).toList();

        this.logger.info("done");
        return new Graph(vertices, teleports);
    }

    private void linkWalkingEdges(final wiki.runescape.oldschool.pathfinder.logic.graph.Graph graph, final Map<Coordinate, GraphVertexReal> vertices, final Predicate<wiki.runescape.oldschool.pathfinder.logic.graph.GraphEdge> edgeFilter) {
        graph.vertices().values().stream()
                .flatMap(vertex -> vertex.edgesOut().stream())
                .filter(edgeFilter)
                .forEachOrdered(lateralWalkEdge -> {
                    assert (lateralWalkEdge.costX2() == 1);
                    assert (lateralWalkEdge.isWalking());
                    final GraphVertexReal from = vertices.get(lateralWalkEdge.from().coordinate());
                    final GraphVertexReal to = vertices.get(lateralWalkEdge.to().coordinate());
                    final GraphEdge newEdge = new GraphEdge(from, to, lateralWalkEdge.title(), true, from, to);
                    from.edgesOut().add(newEdge);
                    to.edgesIn().add(newEdge);
                });
    }

    private void linkTransportEdges(final wiki.runescape.oldschool.pathfinder.logic.graph.Graph graph, final Map<Coordinate, GraphVertexReal> vertices) {
        graph.vertices().values().stream()
                .flatMap(vertex -> vertex.edgesOut().stream())
                .filter(edge -> !WALK_LATERAL.contains(edge.title()) && !WALK_DIAGONAL.contains(edge.title()))
                .forEachOrdered(edge -> {
                    assert (edge.costX2() > 1);
                    // split edge into multiple edges with phantom vertices inbetween
                    final GraphVertexReal first = vertices.get(edge.from().coordinate());
                    final GraphVertexReal last = vertices.get(edge.to().coordinate());

                    final GraphVertexPhantom second = new GraphVertexPhantom();
                    second.fromReal = first;
                    second.from = first;
                    second.toReal = last;
                    first.edgesOut().add(new GraphEdge(first, second, edge.title(), false, first, last));

                    GraphVertexPhantom current = second;
                    for (int i = 2; i < edge.costX2(); i++) {
                        final GraphVertexPhantom next = new GraphVertexPhantom();
                        next.fromReal = current.fromReal;
                        next.toReal = current.toReal;
                        next.from = current;
                        current.to = next;

                        current = next;
                    }

                    final GraphVertexPhantom secondLast = current;
                    secondLast.to = last;
                    last.edgesIn().add(new GraphEdge(secondLast, last, edge.title(), false, first, last));
                });
    }

    private Teleport buildUnweightedTeleport(final wiki.runescape.oldschool.pathfinder.logic.graph.Teleport teleport, GraphVertexReal destination) {
        // First vertex is unknown as teleports can originate from multiple vertices
        final GraphVertexPhantom second = new GraphVertexPhantom();

        GraphVertexPhantom current = second;
        for (int i = 2; i < teleport.costX2(); i++) {
            final GraphVertexPhantom next = new GraphVertexPhantom();
            current.to = next;
            current.toReal = destination;
            next.from = current;
            current = next;
        }

        final GraphVertexPhantom secondLast = current;
        secondLast.to = destination;
        secondLast.toReal = destination;

        return new Teleport(second, teleport.title(), teleport.canTeleportUpTo30Wildy(), destination);
    }
}
