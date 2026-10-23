# New App: Photo-Assisted 3D Modeling

## Prompt

Build a new Android app for creating dimensioned 3D models from a photo or an empty canvas. This is a separate project from Measure It. Do not change Measure It or remove its existing picture measurement and 3D viewing features.

The app should feel like an approachable touch-first CAD tool: drawing and measurements must be precise, dimensions must be meaningful, and the resulting model should be suitable for inspection and eventual 3D printing. Make the actual editor the main experience, not a landing page. Preserve work automatically and make every destructive action undoable.

## Projects and Images

- Provide a project library with thumbnails, user-editable titles and dates. Create a project from an imported or shared image, or from an empty scene.
- Save the original image and editable project data, including geometry, dimensions, calibration, view settings and materials. Reopening a project must restore the same model and dimensions.
- Support an image as a reference under the drawing, with adjustable opacity including fully hidden. Hiding it must not remove geometry.
- Provide an explicit, separate photo-correction tool before modeling: rotate, level, crop, expand the canvas and correct perspective using reference edges. Preview before applying, preserve image detail and avoid unintended cropping, mirroring or 180-degree rotations. Make correction undoable.
- Perspective correction and model calibration are separate operations. Clearly distinguish original image pixels, corrected-image coordinates and model coordinates. A photo cannot reveal unknown depth or correct all perspective from a single known distance.

## Coordinate System and Calibration

- Use a consistent right-handed 3D coordinate system with real-world units: X horizontal, Y vertical and Z depth. The front drawing plane is Z = 0; positive Z brings points toward the user in front view. Label axes and directions consistently in every view, dialog and export.
- A known distance establishes a uniform scale for X, Y and Z by default. Offer an optional advanced workflow to assign separate X and Y references on the front plane and a Z reference in a profile view, explicitly identifying the axis for each reference. Do not guess an axis silently.
- Explain when independent axis scaling is being used to compensate for a distorted image. A single front-facing image does not determine real depth: ask for a known depth or an explicit depth value rather than inventing one. Show missing or contradictory calibration clearly.
- Keep calibration, displayed dimensions, geometry, point positions, 3D view and exported model consistent. Switching units changes presentation, not physical dimensions. Offer mm, cm, m, inches and feet, with configurable 0-2 decimal places; default to mm.
- Allow editing or deleting calibration references and undoing those actions. Never silently distort a square into a rectangle or change a user-entered size.

## Drawing and Shapes

- Draw straight lines, connected broken lines and curves. A broken line is one editable object with shared vertices and a clear Finish control. Curves have an editable control point and can form closed shapes with straight or curved edges.
- Add points to lines by double-tapping, choose a breakpoint or curve point, and delete a point to reconnect its neighbors. Deleting the endpoint of a single straight line can delete that line, with a clear confirmation when needed.
- Detect useful high-contrast edges and corners locally and suggest snaps near the touch. Snap first to nearby vertices and intersections, then line interiors, then reliable image features. Leave the original touch unchanged if there is no reliable target. Manual adjustment must always remain possible.
- Double-tap near crossings or shared points to offer appropriate split, join and merge actions. Preview the affected segments, highlight parts to be retained and removed, and allow retaining remaining lines. Support straight lines, polylines, curves and shapes where geometrically valid.
- Select overlapping shapes to merge, or select a crossing line or shape as a cutting tool. Preview which object will be cut and whether the cutter remains. Preserve valid geometry and provide undo; do not silently delete unrelated segments.
- Allow closed shapes to have a line color, thickness and a matching surface color with adjustable transparency. Joined areas inherit the main shape's material. Inactive vertices should be unobtrusive; active vertices should be easy to identify and move.

## Measurements

- Create straight and connected multi-segment measurements. A segment can be unknown, calculated from calibration, manually entered or measured by a connected laser; distinguish these states visually and in the details.
- When calibration is unavailable, do not invent a size. Support an Unknown option and remember it for subsequent measurements. Offer an explicit setting to calculate unmeasured segments when calibration is available.
- Label connected measurement vertices consecutively A, B, C and so on, continuing beyond Z with AA, AB, etc. Show each segment and the total in a measurements page, without inventing names when the user has not supplied one.
- Provide selection checkboxes for whole measurements and individual segments. Show sums by color and a grand total when appropriate; exclude deselected segments and clearly indicate unknown values.
- Support optional labels, colors and units. Place labels for readability without obscuring endpoints or other labels, and allow manual fine adjustment. Highlight the selected line or segment before editing or deletion.
- Measurement points may have different Z coordinates. Display true 3D length and indicate when the front projection is shorter; use the Pythagorean relationship between projected length and Z difference. Changing Z must preserve fixed lengths or explicitly explain and resolve conflicting constraints.
- Where supported and verified for the specific device, allow a connected Bosch GLM laser measurement to fill any active length field automatically while still allowing manual entry. Show connection status, retain the connection across segment edits and provide exportable diagnostic logs. Do not claim compatibility with other Bluetooth devices without verifying their protocol.

