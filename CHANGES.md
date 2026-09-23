# Changelog

All notable ME Beam Formers changes are grouped by the version in which they first appeared.

## Important compatibility notice

- ME Beam Formers runs only with AE2 Unofficial Deconstructed, not with standard AE2 or AE2 Unofficial Extended
  Life.
- Back up the world before installing or updating the mod.

## Unreleased

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
