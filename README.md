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

## Three drawing planes and one 3D view

The editor has one row: **Front | Top | Profile | 3D**. Every plane has the same tools: Calibrate, Measure, lines, broken lines, curves, Adjust, split/join/cut, Make 3D and Crop.

| Plane | Screen axes | A closed shape is extruded along |
|---|---|---|
| Front | X right, Y up (on the photo) | Z, back from its front face |
| Top | X right, front (+Z) at the bottom | Y, down from its top face; it starts on Y = 0 |
| Profile | front (+Z) on the left, Y up | X, left from its right face; it starts on X = 0 |

- **Editing shapes from other planes:** with **Adjust**, tap a grey shape to select it. Then:
  - drag it to move it along both visible axes;
  - drag the side handles to stretch it along the shared axis;
  - drag the depth handles to change its front or back;
  - drag its dots to move outline points. Each corner has a dot on the facing side, which moves the point, and one on the far side, which slides it and changes the thickness there. Dots that line up in the view move together. A grey shape's corner can be grabbed even before it is selected.

  The new size or offset shows while dragging, each drag is one undo step, and tapping the selected shape again opens its exact values. For example, in Top you can widen a Front box or make it deeper.
- **The same Adjust in every view:**
  - **Points:** tap a point, including a dot on a grey shape. The arrow pad moves it one display step at a time (for example 1 mm), and the move button (four arrows) moves it by distances you type along the axes shown.
  - **Grey shapes:** selecting one also offers exact values, **3D Cut**, **3D Join**, and **Open in its view**. That last one switches to the view the shape was drawn in, where split line by point, join shapes and 2D cut apply to it.
- **Materials:** **Materials** on the main screen lists bars and pipes shared by all projects. Each has a name, a profile (rectangular or round, tube or bar), sizes, wall thickness and a color, and the list starts with a few examples such as a rectangular iron pipe 40 × 20 × 2 mm. Tap a line and then the **Material** button in the top bar, and pick a material: the whole line, including every segment of a broken line, becomes that bar or pipe in 3D and in STL/3MF export. The part is centered on the line, its height runs along the view's depth axis, and curves follow in short straight pieces.
- **Materials from your own drawings:** in any project, draw a cross-section; closed shapes inside it become holes, so a square inside a square is a tube. Tap the project name and choose **Save drawing as material**: it takes the selected shape, or the biggest outline in the drawing. You can also select the shape with Adjust and tap the square-with-plus icon. Name it, for example "Tube 4×4", and set its real width; height and holes scale with it. It joins the shared list, and in any other project a line's **Line details** can use it: the line becomes that shape along its length.
- **Joining crossing lines** happens on the drawing instead of a numbered dialog. Double-tap the crossing and the four parts blink, following curves as they really run. Tap the two to keep: they turn green and the parts to delete blink red. **Keep the other parts** keeps the rest instead, and **Finish** joins.
- **3D Cut and Join:** select a shape, tap the scissors (cut) or the joined-squares icon (join), then tap the other object. This works in any view, including the 3D tab, and the shapes can be from different views.
  - **Cut** removes the second object from the first, for example a hole through a box. You choose whether the cutting object stays in the model or is used up; either way it stays editable in its view.
  - **Join** merges overlapping objects into one printable solid.
  - The exact-values dialog lists every cut and join an object is part of, each with Undo. Results are checked like any solid: closed, outward-facing and with volume.
- **Moving points:** in every plane, with **Adjust**, a corner of your own drawing can be dragged straight away, with no tap first.
- **Fit:** each plane opens zoomed to everything in it, including shapes from the other planes, so every corner and handle is on screen. **Fit** in the status bar does this again, and pinching can zoom out further. Blank canvases also grow to hold everything drawn on them. The status bar says what to tap and drag in each step.
- **Blank canvases:** Top and Profile are blank and start at the picture's size. They grow to fill the screen, with no dark bands, and so does an empty scene's front canvas. Crop resizes them. Each plane stores where its canvas sits in 3D, so cropping or growing never moves anything.
- **Calibration:** a plane uses its own calibration lines if it has any. Otherwise Top and Profile use the front scale, and an empty scene uses 1 pixel per unit. Leveling, rotation and perspective correction work on the photo, so they stay in Front.
- **3D:** shown in place of the canvas. One finger orbits, two fingers pan and zoom, and a tap opens an object's exact values. Sizes, Front, Frame, light, Reset and Save are on one slim row. The model rebuilds after every change, including undo.
- **Saving:** projects store each item's plane and each plane's canvas. Older projects open with everything in Front.

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

- **Bar and pipe corners:** where two lines with a material meet, the pieces overlap at the corner; they aren't mitered. Curved lines are built from 12 straight pieces that overlap slightly at each bend. Each piece is a separate solid in the export; slicers normally merge them.

- **No physical device test yet.** Not checked: touch feel, long-press, pinch smoothness and Bluetooth.
- **No slicer test yet.** The files have not been opened in PrusaSlicer, Cura or Bambu Studio. Open them in your slicer and check them before printing.
- **Bosch GLM support is unchanged from Measure It** and was not tested here. No other laser device is claimed to work.
- **Shape limits:** shapes with holes can't be exported yet; the app reports them. Overlapping solids must be merged or moved apart in Z.
- **Calibration in empty scenes now counts.** Before 0.5.0, an empty scene ignored calibration lines and always used 1 pixel per unit. Older empty scenes that contain a calibration line will show different sizes. Delete that line to go back to 1 pixel per unit.
- **Per-axis (X only / Y only) calibration** applies to the front drawing only. Top and Profile use one uniform scale.
- **Overlap checks across planes** compare bounding boxes and only warn, because a box can overlap where the solids don't. Look in the 3D view. Overlaps within one plane are still checked exactly and block export.
- **Perspective correction** works as in Measure It, using reference edges in the crop tool. It is not covered by automated tests.
