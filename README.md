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

The editor has one row: **Front | Top | Profile | 3D**. Every plane has the same tools: Calibrate, Measure, lines, broken lines, curves, Modify, split/join/cut, Make 3D and Crop.

| Plane | Screen axes | A closed shape is extruded along |
|---|---|---|
| Front | X right, Y up (on the photo) | Z, back from its front face |
| Top | X right, front (+Z) at the bottom | Y, down from its top face; it starts on Y = 0 |
| Profile | front (+Z) on the left, Y up | X, left from its right face; it starts on X = 0 |

- **Editing shapes from other planes:** with **Modify**, tap a grey shape to select it. Then:
  - drag it to move it along both visible axes;
  - drag the side handles to stretch it along the shared axis;
  - drag the depth handles to change its front or back;
  - drag its dots to move outline points. Each corner has a dot on the facing side, which moves the point, and one on the far side, which slides it and changes the thickness there. Dots that line up in the view move together. A grey shape's corner can be grabbed even before it is selected.

  The new size or offset shows while dragging, each drag is one undo step, and tapping the selected shape again opens its exact values. For example, in Top you can widen a Front box or make it deeper.
- **The same Modify in every view:**
  - **Points:** tap a point, including a dot on a grey shape. The arrow pad moves it one display step at a time (for example 1 mm), and the move button (four arrows) moves it by distances you type along the axes shown.
  - **Grey shapes:** selecting one also offers exact values, **3D Cut**, **3D Join**, and **Open in its view**. That last one switches to the view the shape was drawn in, where split line by point, join shapes and 2D cut apply to it.
