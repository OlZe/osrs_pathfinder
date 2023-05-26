package wiki.runescape.oldschool.pathfinder.logic.graph.unweighted;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wiki.runescape.oldschool.pathfinder.logic.Coordinate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GraphBuilder {

    private final Logger logger = LoggerFactory.getLogger(GraphBuilder.class);

    public Graph buildUnweightedGraph(wiki.runescape.oldschool.pathfinder.logic.graph.Graph graph) {
        this.logger.info("Building unweighted graph");

        Map<Coordinate, GraphVertexReal> vertices = new HashMap<>();

        graph.vertices().forEach((coordinate, vertex) ->
                vertices.put(coordinate, new GraphVertexReal(coordinate, new ArrayList<>(), new ArrayList<>(), vertex.wildernessLevel())));

        graph.vertices().values().stream()
                .flatMap(vertex -> vertex.edgesOut().stream())
                .forEachOrdered(edge -> {
                    if (edge.costX2() == 1) {
                        final GraphVertexReal from = vertices.get(edge.from().coordinate());
                        final GraphVertexReal to = vertices.get(edge.to().coordinate());
                        final GraphEdge newEdge = new GraphEdge(from, to, edge.title(), edge.isWalking(), from, to);
                        from.edgesOut().add(newEdge);
                        to.edgesIn().add(newEdge);
                    } else {
                        // edge cost > 1
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
                    }
                });

        Collection<Teleport> teleports = graph.teleports().stream().map(tp -> this.buildUnweightedTeleport(tp, vertices.get(tp.to().coordinate()))).toList();

        this.logger.info("done");

        return new Graph(vertices, teleports);
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
