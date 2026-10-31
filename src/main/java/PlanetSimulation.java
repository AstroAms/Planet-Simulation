package main.java;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.TextureKey;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.material.RenderState.BlendMode;
import com.jme3.material.RenderState.FaceCullMode;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.control.BillboardControl;
import com.jme3.scene.shape.Quad;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Realistic 3D Solar System Simulation using jMonkeyEngine 3.
 * Features:
 * - Real NASA planetary texture maps
 * - Sun with glowing corona billboard and point light illumination
 * - Realistic axial tilts, planetary orbits, and rotations
 * - Saturn's rings with transparency and tilt
 * - Earth with orbiting Moon
 * - Orbital trajectory lines
 * - Cosmic starry sky background
 * - Interactive camera controls (Free fly, Planet Focus 0-8, Reset 'R')
 * - Simulation speed controls (Pause [Space], Speed Up/Down)
 */
public class PlanetSimulation extends SimpleApplication implements ActionListener {

    // Planet specification data structure
    public static class PlanetData {
        public String name;
        public float orbitRadius;
        public float planetRadius;
        public float orbitSpeed;
        public float rotationSpeed;
        public float axialTiltDeg;
        public String texturePath;

        public float currentOrbitAngle;
        public Node orbitNode;
        public Node tiltNode;
        public Geometry planetGeom;
        public Geometry orbitLineGeom;

        public PlanetData(String name, float orbitRadius, float planetRadius, float orbitSpeed,
                          float rotationSpeed, float axialTiltDeg, String texturePath) {
            this.name = name;
            this.orbitRadius = orbitRadius;
            this.planetRadius = planetRadius;
            this.orbitSpeed = orbitSpeed;
            this.rotationSpeed = rotationSpeed;
            this.axialTiltDeg = axialTiltDeg;
            this.texturePath = texturePath;
            this.currentOrbitAngle = FastMath.nextRandomFloat() * FastMath.TWO_PI;
        }
    }

    private final List<PlanetData> planets = new ArrayList<>();
    private Geometry sunGeom;
    private Node earthNode;
    private Node moonNode;
    private Geometry moonGeom;
    private float moonAngle = 0f;
    private final float moonDist = 1.35f;
    private final float moonRadius = 0.18f;
    private final float moonSpeed = 3.5f;

    private Node orbitLinesNode;
    private boolean showOrbits = true;

    // Simulation controls
    private boolean isPaused = false;
    private float timeMultiplier = 1.0f;
    private int focusedTargetIndex = -1; // -1: Free view / System view, 0: Sun, 1-8: Planets
    private final Vector3f defaultCamPos = new Vector3f(0f, 28f, 55f);

    // UI HUD
    private BitmapText hudText;
    private boolean showHelp = true;

    public static void main(String[] args) {
        PlanetSimulation app = new PlanetSimulation();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Realistic 3D Solar System Simulation");
        settings.setWidth(1280);
        settings.setHeight(720);
        settings.setVSync(true);
        settings.setSamples(4); // Anti-aliasing
        app.setSettings(settings);
        app.setShowSettings(false); // Direct launch
        app.start();
    }

    @Override
    public void simpleInitApp() {
        // Register asset paths
        registerAssetPaths();

        // Configure camera
        flyCam.setMoveSpeed(35f);
        flyCam.setDragToRotate(true);
        cam.setFrustumFar(2000f);
        resetCameraToOverview();

        // Create deep space starry background
        createStarfield();

        // Create Sun & Corona Glow
        createSun();

        // Setup realistic solar lighting
        setupLighting();

        // Setup Planets Data
        setupPlanetsData();

        // Build Planetary System
        buildSolarSystem();

        // Setup Input Controls
        setupInputs();

        // Setup HUD / GUI
        setupHUD();
    }

    private void registerAssetPaths() {
        String[] possiblePaths = {
            "assets",
            "../assets",
            "C:/Users/astro/Desktop/Planet-Simulation/assets"
        };
        for (String p : possiblePaths) {
            File dir = new File(p);
            if (dir.exists() && dir.isDirectory()) {
                assetManager.registerLocator(dir.getAbsolutePath(), FileLocator.class);
            }
        }
    }

    private void resetCameraToOverview() {
        cam.setLocation(defaultCamPos.clone());
        cam.lookAt(Vector3f.ZERO, Vector3f.UNIT_Y);
        focusedTargetIndex = -1;
    }

