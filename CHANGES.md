# Changelog

All notable ME Beam Formers changes are grouped by the version in which they first appeared.

## Important compatibility notice

- ME Beam Formers runs only with AE2 Unofficial Deconstructed, not with standard AE2 or AE2 Unofficial Extended
  Life.
- Back up the world before installing or updating the mod.

## 1.0.0 - 2026-10-24

- **Requires AE2UD 1.6.0 or newer.** AE2UD shares its mod id with AE2 and AE2 UEL, so the version range is
  what keeps the game from starting with either of them in its place.
- **The Small Beam Former.** A part for a cable bus that joins its network to another beam former facing it
  along the same line, up to 32 blocks away (`formerRange` in the config), with a visible beam between the
  two. The link carries a normal cable's 8 channels and the former draws 0.5 AE/t. The beam goes through
  anything light goes through, such as glass, leaves or water, but not through a solid block or another cable
  bus; block it and the link drops, clear it and the link comes back by itself. A former takes the colour of
  its cable, and so does the beam: a coloured former joins a transparent one or one of its own colour, never
  one of another colour, so beams of different colours can run side by side without joining their networks.
  Shift and right-click either end with an empty hand to hide or show the beam; the link stays as it is. The
  beam is drawn only while the network has power. The network visualiser shows the link, with the channels
  it carries. The pink lights on the former always glow. Two are made from glowstone dust, a wireless
  receiver, quartz glass, a calculation processor and a fluix glass cable.
  The idea comes from [ME Beam Former](https://modrinth.com/mod/me-beam-former) and the beam former of
  [NAE2](https://github.com/AE2-UEL/NAE2).
- **Beams glow.** With [GregTech CEu](https://github.com/GregTechCEu/GregTech) or
  [Lumenized](https://www.curseforge.com/minecraft/mc-mods/lumenized), which carries GregTech's bloom on its
  own, installed, the beams are drawn through that bloom and shine around their edges. Under an OptiFine shader
  pack, where that bloom is switched off, and without either mod, they are drawn as before. A black beam never
  glows: it is drawn as a dark shade over what is behind it. The pink lights on the Dense Beam Former and the
  Beam Tower glow through that bloom too. Drawing beams through GregTech's bloom follows
  [NAE2](https://github.com/AE2-UEL/NAE2).
- **The Dense Beam Former.** A block that mounts on whatever it is placed against, with the beam pointing away
  from it, and joins the network only through that base. Two of them link at 32 channels; with a Small Beam
  Former at the other end the link carries 8. It pairs, colours, hides and draws power like the small one; its
  colour is that of the cable bus behind its base, and its beam is thicker. A wrench turns it. It is
  made from quartz glass, an engineering processor, two Small Beam Formers, a fluix dense cable and a fluix
  block. The dense beam former as a block of its own follows [NAE2](https://github.com/AE2-UEL/NAE2).
- **The Beam Tower.** A block that mounts like the Dense Beam Former and links to other towers, at any angle,
  up to 32 blocks apart (`towerRange` in the config) and to at most 8 of them (`towerMaxLinks`). Links are made
  with AE2's memory card: sneak and use it on one tower to store it, then use it on another to link the two, or
  to unlink them if they already are. Each link carries 32 channels while nothing stands between the two
  crystals, by the same rule as a beam former's beam, and costs 0.5 AE/t on each tower. The towers take their
  colour like the Dense Beam Former, sneaking with an empty hand hides or shows all of a tower's beams, and
  links survive the towers being unloaded. It is made from wireless boosters, a logic processor, quartz glass,
  a Dense Beam Former and fluix blocks. The idea comes from [ME Beam Former](https://modrinth.com/mod/me-beam-former).
- **The One Probe.** With [The One Probe](https://www.curseforge.com/minecraft/mc-mods/the-one-probe) installed,
  looking at a beam former shows the former it is linked to and how far away, the channels the link carries,
  the block in the way when something blocks the beam, and whether the beam is hidden. A tower shows how many
  links it has, how many are active and the channels running through it, each counted once however many links
  it comes in and goes out by; in the probe's extended mode it lists every link with its channels, or the block
  in its way.
