package main.java;

import com.jme3.app.SimpleApplication;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Sphere;
import com.jme3.light.DirectionalLight;
import com.jme3.texture.Texture;
import com.jme3.asset.TextureKey;

public class PlanetSimulation extends SimpleApplication {

    private Node planetOrbitNode;

    public static void main(String[] args) {
        PlanetSimulation app = new PlanetSimulation();
        app.start();
    }

    @Override
    public void simpleInitApp() {
        flyCam.setMoveSpeed(20);

        // Sun
        Sphere sunMesh = new Sphere(32, 32, 2f);
        Geometry sun = new Geometry("Sun", sunMesh);
        Material sunMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture sunTex = assetManager.loadTexture(new TextureKey("textures/jupiter.jpg", false));
        sunMat.setTexture("DiffuseMap", sunTex);
        sunMat.setBoolean("UseMaterialColors", true);
        sunMat.setColor("Diffuse", ColorRGBA.White);
        sun.setMaterial(sunMat);
        rootNode.attachChild(sun);

        // Directional light to illuminate planets
        DirectionalLight sunLight = new DirectionalLight();
        sunLight.setDirection(new Vector3f(-1, -2, -3).normalizeLocal());
        sunLight.setColor(ColorRGBA.White.mult(2f));
        rootNode.addLight(sunLight);

        // Planet orbit node
        planetOrbitNode = new Node("PlanetOrbit");
        rootNode.attachChild(planetOrbitNode);

        // Example: Earth
        Sphere planetMesh = new Sphere(32, 32, 0.5f);
        Geometry planet = new Geometry("Earth", planetMesh);
        Material planetMat = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        Texture planetTex = assetManager.loadTexture(new TextureKey("textures/jupiter.jpg", false));
        planetMat.setTexture("DiffuseMap", planetTex);
        planetMat.setBoolean("UseMaterialColors", true);
        planetMat.setColor("Diffuse", ColorRGBA.White);
        planet.setMaterial(planetMat);

        // Position planet at orbit radius
        planet.setLocalTranslation(5, 0, 0); // 5 units away from Sun
        planetOrbitNode.attachChild(planet);
    }

    @Override
    public void simpleUpdate(float tpf) {
        // Rotate the planet on its own axis
        planetOrbitNode.getChild("Earth").rotate(0, tpf, 0);

        // Revolve the planet around the Sun
        planetOrbitNode.rotate(0, tpf * 0.5f, 0);
    }
}
