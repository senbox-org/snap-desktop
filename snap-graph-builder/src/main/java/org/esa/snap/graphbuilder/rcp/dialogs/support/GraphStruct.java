package org.esa.snap.graphbuilder.rcp.dialogs.support;

import java.util.ArrayList;
import java.util.List;

public class GraphStruct {
    private List<String> leafs;
    private String id;

    private GraphStruct(String id, List<String> leafs) {
        this.id = id;
        this.leafs = leafs;
    }

    public GraphStruct copy() {
        ArrayList<String> leafs = new ArrayList<>();
        for (String gs : this.leafs) {
            leafs.add(gs);
        }
        return new GraphStruct(this.id, leafs);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof GraphStruct) {
            GraphStruct gs = (GraphStruct) obj;
            if (gs.id.equals(this.id) && gs.leafs.size() == this.leafs.size()) {
                for (String child : gs.leafs) {
                    if (!this.leafs.contains(child))
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    static public List<GraphStruct> copyGraphStruct(GraphNode[] nodes) {
        ArrayList<GraphStruct> list = new ArrayList<>();
        for (GraphNode g: nodes) {
            ArrayList<String> children = new ArrayList<>();
            for (String id: g.getSources()) {
                children.add(id);
            }
            list.add(new GraphStruct(g.getID(), children));
        }

        return list;
    }

    static public boolean deepEqual(List<GraphStruct> a, List<GraphStruct> b) {
        if (a.size() == b.size()) {
            for (GraphStruct key: a) {
                if (!b.contains(key))
                    return false;
            }
            return true;
        }
        return false;
    }
}
