# Prompt pentru Figma (redesign Axela — platformă de inspecție drone/solar)

Te înțeleg perfect — navy + albastru + carduri rotunjite + font sans generic e exact "rețeta" pe care o scuipă orice tool de AI design ca implicit. Ca să ceri altceva, trebuie să fii explicit despre ce vrei să EVITE, nu doar despre ce vrei să facă — altfel se întoarce la aceleași clișee.

Copiază tot ce e mai jos (secțiunea din chenar) și dă-l ca prompt în Figma (Figma AI / First Draft) sau ca brief unui designer. E scris în engleză fiindcă majoritatea tool-urilor AI de design dau rezultate mult mai bune în engleză, dar dacă lucrezi cu un designer uman, tradu-l liber.

---

```
Redesign the UI for "Axela" — a solar panel farm inspection platform where
technicians and admins review drone-captured photos/video, track detected
panel defects, manage repair work orders, and export health reports.

I explicitly do NOT want the generic "AI SaaS dashboard" look. Avoid all of
the following - they read as soulless/templated no matter how well executed:
- Navy/dark-blue sidebar + a single saturated blue accent color as the
  entire palette
- Glassmorphism, frosted blur panels, soft floating drop-shadows on every card
- Teal-to-blue or generic purple-to-blue "SaaS gradient" used lazily as a
  background fill just to look modern
- Perfectly symmetric, centered hero/dashboard layouts with everything in
  neat equal-width rounded cards
- Generic rounded-corner-everything (buttons, cards, inputs all at the same
  8-12px radius) with no other personality
- Generic outline icon sets (Feather/Lucide-style) used identically to every
  other SaaS product
- Inter/Sans-only typography with no display font or personality in headers
- Stock 3D-render illustrations of robots, clouds, or abstract blobs

Instead, take a distinct creative direction rooted in what this product
actually is - aerial drone inspection of solar infrastructure:
- Visual language borrowed from flight instruments, topographic/orthomosaic
  maps, blueprints, and technical survey drawings - not generic "tech
  startup" abstraction
- Palette: near-black background (not pure #000, a soft off-black), with
  a vivid magenta-to-crimson gradient as the ONE accent color - used
  sparingly on focal elements (primary buttons, active nav states, key
  numbers, alert badges) the same way angular.dev uses its pink/red brand
  gradient. Everything else stays desaturated near-black/near-white/grey
  so the accent actually pops instead of competing with itself. For
  defect/heat data specifically, still reference real thermal-imaging
  color science (FLIR-style ramps) rather than plain red=bad/green=good -
  that's a separate, deliberate exception to the one-accent rule because
  it's functional data encoding, not decoration
- Typography pairing: a confident, oversized bold sans for headings (think
  angular.dev's huge tight-tracked display headlines - Inter Tight or
  similar, not a timid corporate weight), paired with a monospace font
  (DM Mono or similar) for data, coordinates, telemetry, IDs, timestamps -
  like reading flight logs, not marketing copy
- Asymmetric, grid-based layouts inspired by the solar panel arrays
  themselves - literal row/column grid motifs used as a structural design
  element, not just for the farm map screen
  overlays, dashed flight-path lines, north-arrow/compass marks,
  scan-line or radar-sweep motifs used sparingly as accents
- Texture: subtle grain, paper/print texture, or satellite-imagery-style
  desaturation instead of flat vector gradients everywhere
- Real or realistic photographic references (aerial farm shots, drone
  hardware, thermal imagery) over generic icon illustrations where imagery
  is used at all
- Purposeful asymmetry and varied corner radii/shapes per component type
  (e.g., sharp technical corners on data/telemetry panels, softer shapes
  only where warmth is intentional) rather than one uniform radius rule
  everywhere

Keep the information density and functional layout of a real operations
tool (data tables, filters, status badges, KPI cards, forms) - I'm not
asking for a marketing site, I'm asking for the same functional dashboard
to stop looking like a default AI-generated template and start looking
like it was designed by someone who actually thought about drones, solar
farms, and aerial survey work.

Also redesign the logo/app icon - the current one is a generic abstract
blue geometric X/bowtie mark with no real connection to the product. It
needs to change along with everything else, or it'll look like a leftover
from the old generic version sitting inside the new design. Take it in the
same direction: something referencing an aerial/top-down view of a solar
panel array, a stylized drone silhouette, a radar/scan sweep, or a compass/
flight-path mark - not another abstract crossed-line mark.

Palette and typography reference, quite literally: angular.dev. Match its
overall feel closely - near-black background, the vivid magenta/crimson
gradient accent used sparingly on focal elements, huge bold tight-tracked
headlines, monospace for code/data snippets inside dark bordered panels,
pill-shaped badges with a gradient outline instead of a solid fill, subtle
diagonal dot/slash texture in background areas instead of empty flat
black. Bring that exact color and type confidence to this product, just
reskinned around aerial/flight-instrument content instead of a JS
framework's marketing site.

Reference for the QUALITY BAR of composition (not colors - angular.dev
above already covers that): look at "Mission Control" by Clay
(awwwards.com/sites/mission-control-1). Copy the way it FEELS: confident
oversized display type with real breathing room around it, a
floating/asymmetric composition instead of a boxed-in grid of equal
cards, subtle ambient motion and depth instead of a static flat page, and
restraint - one glowing focal point per screen instead of every element
competing for attention. That's the difference between something a
designer crafted and something a template generator spat out.

Show me: a login screen, the main dashboard, the farm map/panel grid view,
and the logo/icon itself as a standalone mark, all in this new direction.
```

---

Câteva sfaturi ca să iasă și mai bine:

- Dacă tool-ul de AI îți dă înapoi tot ceva generic, spune-i explicit ce anume a păstrat din lista de "evită" (ex: "still using glassmorphism cards, remove that") — modelele au tendința să revină la clișee dacă nu le respingi punctual.
- Adaugă poze de referință dacă poți — 2-3 screenshot-uri cu hărți topografice, cockpit/instrumente de zbor, sau termograme reale ajută enorm mai mult decât textul singur.
- Cere explicit varianta de login + dashboard + farm map, ca în prompt — dacă ceri doar "un design", de multe ori întorc un singur ecran needitat de restul sistemului.
- Dacă Figma AI acceptă atașare de imagini ca referință vizuală, atașează un screenshot chiar de pe angular.dev (pagina principală) cu mențiunea "match this palette and typography closely" — asta e referința de culoare/font principală acum. Poți atașa și mission-control-1 de pe awwwards.com separat, cu mențiunea "reference for composition/craft feel only, not palette" — pentru asta chiar nu vrei paleta lor, doar atitudinea.
