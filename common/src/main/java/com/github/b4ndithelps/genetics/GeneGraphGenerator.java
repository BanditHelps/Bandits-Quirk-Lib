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
        sb.append("flowchart TD\n");
        // Visual styling for categories
        sb.append("classDef cosmetic fill:#E3F2FD,stroke:#1E88E5,stroke-width:2px,color:#0D47A1;\n");
        sb.append("classDef resistance fill:#E8F5E9,stroke:#43A047,stroke-width:2px,color:#1B5E20;\n");
        sb.append("classDef builder fill:#FFFDE7,stroke:#F9A825,stroke-width:2px,color:#F57F17;\n");
        sb.append("classDef quirk fill:#FCE4EC,stroke:#D81B60,stroke-width:2px,color:#880E4F;\n");
        sb.append("classDef unknown fill:#ECEFF1,stroke:#607D8B,stroke-width:1.5px,color:#263238;\n");

        // nodes
        for (Gene gene : GeneRegistry.all()) {
            String id = safe(gene.getId());
            String label = labelFor(gene.getId());
            sb.append(id).append("[").append(label).append("]\n");
            String cls;
            try {
                Gene.Category cat = gene.getCategory();
                cls = cat == null ? "unknown" : switch (cat) {
                    case cosmetic -> "cosmetic";
                    case resistance -> "resistance";
                    case builder -> "builder";
                    case quirk -> "quirk";
                };
            } catch (Throwable t) { cls = "unknown"; }
            sb.append("class ").append(id).append(' ').append(cls).append(";\n");
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
                    sb.append(bNode).append(" -- |min ")
                            .append(gene.getCombinationRecipe().getBuilderMinQuality())
                            .append("| --> ")
                            .append(safe(target)).append("\n");
                }
            }
        }

        return sb.toString();
    }

    private static String wrapInHtml(String mermaidSource) {
        String header = """
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Gene Flowchart</title>
                <style>
                :root { --bg:#0f1115; --panel:#171a21; --text:#e6e6e6; --muted:#9aa4ad; --accent:#4FC3F7; }
                html, body { margin:0; padding:0; height:100%; background:var(--bg); color:var(--text); font-family:system-ui,-apple-system,Segoe UI,Roboto,Ubuntu,Inter,sans-serif; }
                #toolbar { position:sticky; top:0; z-index:10; display:flex; gap:.5rem; align-items:center; padding:.6rem .8rem; background:linear-gradient(180deg, rgba(23,26,33,0.97), rgba(23,26,33,0.92)); border-bottom:1px solid #222835; backdrop-filter: blur(6px); }
                #toolbar input[type="text"] { flex:1 1 360px; min-width:200px; max-width:60vw; padding:.5rem .7rem; background:#0e1117; color:var(--text); border:1px solid #2a3140; border-radius:8px; outline:none; }
                #toolbar input[type="text"]::placeholder { color:#7b8794; }
                #toolbar .btn { padding:.45rem .7rem; background:#0e1117; color:var(--text); border:1px solid #2a3140; border-radius:8px; cursor:pointer; }
                #toolbar .btn:hover { border-color:#3a4458; background:#121620; }
                #toolbar label { color:var(--muted); display:flex; align-items:center; gap:.35rem; }
                #main { display:flex; height:calc(100vh - 56px); min-height:300px; }
                #sidebar { width:320px; max-width:40vw; border-left:1px solid #222835; background:#0e1117; padding:.75rem .9rem; overflow:auto; display:none; }
                #sidebar.open { display:block; }
                #sidebar h3 { margin:.2rem 0 .6rem; font-size:1rem; color:#dcdcdc; }
                #sidebar .meta { color:var(--muted); font-size:.9rem; }
                #sidebar .actions { display:flex; gap:.4rem; margin:.6rem 0; }
                #graph-container { position:relative; flex:1; }
                #graph { position:absolute; inset:0; }
                #graph .mermaid { width:100%; height:100%; }
                #graph svg { width:100%; height:100%; background:var(--bg); }
                .node.match > rect, .node.match > circle, .node.match > polygon { stroke-width:3px !important; filter: drop-shadow(0 0 6px rgba(79,195,247,0.6)); }
                .node.dimmed { opacity:.18; }
                .edgePaths.dimmed path { opacity:.15; }
                .node.selected > rect, .node.selected > circle, .node.selected > polygon { stroke:#4FC3F7 !important; stroke-width:3px !important; filter: drop-shadow(0 0 8px rgba(79,195,247,0.7)); }
                .node.neighbor > rect, .node.neighbor > circle, .node.neighbor > polygon { stroke:#81C784 !important; stroke-width:2.5px !important; }
                .edgePath.highlight path { stroke:#4FC3F7 !important; stroke-width:3px !important; opacity:1 !important; }
                .edgePath.dim { opacity:.15; }
                .legend { position:absolute; right:12px; bottom:12px; background:#0e1117cc; border:1px solid #2a3140; border-radius:8px; padding:.5rem .7rem; color:var(--muted); font-size:.85rem; }
                .legend .row { display:flex; align-items:center; gap:.4rem; margin:.2rem 0; }
                .legend .swatch { width:12px; height:12px; border-radius:3px; border:1px solid #2a3140; }
                .swatch.cosmetic { background:#E3F2FD; border-color:#1E88E5; }
                .swatch.resistance { background:#E8F5E9; border-color:#43A047; }
                .swatch.builder { background:#FFFDE7; border-color:#F9A825; }
                .swatch.quirk { background:#FCE4EC; border-color:#D81B60; }
                .swatch.unknown { background:#ECEFF1; border-color:#607D8B; }
                </style>
                <script src="https://cdn.jsdelivr.net/npm/mermaid@10/dist/mermaid.min.js"></script>
                <script src="https://cdn.jsdelivr.net/npm/svg-pan-zoom@3.6.1/dist/svg-pan-zoom.min.js"></script>
                <script>
                // Configure Mermaid to auto-render and allow HTML labels
                if (window.mermaid) {
                  try { window.mermaid.initialize({ startOnLoad: true, theme: 'dark', securityLevel: 'loose', flowchart: { curve: 'basis', rankSpacing: 60, nodeSpacing: 40, htmlLabels: true } }); } catch (e) {}
                }
                </script>
                </head>
                <body>
                <div id="toolbar">
                    <input type="text" id="searchBox" placeholder="Search genes (name or id)…" />
                    <label><input type="checkbox" id="dimOthers" checked /> Dim non-matches</label>
                    <button class="btn" id="zoomIn">Zoom In</button>
                    <button class="btn" id="zoomOut">Zoom Out</button>
                    <button class="btn" id="reset">Reset</button>
                    <button class="btn" id="fit">Fit</button>
                    <button class="btn" id="exportSvg">Export SVG</button>
                </div>
                <div id="main">
                  <div id="graph-container">
                    <div id="graph">
                      <div class="mermaid" id="mermaid">
                """;
        String footer = """
                      </div>
                    </div>
                    <div class="legend">
                      <div><strong>Legend</strong></div>
                      <div class="row"><span class="swatch cosmetic"></span><span>Cosmetic</span></div>
                      <div class="row"><span class="swatch resistance"></span><span>Resistance</span></div>
                      <div class="row"><span class="swatch builder"></span><span>Builder</span></div>
                      <div class="row"><span class="swatch quirk"></span><span>Quirk</span></div>
                      <div class="row"><span class="swatch unknown"></span><span>Unknown</span></div>
                    </div>
                  </div>
                  <div id="sidebar">
                    <h3>Details</h3>
                    <div id="details">Click a node to see details.</div>
                    <div class="actions">
                      <button class="btn" id="btnZoomTo">Zoom to node</button>
                      <button class="btn" id="btnClearSel">Clear</button>
                    </div>
                  </div>
                </div>
                <script>
                (function() {
                  let panZoom = null;
                  let selectedId = null;
                  function textOfNode(node) {
                    if (!node) return '';
                    // Mermaid v10 uses either text elements or foreignObject with <div>
                    const fo = node.querySelector('foreignObject');
                    if (fo) return fo.textContent || '';
                    const t = node.querySelector('text');
                    return t ? (t.textContent || '') : (node.textContent || '');
                  }
                  function collectNodes(root) {
                    return Array.from(root.querySelectorAll('g.node'));
                  }
                  function applySearch(root, query, dimOthers) {
                    const q = (query || '').trim().toLowerCase();
                    const nodes = collectNodes(root);
                    let matched = 0;
                    nodes.forEach(n => {
                      n.classList.remove('match','dimmed');
                      const txt = textOfNode(n).toLowerCase();
                      if (q.length === 0) return; // nothing to add
                      if (txt.includes(q)) { n.classList.add('match'); matched++; }
                    });
                    if (dimOthers && q.length > 0) {
                      nodes.forEach(n => { if (!n.classList.contains('match')) n.classList.add('dimmed'); });
                      // Optionally dim edges a bit to emphasize nodes
                      const edgePaths = root.querySelector('.edgePaths');
                      if (edgePaths) edgePaths.classList.add('dimmed');
                    } else {
                      nodes.forEach(n => n.classList.remove('dimmed'));
                      const edgePaths = root.querySelector('.edgePaths');
                      if (edgePaths) edgePaths.classList.remove('dimmed');
                    }
                    return matched;
                  }
                  function clearSelection(container) {
                    selectedId = null;
                    container.querySelectorAll('.node.selected,.node.neighbor').forEach(n=>n.classList.remove('selected','neighbor'));
                    container.querySelectorAll('.edgePath').forEach(e=>{ e.classList.remove('highlight','dim'); });
                    document.getElementById('details').textContent = 'Click a node to see details.';
                    document.getElementById('sidebar').classList.remove('open');
                  }
                  function selectNode(container, node) {
                    if (!node) return;
                    const id = node.getAttribute('id') || node.dataset.id || '';
                    selectedId = id;
                    // Reset previous
                    container.querySelectorAll('.node.selected,.node.neighbor').forEach(n=>n.classList.remove('selected','neighbor'));
                    container.querySelectorAll('.edgePath').forEach(e=>{ e.classList.remove('highlight'); e.classList.add('dim'); });
                    node.classList.add('selected');
                    // Highlight connected edges by class LS-id / LE-id
                    const edges = Array.from(container.querySelectorAll('.edgePath'));
                    const connected = edges.filter(e => e.classList.contains('LS-' + id) || e.classList.contains('LE-' + id));
                    connected.forEach(e => { e.classList.add('highlight'); e.classList.remove('dim'); });
                    // Find neighbor nodes from edge classes
                    const neighbors = new Set();
                    connected.forEach(e => {
                      const cls = Array.from(e.classList);
                      cls.forEach(c => {
                        if (c.startsWith('LS-')) neighbors.add(c.substring(3));
                        if (c.startsWith('LE-')) neighbors.add(c.substring(3));
                      });
                    });
                    neighbors.forEach(nid => {
                      if (nid === id) return;
                      const nb = container.querySelector('g.node#' + CSS.escape(nid));
                      if (nb) nb.classList.add('neighbor');
                    });
                    // Sidebar details
                    const details = document.getElementById('details');
                    const name = textOfNode(node).trim();
                    const inCount = edges.filter(e => e.classList.contains('LE-' + id)).length;
                    const outCount = edges.filter(e => e.classList.contains('LS-' + id)).length;
                    details.innerHTML = '<div class="meta">' +
                      '<div><strong>Name:</strong> ' + (name || '(unknown)') + '</div>' +
                      '<div><strong>ID:</strong> ' + id + '</div>' +
                      '<div><strong>Incoming:</strong> ' + inCount + '</div>' +
                      '<div><strong>Outgoing:</strong> ' + outCount + '</div>' +
                      '</div>';
                    document.getElementById('sidebar').classList.add('open');
                  }
                  function zoomToNode(node) {
                    if (!node || !panZoom) return;
                    try {
                      const bbox = node.getBBox();
                      const sizes = panZoom.getSizes();
                      const pad = 1.6;
                      const scale = Math.min(sizes.width / (bbox.width * pad), sizes.height / (bbox.height * pad));
                      const cx = bbox.x + bbox.width/2;
                      const cy = bbox.y + bbox.height/2;
                      panZoom.zoom(scale);
                      const view = panZoom.getSizes();
                      const panX = - (cx * scale) + (view.width / 2);
                      const panY = - (cy * scale) + (view.height / 2);
                      panZoom.pan({x: panX, y: panY});
                    } catch (e) {}
                  }
                  function exportCurrentSvg(svg) {
                    const serializer = new XMLSerializer();
                    let source = serializer.serializeToString(svg);
                    // Add XML declaration and proper namespace if missing
                    if (!source.match(/^<\\?xml/)) source = '<?xml version="1.0" standalone="no"?>\\n' + source;
                    const blob = new Blob([source], {type:'image/svg+xml;charset=utf-8'});
                    const url = URL.createObjectURL(blob);
                    const a = document.createElement('a');
                    a.href = url; a.download = 'gene-flowchart.svg';
                    document.body.appendChild(a); a.click();
                    setTimeout(() => { document.body.removeChild(a); URL.revokeObjectURL(url); }, 0);
                  }
                  function initOnceSvgReady() {
                    const src = document.getElementById('mermaid');
                    const graph = document.getElementById('graph');
                    const container = document.getElementById('graph-container');
                    const findSvg = () => (src.querySelector('svg') || graph.querySelector('svg'));
                    let svg = findSvg();
                    if (!svg) {
                      // Observe for rendered SVG
                      const obs = new MutationObserver(() => {
                        svg = findSvg();
                        if (svg) { obs.disconnect(); afterSvg(svg, container, graph); }
                      });
                      obs.observe(src, { childList: true, subtree: true });
                    } else {
                      afterSvg(svg, container, graph);
                    }
                  }
                  function afterSvg(svg, container, graph) {
                    // Initialize pan/zoom with wheel and drag support
                    panZoom = svgPanZoom(svg, {
                      zoomEnabled: true,
                      panEnabled: true,
                      mouseWheelZoomEnabled: true,
                      dblClickZoomEnabled: true,
                      zoomScaleSensitivity: 0.4,
                      minZoom: 0.2,
                      maxZoom: 10,
                      controlIconsEnabled: false,
                      fit: true,
                      center: true,
                      contain: false,
                      eventsListenerElement: graph
                    });
                    const fit = () => { try { panZoom.resize(); panZoom.fit(); panZoom.center(); } catch (e) {} };
                    // Controls
                    document.getElementById('zoomIn').addEventListener('click', () => panZoom.zoomBy(1.2));
                    document.getElementById('zoomOut').addEventListener('click', () => panZoom.zoomBy(0.8333));
                    document.getElementById('reset').addEventListener('click', () => { panZoom.resetZoom(); panZoom.center(); });
                    document.getElementById('fit').addEventListener('click', fit);
                    document.getElementById('exportSvg').addEventListener('click', () => exportCurrentSvg(svg));
                    document.getElementById('btnClearSel').addEventListener('click', () => clearSelection(graph));
                    document.getElementById('btnZoomTo').addEventListener('click', () => {
                      if (!selectedId) return;
                      const n = graph.querySelector('g.node#' + CSS.escape(selectedId));
                      if (n) zoomToNode(n);
                    });
                    // Search
                    const searchBox = document.getElementById('searchBox');
                    const dimCheckbox = document.getElementById('dimOthers');
                    const refreshSearch = () => applySearch(graph, searchBox.value, dimCheckbox.checked);
                    searchBox.addEventListener('input', refreshSearch);
                    dimCheckbox.addEventListener('change', refreshSearch);
                    // Node click handlers for selection and focus
                    graph.addEventListener('click', (ev) => {
                      const g = ev.target.closest('g.node');
                      if (!g) return;
                      selectNode(graph, g);
                    });
                    // Initial fit and search
                    setTimeout(() => { fit(); refreshSearch(); }, 30);
                    window.addEventListener('resize', () => setTimeout(fit, 30));
                  }
                  window.addEventListener('load', () => { initOnceSvgReady(); });
                })();
                </script>
                </body>
                </html>
                """;
        // Escape HTML so raw Mermaid code sits safely inside the hidden container.
        String escaped = mermaidSource
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
        return header + escaped + footer;
    }

    private static String safe(ResourceLocation id) {
        return id.getNamespace().replaceAll("[^a-z0-9_]", "_") + "_" + id.getPath().replaceAll("[^a-z0-9_]", "_");
    }

    private static String labelFor(ResourceLocation id) {
        String key = id.toString();
        String translated = TranslationHelper.getEnUs().getOrDefault(key, key);
        return translated.replace(":", "<br/>");
    }
}


