/* 
 * Copyright (c) 2022, Chad Juliano, Kinetica DB Inc.
 * 
 * SPDX-License-Identifier: MIT
 */

package io.github.chadj2.mesh.sphere;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.vecmath.Point3f;

import io.github.chadj2.mesh.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.javagl.jgltf.impl.v2.Material;
import de.javagl.jgltf.impl.v2.Mesh;
import de.javagl.jgltf.impl.v2.MeshPrimitive;
import de.javagl.jgltf.impl.v2.Node;
import io.github.chadj2.mesh.MeshGltfWriter;
import io.github.chadj2.mesh.MeshGltfWriter.AlphaMode;

/**
 * Generate a large set of spheres of various sizes, colors, and transparencies. 
 * Spheres of the same color will get reused to reduce the file size. 
 * @author Chad Juliano
 */
public class SphereFactory extends SphereFactoryBase {
    
    private final static Logger LOG = LoggerFactory.getLogger(SphereFactory.class);

    protected final MeshGltfWriter _writer;
    private final IcosphereBuilder _builder = new IcosphereBuilder("icosphere");

    /**
     * Map of color/lod to mesh indices.
     */
    private final Map<String, Integer> _colorLodToMeshIdx = new HashMap<>();
    
    /**
     * Map of lod to mesh indices.
     */
    private final Map<Integer, Integer> _lodToMeshIdx = new HashMap<>();
    
    public SphereFactory(MeshGltfWriter _writer) {
        super("sphere");
        this._builder.setIsPatterned(false);
        this._builder.setColor(this.getColor());
        
        // need to set BLEND mode or transparency does not work.
        _writer.setAlphaMode(AlphaMode.BLEND);
        
        this._writer = _writer;
    }

    @Override
    public void build() {
        // do nothing
    }
    
    /**
     * Add a sphere at the given position.
     * @param pos sphere position
     * @param eventId sphere ID for click events
     * @throws Exception
     */
    @Override
    public Node addSphere(Point3f pos, String eventId) throws Exception {
        Integer meshIdx = getMeshColorLod();
        getTransform().transform(pos);
        
        Node node = new Node();
        int nodeIdx = this._writer.addNode(node);
        node.setMesh(meshIdx);
        node.setName(String.format("%s[%d]-node", getName(), nodeIdx));

        if(LOG.isDebugEnabled()) {
            String colorStr = String.format("r=%d,g=%d,b=%d,a=%d", 
                    this.getColor().r,
                    this.getColor().g,
                    this.getColor().b,
                    this.getColor().a);
            LOG.debug("Add Sphere: pos=<{}> radius=<{}> color=<{}> ", pos, this.getRadius(), colorStr);
        }
        
        float[] scale = new float[] { this.getRadius(), this.getRadius(), this.getRadius() };
        node.setScale(scale);
        float[] translation = {pos.x, pos.y, pos.z};
        node.setTranslation(translation);
        
        // add the eventId to the extras
        final Map<String, Object> extras = new HashMap<>();
        extras.put("eventId", eventId);
        node.setExtras(extras);
        
        return node;
    }
    
    /**
     * Create a new mesh for the color/LOD or return a cached version.
     * @return
     * @throws Exception
     */
    protected int getMeshColorLod() throws Exception {
        String key = String.format("%X-%d", this.getColor().argb(), this.getMaxDetail());
        Integer meshIdx = this._colorLodToMeshIdx.get(key);
        if(meshIdx != null) {
            // found a cached mesh for this color/lod combo
            return meshIdx;
        }
        
        meshIdx = getMeshLod();
        
        // add this sphere to the cache.
        this._colorLodToMeshIdx.put(key, meshIdx);
        return meshIdx;
    }
    
    /**
     * Create a new mesh for the LOD or return a cached version.
     * @return
     * @throws Exception
     */
    private int getMeshLod() throws Exception {
        Integer meshIdx = this._lodToMeshIdx.get(this.getMaxDetail());
        if(meshIdx != null) {
            // found a mesh for the LOD. 
            // create a copy of this mesh with the new color
            int newMeshIdx = copyMesh(meshIdx, this.getColor());
            return newMeshIdx;
        }
        
        // create a new mesh for this LOD
        meshIdx = createMesh(this.getColor(), this.getMaxDetail());

        // add this sphere to the cache.
        this._lodToMeshIdx.put(this.getMaxDetail(), meshIdx);
        return meshIdx;
    }
    
    /**
     * Copy a mesh with a material having a new color.
     * @param origMeshIdx
     * @return
     */
    private int copyMesh(int origMeshIdx, Color color) {
        List<Mesh> meshList = this._writer.getGltf().getMeshes();
        Mesh origMesh = meshList.get(origMeshIdx);
        
        Mesh newMesh = new Mesh();
        meshList.add(newMesh);
        int newMeshIdx = meshList.indexOf(newMesh);

        LOG.debug("Copy Mesh: <{}> {} -> {}", origMesh.getName(), origMeshIdx, newMeshIdx);
        String name = String.format("%s[%d]", origMesh.getName(), newMeshIdx);
        newMesh.setName(name);
        
        MeshPrimitive newMeshPr = new MeshPrimitive();
        newMesh.addPrimitives(newMeshPr);
        MeshPrimitive orighMeshPr = origMesh.getPrimitives().get(0);
        
        newMeshPr.setIndices(orighMeshPr.getIndices());
        newMeshPr.setMaterial(orighMeshPr.getMaterial());
        newMeshPr.setMode(orighMeshPr.getMode());
        newMeshPr.setAttributes(orighMeshPr.getAttributes());
        
        // create the new material
        Material material = newMaterial(color);
        int materialIdx = this._writer.getGltf().getMaterials().indexOf(material);
        newMeshPr.setMaterial(materialIdx);
        
        return newMeshIdx;
    }
    
    /**
     * Create a new mesh for the given color and LOD.
     * @param color
     * @param lod
     * @return
     * @throws Exception
     */
    private int createMesh(Color color, int lod) throws Exception {
        // Get the next mesh ID.
        int meshIdx = 0;
        List<Mesh> meshList = this._writer.getGltf().getMeshes();
        if(meshList != null) {
            meshIdx = meshList.size();
        }

        this._builder.addIcosphere(lod);
        
        // set the name of the builder so that all objects in the JSON can be
        // identified with this sphere
        String name = String.format("%s(%d)[%d]", getName(), lod, meshIdx);
        this._builder.setName(name);
        LOG.info("Create Sphere for LOD: <{}> {}", lod, name);
        
        Material material = newMaterial(color);
        this._builder.setMaterial(material);
        meshIdx = this._builder.buildMesh(this._writer);
        
        return meshIdx;
    }
    
    /**
     * Get a new material with an appropriate color.
     * @return
     */
    private Material newMaterial(Color color) {
        return this._writer.newBlendMaterial("sphere", 0.7f, 0.5f, color);
    }
    
}
