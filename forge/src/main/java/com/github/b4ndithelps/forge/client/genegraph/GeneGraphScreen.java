package com.github.b4ndithelps.forge.client.genegraph;

import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneCombinationRecipe;
import com.github.b4ndithelps.genetics.GeneRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple pannable/zoomable DAG-like view of all genes and their combination paths.
 */
public class GeneGraphScreen extends Screen {
    private static final int NODE_WIDTH = 120;
    private static final int NODE_HEIGHT = 26;
    private static final int COL_GAP = 140;
    private static final int ROW_GAP = 32;
    private static final int PADDING = 16;

    private static final int COLOR_BG = 0xCC0F1115; // translucent dark
    private static final int COLOR_PANEL = 0xD0171A21;
    private static final int COLOR_TEXT = 0xFFE6E6E6;
    private static final int COLOR_EDGE = 0xFF3A4458;

    private static final int COLOR_COSMETIC = 0xFFE3F2FD;
    private static final int COLOR_RESISTANCE = 0xFFE8F5E9;
    private static final int COLOR_BUILDER = 0xFFFFFDE7;
    private static final int COLOR_LOWEND = 0xFFE6CCFF;
    private static final int COLOR_QUIRK = 0xFFFCE4EC;
    private static final int COLOR_UNKNOWN = 0xFFECEFF1;

    private float panX = 0f;
    private float panY = 0f;
    private float zoom = 1.0f;
    private boolean dragging = false;
    private double lastMouseX = 0;
    private double lastMouseY = 0;

    private final Map<ResourceLocation, Node> nodes = new HashMap<>();
    private final List<Edge> edges = new ArrayList<>();

    // Modal details state
    private Gene openGene = null;
    private int modalX, modalY, modalW, modalH;
    private int closeL, closeT, closeR, closeB;

    private static final class Node {
        final ResourceLocation id;
        final Gene gene;
        int depth;
        float x;
        float y;
        Node(ResourceLocation id, Gene gene) { this.id = id; this.gene = gene; }
    }
    private record Edge(ResourceLocation from, ResourceLocation to, int minQ) {}

    public GeneGraphScreen() {
        super(Component.literal("Gene Graph"));
    }

    @Override
    protected void init() {
        super.init();
        buildGraph();
        layoutGraph();
        // Start centered-ish
        panX = this.width * 0.5f - 200f;
        panY = this.height * 0.5f - 120f;
    }

    private void buildGraph() {
        nodes.clear();
        edges.clear();

        for (Gene g : GeneRegistry.all()) {
            nodes.put(g.getId(), new Node(g.getId(), g));
        }

        // ensure builder nodes present (already are as category builder in registry)
        List<ResourceLocation> builderOrder = ClientGeneGraphState.getBuilderOrder();

        for (Gene g : GeneRegistry.all()) {
            GeneCombinationRecipe recipe = g.getCombinationRecipe();
            if (recipe == null) continue;
            // explicit reqs
            for (GeneCombinationRecipe.Requirement req : recipe.getExplicitRequirements()) {
                edges.add(new Edge(req.getGeneId(), g.getId(), Math.max(0, req.getMinQuality())));
            }
            // builders: connect first N in world order
            int count = Math.max(0, recipe.getBuilderCount());
            for (int i = 0; i < Math.min(count, builderOrder.size()); i++) {
                edges.add(new Edge(builderOrder.get(i), g.getId(), Math.max(0, recipe.getBuilderMinQuality())));
            }
        }
    }

    private void layoutGraph() {
        // compute depth by longest path from sources
        Map<ResourceLocation, Integer> depthMemo = new HashMap<>();
        Set<ResourceLocation> visiting = new HashSet<>();

        // build reverse adjacency (incoming)
        Map<ResourceLocation, List<ResourceLocation>> incoming = new HashMap<>();
        for (Edge e : edges) {
            incoming.computeIfAbsent(e.to(), k -> new ArrayList<>()).add(e.from());
        }

        for (Node n : nodes.values()) {
            n.depth = computeDepth(n.id, incoming, depthMemo, visiting);
        }

        // group by depth
        Map<Integer, List<Node>> byDepth = new HashMap<>();
        for (Node n : nodes.values()) byDepth.computeIfAbsent(n.depth, k -> new ArrayList<>()).add(n);
        int maxDepth = byDepth.keySet().stream().mapToInt(Integer::intValue).max().orElse(0);

        for (int d = 0; d <= maxDepth; d++) {
            List<Node> col = byDepth.getOrDefault(d, List.of());
            col.sort(Comparator.comparing(a -> a.id.toString()));
            for (int i = 0; i < col.size(); i++) {
                Node n = col.get(i);
                n.x = PADDING + d * (NODE_WIDTH + COL_GAP);
                n.y = PADDING + i * (NODE_HEIGHT + ROW_GAP);
            }
        }
    }

