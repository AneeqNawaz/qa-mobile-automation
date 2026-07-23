# BUG: Extras/NeuroBooster tile checkmark is drawn but has no accessibility node in-session

**Status:** Open — to investigate later
**Area:** MED app · Extras (NeuroBooster) listing · tile "discovered" checkmark
**Type:** Accessibility gap (product) — also blocks efficient test automation
**Platform:** Confirmed on Android (physical Samsung SM-S721B, Android 16 / API 36). iOS behaves the same (tick verified only after a list reload).
**Date raised:** 2026-07-23

---

## Summary

When a NeuroBooster mini-exercise is completed and the user returns to the Extras listing, the
tile's "discovered" **checkmark (✓) is drawn immediately** and is visible to the human eye. However,
the checkmark is **not exposed as an accessibility node** until the Extras list is **reloaded**
(e.g. by switching to another bottom-nav tab and back). Until that reload:

- the pixels show the ✓,
- but the accessibility tree (what Appium/UiAutomator2 — and a screen reader — can read) has **no
  `checkmark` node** for that tile.

The section **progress counter** (`"k / N discovered"`) DOES update immediately as a proper node.
Only the per-tile checkmark node is missing until reload.

## Impact

1. **Accessibility (real users):** a screen reader (TalkBack / VoiceOver) would **not announce** that
   a mini-exercise is now discovered until the list is reloaded. The completion is silently
   unavailable to assistive tech, even though it's visually obvious.
2. **Test automation:** the E2E Extras content regression cannot read the tick in-session, so it must
   **leave Extras and re-enter** (forcing a reload) before verifying that every completed tile shows
   its ✓ — an extra full-list scroll per section. If the app exposed the node in-session, that whole
   step could be dropped.

## Evidence

- **Immediate per-tile read fails.** With a check right after each completion, `isTileDiscovered()`
  (`resolveTile(...).findElements(id/checkmark)`, incl. a 5 s poll) returned **false for ~11 of 19
  freshly-completed body tiles**, while the same tiles' progress count incremented correctly and the
  ✓ was visible on screen. (Run: `body-tick-validate`, 2026-07-23.)
- **After a reload it reads fine.** Leaving to the Training tab and back, then capturing the list,
  finds the `checkmark` node for every completed tile (0 failures). This is the workaround currently
  used by `ExtrasContentVerifier.verifyAllTicks`.
- **The node genuinely exists once bound.** A live page-source dump of a settled listing shows
  `resource-id="nn.mobile.app.med:id/checkmark"` as a descendant of `domainCardView`
  (`checkmark < ViewGroup < domainCardView`) — so the id/hierarchy are correct; the node is simply
  **not attached in-session** right after completion.

## Steps to reproduce

1. Open the MED app on a Parkinson (or MCI) account, go to **Extras**.
2. Open any NeuroBooster tile, complete it (body: tap the completion CTA; cognitive: watch video +
   pass quiz), and return to the Extras listing.
3. Observe: the tile shows a ✓ (visible), and the section header shows an incremented
   `"k / N discovered"`.
4. Inspect the accessibility tree (Appium Inspector / `uiautomator dump` / a screen reader): the
   completed tile has **no `id/checkmark` node** (progress text node IS present).
5. Switch to another bottom-nav tab and back to Extras → the list reloads → the `id/checkmark` node
   now appears for the completed tile.

## Expected vs actual

- **Expected:** as soon as the ✓ is drawn, the corresponding `id/checkmark` accessibility node is
  attached, so assistive tech and automation can read the discovered state in-session.
- **Actual:** the node is missing until the list rebinds on reload.

## Root-cause hypothesis (for the dev team)

The list cell likely draws the checkmark (canvas/compose/visibility change) without triggering an
accessibility-tree update / `sendAccessibilityEvent` / cell rebind. A reload rebinds the RecyclerView
cell, at which point the node is created. Likely fix: on completion, update the cell's accessibility
node (e.g. mark the checkmark `View` important-for-accessibility and notify, or invalidate/rebind the
cell) so the ✓ is exposed without a full reload.

## Current workaround (in the automation)

`ExtrasContentVerifier.verifyAllTicks()` verifies ticks in ONE refreshed sweep per flow: tap Training
tab → tap Extras tab (force reload) → `captureContent()` → assert each completed tile's
`discovered == true` (keyed by **section + subtitle**, since cognitive subtitles repeat across
sections). If/when the app exposes the checkmark node in-session, this reload + sweep can be removed
and the tick checked per tile immediately.

## Related

- Android "lands at top after NB completion" (separate open product issue) — makes the per-tile
  re-scroll costly and is why the tick sweep does one pass rather than per-tile reads.