- **Materials:** **Materials** on the main screen lists bars and pipes shared by all projects. Each has a name, a profile (rectangular or round, tube or bar), sizes, wall thickness and a color, and the list starts with a few examples such as a rectangular iron pipe 40 × 20 × 2 mm. Tap a line and then the **Material** button in the top bar, and pick a material: the whole line, including every segment of a broken line, becomes that bar or pipe in 3D and in STL/3MF export. The part is centered on the line, its height runs along the view's depth axis, and curves follow in short straight pieces.
- **Materials from your own drawings:** in any project, draw a cross-section; closed shapes inside it become holes, so a square inside a square is a tube. Tap the project name and choose **Save drawing as material**: it takes the selected shape, or the biggest outline in the drawing. You can also select the shape with Modify and tap the square-with-plus icon. Name it, for example "Tube 4×4", and set its real width; height and holes scale with it. It joins the shared list, and in any other project a line's **Line details** can use it: the line becomes that shape along its length.
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
- **Several points:** in Modify, drag a box on empty space. It now also picks end points of lines from other views. They move together with the arrows or by dragging, and the align buttons line all of them up.
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
- **Materials: bars, pipes and sheets.** A material is either a bar or pipe (a line becomes it) or a sheet with a thickness, such as steel sheet 3 mm, plywood 18 mm or glass 6 mm (a closed shape becomes it). Each has a look: plain, metal (shiny), wood (grain), glass (see-through) or stone. Save a drawing as either kind from the file menu, or add one on the Materials page.
- **Back, Bottom and Left:** long-press Front, Top or Profile (each shows ▾) to choose its other side: Front or Back, Top or Bottom, Profile (from the right) or Left. The other side shows the same plane mirrored, as seen from behind, below or the left; everything can be drawn and edited there. A tap on the button opens the side chosen last.
- **Material button:** with a line or shape selected (Modify, or one item in Select), **Material** is right after Properties in the top row. A line opens a short list of bars and pipes, with turning around the line; a shape opens its Made of choice.
- **Made of:** select a shape and tap **Material** (or Properties) to choose what it is made of: nothing, a sheet (the shape becomes that sheet, at its thickness), or a frame of lines (every edge becomes the chosen bar or pipe). A line's Properties lists bars and pipes. **Materials used** lists bar lengths and sheet areas.
- **Import 3D models:** file menu → **Import 3D model** reads STL, OBJ or 3MF files (for example from Printables or Thingiverse). The model is one solid piece: it shows as a dashed outline in every view and fully in 3D. Tap it to select it (drag to move), tap again for its size (proportions kept), exact position, 90° turns, color and look (metal, wood, glass…). It is saved with the project, included in an exported project .zip, and in STL/3MF export. Its single triangles can't be edited.
- **Flat drawing views:** shapes with depth are drawn flat, like the other shapes, with no perspective sides and no depth or vanishing-point handles (those came from the photo-measuring prototype). Depth is set in Properties and seen in View 3D.
- **Clone and copy:** Modify has **Clone lines** (just the outline) and **Clone all** (with material, fill and 3D depth); Select mode has **Copy lines** and **Copy all**. Copies get their own ids and shape numbers, so the original always stays as it was. Grouped items are cloned together.
- **Selection looks:** in Select mode a whole object (shape, broken line, 3D object) gets a wide orange band, a single line a thin one, and picked points a ring.
- **Turn:** with something selected (Modify or Select mode, or an imported model), **Turn ↺ / Turn ↻** in the top row turn it around its center in the current view, by 15°, 90° or 5° (tap the step button to change). Groups turn together. A shape turned so it lies flat in another view moves to that view (with its depth, groups, cuts and joins), e.g. draw in Front, turn 90° in Profile, and it lies flat in Top.
- **Rectangle and circle:** in the Lines menu (hold the Lines button). Drag from a corner to the opposite corner (rectangle) or from the center outward (circle): the shape follows the finger until you lift it. A rectangle blinks green and snaps to a square when its sides are nearly equal. Tapping two points works too. After each rectangle or circle the tool switches off and the new shape stays selected, ready for Material, Grow or Modify; a circle's center never snaps onto other lines.
- **Modify** (was Modify) is an icon in the bottom bar; its border blinks orange while it is on. Tap a line or shape first, then Modify: it stays selected. The most used buttons come first (Properties, Material, Turn, Clone, Weld, Delete); View 3D, Fit and Sizes move to the end while something is selected.
- **Grow a 3D object from any shape:** select a closed shape (rectangle, circle, any outline) or a 3D object from another view in Modify and tap **Grow**: blinking orange arrows appear (tap Grow again, or pick something else, to hide them). In the shape's own view drag the arrow up to make it deeper; in the other views drag the arrow on its front or back side. The depth shows while you drag.
- **Flat shapes and 3D:** shapes without depth are simply left out of View 3D and export (they no longer block it). Shapes with depth or a sheet material are drawn filled. A circle is drawn as a plain circle: no joint dots, and when selected it turns red as a whole. Corner dots of other shapes stay the same small size at any zoom. A circle shows one label (Ø diameter and material) instead of one per piece; other shapes name their material once.
- **Selection follows you between views:** a selected line, shape, 3D object, end point or set of points stays selected when you switch to another view, with the fine-move arrows, so you can keep moving it there. A circle always selects as a whole (its joints aren't points to pick).
- **Circles and ovals are one simple shape:** no loose joint points in any view. Select a circle in Modify and drag its four square handles: the side ones set the width, top and bottom the height (the center stays), so different sizes make an oval. Properties' Width and Height do the same, around the center. The label shows Ø diameter, or width × height for an oval.
- **Borders and outlines:** filled shapes (a fill, a sheet material or depth) have a thin single border and no corner dots (dots show while selected), on screen, seen edge-on from other views, and in exported pictures; the thick double line is for plain lines and unfilled outlines. **Outline** (Modify, or Select mode) makes the selected shape's or object's border into plain lines in the same place; the original keeps its fill, material and depth, and the new outline is selected so you can move or use it.
- **Objects stay in one piece in every view:** a 3D object's lines, points, selection band, turns and arrows all sit on the object as it is drawn (objects from Top and Profile used to show their lines and points a depth-length away). A picked 3D object is framed whole, front and back. A circle seen edge-on gets no size label.
- **Selected thing in the title:** with something selected, its name (Shape 1 · Plywood 18 mm, Circle, Line, Point…) replaces the file name, in red with a slowly blinking border; tap it for its properties. Deselect and the file name returns. **Delete** sits next to Undo whenever something is selected (Undo brings it back); the Properties and Delete icons are gone from the toolbar.
- **Cutting:** Select, tap the shape to cut, then the cutter, then **Cut 1st**. Shapes in the same view are cut in the drawing (3D follows, depth and material kept): a cutter wholly inside becomes a hole, otherwise its bite is taken out and the cutter is used up. Objects from different views are cut in 3D (Keep it / Use it up). Groups (a ring with its hole) count as one pick. 3D outlines of curves are simplified so cuts and joins stay quick.
- **Moving in depth:** moving 3D objects toward or away from the viewer (sideways in Top or Profile) moves them exactly with the arrows or finger; objects used to move twice as far as plain lines, so things drifted apart. Joints of smooth outlines (circles, curved cuts, holes) aren't shown or picked as points.
- **Zoom:** pinch in as far as you like; zooming out stops when everything fits on screen. Lines and marks don't grow beyond a set size when zoomed far in.
- **Holes and rings:** Cut 1st with a shape lying wholly inside another (for example a small circle inside a big one) makes it a hole: it stays empty in the drawing and goes right through in 3D and export, so two circles make a ring. A hole belongs to its shape: it moves, turns, resizes, copies and deletes with it, and tapping a hole picks the whole shape.
- **Debug log:** Settings → **Copy debug log** copies the last actions, messages and any crash, with a short summary of the open project, to paste into a message.
- **Weld:** lines that touch (an end on the other line's end or anywhere along it) can be welded: the touched line is split at that point so both share a corner, and they become one object that moves together. In Select mode pick them and tap **Merge/weld**; in Modify select a line and tap **Weld** to weld it to every line it touches.
- **Drawing onto lines:** with Lines on (straight, broken, curve or circle), a tap on an existing line starts or ends the new line exactly there: on its end point when close to one, else on the nearest point of the line. Selecting lines is for when no drawing tool is on.
- **Curve and circle:** the Lines button shows the line type in use (straight, curve or circle); hold it to choose. **Circle:** tap the center, then a point on the circle.
- **Filled or lines only:** a shape's Properties → Fill: **Lines only** (just the outline, no 3D body), **Filled**, or 50% / 90% see-through.
- **View 3D** in the top row shows the whole model in 3D (nothing is converted); **Back to 2D** returns.
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
- **Moving end points while drawing:** with Lines or Measure chosen, drag an existing end point to move it, as in Modify. Tapping still draws.
- **Joining crossing lines** happens on the drawing instead of a numbered dialog. Double-tap the crossing and the four parts blink, following curves as they really run. Tap the two to keep: they turn green and the parts to delete blink red. **Keep the other parts** keeps the rest instead, and **Finish** joins.
- **3D Cut and Join:** select a shape, tap the scissors (cut) or the joined-squares icon (join), then tap the other object. This works in any view, including the 3D tab, and the shapes can be from different views.
  - **Cut** removes the second object from the first, for example a hole through a box. You choose whether the cutting object stays in the model or is used up; either way it stays editable in its view.
  - **Join** merges overlapping objects into one printable solid.
  - The exact-values dialog lists every cut and join an object is part of, each with Undo. Results are checked like any solid: closed, outward-facing and with volume.
- **Moving points:** in every plane, with **Modify**, a corner of your own drawing can be dragged straight away, with no tap first.
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
