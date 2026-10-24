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
- **Select (everything, every view):** tap **Select** in the grey bar. It works the same in Front, Top and Profile, on anything drawn in any view: points, lines, broken lines, curves, shapes and 3D objects.
  - Tap a point to pick it (orange ring), or tap a line, shape or object to pick it (orange band). Tap again to unpick.
  - Or drag a box: items lying fully inside are picked whole, and other points inside the box are picked as points.
  - Drag a picked item to move them all; the arrows move them one step.
  - **Group** keeps items together: picking one picks the group. Groups can span views and are saved; **Ungroup** splits them.
  - **Merge:** 3D objects join into one solid. Two overlapping shapes drawn in the same view become one outline.
  - **Cut 1st by 2nd:** the second item is cut out of the first: a shape by a shape or straight line in the same view, or one 3D object by another.
  - **Line up ⬌/⬍** puts their centers on one row or column of this view. **Space ⬌/⬍** spaces three or more evenly.
  - **Line up** and **Space** work on points too, mixed with items.
  - **Properties** opens the settings of the one picked thing.
  - **Make 3D** gives picked shapes 20 mm depth, and **Copy** duplicates picked items.
  - **Delete** removes picked items. For a picked point, it removes the lines ending there. **Done** leaves Select.
- **Several points:** in Adjust, drag a box on empty space. It now also picks end points of lines from other views. They move together with the arrows or by dragging, and the align buttons line all of them up.
- **Properties:** whenever something is selected, the grey bar shows **Properties**. It opens the right settings for it:
  - a **line**: length, color, thickness, material and turn;
  - a **shape**: width, height, color, fill transparency and 3D depth;
  - a **point**: move it by exact distances;
  - a **measurement**: its details;
  - a **grey shape** from another view: its position and depth.
- **Find** sits where the 3D tab was, in the Front | Top | Profile row. The 3D view and Full view open from the Find list. **Fit** is in the grey bar. Crop shows only for a photo in Front; blank canvases have no edge.
- **Selection follows you between views:** the selected line is drawn thick and blue in the other views (a dot when it points straight at you), the view zooms to include it, and it is selected again when you come back.
- **Find list:** it lists every line, broken line, curve, shape and 3D object in every view. Each is named by its points (A–B–C…, letters continue AA, AB…) and shown with a small drawing, its kind, material and view. Tapping one opens its view, selects it and zooms to it. **Full view** moved into the finder.
- **Sizes along each axis:** a straight line's **Properties** shows how far it reaches along X, Y and Z, and you can type each. **No depth here** removes the part going into depth in the current view, so the line lies flat. A label adds "(… seen here)" when a line looks shorter than its true length because part of it goes into depth.
- **Screen layout:** every action is an icon in one fixed row at the top (Fit and Sizes always first, then what fits the current selection). The bottom bar always holds the tools plus **Select** and **Find**, so nothing jumps between top and bottom. Align H and Align V are the same buttons, doing the same thing, for points and for whole items. **Measurements** is in the file menu (tap the project name).
- **Everything is 3D:** a point, then a shape, then an object. There is no Make 3D button: type a **Depth** in a shape's Properties and it becomes a 3D object.
- **Line ends:** every line shows dots at its ends, and a line pointing straight into the screen (for example a post seen from Top) shows as a ring. If it has a material, its cut shape (for example the 4×4 tube's square with its hole) is drawn there.
- **Show or hide sizes:** the **Sizes** button next to Select turns the size labels on lines and shapes on or off, in every view. The choice is remembered.
- **Select box:** anything lying fully inside the box is picked whole, so Align and Even move it without changing its size. Only points of items that stick out of the box are picked one by one.
- **Switching views** keeps every line's size and position, even when Front, Top and Profile each have their own calibration.
- **Box selection of points:** in Select, a dragged box picks the points inside it. Switch to **Box: items** to pick whole items instead. **Align ⬌/⬍** and **Even ⬌/⬍** order the picked points and items. **Clear** unpicks everything.
- **Lengths are true 3D lengths:** a line's label shows its real length, depth included, so it reads the same in every view, along with its material. Setting a length in Properties scales the whole line in 3D. Pipe and bar bodies are shown only in 3D, so they no longer cover the lines in the flat views.
- **Every view edits everything the same way:** each view shows all lines, broken lines, curves and shapes from all views, projected into it. They're drawn the same as lines drawn there, are selected the same way, and get the same tools: breakpoints, curve points, copy, Make 3D, Properties, the arrow pad and the Δ readout, splitting, joining and cutting. Changes go back to where each line lives in 3D, so the other views, 3D and export follow. Make 3D on a shape from another view moves it into the current view and extrudes it along this view's depth. 3D objects keep their own handles. Measurements stay in the view they were made in.
- **What a front line looks like elsewhere:** a line drawn in Front with no depth lies in the Z = 0 plane. From Top you see its X extent along the dashed Z = 0 line, and a vertical line is just a point. From Profile you see its Y extent along that line. Give its ends some Z and it leaves the dashed line.
- **Turning a material:** in a line's Material dialog, −90°/−15°/+15°/+90° turn the profile around the line, for example so a 20 × 40 tube shows its narrow side to the front. The small picture shows the bar's end.
- **Materials used:** tap the project name, then **Materials used (lengths)**, for every material, every piece's true length (curves and depth included) and the total.
- **Moving end points while drawing:** with Lines or Measure chosen, drag an existing end point to move it, as in Adjust. Tapping still draws.
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