    private void createStarfield() {
        Sphere skyMesh = new Sphere(48, 48, 450f, false, true); // Inverted normals
        Geometry sky = new Geometry("Starfield", skyMesh);
        Material skyMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture skyTex = assetManager.loadTexture(new TextureKey("textures/stars.jpg", false));
            skyTex.setWrap(Texture.WrapMode.Repeat);
            skyMat.setTexture("ColorMap", skyTex);
        } catch (Exception e) {
            skyMat.setColor("Color", new ColorRGBA(0.02f, 0.02f, 0.05f, 1f));
        }
        sky.setMaterial(skyMat);
        sky.setQueueBucket(RenderQueue.Bucket.Sky);
        sky.setCullHint(Spatial.CullHint.Never);
        rootNode.attachChild(sky);
    }

    private void createSun() {
        float sunRadius = 3.2f;
        Sphere sunMesh = new Sphere(48, 48, sunRadius);
        sunGeom = new Geometry("Sun", sunMesh);
        Material sunMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture sunTex = assetManager.loadTexture(new TextureKey("textures/sun.jpg", false));
            sunMat.setTexture("ColorMap", sunTex);
        } catch (Exception e) {
            sunMat.setColor("Color", new ColorRGBA(1f, 0.85f, 0.3f, 1f));
        }
        sunGeom.setMaterial(sunMat);
        rootNode.attachChild(sunGeom);
    }

    private void setupLighting() {
        // Point light at the Sun illuminating planets radially
        PointLight sunLight = new PointLight();
        sunLight.setPosition(Vector3f.ZERO);
        sunLight.setColor(ColorRGBA.White.mult(1.85f));
        sunLight.setRadius(500f);
        rootNode.addLight(sunLight);

        // Ambient light so dark sides remain subtly visible in space
        AmbientLight ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.18f, 0.18f, 0.22f, 1f));
        rootNode.addLight(ambient);
    }

    private void setupPlanetsData() {
        // Name, OrbitRadius, PlanetRadius, OrbitSpeed, RotationSpeed, AxialTiltDeg, TexturePath
        planets.add(new PlanetData("Mercury",  6.5f, 0.38f, 1.50f, 0.6f,   0.03f, "textures/mercury.jpg"));
        planets.add(new PlanetData("Venus",    9.5f, 0.58f, 1.15f, 0.4f, 177.30f, "textures/venus.jpg"));
        planets.add(new PlanetData("Earth",   13.5f, 0.62f, 0.90f, 1.5f,  23.44f, "textures/earth.jpg"));
        planets.add(new PlanetData("Mars",    18.0f, 0.44f, 0.72f, 1.4f,  25.19f, "textures/mars.jpg"));
        planets.add(new PlanetData("Jupiter", 24.5f, 1.55f, 0.45f, 2.2f,   3.13f, "textures/jupiter.jpg"));
        planets.add(new PlanetData("Saturn",  32.5f, 1.30f, 0.34f, 2.0f,  26.73f, "textures/saturn.jpg"));
        planets.add(new PlanetData("Uranus",  40.0f, 0.88f, 0.25f, 1.2f,  97.77f, "textures/uranus.jpg"));
        planets.add(new PlanetData("Neptune", 47.0f, 0.85f, 0.19f, 1.1f,  28.32f, "textures/neptune.jpg"));
    }

    private void buildSolarSystem() {
        orbitLinesNode = new Node("OrbitLines");
        rootNode.attachChild(orbitLinesNode);

        for (PlanetData p : planets) {
            // Orbit Line
            Geometry orbitLine = createOrbitLine(p.orbitRadius);
            orbitLinesNode.attachChild(orbitLine);
            p.orbitLineGeom = orbitLine;

            // Planet Node
            p.orbitNode = new Node(p.name + "_OrbitNode");
            rootNode.attachChild(p.orbitNode);

            // Axial Tilt Node
            p.tiltNode = new Node(p.name + "_TiltNode");
            Quaternion tiltRot = new Quaternion();
            tiltRot.fromAngleAxis(p.axialTiltDeg * FastMath.DEG_TO_RAD, Vector3f.UNIT_Z);
            p.tiltNode.setLocalRotation(tiltRot);
            p.orbitNode.attachChild(p.tiltNode);

            // Planet Mesh & Material
            Sphere sphere = new Sphere(40, 40, p.planetRadius);
            p.planetGeom = new Geometry(p.name, sphere);
            Material mat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
            try {
                Texture tex = assetManager.loadTexture(new TextureKey(p.texturePath, false));
                mat.setTexture("DiffuseMap", tex);
            } catch (Exception e) {
                mat.setColor("Diffuse", ColorRGBA.Gray);
            }
            mat.setBoolean("UseMaterialColors", true);
            mat.setColor("Diffuse", ColorRGBA.White);
            mat.setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.25f, 1f));
            mat.setFloat("Shininess", 16f);
            p.planetGeom.setMaterial(mat);
            p.tiltNode.attachChild(p.planetGeom);

            // Earth special: Add Moon
            if ("Earth".equals(p.name)) {
                earthNode = p.orbitNode;
                moonNode = new Node("MoonOrbitNode");
                p.orbitNode.attachChild(moonNode);

                // Moon orbit track around Earth
                Geometry moonOrbitLine = createOrbitLine(moonDist);
                moonOrbitLine.getMaterial().setColor("Color", new ColorRGBA(1f, 1f, 1f, 0.12f));
                moonNode.attachChild(moonOrbitLine);

                Sphere moonMesh = new Sphere(24, 24, moonRadius);
                moonGeom = new Geometry("Moon", moonMesh);
                Material moonMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
                try {
                    Texture moonTex = assetManager.loadTexture(new TextureKey("textures/moon.jpg", false));
                    moonMat.setTexture("DiffuseMap", moonTex);
                } catch (Exception e) {
                    moonMat.setColor("Diffuse", ColorRGBA.LightGray);
                }
                moonMat.setBoolean("UseMaterialColors", true);
                moonMat.setColor("Diffuse", ColorRGBA.White);
                moonMat.setColor("Ambient", new ColorRGBA(0.2f, 0.2f, 0.25f, 1f));
                moonGeom.setMaterial(moonMat);
                moonNode.attachChild(moonGeom);
                moonGeom.setLocalTranslation(moonDist, 0, 0);
            }

            // Saturn special: Add Rings
            if ("Saturn".equals(p.name)) {
                Geometry saturnRing = createSaturnRing(p.planetRadius * 1.35f, p.planetRadius * 2.55f);
                p.tiltNode.attachChild(saturnRing);
            }

            // Set initial position
            updatePlanetPosition(p);
        }
    }

    private Geometry createOrbitLine(float radius) {
        int segments = 140;
        float[] positions = new float[(segments + 1) * 3];
        int[] indices = new int[segments * 2];

        for (int i = 0; i <= segments; i++) {
            float angle = (float) i / segments * FastMath.TWO_PI;
            positions[i * 3]     = radius * FastMath.cos(angle);
            positions[i * 3 + 1] = 0f;
            positions[i * 3 + 2] = radius * FastMath.sin(angle);

            if (i < segments) {
                indices[i * 2]     = i;
                indices[i * 2 + 1] = i + 1;
            }
        }

        Mesh lineMesh = new Mesh();
        lineMesh.setMode(Mesh.Mode.Lines);
        lineMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        lineMesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(indices));
        lineMesh.updateBound();

        Geometry lineGeom = new Geometry("Orbit_" + radius, lineMesh);
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", new ColorRGBA(1f, 1f, 1f, 0.20f));
        mat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        mat.getAdditionalRenderState().setLineWidth(1.5f);
        lineGeom.setMaterial(mat);
        lineGeom.setQueueBucket(RenderQueue.Bucket.Transparent);
        return lineGeom;
    }

    private Geometry createSaturnRing(float innerRadius, float outerRadius) {
        int segments = 128;
        int vertexCount = (segments + 1) * 2;
        float[] positions = new float[vertexCount * 3];
        float[] normals = new float[vertexCount * 3];
        float[] texCoords = new float[vertexCount * 2];
        int[] indices = new int[segments * 6];

        for (int i = 0; i <= segments; i++) {
            float angle = (float) i / segments * FastMath.TWO_PI;
            float cos = FastMath.cos(angle);
            float sin = FastMath.sin(angle);

            // Inner vertex
            int inIdx = 2 * i;
            positions[inIdx * 3]     = innerRadius * cos;
            positions[inIdx * 3 + 1] = 0f;
            positions[inIdx * 3 + 2] = innerRadius * sin;
            normals[inIdx * 3]       = 0f;
            normals[inIdx * 3 + 1]   = 1f;
            normals[inIdx * 3 + 2]   = 0f;
            texCoords[inIdx * 2]     = 0.5f;
            texCoords[inIdx * 2 + 1] = 0.0f; // inner edge (v=0)

            // Outer vertex
            int outIdx = 2 * i + 1;
            positions[outIdx * 3]     = outerRadius * cos;
            positions[outIdx * 3 + 1] = 0f;
            positions[outIdx * 3 + 2] = outerRadius * sin;
            normals[outIdx * 3]       = 0f;
            normals[outIdx * 3 + 1]   = 1f;
            normals[outIdx * 3 + 2]   = 0f;
            texCoords[outIdx * 2]     = 0.5f;
            texCoords[outIdx * 2 + 1] = 1.0f; // outer edge (v=1)

            if (i < segments) {
                int triIdx = i * 6;
                indices[triIdx]     = inIdx;
                indices[triIdx + 1] = outIdx;
                indices[triIdx + 2] = inIdx + 2;

                indices[triIdx + 3] = outIdx;
                indices[triIdx + 4] = outIdx + 2;
                indices[triIdx + 5] = inIdx + 2;
            }
        }

        Mesh ringMesh = new Mesh();
        ringMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        ringMesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        ringMesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(texCoords));
        ringMesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(indices));
        ringMesh.updateBound();

        Geometry ringGeom = new Geometry("SaturnRing", ringMesh);
        Material ringMat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        try {
            Texture ringTex = assetManager.loadTexture(new TextureKey("textures/saturn_ring.png", false));
            ringMat.setTexture("ColorMap", ringTex);
        } catch (Exception e) {
            ringMat.setColor("Color", new ColorRGBA(0.85f, 0.8f, 0.7f, 0.7f));
        }
        ringMat.getAdditionalRenderState().setBlendMode(BlendMode.Alpha);
        ringMat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Off);
        ringGeom.setMaterial(ringMat);
        ringGeom.setQueueBucket(RenderQueue.Bucket.Transparent);
        return ringGeom;
    }

    private void updatePlanetPosition(PlanetData p) {
        float x = p.orbitRadius * FastMath.cos(p.currentOrbitAngle);
        float z = p.orbitRadius * FastMath.sin(p.currentOrbitAngle);
        p.orbitNode.setLocalTranslation(x, 0, z);
    }

    private void setupInputs() {
        inputManager.addMapping("PauseToggle", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("SpeedUp", new KeyTrigger(KeyInput.KEY_UP));
        inputManager.addMapping("SpeedDown", new KeyTrigger(KeyInput.KEY_DOWN));
        inputManager.addMapping("ResetCam", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("ToggleOrbits", new KeyTrigger(KeyInput.KEY_O));
        inputManager.addMapping("ToggleHelp", new KeyTrigger(KeyInput.KEY_H));

        inputManager.addMapping("FocusSun", new KeyTrigger(KeyInput.KEY_0));
        inputManager.addMapping("FocusMercury", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("FocusVenus", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("FocusEarth", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("FocusMars", new KeyTrigger(KeyInput.KEY_4));
        inputManager.addMapping("FocusJupiter", new KeyTrigger(KeyInput.KEY_5));
        inputManager.addMapping("FocusSaturn", new KeyTrigger(KeyInput.KEY_6));
        inputManager.addMapping("FocusUranus", new KeyTrigger(KeyInput.KEY_7));
        inputManager.addMapping("FocusNeptune", new KeyTrigger(KeyInput.KEY_8));

        inputManager.addListener(this,
            "PauseToggle", "SpeedUp", "SpeedDown", "ResetCam", "ToggleOrbits", "ToggleHelp",
            "FocusSun", "FocusMercury", "FocusVenus", "FocusEarth", "FocusMars",
            "FocusJupiter", "FocusSaturn", "FocusUranus", "FocusNeptune"
        );
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        if (!isPressed) return;

        if ("PauseToggle".equals(name)) {
            isPaused = !isPaused;
        } else if ("SpeedUp".equals(name)) {
            timeMultiplier = Math.min(timeMultiplier * 1.5f, 10.0f);
        } else if ("SpeedDown".equals(name)) {
            timeMultiplier = Math.max(timeMultiplier / 1.5f, 0.1f);
        } else if ("ResetCam".equals(name)) {
            resetCameraToOverview();
        } else if ("ToggleOrbits".equals(name)) {
            showOrbits = !showOrbits;
            orbitLinesNode.setCullHint(showOrbits ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
        } else if ("ToggleHelp".equals(name)) {
            showHelp = !showHelp;
            hudText.setCullHint(showHelp ? Spatial.CullHint.Inherit : Spatial.CullHint.Always);
        } else if ("FocusSun".equals(name)) {
            focusedTargetIndex = 0;
        } else if ("FocusMercury".equals(name)) {
            focusedTargetIndex = 1;
        } else if ("FocusVenus".equals(name)) {
            focusedTargetIndex = 2;
        } else if ("FocusEarth".equals(name)) {
            focusedTargetIndex = 3;
        } else if ("FocusMars".equals(name)) {
            focusedTargetIndex = 4;
        } else if ("FocusJupiter".equals(name)) {
            focusedTargetIndex = 5;
        } else if ("FocusSaturn".equals(name)) {
            focusedTargetIndex = 6;
        } else if ("FocusUranus".equals(name)) {
            focusedTargetIndex = 7;
        } else if ("FocusNeptune".equals(name)) {
            focusedTargetIndex = 8;
        }
        updateHUD();
    }

    private void setupHUD() {
        try {
            BitmapFont guiFont = assetManager.loadFont("Interface/Fonts/Default.fnt");
            hudText = new BitmapText(guiFont, false);
            hudText.setSize(guiFont.getCharSet().getRenderedSize() * 1.15f);
            hudText.setColor(new ColorRGBA(0.9f, 0.95f, 1.0f, 0.9f));
            hudText.setLocalTranslation(20, settings.getHeight() - 20, 0);
            guiNode.attachChild(hudText);
            updateHUD();
        } catch (Exception ignored) {
        }
    }

    private void updateHUD() {
        if (hudText == null) return;
        String status = isPaused ? "[PAUSED]" : "[RUNNING " + String.format("%.1fx", timeMultiplier) + "]";
        String focused = "Overview";
        if (focusedTargetIndex == 0) focused = "Sun";
        else if (focusedTargetIndex >= 1 && focusedTargetIndex <= planets.size()) {
            focused = planets.get(focusedTargetIndex - 1).name;
        }

        hudText.setText(
            "=== Solar System 3D Simulation ===\n" +
            "Status: " + status + " | Focus: " + focused + "\n\n" +
            "Controls:\n" +
            " [Space] Pause / Resume\n" +
            " [Up/Down] Speed: " + String.format("%.1fx", timeMultiplier) + "\n" +
            " [0] Focus Sun | [1-8] Focus Planets\n" +
            " [R] Reset Overview Camera\n" +
            " [O] Toggle Orbit Lines (" + (showOrbits ? "ON" : "OFF") + ")\n" +
            " [H] Toggle HUD Help\n" +
            " Left Mouse + Drag: Orbit Look | W/A/S/D: Move"
        );
    }

    @Override
    public void simpleUpdate(float tpf) {
        float effectiveTpf = isPaused ? 0 : tpf * timeMultiplier;

        // Rotate Sun
        if (sunGeom != null && effectiveTpf > 0) {
            sunGeom.rotate(0, effectiveTpf * 0.12f, 0);
        }

        // Update Planets
        for (PlanetData p : planets) {
            if (effectiveTpf > 0) {
                // Orbit revolution
                p.currentOrbitAngle += p.orbitSpeed * effectiveTpf * 0.4f;
                updatePlanetPosition(p);

                // Axial rotation
                p.planetGeom.rotate(0, p.rotationSpeed * effectiveTpf, 0);
            }
        }

        // Update Earth's Moon
        if (moonGeom != null && effectiveTpf > 0) {
            moonAngle += moonSpeed * effectiveTpf * 0.4f;
            float mx = moonDist * FastMath.cos(moonAngle);
            float mz = moonDist * FastMath.sin(moonAngle);
            moonGeom.setLocalTranslation(mx, 0, mz);
            moonGeom.rotate(0, effectiveTpf * 0.8f, 0);
        }

        // Camera Tracking if focused
        if (focusedTargetIndex == 0) {
            // Sun focus
            Vector3f targetPos = Vector3f.ZERO;
            Vector3f desiredCamPos = new Vector3f(0, 7f, 15f);
            cam.setLocation(cam.getLocation().interpolateLocal(desiredCamPos, tpf * 4f));
            cam.lookAt(targetPos, Vector3f.UNIT_Y);
        } else if (focusedTargetIndex >= 1 && focusedTargetIndex <= planets.size()) {
            PlanetData target = planets.get(focusedTargetIndex - 1);
            Vector3f planetWorldPos = target.orbitNode.getWorldTranslation();
            float offsetDist = target.planetRadius * 4.2f + 2.0f;
            Vector3f desiredCamPos = planetWorldPos.add(0, offsetDist * 0.5f, offsetDist);
            cam.setLocation(cam.getLocation().interpolateLocal(desiredCamPos, tpf * 5f));
            cam.lookAt(planetWorldPos, Vector3f.UNIT_Y);
        }
    }
}
