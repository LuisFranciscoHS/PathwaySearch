package no.uib.pap.pathwaymatcher.stages;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.ParseException;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.TreeMultimap;

import no.uib.pap.model.Pathway;
import no.uib.pap.model.Proteoform;
import no.uib.pap.model.ProteoformFormat;
import no.uib.pap.model.Reaction;
import no.uib.pap.pathwaymatcher.Matching.ProteoformMatcher;
import no.uib.pap.pathwaymatcher.Matching.ProteoformMatcherFlexible;

/*
Query to check the examples.
// Get Reactions, Pathways and TopLevelPathways by Ewas
MATCH (tlp:TopLevelPathway)-[:hasEvent*]->(p:Pathway)-[:hasEvent*]->(rle:ReactionLikeEvent),
(rle)-[:input|output|catalystActivity|disease|physicalEntity|regulatedBy|regulator|hasComponent|hasMember|hasCandidate*]->(pe:PhysicalEntity)
WHERE tlp.speciesName = "Homo sapiens" AND p.speciesName = "Homo sapiens" AND rle.speciesName = "Homo sapiens"
AND pe.stId = "R-HSA-74673"
RETURN DISTINCT pe.stId,  rle.stId AS Reaction, rle.displayName as ReactionDisplayName, p.stId AS Pathway, p.displayName AS PathwayDisplayName, tlp.stId as TopLevelPathwayStId, tlp.displayName as TopLevelPathwayDisplayName
ORDER BY Reaction
 */

class FinderTest {

    static ProteoformFormat pf;
    static ProteoformMatcher matcher;
    static Set<Proteoform> hitProteoforms;
    static SetMultimap<Proteoform, String> mapping;
    private TreeMultimap<Proteoform, Reaction> result;


    @BeforeAll
    static void setUp() {
    	pf = ProteoformFormat.SIMPLE;
        matcher = new ProteoformMatcherFlexible();
    }

    @BeforeEach
    void setEachUp() {
        hitProteoforms = new HashSet<>();
        mapping = HashMultimap.create();
    }

    @Test
    void searchOneReactionWithoutTopLevelPathwaysTest() {

        try {
            Proteoform proteoform = pf.getProteoform("P60880");
            mapping.put(proteoform, "R-HSA-5244499");
            result = Finder.search(mapping);
            assertEquals(1, result.values().size());    // One reaction

            Reaction expectedReaction = new Reaction("R-HSA-194818", "BoNT/A LC cleaves target cell SNAP25");
            assertTrue(result.containsValue(expectedReaction));
            for (Reaction reaction : result.values()) {
                assertEquals(4, reaction.getPathwaySet().size());
                assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-5250968", "Toxicity of botulinum toxin inputType A (BoNT/A)")));
                assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-168799", "Neurotoxicity of clostridium toxins")));
                assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-5339562", "Uptake and actions of bacterial toxins")));
                assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-5663205", "Infectious disease")));
                for (Pathway pathway : reaction.getPathwaySet()) {
                    assertEquals(0, pathway.getTopLevelPathwaySet().size());
                }
            }

        } catch (ParseException e) {
            fail("Proteoforms should be parsed correctly.");
        }
    }

    @Test
    void searchOneReactionWithTopLevelPathwaysTest() {

        try {
            Proteoform proteoform = pf.getProteoform("P60880");
            mapping.put(proteoform, "R-HSA-5244499");
            result = Finder.search(mapping);
            assertEquals(1, result.values().size());    // One reaction

            Reaction expectedReaction = new Reaction("R-HSA-194818", "BoNT/A LC cleaves target cell SNAP25");
            assertTrue(result.containsValue(expectedReaction));
            for (Reaction reaction : result.values()) {
                assertEquals(4, reaction.getPathwaySet().size());
                Pathway tlp = new Pathway("R-HSA-1643685", "Disease");
                assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-5250968", "Toxicity of botulinum toxin inputType A (BoNT/A)")));
                assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-168799", "Neurotoxicity of clostridium toxins")));
                assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-5339562", "Uptake and actions of bacterial toxins")));
                assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-5663205", "Infectious disease")));
                for (Pathway pathway : reaction.getPathwaySet()) {
                    assertEquals(1, pathway.getTopLevelPathwaySet().size());
                    assertTrue(pathway.getTopLevelPathwaySet().contains(new Pathway("R-HSA-1643685", "Disease")));
                }
            }
        } catch (ParseException e) {
            fail("Proteoforms should be parsed correctly.");
        }
    }

