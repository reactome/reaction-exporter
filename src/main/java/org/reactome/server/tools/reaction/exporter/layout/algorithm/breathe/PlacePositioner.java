package org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe;

import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;

import java.util.*;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.breathe.Place.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.*;

public class PlacePositioner {
    /**
     * Where do I want to be with respect to other compartments
     */
    private final static Map<EntityRole, List<Place>> PREFERENCES = new HashMap<EntityRole, List<Place>>() {
        {
            put(INPUT, Arrays.asList(LEFT, TOP, BOTTOM));
            put(OUTPUT, Arrays.asList(RIGHT, TOP, BOTTOM));
            put(CATALYST, Arrays.asList(TOP, LEFT, RIGHT));
            put(POSITIVE_REGULATOR, Arrays.asList(BOTTOM, LEFT, RIGHT));
            put(NEGATIVE_REGULATOR, Arrays.asList(BOTTOM, LEFT, RIGHT));
        }
    };

    /**
     * Where will I allow other compartments to be placed with respect to me
     */
    private final static Map<EntityRole, List<Place>> ALLOWED = new HashMap<EntityRole, List<Place>>() {
        {
            put(INPUT, Arrays.asList(RIGHT, TOP, BOTTOM));
            put(OUTPUT, Arrays.asList(LEFT, TOP, BOTTOM));
            put(CATALYST, Arrays.asList(BOTTOM, LEFT, RIGHT));
            put(POSITIVE_REGULATOR, Arrays.asList(TOP, LEFT, RIGHT));
            put(NEGATIVE_REGULATOR, Arrays.asList(TOP, LEFT, RIGHT));
        }
    };
    public static final Comparator<Box> VERTICAL = (a, b) -> compare(a, b, TOP, BOTTOM);
    public static final Comparator<Box> HORIZONTAL = (a, b) -> compare(a, b, LEFT, RIGHT);

    private static int compare(Box a, Box b, Place start, Place end) {
        final Collection<EntityRole> aRoles = a.getContainedRoles();
        final Collection<EntityRole> bRoles = b.getContainedRoles();
        final Place aPlace = haggle(aRoles, bRoles);
        final Place bPlace = haggle(bRoles, aRoles);
        if (aPlace == bPlace) return 0;
        if (aPlace == start) return -1;
        if (aPlace == end) return 1;
        if (bPlace == start) return 1;
        if (bPlace == end) return -1;
        return 0;
    }

    /**
     * Computes the relative position of 2 sets of {@link EntityRole}s.
     *
     * @param a first set of entity roles
     * @param b second set of entity roles
     * @return the relative position of a with respect to b
     */
    public static Place haggle(Collection<EntityRole> a, Collection<EntityRole> b) {
        a = simplify(a);
        b = simplify(b);
        final List<Place> preferences = getPreferences(a, PREFERENCES);
        final List<Place> allowed = getPreferences(b, ALLOWED);
        // general case
        for (final Place preference : preferences) {
            if (allowed.contains(preference)) return preference;
        }
        if (preferences.isEmpty() && allowed.isEmpty()) return LEFT;  // could be random
        else if (preferences.isEmpty()) return allowed.get(0);
        else if (allowed.isEmpty()) return preferences.get(0);
            // mismatch between both lists
        else return preferences.get(0);
    }

    private static List<Place> getPreferences(Collection<EntityRole> a, Map<EntityRole, List<Place>> takeFrom) {
        final List<Place> preferences = new ArrayList<>();
        for (final EntityRole role : a) {
            if (preferences.isEmpty()) preferences.addAll(takeFrom.get(role));
            else {
                final List<Place> places = takeFrom.get(role);
                preferences.retainAll(places);
            }
        }
        return preferences;
    }

    private static Collection<EntityRole> simplify(Collection<EntityRole> b) {
        b = new ArrayList<>(b);
        if (b.contains(POSITIVE_REGULATOR)) b.add(NEGATIVE_REGULATOR);
        b.remove(POSITIVE_REGULATOR);
        return b;
    }

}