    private int computeDepth(ResourceLocation id,
                              Map<ResourceLocation, List<ResourceLocation>> incoming,
                              Map<ResourceLocation, Integer> memo,
                              Set<ResourceLocation> visiting) {
        Integer cached = memo.get(id);
        if (cached != null) return cached;
        if (!incoming.containsKey(id)) { memo.put(id, 0); return 0; }
        if (!visiting.add(id)) { memo.put(id, 0); return 0; } // cycle guard -> treat as source
        int best = 0;
        for (ResourceLocation src : incoming.getOrDefault(id, List.of())) {
            best = Math.max(best, 1 + computeDepth(src, incoming, memo, visiting));
        }
        visiting.remove(id);
        memo.put(id, best);
        return best;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Dim background
        g.fill(0, 0, this.width, this.height, COLOR_BG);

        // Panel
        int panelX = 8, panelY = 8, panelW = this.width - 16, panelH = this.height - 16;
        g.fill(panelX, panelY, panelX + panelW, panelY + panelH, COLOR_PANEL);

        // Title
        g.drawString(this.font, this.title, panelX + 8, panelY + 6, COLOR_TEXT);

        // Graph (clip to panel bounds)
        g.enableScissor(panelX, panelY, panelX + panelW, panelY + panelH);
        g.pose().pushPose();
        g.pose().translate(panX, panY, 0);
        g.pose().scale(zoom, zoom, 1f);

        // Edges first
        for (Edge e : edges) {
            Node a = nodes.get(e.from());
            Node b = nodes.get(e.to());
            if (a == null || b == null) continue;
            int ax = Math.round(a.x + NODE_WIDTH);
            int ay = Math.round(a.y + NODE_HEIGHT / 2f);
            int bx = Math.round(b.x);
            int by = Math.round(b.y + NODE_HEIGHT / 2f);
            int midX = (ax + bx) / 2;
            // L-shape: a -> midX, then vertical to by, then -> b
            thickHLine(g, ax, midX, ay, COLOR_EDGE);
            thickVLine(g, ay, by, midX, COLOR_EDGE);
            thickHLine(g, midX, bx, by, COLOR_EDGE);

            // Edge label for min quality if applicable
            if (e.minQ() > 0) {
                String label = "\u2265" + e.minQ() + "%"; // ≥Q%
                int textW = this.font.width(label);
                int lx = midX + 4;
                int ly = by - 8 - this.font.lineHeight / 2;
                // small pill background for readability
                int padX = 3, padY = 2;
                int bgL = lx - padX - 1;
                int bgT = ly - padY;
                int bgR = lx + textW + padX + 1;
                int bgB = ly + this.font.lineHeight + padY - 2;
                g.fill(bgL, bgT, bgR, bgB, 0xCC0E1117);
                hLine(g, bgL, bgR, bgT, 0xFF2A3140);
                hLine(g, bgL, bgR, bgB, 0xFF2A3140);
                vLine(g, bgT, bgB, bgL, 0xFF2A3140);
                vLine(g, bgT, bgB, bgR, 0xFF2A3140);
                g.drawString(this.font, label, lx, ly, 0xFFBFD7FF, false);
            }
        }

        // Nodes
        for (Node n : nodes.values()) {
            int x = Math.round(n.x);
            int y = Math.round(n.y);
            int w = NODE_WIDTH;
            int h = NODE_HEIGHT;
            int fill = switch (safeCat(n.gene)) {
                case cosmetic -> COLOR_COSMETIC;
                case resistance -> COLOR_RESISTANCE;
                case builder -> COLOR_BUILDER;
                case lowend -> COLOR_LOWEND;
                case quirk -> COLOR_QUIRK;
                default -> COLOR_UNKNOWN;
            };
            // gradient for nicer look
            int top = mix(fill, 0xFFEFF3F8, 0.14f);
            int bot = mix(fill, 0xFFCAD3E1, 0.08f);
            g.fillGradient(x, y, x + w, y + h, top, bot);
            // outline
            hLine(g, x, x + w, y, 0xFF2A3140);
            hLine(g, x, x + w, y + h, 0xFF2A3140);
            vLine(g, y, y + h, x, 0xFF2A3140);
            vLine(g, y, y + h, x + w, 0xFF2A3140);
            // label
            String text = labelOf(n.gene.getId());
            int tx = x + 6;
            int ty = y + (h - this.font.lineHeight) / 2;
            g.drawString(this.font, text, tx, ty, 0xFF0D0F14, false);
        }

        g.pose().popPose();
        g.disableScissor();

        super.render(g, mouseX, mouseY, partialTick);

        // Modal gene details overlay
        if (openGene != null) {
            // dim backdrop slightly
            g.fill(0, 0, this.width, this.height, 0x990F1115);

            // layout
            modalW = Math.min(360, this.width - 48);
            int innerPad = 10;
            Component titleC = Component.literal(labelOf(openGene.getId()));
            Component catC = Component.literal("Category: " + String.valueOf(openGene.getCategory()));
            Component rarC = Component.literal("Rarity: " + String.valueOf(openGene.getRarity()));
            Component combC = Component.literal("Combinable: " + (openGene.isCombinable() ? "Yes" : "No"));
            Component qualC = Component.literal("Quality: " + openGene.getQualityMin() + " - " + openGene.getQualityMax());
            List<Component> header = List.of(catC, rarC, combC, qualC);

            int contentW = modalW - innerPad * 2;
            List<net.minecraft.util.FormattedCharSequence> descLines = this.font.split(Component.literal(openGene.getDescription()), contentW);

            int lineH = this.font.lineHeight;
            int titleH = lineH + 2;
            int headerH = header.size() * lineH + 6;
            int descH = Math.max(lineH, descLines.size() * lineH);
            modalH = 16 + titleH + headerH + descH + innerPad * 2;
            modalH = Math.min(modalH, this.height - 48); // cap at screen height minus margins
            modalX = (this.width - modalW) / 2;
            modalY = (this.height - modalH) / 2;

            // panel styled to match graph panel
            int gradTop = mix(COLOR_PANEL, 0xFF202531, 0.10f);
            int gradBot = mix(COLOR_PANEL, 0xFF0B0E14, 0.18f);
            g.fillGradient(modalX, modalY, modalX + modalW, modalY + modalH, gradTop, gradBot);
            hLine(g, modalX, modalX + modalW, modalY, COLOR_EDGE);
            hLine(g, modalX, modalX + modalW, modalY + modalH, COLOR_EDGE);
            vLine(g, modalY, modalY + modalH, modalX, COLOR_EDGE);
            vLine(g, modalY, modalY + modalH, modalX + modalW, COLOR_EDGE);

            // close button (X)
            int closeSize = 12;
            closeR = modalX + modalW - innerPad;
            closeL = closeR - closeSize;
            closeT = modalY + innerPad;
            closeB = closeT + closeSize;
            int btnBg = 0x663A4458;
            g.fill(closeL, closeT, closeR, closeB, btnBg);
            hLine(g, closeL, closeR, closeT, COLOR_EDGE);
            hLine(g, closeL, closeR, closeB, COLOR_EDGE);
            vLine(g, closeT, closeB, closeL, COLOR_EDGE);
            vLine(g, closeT, closeB, closeR, COLOR_EDGE);
            String xStr = "x";
            int xW = this.font.width(xStr);
            int xX = closeL + (closeSize - xW) / 2;
            int xY = closeT + (closeSize - this.font.lineHeight) / 2 + 1;
            g.drawString(this.font, xStr, xX, xY, COLOR_TEXT, false);

            // title
            int tx = modalX + innerPad;
            int ty = modalY + innerPad;
            g.drawString(this.font, titleC, tx, ty, COLOR_TEXT, false);
            ty += titleH;
            // underline
            hLine(g, modalX + innerPad, modalX + modalW - innerPad, ty - 2, 0xFF3A4458);

            // header rows
            int secondary = 0xFFBFD7FF;
            for (Component c : header) {
                g.drawString(this.font, c, tx, ty, secondary, false);
                ty += lineH;
            }
            ty += 4;

            // description label and text
            g.drawString(this.font, Component.literal("Description:"), tx, ty, COLOR_TEXT, false);
            ty += lineH;
            int descBottom = Math.min(ty + descH, modalY + modalH - innerPad);
            int drawLines = Math.max(0, (descBottom - ty) / lineH);
            for (int i = 0; i < drawLines && i < descLines.size(); i++) {
                g.drawString(this.font, descLines.get(i), tx, ty, secondary);
                ty += lineH;
            }
        }
    }

