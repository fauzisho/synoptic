package model;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import synoptic.invariants.ITemporalInvariant;

/**
 * Extends the EncodedAutomaton class to encode the intersection of multiple
 * InvModel Automatons.
 * 
 * @author Jenny
 */
public class InvsModel extends EncodedAutomaton {

    // The set of invariants represented by this Automaton.
    private Set<ITemporalInvariant> invariants;

    /**
     * Constructs a new InvsModel that accepts all strings.
     */
    public InvsModel() {
        invariants = new HashSet<ITemporalInvariant>();
    }

    /**
     * Returns true if the given sequence of Strings are accepted by this model.
     */
    @Override
    public boolean run(List<String> statements) {
        if (invariants.size() == 0) {
            return false;
        }
        return super.run(statements);
    }

    /**
     * Returns the set of ITemporalInvariants composing this model.
     */
    public Set<ITemporalInvariant> getInvariants() {
        return Collections.unmodifiableSet(invariants);
    }

    /**
     * Intersects this InvsModel with all of the given InvModels and adds each
     * to this model's list of invariants.
     */
    public void intersectWith(List<InvModel> invs) {
        for (InvModel inv : invs) {
            this.intersectWith(inv);
        }
    }

    /**
     * Intersects this InvsModel with the given InvsModel, maintaining
     * invariants of both.
     */
    public void intersectWith(InvsModel other) {
        this.invariants.addAll(other.invariants);
        super.intersectWith(other);
    }

    /**
     * Intersects this InvsModel with the given InvModel and adds the invariant
     * to this model's list of invariants.
     */
    public void intersectWith(InvModel inv) {
        invariants.add(inv.getInvariant());
        super.intersectWith(inv);
    }
}
