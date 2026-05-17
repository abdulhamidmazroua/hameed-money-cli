package org.hameed.hameedmoneycli.model.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RequiredArgsConstructor
public class FinancialOracle {
    // Map<SourceAssetId, Map<TargetAssetId, Price>>
    private final Map<Long, Map<Long, BigDecimal>> adjacencyList = new HashMap<>();

    public void addGraphNode(MarketQuote marketQuote) {
        Long baseId = marketQuote.getBaseAsset().getId();
        Long quoteId = marketQuote.getQuoteAsset().getId();
        BigDecimal price = marketQuote.getPrice();

        // 1. Add the Forward Edge (e.g., USD -> EGP: 48.5)
        adjacencyList.computeIfAbsent(baseId, k -> new HashMap<>())
                .put(quoteId, price);

        // 2. Add the Inverse Edge (e.g., EGP -> USD: 1/48.5)
        // This allows the Oracle to "walk backwards" through quotes
        if (!price.equals(BigDecimal.ZERO)) {
            adjacencyList.computeIfAbsent(quoteId, k -> new HashMap<>())
                    .put(baseId, BigDecimal.ONE.divide(price, 10, RoundingMode.HALF_UP));
        }

        // 3. Add Identity Edges
        adjacencyList.computeIfAbsent(baseId, k -> new HashMap<>()).put(baseId, BigDecimal.ONE);
        adjacencyList.computeIfAbsent(quoteId, k -> new HashMap<>()).put(quoteId, BigDecimal.ONE);
    }

    public BigDecimal getRate(Long fromId, Long toId) {
        // Use Breadth-First Search (BFS) to find the shortest path
        // e.g., AAPL -> USD -> EGP
        Path path = findShortestPath(fromId, toId);

        return path.edges().stream()
                .map(Edge::price)
                .reduce(BigDecimal.ONE, BigDecimal::multiply);
    }

    private Path findShortestPath(Long fromId, Long toId) {
        Queue<Long> queue = new LinkedList<>();
        Map<Long, Long> parent = new HashMap<>();
        Map<Long, BigDecimal> distance = new HashMap<>();

        queue.offer(fromId);
        distance.put(fromId, BigDecimal.ONE);
        parent.put(fromId, null);

        while (!queue.isEmpty()) {
            Long current = queue.poll();

            if (current.equals(toId)) {
                List<Edge> edges = new ArrayList<>();
                Long node = toId;

                while (parent.get(node) != null) {
                    Long prev = parent.get(node);
                    BigDecimal price = adjacencyList.get(prev).get(node);
                    edges.add(0, new Edge(price));
                    node = prev;
                }

                return new Path(edges);
            }

            Map<Long, BigDecimal> neighbors = adjacencyList.getOrDefault(current, new HashMap<>());
            for (Long neighbor : neighbors.keySet()) {
                if (!distance.containsKey(neighbor)) {
                    distance.put(neighbor, BigDecimal.ONE);
                    parent.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }

        return new Path(new ArrayList<>());
    }

    private record Path(
            List<Edge> edges
    ) {
    }

    private record Edge(
            BigDecimal price
    ) {
    }


}