    private void hLine(GuiGraphics g, int x1, int x2, int y, int color) {
        if (x2 < x1) { int t = x1; x1 = x2; x2 = t; }
        g.fill(x1, y, x2, y + 1, color);
    }
    private void vLine(GuiGraphics g, int y1, int y2, int x, int color) {
        if (y2 < y1) { int t = y1; y1 = y2; y2 = t; }
        g.fill(x, y1, x + 1, y2, color);
    }

    private void thickHLine(GuiGraphics g, int x1, int x2, int y, int color) {
        if (x2 < x1) { int t = x1; x1 = x2; x2 = t; }
        g.fill(x1, y - 1, x2, y + 2, color);
    }
    private void thickVLine(GuiGraphics g, int y1, int y2, int x, int color) {
        if (y2 < y1) { int t = y1; y1 = y2; y2 = t; }
        g.fill(x - 1, y1, x + 2, y2, color);
    }

    private Node nodeAt(double mouseX, double mouseY) {
        float gx = (float)((mouseX - panX) / zoom);
        float gy = (float)((mouseY - panY) / zoom);
        for (Node n : nodes.values()) {
            if (gx >= n.x && gx <= n.x + NODE_WIDTH && gy >= n.y && gy <= n.y + NODE_HEIGHT) return n;
        }
        return null;
    }