    @Test
    void searchWithTopLevelPathways2Test() {
        try {
            Proteoform proteoform = pf.getProteoform("P01308;00798:31,00798:43");
            mapping.put(proteoform, "R-HSA-429343");
            result = Finder.search(mapping);
            assertEquals(1, result.values().size());    // One reaction

            Reaction expectedReaction = new Reaction("R-HSA-977136", "Amyloid precursor proteins form ordered fibrils");
            assertTrue(result.containsValue(expectedReaction));
            for (Reaction reaction : result.values()) {
                assertEquals(1, reaction.getPathwaySet().size());
                Pathway tlp = new Pathway("R-HSA-392499", "Metabolism of proteins");
                assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-977225", "Amyloid fiber formation")));
                for (Pathway pathway : reaction.getPathwaySet()) {
                    assertEquals(1, pathway.getTopLevelPathwaySet().size());
                    assertTrue(pathway.getTopLevelPathwaySet().contains(new Pathway("R-HSA-392499", "Metabolism of proteins")));
                }
            }
        } catch (ParseException e) {
            fail("Proteoforms should be parsed correctly.");
        }
    }

    @Test
    void searchWithBrokenStId() {
        try {
            Proteoform proteoform = pf.getProteoform("P01308;00798:31,00798:43");
            mapping.put(proteoform, "R-HSA-111111");
            result = Finder.search(mapping);
            assertEquals(0, result.values().size());
        } catch (ParseException e) {
            fail("Proteoforms should be parsed correctly.");
        }
    }

    @Test
    void ewasMappingToMultipleReactions() {
        try {
            Proteoform proteoform = pf.getProteoform("P01308;00798:31,00798:43");
            mapping.put(proteoform, "R-HSA-74673");
            result = Finder.search(mapping);
            assertEquals(13, result.values().size());    // One reaction

            assertTrue(result.containsValue(new Reaction("R-HSA-74711", "Phosphorylation of IRS")));
            assertTrue(result.containsValue(new Reaction("R-HSA-110011", "Binding of Grb10 to the insulin receptor")));
            assertTrue(result.containsValue(new Reaction("R-HSA-265166", "Exocytosis of Insulin")));
            assertTrue(result.containsValue(new Reaction("R-HSA-422048", "Acyl Ghrelin and C-Ghrelin are secreted")));

            for (Reaction reaction : result.values()) {
                if (reaction.getStId().equals("R-HSA-422048")) {
                    assertEquals(2, reaction.getPathwaySet().size());

                    Pathway tlp = new Pathway("R-HSA-392499", "Metabolism of proteins");
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-422085", "Synthesis, secretion, and deacylation of Ghrelin")));
                    for (Pathway pathway : reaction.getPathwaySet()) {
                        assertEquals(1, pathway.getTopLevelPathwaySet().size());
                        assertTrue(pathway.getTopLevelPathwaySet().contains(tlp));
                    }
                }

                if (reaction.getStId().equals("R-HSA-74711")) {
                    assertEquals(4, reaction.getPathwaySet().size());

                    Pathway tlp = new Pathway("R-HSA-162582", "Signal Transduction");
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-74713", "IRS activation")));
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-74751", "Insulin receptor signalling cascade")));
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-74752", "Signaling by Insulin receptor")));
                    for (Pathway pathway : reaction.getPathwaySet()) {
                        assertEquals(1, pathway.getTopLevelPathwaySet().size());
                        assertTrue(pathway.getTopLevelPathwaySet().contains(tlp));
                    }
                }

                if (reaction.getStId().equals("R-HSA-265166")) {
                    assertEquals(2, reaction.getPathwaySet().size());

                    Pathway tlp = new Pathway("R-HSA-1430728", "Metabolism");
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-422356", "Regulation of insulin secretion")));
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-163685", "Integration of energy metabolism")));
                    for (Pathway pathway : reaction.getPathwaySet()) {
                        assertEquals(1, pathway.getTopLevelPathwaySet().size());
                        assertTrue(pathway.getTopLevelPathwaySet().contains(tlp));
                    }
                }

                if (reaction.getStId().equals("R-HSA-110011")) {
                    assertEquals(4, reaction.getPathwaySet().size());

                    Pathway tlp = new Pathway("R-HSA-162582", "Signal Transduction");
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-74749", "Signal attenuation")));
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-74751", "Insulin receptor signalling cascade")));
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-74752", "Signaling by Insulin receptor")));
                    assertTrue(reaction.getPathwaySet().contains(new Pathway("R-HSA-9006934", "Signaling by Receptor Tyrosine Kinases")));
                    for (Pathway pathway : reaction.getPathwaySet()) {
                        assertEquals(1, pathway.getTopLevelPathwaySet().size());
                        assertTrue(pathway.getTopLevelPathwaySet().contains(tlp));
                    }
                }
            }
        } catch (ParseException e) {
            fail("Proteoforms should be parsed correctly.");
        }
    }
}