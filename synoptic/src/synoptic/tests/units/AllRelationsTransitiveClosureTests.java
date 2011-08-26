package synoptic.tests.units;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import synoptic.algorithms.graph.TransitiveClosure;
import synoptic.invariants.miners.AllRelationsTransitiveClosure;
import synoptic.model.ChainsTraceGraph;
import synoptic.model.Event;
import synoptic.model.EventNode;
import synoptic.model.Transition;

public class AllRelationsTransitiveClosureTests {

    @Test
    public void constructorSimpleTest() {

        ChainsTraceGraph g = new ChainsTraceGraph();
        EventNode a = new EventNode(new Event("a"));
        EventNode b = new EventNode(new Event("b"));
        EventNode c = new EventNode(new Event("c"));
        EventNode d = new EventNode(new Event("d"));

        a.addTransition(new Transition<EventNode>(a, b, "followed by"));
        a.addTransition(new Transition<EventNode>(a, c, "after"));
        b.addTransition(new Transition<EventNode>(b, d, "followed by"));

        g.add(a);
        g.add(b);
        g.add(c);
        g.add(d);

        AllRelationsTransitiveClosure tcs = new AllRelationsTransitiveClosure(g);

        assertEquals(2, tcs.getRelations().size());
    }

    @Test
    public void isReachableTest() {
        ChainsTraceGraph g = new ChainsTraceGraph();
        EventNode a = new EventNode(new Event("a"));
        EventNode b = new EventNode(new Event("b"));
        EventNode c = new EventNode(new Event("c"));
        EventNode d = new EventNode(new Event("d"));

        a.addTransition(new Transition<EventNode>(a, b, "followed by"));
        a.addTransition(new Transition<EventNode>(a, c, "after"));
        b.addTransition(new Transition<EventNode>(b, d, "followed by"));

        g.add(a);
        g.add(b);
        g.add(c);
        g.add(d);

        AllRelationsTransitiveClosure tcs = new AllRelationsTransitiveClosure(g);

        // Initially failed - changed header of isReachable to accept a String
        // rather than Action
        assertTrue(tcs.isReachable(a, d, "followed by"));
        assertFalse(tcs.isReachable(a, d, "after"));

        assertTrue(tcs.isReachable(a, c, "after"));
    }

    @Test
    public void getTest() {
        ChainsTraceGraph g = new ChainsTraceGraph();
        EventNode a = new EventNode(new Event("a"));
        EventNode b = new EventNode(new Event("b"));
        EventNode c = new EventNode(new Event("c"));
        EventNode d = new EventNode(new Event("d"));

        a.addTransition(new Transition<EventNode>(a, b, "followed by"));
        b.addTransition(new Transition<EventNode>(b, c, "followed by"));
        c.addTransition(new Transition<EventNode>(c, d, "followed by"));
        c.addTransition(new Transition<EventNode>(c, a, "pow"));
        d.addTransition(new Transition<EventNode>(d, c, "pow"));

        g.add(a);
        g.add(b);
        g.add(c);
        g.add(d);

        AllRelationsTransitiveClosure tcs = new AllRelationsTransitiveClosure(g);

        TransitiveClosure tc = g.getTransitiveClosure("followed by");
        assertTrue(tc.isEqual(tcs.get("followed by")));

        TransitiveClosure tc2 = g.getTransitiveClosure("pow");
        assertTrue(tc2.isEqual(tcs.get("pow")));

        assertFalse(tc.isEqual(tcs.get("pow")));
        assertFalse(tc2.isEqual(tcs.get("followed by")));

    }

    @Test
    public void getRelationsTest() {
        ChainsTraceGraph g = new ChainsTraceGraph();
        EventNode a = new EventNode(new Event("a"));
        EventNode b = new EventNode(new Event("b"));
        EventNode c = new EventNode(new Event("c"));
        EventNode d = new EventNode(new Event("d"));

        a.addTransition(new Transition<EventNode>(a, b, "followed by"));
        a.addTransition(new Transition<EventNode>(a, c, "after"));
        b.addTransition(new Transition<EventNode>(b, d, "followed by"));

        g.add(a);
        g.add(b);
        g.add(c);
        g.add(d);

        AllRelationsTransitiveClosure tcs = new AllRelationsTransitiveClosure(g);

        assertEquals(2, tcs.getRelations().size());

        Set<String> r = new HashSet<String>();
        r.add("followed by");
        r.add("after");

        assertTrue(r.equals(tcs.getRelations()));

        r.add("meh");
        assertFalse(r.equals(tcs.getRelations()));
    }
}