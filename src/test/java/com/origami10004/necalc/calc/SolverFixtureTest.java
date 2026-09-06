package com.origami10004.necalc.calc;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.origami10004.necalc.data.RecipeEntry;
import com.origami10004.necalc.data.ProductionStep;
import com.origami10004.necalc.data.ingredient.Ingredients;
import org.junit.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/** Loads the same three files users can provide, without Minecraft registries. */
public class SolverFixtureTest {
    @Test
    public void solvesFixtureWithUnknownItems() {
        Fixture fixture = Fixture.load("solver/basic");
        Solver.Result result = Solver.solve(fixture.targets, fixture.recipes, fixture.machineSpeeds);

        assertFalse(result.steps.isEmpty());
        ProductionStep step = result.steps.get(0);
        assertEquals("item:bugreport:finished_product:0:1.0", step.getPrimaryOutput().serialize());
        assertEquals(0.05, step.getMachineCount(), 1e-9);
        assertEquals(1.0, result.inputRates.get(Fixture.ingredient("item:bugreport:raw_material:0:1.0")).rate, 1e-9);
    }

    @Test
    public void solvesNcFissionFixtureWithUnknownItems() {
        Fixture fixture = Fixture.load("solver/NC fission");
        Solver.Result result = Solver.solve(fixture.targets, fixture.recipes, fixture.machineSpeeds);

        assertFalse("NC Fission fixture produced no production steps", result.steps.isEmpty());
        assertFalse("NC Fission fixture produced no input rates", result.inputRates.isEmpty());
        for (Solver.Input input : result.inputRates.values()) {
            assertFalse(Double.isNaN(input.rate));
            assertFalse(Double.isInfinite(input.rate));
        }
    }

    private static class Fixture {
        final List<Ingredients> targets;
        final List<RecipeEntry> recipes;
        final Map<Ingredients, Integer> machineSpeeds;

        private Fixture(List<Ingredients> targets, List<RecipeEntry> recipes,
                        Map<Ingredients, Integer> machineSpeeds) {
            this.targets = targets;
            this.recipes = recipes;
            this.machineSpeeds = machineSpeeds;
        }

        static Fixture load(String folder) {
            JsonObject targetJson = read(folder + "/targets.json");
            JsonObject recipeJson = read(folder + "/recipes.json");
            JsonObject machineJson = read(folder + "/machines.json");

            List<Ingredients> targets = new ArrayList<>();
            for (String value : strings(targetJson.getAsJsonArray("targets"))) {
                targets.add(ingredient(value));
            }

            List<RecipeEntry> recipes = new ArrayList<>();
            for (int i = 0; i < recipeJson.getAsJsonArray("recipes").size(); i++) {
                JsonObject json = recipeJson.getAsJsonArray("recipes").get(i).getAsJsonObject();
                ArrayList<Ingredients> inputs = ingredients(json.getAsJsonArray("inputs"));
                ArrayList<Ingredients> outputs = ingredients(json.getAsJsonArray("outputs"));
                recipes.add(new RecipeEntry(inputs, ingredient(json.get("machine").getAsString()),
                        outputs, json.get("time").getAsInt()));
            }

            Map<Ingredients, Integer> speeds = new HashMap<>();
            for (String value : strings(machineJson.getAsJsonArray("machines"))) {
                Ingredients machine = ingredient(value);
                speeds.put(machine, (int) machine.getValue());
            }
            return new Fixture(targets, recipes, speeds);
        }

        private static ArrayList<Ingredients> ingredients(JsonArray array) {
            ArrayList<Ingredients> result = new ArrayList<>();
            for (String value : strings(array)) result.add(ingredient(value));
            return result;
        }

        private static List<String> strings(JsonArray array) {
            List<String> result = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) result.add(array.get(i).getAsString());
            return result;
        }

        private static JsonObject read(String path) {
            InputStreamReader reader = new InputStreamReader(
                    SolverFixtureTest.class.getClassLoader().getResourceAsStream(path), StandardCharsets.UTF_8);
            return new JsonParser().parse(reader).getAsJsonObject();
        }

        private static Ingredients ingredient(String serialized) {
            return new FixtureIngredient(serialized);
        }
    }

    private static class FixtureIngredient extends Ingredients {
        private final String identity;
        private final String serialized;

        FixtureIngredient(String serialized) {
            super(parseValue(serialized));
            this.serialized = serialized;
            this.identity = parseIdentity(serialized);
        }

        private static double parseValue(String serialized) {
            try {
                if (serialized.startsWith("item:")) {
                    String[] parts = serialized.split(":", 6);
                    return Double.parseDouble(parts[4]);
                }
                if (serialized.startsWith("fluid:")) {
                    String rest = serialized.substring(6);
                    return Double.parseDouble(rest.substring(rest.lastIndexOf('|') + 1).split(":", 2)[0]);
                }
                return Double.parseDouble(serialized.substring(serialized.lastIndexOf(':') + 1));
            } catch (Exception exception) {
                throw new IllegalArgumentException("Invalid fixture ingredient: " + serialized, exception);
            }
        }

        private static String parseIdentity(String serialized) {
            if (serialized.startsWith("item:")) {
                String[] parts = serialized.split(":", 6);
                return parts[0] + ":" + parts[1] + ":" + parts[2] + ":" + parts[3]
                        + (parts.length > 5 ? ":" + parts[5] : "");
            }
            if (serialized.startsWith("fluid:")) {
                String rest = serialized.substring(6);
                int pipe = rest.lastIndexOf('|');
                return "fluid:" + rest.substring(0, pipe)
                        + (rest.substring(pipe + 1).contains(":")
                        ? ":" + rest.substring(pipe + 1).split(":", 2)[1] : "");
            }
            return serialized.substring(0, serialized.lastIndexOf(':'));
        }

        @Override public void render(com.origami10004.necalc.gui.GuiCommon parent, int x, int y) { }
        @Override public String getDisplayName() { return identity; }
        @Override public List<String> getTooltip(net.minecraft.client.Minecraft mc) { return Collections.emptyList(); }
        @Override public String formatValue(double value) { return Double.toString(value); }
        @Override public String serialize() { return serialized; }
        @Override public Ingredients copy() { return new FixtureIngredient(serialized); }
        @Override public Object getStack() { return null; }

        @Override public boolean equals(Object object) {
            return object instanceof FixtureIngredient && identity.equals(((FixtureIngredient) object).identity);
        }

        @Override public int hashCode() { return identity.hashCode(); }
    }
}
