package wiki.runescape.oldschool.server;

import wiki.runescape.oldschool.logic.Graph;

import java.util.Collection;

public class TransportsTeleportsHttpHandler extends JsonHttpHandler<Void> {

    private final Graph graph;

    public TransportsTeleportsHttpHandler(final Graph graph) {
        super(Void.class);
        this.graph = graph;
    }

    @Override
    protected Reply handle(final Request<Void> request) {
        Collection<String> allTransportsTeleports = graph.getAllTeleportsTransports();
        return new Reply(false, allTransportsTeleports);
    }
}
