package com.github.b4ndithelps.forge.client.genegraph;

import com.github.b4ndithelps.genetics.Gene;
import com.github.b4ndithelps.genetics.GeneCombinationRecipe;
import com.github.b4ndithelps.genetics.GeneRegistry;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.*;

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

    // Edge interaction state
    private Edge selectedEdge = null;
    private Edge hoveredEdge = null;

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

        // build degree counts
        Map<ResourceLocation, Integer> inCount = new HashMap<>();
        Map<ResourceLocation, Integer> outCount = new HashMap<>();
        for (Edge e : edges) {
            inCount.put(e.to(), inCount.getOrDefault(e.to(), 0) + 1);
            outCount.put(e.from(), outCount.getOrDefault(e.from(), 0) + 1);
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
            // Order within column by barycenter of incoming neighbors (reduces crossings and vertical stacking)
            Map<ResourceLocation, Integer> prevIndex = new HashMap<>();
            List<Node> prev = byDepth.getOrDefault(d - 1, List.of());
            for (int i2 = 0; i2 < prev.size(); i2++) prevIndex.put(prev.get(i2).id, i2);
            col.sort((a, bNode) -> {
                float aScore = 0f, bScore = 0f; int aCnt = 0, bCnt = 0;
                for (Edge e : edges) {
                    if (e.to().equals(a.id)) { Integer idx = prevIndex.get(e.from()); if (idx != null) { aScore += idx; aCnt++; } }
                    if (e.to().equals(bNode.id)) { Integer idx = prevIndex.get(e.from()); if (idx != null) { bScore += idx; bCnt++; } }
                }
                float aB = aCnt > 0 ? aScore / aCnt : Float.MAX_VALUE;
                float bB = bCnt > 0 ? bScore / bCnt : Float.MAX_VALUE;
                if (aB == bB) return a.id.toString().compareTo(bNode.id.toString());
                return Float.compare(aB, bB);
            });

            int baseX = PADDING + d * (NODE_WIDTH + COL_GAP);
            int subGap = Math.max(30, COL_GAP / 4); // keep within this column's width

            for (int i = 0; i < col.size(); i++) {
                Node n = col.get(i);
                int in = inCount.getOrDefault(n.id, 0);
                int out = outCount.getOrDefault(n.id, 0);
                // Sub-columns by role to reduce edge stacking
                int sub;
                if (in == 0 && out == 0) sub = 3;         // isolated far right
                else if (in == 0 && out > 0) sub = 0;      // pure source left
                else if (in > 0 && out > 0) sub = 1;       // both center-left
                else /* in > 0 && out == 0 */ sub = 2;     // pure sink center-right

                n.x = baseX + sub * subGap;
                n.y = PADDING + i * (NODE_HEIGHT + ROW_GAP);
            }

            // Ensure the incoming corridor has clearance from this column's left edges
            if (d > 0) {
                // Find the maximal lane midX for edges targeting this column
                int maxMidX = Integer.MIN_VALUE;
                for (Edge e : edges) {
                    Node src = nodes.get(e.from());
                    Node dst = nodes.get(e.to());
                    if (dst != null && dst.depth == d && src != null && src.depth == d - 1) {
                        int ax = Math.round(src.x + NODE_WIDTH);
                        int bx = Math.round(dst.x);
                        int mid = (ax + bx) / 2;
                        if (mid > maxMidX) maxMidX = mid;
                    }
                }
                if (maxMidX != Integer.MIN_VALUE) {
                    // Compute current minimum left edge in this column
                    int minLeft = Integer.MAX_VALUE;
                    for (Node n : col) minLeft = Math.min(minLeft, Math.round(n.x));
                    int CLEAR = 8; // pixels of clearance between lane and any node left edge
                    int requiredLeft = maxMidX + CLEAR;
                    if (minLeft < requiredLeft) {
                        float delta = requiredLeft - minLeft;
                        for (Node n : col) n.x += delta;
                    }
                }
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

        // Edge routing: unique vertical lanes, source/target port offsets, and label offsets
        Map<Edge, Integer> outYOffset = computeOutgoingYOffsetByEdge();
        Map<Edge, Integer> inYOffset = computeIncomingYOffsetByEdge();
        Map<Edge, Integer> midXByEdge = computeMidXByEdge();
        Map<Edge, Integer> labelYOffsetByEdge = computeLabelYOffsetByEdge(midXByEdge);

        // non-highlighted edges
        for (Edge e : edges) {
            if (e.equals(selectedEdge) || e.equals(hoveredEdge)) continue;
            Node a = nodes.get(e.from());
            Node b = nodes.get(e.to());
            if (a == null || b == null) continue;
            int ax = Math.round(a.x + NODE_WIDTH);
            int ay = Math.round(a.y + NODE_HEIGHT / 2f);
            int bx = Math.round(b.x);
            int by = Math.round(b.y + NODE_HEIGHT / 2f);
            int midX = midXByEdge.getOrDefault(e, (ax + bx) / 2);
            int color = mix(COLOR_EDGE, colorForEdge(e), 0.35f) & 0xCCFFFFFF;
            int sOff = outYOffset.getOrDefault(e, 0);
            int tOff = inYOffset.getOrDefault(e, 0);
            drawEdgeWithLabel(g, e, ax, ay, bx, by, midX, sOff, tOff, labelYOffsetByEdge.getOrDefault(e, 0), color, false);
        }
        // hovered then selected on top
        if (hoveredEdge != null) drawEdgeTop(g, hoveredEdge, midXByEdge, outYOffset, inYOffset, labelYOffsetByEdge, 0xFFE8D38A);
        if (selectedEdge != null) drawEdgeTop(g, selectedEdge, midXByEdge, outYOffset, inYOffset, labelYOffsetByEdge, 0xFFFFD36A);

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
            List<FormattedCharSequence> descLines = this.font.split(Component.literal(openGene.getDescription()), contentW);

            int lineH = this.font.lineHeight;
            int titleH = lineH + 2;
            int headerH = header.size() * lineH + 6;
            int descH = Math.max(lineH, descLines.size() * lineH);
			int mobsCount = openGene.getMobs() == null ? 0 : openGene.getMobs().size();
			int mobsH = mobsCount > 0 ? (lineH /*label*/ + mobsCount * lineH) : 0;
			modalH = 16 + titleH + headerH + descH + mobsH + innerPad * 2;
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
			// mobs list (if any)
			if (mobsCount > 0) {
				ty += 4;
				g.drawString(this.font, Component.literal("Mobs:"), tx, ty, COLOR_TEXT, false);
				ty += lineH;
				for (String mobIdOrTag : openGene.getMobs()) {
					String label = "- " + mobDisplayName(mobIdOrTag);
					g.drawString(this.font, label, tx, ty, secondary, false);
					ty += lineH;
					// stop if we run out of space
					if (ty >= modalY + modalH - innerPad) break;
				}
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

    private void drawEdgeTop(GuiGraphics g,
                              Edge e,
                              Map<Edge, Integer> midXByEdge,
                              Map<Edge, Integer> outYOffset,
                              Map<Edge, Integer> inYOffset,
                              Map<Edge, Integer> labelYOffsetByEdge,
                              int accent) {
        Node a = nodes.get(e.from());
        Node b = nodes.get(e.to());
        if (a == null || b == null) return;
        int ax = Math.round(a.x + NODE_WIDTH);
        int ay = Math.round(a.y + NODE_HEIGHT / 2f);
        int bx = Math.round(b.x);
        int by = Math.round(b.y + NODE_HEIGHT / 2f);
        int midX = midXByEdge.getOrDefault(e, (ax + bx) / 2);
        int edgeCol = mix(COLOR_EDGE, accent, 0.65f);
        int sOff = outYOffset.getOrDefault(e, 0);
        int tOff = inYOffset.getOrDefault(e, 0);
        drawEdgeWithLabel(g, e, ax, ay, bx, by, midX, sOff, tOff, labelYOffsetByEdge.getOrDefault(e, 0), edgeCol, true);
    }

    private void drawEdgeWithLabel(GuiGraphics g,
                                   Edge e,
                                   int ax, int ay,
                                   int bx, int by,
                                   int midX,
                                   int sOff,
                                   int tOff,
                                   int labelYOffset,
                                   int color,
                                   boolean strong) {
        // Source stub
        if (strong) thickVLine(g, ay, ay + sOff, ax, color); else vLine(g, ay, ay + sOff, ax, color);
        int sy = ay + sOff;
        // Horizontal to lane
        if (strong) thickHLine(g, ax, midX, sy, color); else hLine(g, ax, midX, sy, color);
        // Vertical lane to near target
        int tyOff = by + tOff;
        if (strong) thickVLine(g, sy, tyOff, midX, color); else vLine(g, sy, tyOff, midX, color);
        // Horizontal to target x at offset
        if (strong) thickHLine(g, midX, bx, tyOff, color); else hLine(g, midX, bx, tyOff, color);
        // Target stub
        if (strong) thickVLine(g, tyOff, by, bx, color); else vLine(g, tyOff, by, bx, color);

        if (e.minQ() > 0) {
            String label = "\u2265" + e.minQ() + "%";
            int textW = this.font.width(label);
            int lx = midX + 4;
            int ly = (by + tOff) - 8 - this.font.lineHeight / 2 + labelYOffset;
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

    private Map<Edge, Integer> computeMidXByEdge() {
        Map<Edge, Integer> midXByEdge = new HashMap<>();
        // Group edges by their base corridor (rounded mid between source/right and target/left)
        Map<Integer, List<Edge>> byCorridor = new HashMap<>();
        for (Edge e : edges) {
            Node a = nodes.get(e.from());
            Node b = nodes.get(e.to());
            if (a == null || b == null) continue;
            int ax = Math.round(a.x + NODE_WIDTH);
            int bx = Math.round(b.x);
            int baseMid = Math.round((ax + bx) / 2f);
            byCorridor.computeIfAbsent(baseMid, k -> new ArrayList<>()).add(e);
        }
        final int MID_SEP = 14; // separation between parallel lanes in the same corridor
        for (var entry : byCorridor.entrySet()) {
            int baseMid = entry.getKey();
            List<Edge> list = entry.getValue();
            // sort by the average of source/target y to keep lanes stable and reduce crossings
            list.sort(Comparator.comparingInt(e -> {
                Node a = nodes.get(e.from());
                Node b = nodes.get(e.to());
                int ay = Math.round(a.y + NODE_HEIGHT / 2f);
                int by = Math.round(b.y + NODE_HEIGHT / 2f);
                return (ay + by) / 2;
            }));
            int n = list.size();
            int start = -((n - 1) * MID_SEP) / 2;
            for (int i = 0; i < n; i++) {
                midXByEdge.put(list.get(i), baseMid + start + i * MID_SEP);
            }
        }
        return midXByEdge;
    }

    private Map<Edge, Integer> computeLabelYOffsetByEdge(Map<Edge, Integer> midXByEdge) {
        Map<Edge, Integer> offsets = new HashMap<>();
        Map<ResourceLocation, List<Edge>> byTarget = new HashMap<>();
        for (Edge e : edges) byTarget.computeIfAbsent(e.to(), k -> new ArrayList<>()).add(e);
        final int LABEL_SEP = 8;
        for (var entry : byTarget.entrySet()) {
            List<Edge> list = entry.getValue();
            list.sort(Comparator.comparingInt(e -> Math.round(nodes.get(e.from()).y + NODE_HEIGHT / 2f)));
            for (int i = 0; i < list.size(); i++) {
                offsets.put(list.get(i), ((i % 3) - 1) * LABEL_SEP); // cycle -sep, 0, +sep
            }
        }
        return offsets;
    }

    private Map<Edge, Integer> computeOutgoingYOffsetByEdge() {
        Map<Edge, Integer> off = new HashMap<>();
        Map<ResourceLocation, List<Edge>> bySource = new HashMap<>();
        for (Edge e : edges) bySource.computeIfAbsent(e.from(), k -> new ArrayList<>()).add(e);
        final int PORT_SEP = 6;
        int maxOff = (NODE_HEIGHT / 2) - 3;
        for (var entry : bySource.entrySet()) {
            List<Edge> list = entry.getValue();
            list.sort(Comparator.comparingInt(e -> Math.round(nodes.get(e.to()).y + NODE_HEIGHT / 2f)));
            int n = list.size();
            int start = -((n - 1) * PORT_SEP) / 2;
            for (int i = 0; i < n; i++) {
                int v = start + i * PORT_SEP;
                if (v < -maxOff) v = -maxOff; else if (v > maxOff) v = maxOff;
                off.put(list.get(i), v);
            }
        }
        return off;
    }

    private Map<Edge, Integer> computeIncomingYOffsetByEdge() {
        Map<Edge, Integer> off = new HashMap<>();
        Map<ResourceLocation, List<Edge>> byTarget = new HashMap<>();
        for (Edge e : edges) byTarget.computeIfAbsent(e.to(), k -> new ArrayList<>()).add(e);
        final int PORT_SEP = 6;
        int maxOff = (NODE_HEIGHT / 2) - 3;
        for (var entry : byTarget.entrySet()) {
            List<Edge> list = entry.getValue();
            list.sort(Comparator.comparingInt(e -> Math.round(nodes.get(e.from()).y + NODE_HEIGHT / 2f)));
            int n = list.size();
            int start = -((n - 1) * PORT_SEP) / 2;
            for (int i = 0; i < n; i++) {
                int v = start + i * PORT_SEP;
                if (v < -maxOff) v = -maxOff; else if (v > maxOff) v = maxOff;
                off.put(list.get(i), v);
            }
        }
        return off;
    }

    private int colorForEdge(Edge e) {
        int h = e.from().hashCode() * 31 + e.to().hashCode();
        int r = 160 + ((h >> 0) & 0x3F);
        int gC = 160 + ((h >> 6) & 0x3F);
        int b = 160 + ((h >> 12) & 0x3F);
        return (0xFF << 24) | (r << 16) | (gC << 8) | b;
    }
    private Node nodeAt(double mouseX, double mouseY) {
        float gx = (float)((mouseX - panX) / zoom);
        float gy = (float)((mouseY - panY) / zoom);
        for (Node n : nodes.values()) {
            if (gx >= n.x && gx <= n.x + NODE_WIDTH && gy >= n.y && gy <= n.y + NODE_HEIGHT) return n;
        }
        return null;
    }

    private Edge edgeAt(double mouseX, double mouseY) {
        float gx = (float)((mouseX - panX) / zoom);
        float gy = (float)((mouseY - panY) / zoom);
        Map<Edge, Integer> midXByEdge = computeMidXByEdge();
        Map<Edge, Integer> outYOffset = computeOutgoingYOffsetByEdge();
        Map<Edge, Integer> inYOffset = computeIncomingYOffsetByEdge();
        final float tol = 3.5f;
        for (Edge e : edges) {
            Node a = nodes.get(e.from());
            Node b = nodes.get(e.to());
            if (a == null || b == null) continue;
            float ax = a.x + NODE_WIDTH;
            float ay = a.y + NODE_HEIGHT / 2f;
            float bx = b.x;
            float by = b.y + NODE_HEIGHT / 2f;
            float mx = midXByEdge.getOrDefault(e, Math.round((ax + bx) / 2f));
            float sy = ay + outYOffset.getOrDefault(e, 0);
            float ty = by + inYOffset.getOrDefault(e, 0);
            if (nearSegment(gx, gy, ax, ay, ax, sy, tol)) return e; // source stub
            if (nearSegment(gx, gy, ax, sy, mx, sy, tol)) return e; // to lane
            if (nearSegment(gx, gy, mx, sy, mx, ty, tol)) return e; // lane
            if (nearSegment(gx, gy, mx, ty, bx, ty, tol)) return e; // to target x
            if (nearSegment(gx, gy, bx, ty, bx, by, tol)) return e; // target stub
        }
        return null;
    }

    private boolean nearSegment(float px, float py, float x1, float y1, float x2, float y2, float tol) {
        if (Math.abs(y1 - y2) < 1e-3) {
            float minx = Math.min(x1, x2), maxx = Math.max(x1, x2);
            if (px >= minx - tol && px <= maxx + tol && Math.abs(py - y1) <= tol) return true;
            return false;
        }
        if (Math.abs(x1 - x2) < 1e-3) {
            float miny = Math.min(y1, y2), maxy = Math.max(y1, y2);
            if (py >= miny - tol && py <= maxy + tol && Math.abs(px - x1) <= tol) return true;
            return false;
        }
        float vx = x2 - x1, vy = y2 - y1;
        float wx = px - x1, wy = py - y1;
        float c1 = vx * wx + vy * wy;
        if (c1 <= 0) return (wx * wx + wy * wy) <= tol * tol;
        float c2 = vx * vx + vy * vy;
        if (c2 <= c1) {
            float dx = px - x2, dy = py - y2;
            return (dx * dx + dy * dy) <= tol * tol;
        }
        float b = c1 / c2;
        float projx = x1 + b * vx, projy = y1 + b * vy;
        float dx = px - projx, dy = py - projy;
        return (dx * dx + dy * dy) <= tol * tol;
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
	
	private String mobDisplayName(String idOrTag) {
		if (idOrTag == null || idOrTag.isEmpty()) return "";
		int idx = idOrTag.indexOf(':');
		if (idx > 0) {
			// Try to use standard entity translation key: entity.<ns>.<path>
			String key = "entity." + idOrTag.substring(0, idx) + "." + idOrTag.substring(idx + 1);
			String translated = Component.translatable(key).getString();
			// If translation not present, fall back to raw id
			return translated != null && !translated.equals(key) ? translated : idOrTag;
		}
		// Tags or custom categories (e.g., "humanoid")
		return idOrTag;
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
            Edge edgeHit = edgeAt(mouseX, mouseY);
            if (edgeHit != null) {
                selectedEdge = edgeHit;
                return true;
            }
            Node hit = nodeAt(mouseX, mouseY);
            if (hit != null) {
                openGene = hit.gene;
                return true;
            }
            selectedEdge = null;
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
        hoveredEdge = edgeAt(mouseX, mouseY);
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

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (openGene != null) return;
        hoveredEdge = edgeAt(mouseX, mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    private static float clamp(float v, float lo, float hi) { return v < lo ? lo : (Math.min(v, hi)); }

    @Override
    public boolean isPauseScreen() { return false; }
}