## Editing and Interaction

- Use stable touch targets and layouts that respect Android system bars. Pinch zoom around the fingers; pan smoothly with two fingers even when an object is selected.
- In Adjust mode, tap or long-press to select a point, segment, line or shape. Give a brief vibration and a clear blinking or thicker highlight when selection is confirmed. Drag only the selected item; dragging empty space pans or deselects as appropriate.
- Supply fine-adjust arrows for precise movement, including curve control points and connected vertices. Holding an arrow moves continuously; pause at exact horizontal or vertical alignment and indicate alignment. Avoid repeated blinking while alignment remains unchanged.
- Provide rectangle selection of multiple points or objects, with the ability to deselect individual items. Allow group movement and horizontal/vertical alignment. When arranging objects, align objects rather than collapsing all their points together. Identify fixed-size references and ask which one to preserve when constraints conflict.
- Show movement deltas in the current units and a translucent scale grid when useful. Support step-by-step undo for drawing, editing, selection-dependent geometry changes, calibration, image correction and 3D operations.

## 3D Modeling

- Turn a closed shape into a 3D object by extrusion. Use an explicit action and a draggable depth handle; edit exact depth and Z position numerically, and remove extrusion without deleting the original shape.
- Let users move a whole object forward or backward relative to Z = 0, or select at least two vertices and set their Z position. Preserve existing thickness unless the user explicitly changes it. Save and restore all point depths and thicknesses.
- For curved or irregular closed shapes, generate filled front/back surfaces and side surfaces that follow the actual outline. Handle concave outlines correctly; detect and explain invalid or self-intersecting geometry instead of exporting a misleading model.
- Support independent object materials and subtle face shading. A draggable light direction in the 3D viewer should make faces toward the light brighter and opposite faces darker, with restrained variation. Avoid darkening overlapping translucent surfaces twice.
- Allow a shared perspective direction for visual extrusion previews, but keep the actual model dimensions and Z coordinates independent of a decorative vanishing-point handle. Ensure auxiliary extrusion lines sit behind real foreground geometry.

## Views and Export

- Offer distinct front, top, profile and freely orbitable 3D views, with smooth rotation, pan and pinch zoom. Orthographic front/top/profile views should support the editing and calibration operations appropriate to their planes, not merely show a read-only screenshot. Returning to the editor should return directly to the project.
- Provide optional visibility controls for reference frame, front indicator and measurements, initially off. Keep controls compact. The 3D-view icon must be distinct from the action that turns a shape into a 3D object.
- Export an image of the current 3D view without the imported photo or editor controls. Optionally include measurements only when explicitly requested. Export annotated 2D images with an optional legend outside the drawing area.
- Save and reopen a portable editable project, separate from image and mesh exports. Export printable 3D geometry in STL and, where supported, 3MF. Export only intended 3D objects, not the photo, 2D guide lines, labels, measurement arrows or reference frame. Preserve dimensions in millimeters and preserve color when the chosen format supports it.
- Validate mesh closure, normals, positive thickness, intersections and unsupported holes before export. Explain any problems and identify the affected object. Verify exported files in an independent 3D viewer and slicer; do not describe a model as print-ready without that validation.

## Acceptance and Quality

- Test from an empty project and a photo project: correction, calibration, drawing, extrusion, Z edits, save/reopen, undo, view changes and export.
- Test known dimensions, unit conversion, X/Y/Z directions and front projection math using automated geometry tests. Include concave shapes, curves, multiple objects, bad calibration and invalid meshes.
- Verify touch behavior and layout on Android portrait and landscape screens, including system insets, long-press selection, smooth pinch/pan, and large and small models. Test on an actual device where possible.
- Keep operations responsive; run expensive detection, correction and mesh generation off the UI thread. Report progress and errors, and avoid losing unsaved work.
- Release an incremented Android APK and provide a direct download link after build and verification. Clearly state any device-testing or format limitations.
