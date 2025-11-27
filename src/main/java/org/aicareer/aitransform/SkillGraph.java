package org.aicareer.aitransform;

import java.util.*;

public class SkillGraph {

    public static class Edge {
        private final String from;
        private final String to;
        private final double weight; // e.g. P(to | from)

        public Edge(String from, String to, double weight) {
            this.from = from;
            this.to = to;
            this.weight = weight;
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        public double getWeight() {
            return weight;
        }

        @Override
        public String toString() {
            return from + " -> " + to + " (" + weight + ")";
        }
    }

    private final Set<String> skills;
    private final List<Edge> edges;

    public SkillGraph(Set<String> skills, List<Edge> edges) {
        this.skills = skills;
        this.edges = edges;
    }

    public Set<String> getSkills() {
        return skills;
    }

    public List<Edge> getEdges() {
        return edges;
    }
}
