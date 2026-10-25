package mods.eln.misc;

import mods.eln.misc.Obj3D.Obj3DPart;

import mods.eln.Eln;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Utility class used to load all eln models and corresponding obj files.
 */
public class Obj3DFolder {

    private Map<String, Obj3D> nameToObjHash = new HashMap<String, Obj3D>();
    private final Obj3D missingObj = new Obj3D();
    private final Set<String> missingObjNamesWarned = new HashSet<String>();

    /**
     * Load all obj models available in the release mod asset folder.
     */
    public void loadAllElnModels() {
        // 1.20.1 serves mod resources from the mod file system (a jar or the dev resource folders), so walk that
        // instead of locating the jar through the code source as 1.7.10 did.
        Path modelRoot = ModList.get().getModFileById(Eln.MODID).getFile().findResource("assets", "eln", "model");
        if (!Files.isDirectory(modelRoot)) return;
        int modelCount = 0;
        try (Stream<Path> files = Files.walk(modelRoot)) {
            for (Path file : (Iterable<Path>) files::iterator) {
                if (!file.getFileName().toString().toLowerCase().endsWith(".obj")) continue;
                String filename = modelRoot.relativize(file).toString().replace('\\', '/');
                Utils.println(String.format("Loading model %03d '%s'", ++modelCount, filename));
                loadObj(filename);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Load an obj file of a model.
     *
     * @param modelPath path inside model folder (ex. Vumeter/Vumeter.obj)
     */
    private void loadObj(String modelPath) {
        Obj3D obj = new Obj3D();
        if (obj.loadFile(modelPath)) {
            String tag = modelPath.replaceAll(".obj", "").replaceAll(".OBJ", "");
            tag = tag.substring(tag.lastIndexOf('/') + 1, tag.length());
            if (nameToObjHash.containsKey(tag)) {
                Utils.println("Double load of model " + tag);
            }
            nameToObjHash.put(Utils.resourcePath(tag), obj);    // name of the file, without extension
            Utils.println(String.format(" - model '%s' loaded", modelPath));
        } else {
            Utils.println(String.format(" - unable to load model '%s'", modelPath));
        }
    }

    public Obj3D getObj(String obj3DName) {
        Obj3D obj = nameToObjHash.get(Utils.resourcePath(obj3DName));
        if (obj == null) {
            if (missingObjNamesWarned.add(obj3DName)) {
                Utils.println(String.format("Missing model '%s', using fallback empty model", obj3DName));
            }
            return missingObj;
        }
        return obj;
    }

    public Obj3DPart getPart(String objName, String partName) {
        Obj3D obj = getObj(objName);
        if (obj == null) return null;
        return obj.getPart(partName);
    }

    public void draw(String objName, String partName) {
        Obj3DPart part = getPart(objName, partName);
        if (part != null) part.draw();
    }

    public Set<String> getObjectList() {
        return nameToObjHash.keySet();
    }
}
