package org.reactome.server.tools.reaction.exporter.layout.algorithm.box;

import org.reactome.server.tools.reaction.exporter.layout.common.EntityRole;

import java.util.*;
import java.util.stream.Collectors;

import static org.reactome.server.tools.reaction.exporter.layout.algorithm.box.Place.*;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.NEGATIVE_REGULATOR;
import static org.reactome.server.tools.reaction.exporter.layout.common.EntityRole.POSITIVE_REGULATOR;

/**
 * Helper class to decide relative position of elements: compartment vs compartment or compartment vs reaction
 *
 * @author Pascual Lorente (plorente@ebi.ac.uk)
 */
public class PlacePositioner {

    /**
     * Where do I want to be with respect to other compartments
     */
    private final static Map<Place, List<Place>> PREFERRED = new HashMap<Place, List<Place>>() {
        {
            put(LEFT, Arrays.asList(LEFT, BOTTOM, TOP));
            put(RIGHT, Arrays.asList(RIGHT, BOTTOM, TOP));
            put(TOP, Arrays.asList(TOP, RIGHT, LEFT));
            put(BOTTOM, Arrays.asList(BOTTOM, RIGHT, LEFT));
            put(CENTER, Collections.emptyList());
        }
    };

    /**
     * Where will I allow other compartments to be placed with respect to me
     */
    private final static Map<Place, List<Place>> ALLOWED = new HashMap<Place, List<Place>>() {
        {
            put(LEFT, Arrays.asList(RIGHT, TOP, BOTTOM));
            put(RIGHT, Arrays.asList(LEFT, TOP, BOTTOM));
            put(TOP, Arrays.asList(BOTTOM, LEFT, RIGHT));
            put(BOTTOM, Arrays.asList(TOP, LEFT, RIGHT));
            put(CENTER, Arrays.asList(LEFT, RIGHT, TOP, BOTTOM));
        }
    };
    private static final List<Place> DEFAULT_ALLOWANCES = Collections.unmodifiableList(Arrays.asList(LEFT, RIGHT, TOP, BOTTOM));

    /*
    This short piece of code generates 2 comparators to sort vertically or horizontally any number of compartments

    public static final Comparator<Box> VERTICAL = (a, b) -> compare(a, b, TOP, BOTTOM);
    public static final Comparator<Box> HORIZONTAL = (a, b) -> compare(a, b, LEFT, RIGHT);

    private static int compare(Box a, Box b, Place start, Place end) {
        final Collection<Place> aPlaces = a.getBusyPlaces();
        final Collection<Place> bPlaces = b.getBusyPlaces();
        final Place aPlace = getRelativePosition(aPlaces, bPlaces);
        final Place bPlace = getRelativePosition(bPlaces, aPlaces);
        if (aPlace == bPlace) return 0;
        if (aPlace == start) return -1;
        if (aPlace == end) return 1;
        if (bPlace == start) return 1;
        if (bPlace == end) return -1;
        return 0;
    }
     */

    /**
     * Computes the relative position of 2 sets of {@link Place}s.
     *
     * @param a first set of places
     * @param b second set of places
     * @return the relative position of a with respect to b
     */
    public static Place getRelativePosition(Collection<Place> a, Collection<Place> b) {
        // NOTE: there are 768 possible combinations, I've been tempted more than once to hardcode them
        final List<Place> preferences = getPreferences(a);
        final List<Place> allowed = getAllowances(b);

        if (allowed.isEmpty()) return null;
        if (preferences.isEmpty()) return allowed.get(0);

        // general case, take the first allowed preference
        for (final Place preference : preferences) {
            if (allowed.contains(preference)) {
                return preference;
            }
        }

        // Lists do not contain a common Place
        // Here the reaction plays an important role in, at least, 2 known cases:
        //   +-------+  +-------+
        //   |   C   |  |       |
        //   |   X   |  | I   O |
        //   |   R   |  |       |
        //   +-------+  +-------+
        //  Here, lines from input and output to reaction will overlap, the only solution is to places one box on top
        //  of the other
        //   +-------+  +-------+
        //   |   C   |  |       |
        //   |       |  | I X O |
        //   |   R   |  |       |
        //   +-------+  +-------+
        // On the contrary, if the reaction is in the input+output box,the boxes should be side by side
        // In both cases, we violate the preferences of the box with the reaction in favor of the one without.

        if (b.contains(CENTER)) return preferences.get(0);
        else return allowed.get(0);
    }

    /**
     * Collapses both regulators roles into NEGATIVE_REGULATOR.
     *
     * @param roles a list of roles
     * @return the list of roles replacing any POSITIVE_REGULATOR
     * @deprecated Using roles in the layout is a bad practice because it contains two roles that share a place, and it
     * lacks a Reaction role. Used {@link PlacePositioner#getPlace(EntityRole)} and {@link
     * PlacePositioner#getPlaces(Collection)} instead.
     */
    @Deprecated
    public static Collection<EntityRole> simplify(Collection<EntityRole> roles) {
        if (roles.isEmpty()) return Collections.emptyList();
        roles = EnumSet.copyOf(roles);
        if (roles.contains(POSITIVE_REGULATOR)) roles.add(NEGATIVE_REGULATOR);
        roles.remove(POSITIVE_REGULATOR);
        return roles;
    }

    public static List<Place> getPreferences(Collection<Place> places) {
        if (places.isEmpty()) return Collections.emptyList();
        return buildOrderedIntersection(places, PREFERRED);
    }

    public static List<Place> getAllowances(Collection<Place> places) {
        if (places.isEmpty()) return DEFAULT_ALLOWANCES;
        return buildOrderedIntersection(places, ALLOWED);
    }

    private static List<Place> buildOrderedIntersection(Collection<Place> places, Map<Place, List<Place>> takeFrom) {
        // order matters, that's why we take the first one as the initial list
        // places should have at least one element
        final Iterator<Place> iterator = places.iterator();
        final List<Place> result = new ArrayList<>(takeFrom.get(iterator.next()));
        while (iterator.hasNext()) result.retainAll(takeFrom.get(iterator.next()));
        return result;
    }

    public static Collection<Place> getPlaces(Collection<EntityRole> roles) {
        return roles.stream()
                .map(PlacePositioner::getPlace)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(Place.class)));
    }

    /**
     * Simple, yet necessary method to map from an {@link EntityRole} to a {@link Place}
     *
     * @param role a role
     * @return the place where this role goes
     */
    public static Place getPlace(EntityRole role) {
        switch (role) {
            case INPUT:
                return LEFT;
            case OUTPUT:
                return RIGHT;
            case CATALYST:
                return TOP;
            case NEGATIVE_REGULATOR:
            case POSITIVE_REGULATOR:
                return BOTTOM;
        }
        return CENTER;
    }
}
