package com.github.b4ndithelps.genetics;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Produces a Mermaid graph and simple HTML page visualizing gene combination relationships.
 */
public final class GeneGraphGenerator {
    private GeneGraphGenerator() {}

    public static void writeGraphHtml(Path outputDir, MinecraftServer server) throws IOException {
        Files.createDirectories(outputDir);
        String mermaid = buildMermaid(server);
        String html = wrapInHtml(mermaid);
        Files.writeString(outputDir.resolve("gene-flowchart.html"), html, StandardCharsets.UTF_8);
        Files.writeString(outputDir.resolve("gene-flowchart.mmd"), mermaid, StandardCharsets.UTF_8);
    }

    private static String buildMermaid(MinecraftServer server) {
        StringBuilder sb = new StringBuilder();
        sb.append("graph TD\n");

        // nodes
        for (Gene gene : GeneRegistry.all()) {
            String id = safe(gene.getId());
            String label = labelFor(gene.getId());
            sb.append(id).append("[").append(label).append("]\n");
        }

        // edges
        for (Gene gene : GeneRegistry.all()) {
            if (gene.getCombinationRecipe() == null) continue;
            ResourceLocation target = gene.getId();
            for (GeneCombinationRecipe.Requirement req : gene.getCombinationRecipe().getExplicitRequirements()) {
                sb.append(safe(req.getGeneId())).append(" --> ").append(safe(target)).append("\n");
            }
            int builderCount = gene.getCombinationRecipe().getBuilderCount();
            if (builderCount > 0) {
                // Use world-specific builder genes; we show the first N in order
                List<ResourceLocation> builderIds = com.github.b4ndithelps.genetics.GeneCombinationService.getOrCreateBuilderGenes(server);
                for (int i = 0; i < Math.min(builderCount, builderIds.size()); i++) {
                    ResourceLocation bId = builderIds.get(i);
                    String bNode = safe(bId);
                    sb.append(bNode).append("[").append(labelFor(bId)).append("]\n");
                    sb.append(bNode).append(" -- min ").append(gene.getCombinationRecipe().getBuilderMinQuality()).append(" --> ").append(safe(target)).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private static String wrapInHtml(String mermaidSource) {
        String header = """
                <!DOCTYPE html>
                <html><head><meta charset="utf-8"><title>Gene Flowchart</title>
                <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
                <script>mermaid.initialize({ startOnLoad: true });</script>
                </head><body><div class="mermaid">
                """;
        String footer = """
                </div></body></html>
                """;
        return header + mermaidSource.replace("<", "&lt;") + footer;
    }

    private static String safe(ResourceLocation id) {
        return id.getNamespace().replaceAll("[^a-z0-9_]", "_") + "_" + id.getPath().replaceAll("[^a-z0-9_]", "_");
    }

    private static String labelFor(ResourceLocation id) {
        String key = id.toString();
        String translated = TranslationHelper.getEnUs().getOrDefault(key, key);
        return translated.replace(":", "\\n");
    }
}