    private Gene.Category safeCat(Gene g) {
        try { return g.getCategory(); } catch (Throwable t) { return null; }
    }

    private String labelOf(ResourceLocation id) {
        // Resolve via Minecraft's translation system so resource packs/dev env work
        return Component.translatable(id.toString()).getString();
    }

    private int mix(int argbA, int argbB, float t) {
        t = t < 0 ? 0 : (t > 1 ? 1 : t);
        int aA = (argbA >>> 24) & 0xFF, rA = (argbA >>> 16) & 0xFF, gA = (argbA >>> 8) & 0xFF, bA = (argbA) & 0xFF;
        int aB = (argbB >>> 24) & 0xFF, rB = (argbB >>> 16) & 0xFF, gB = (argbB >>> 8) & 0xFF, bB = (argbB) & 0xFF;
        int a = Math.round(aA * (1 - t) + aB * t);
        int r = Math.round(rA * (1 - t) + rB * t);
        int gC = Math.round(gA * (1 - t) + gB * t);
        int b = Math.round(bA * (1 - t) + bB * t);
        return (a << 24) | (r << 16) | (gC << 8) | b;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // If modal is open, handle close button only
        if (openGene != null) {
            if (button == 0) {
                if (mouseX >= closeL && mouseX <= closeR && mouseY >= closeT && mouseY <= closeB) {
                    openGene = null;
                    return true;
                }
            }
            return true; // consume clicks while modal open
        }

        if (button == 0) {
            Node hit = nodeAt(mouseX, mouseY);
            if (hit != null) {
                openGene = hit.gene;
                return true;
            }
        }
        this.dragging = true;
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (openGene != null) return true;
        if (this.dragging) {
            this.panX += (float)(mouseX - lastMouseX);
            this.panY += (float)(mouseY - lastMouseY);
            this.lastMouseX = mouseX;
            this.lastMouseY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (openGene != null) return true;
        float old = this.zoom;
        this.zoom = clamp(this.zoom + (float)delta * 0.1f, 0.5f, 2.0f);
        // optional: zoom towards cursor (simple implementation)
        if (Math.abs(this.zoom - old) > 1e-3) {
            float cx = (float)mouseX - panX;
            float cy = (float)mouseY - panY;
            float scale = this.zoom / old;
            panX = (float)mouseX - cx * scale;
            panY = (float)mouseY - cy * scale;
        }
        return true;
    }

    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : (v > hi ? hi : v); }

    @Override
    public boolean isPauseScreen() { return false; }
}


