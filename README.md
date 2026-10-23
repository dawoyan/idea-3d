# Idea 3D

Android app for building dimensioned 3D models from a photo or an empty scene. It started as a copy of Measure It (mejRit) under its own package, `com.davoyans.idea3d`, so the two apps install side by side. The spec is in [idea-3d.md](idea-3d.md).

## Install

Download the APK from the [latest release](https://github.com/dawoyan/idea-3d/releases/latest) and open it on the phone. You may need to allow installs from your browser or file manager.

## Coordinate system

- Right-handed, millimeters: X right, Y up, Z toward you in the front view. The drawing plane is Z = 0.
- Lengths are stored in millimeters. Switching between mm, cm, m, in and ft changes only how they are shown, with 0 to 2 decimals.
- Image pixels become model millimeters only through calibration. One known distance gives one scale for X, Y and Z. As an advanced option, references can be marked X only or Y only. When X and Y scales differ, a banner explains that this compensates for a stretched photo. Missing, incomplete or contradicting references are flagged.
- A single photo does not show depth, so Z values and thicknesses are always entered by you.
- STL and 3MF exports use the same axes. A slicer treats Z as up, so an extruded shape lies flat with its front face on top.

## Three drawing planes

The editor has a **Front | Top | Profile** switch. Every plane has the same tools: lines, broken lines, curves, measuring, Adjust, split/join/cut and Make 3D.

| Plane | Screen axes | A closed shape is extruded along |
|---|---|---|
| Front | X right, Y up (on the photo) | Z, back from its front face |
| Top | X right, front (+Z) at the bottom | Y, down from its top face; it starts on Y = 0 |
| Profile | front (+Z) on the left, Y up | X, left from its right face; it starts on X = 0 |

- The Top and Profile canvases are blank and the size of the picture. They share X (Top) or Y (Profile) with the front drawing, and put Z = 0 across the middle as a dashed line.
- Objects from the other planes show as grey outlines, so new shapes can be lined up with them.
- Only the photo can be calibrated, so Calibrate and Crop appear only in Front. Top and Profile use the front scale.
- Dialogs name the right depth axis for the plane, for example "Depth Y" in Top.
- In the 3D viewer, any flat view can move or resize an object along its extrusion axis when that axis is visible. For example, a Top shape is raised in Front or Profile. Snapping uses every object's sides along that axis. The **Draw** button opens the editor in the plane you're looking at.
- Projects save each item's plane. Older projects open with everything in Front.

## What's new compared with Measure It

- **Geometry core** (`app/src/main/java/com/davoyans/idea3d/core`): pure Kotlin code for:
  - unit conversion and point names (A…Z, AA…)
  - true 3D length vs front-view length
  - calibration
  - ear-clipping triangulation, which handles concave outlines
  - extrusion with a Z value and thickness at each vertex
  - mesh validation: closed surfaces, outward normals, degenerate triangles, positive thickness, self-crossing outlines and overlapping solids
  - STL and 3MF writers
- **Export checks:** export builds and checks the model off the UI thread. Problems are listed for each object. When the model passes, it shows each object's size and offers binary STL or 3MF. 3MF is in millimeters and keeps each object's color.
- **3D viewer:**
  - Front, Top, Profile and 3D views can be switched inside the viewer.
  - An axis indicator uses the same X/Y/Z colors in every view.
  - In the flat views, one finger pans; in the 3D view, one finger orbits.
  - Tapping an object sets its front-face Z and depth, or removes its 3D form while keeping the shape. These edits can be undone.
  - **Top and Profile views are editors for depth (Z).** Tap a shape to select it. Then:
    - drag the shape to move it forward or back in Z;
    - drag the round front or back handle to change its depth from that side;
    - drag a dot to move outline points in Z, which slants or tilts the shape. Points that line up in the view move together.
  - While dragging, the new value is shown live. Edits snap to Z = 0, to other objects' front and back faces, and to the current unit step.
  - Each drag is one undo step, and the viewer has its own Undo button. The camera stays put after an edit.
  - Overlapping objects are shown with a warning instead of closing the viewer, so you can move them apart. Export still refuses them.
- **Measurement editor:** shows the true 3D length, the shorter front-view length and the Pythagoras relation. It explains when a Z change conflicts with a fixed length and shows where the length came from.
- **Library:** thumbnails include the drawn shapes. Deleted projects can be restored with Undo.
- **Bug fixes:**
  - The canvas no longer moves between the two taps of a line.
  - The 3D viewer now shows the rebuilt model after an edit.
  - Dark materials stay light enough to show shading.

## Testing

- `./gradlew testDebugUnitTest` runs 34 JVM tests:
  - unit conversion and decimals
  - point names beyond Z
  - X/Y/Z directions and right-handedness
  - front-view math
  - uniform, per-axis, incomplete, contradictory and zero calibration
  - boxes, concave L and star shapes, curved discs, sloped Z, clockwise input
  - self-crossing, zero-thickness, degenerate, open and inverted meshes
  - overlapping, stacked and nested solids
  - STL byte layout and the 3MF package
- **Manual test on an Android 14 emulator (1080×2400)** with the debug build. The signed release APK was then installed and launched on the same emulator. Steps:
  - Library, then a new empty scene.
  - Drew a concave L with a broken line and made it 3D.
  - Set depth to 20 mm, and set Z to 15 mm in the viewer.
  - Switched Front, Top, Profile and 3D views.
  - Exported STL and 3MF.
  - Reopened the project and checked its thumbnail.
  - Deleted the project and restored it with Undo.
- **Independent file check:** the exported files were checked with a separate stdlib Python script. Both had zero open edges, consistent orientation, positive volume, the expected 925.93 × 1481.48 × 20 mm size and Z from −5 to 15 mm.

## Limits

- **No physical device test yet.** Not checked: touch feel, long-press, pinch smoothness and Bluetooth.
- **No slicer test yet.** The files have not been opened in PrusaSlicer, Cura or Bambu Studio. Open them in your slicer and check them before printing.
- **Bosch GLM support is unchanged from Measure It** and was not tested here. No other laser device is claimed to work.
- **Shape limits:** shapes with holes can't be exported yet; the app reports them. Overlapping solids must be merged or moved apart in Z.
- **Calibration** exists only in the Front plane. Per-axis (X only / Y only) calibration applies to the front drawing; Top and Profile use one uniform scale.
- **Overlap checks across planes** compare bounding boxes and only warn, because a box can overlap where the solids don't. Look in the 3D view. Overlaps within one plane are still checked exactly and block export.
- **Perspective correction** works as in Measure It, using reference edges in the crop tool. It is not covered by automated tests.
