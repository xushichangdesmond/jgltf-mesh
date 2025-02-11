/* 
 * Copyright (c) 2019, Chad Juliano, Kinetica DB Inc.
 * 
 * SPDX-License-Identifier: MIT
 */

package io.github.chadj2.mesh;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.vecmath.Point2f;
import javax.vecmath.Point3f;
import javax.vecmath.Tuple3f;
import javax.vecmath.Vector3f;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.javagl.jgltf.impl.v2.Material;
import de.javagl.jgltf.impl.v2.MeshPrimitive;
import de.javagl.jgltf.impl.v2.Node;
import io.github.chadj2.mesh.buffer.BufferVecFloat2;
import io.github.chadj2.mesh.buffer.BufferVecFloat3;
import io.github.chadj2.mesh.buffer.TriangleIndices;

/**
 * Build 3D Geometry from triangles or squares. Tangents, indices, and normals are automatically
 * generated.
 * @author Chad Juliano
 */
public class TriangleBuilder extends TopologyBuilder {
    
    private final static Logger LOG = LoggerFactory.getLogger(TriangleBuilder.class);
    
    /** The indices keep track of connectivity between triangle vertices. */
    protected final List<Integer> _indicesList = new ArrayList<Integer>();
    
    /** Suppress additions of normal vectors  */
    private boolean _supressNormals = false;

    /** Material for the mesh */
    private Material _material = null;

    /**
     * @param _name Name of the glTF mesh node.
     */
    public TriangleBuilder(String _name) {
        super(_name, TopologyMode.TRIANGLES);
    }
    
    /**
     * Enable or disable suppression of normals.
     */
    public void supressNormals(boolean _isEnabled) {
        this._supressNormals = _isEnabled;
    }
    
    /**
     * Set a Material that will be used when generating the mesh.
     * @param _material Material from the GltfWriter
     * @see MeshGltfWriter#newTextureMaterial(String)
     */
    public void setMaterial(Material _material) {
        this._material = _material;
    }
    
    @Override
    public void clear() { this._indicesList.clear(); }
    
    /**
     * This method should be called when all shapes have added. It will serialize the MeshVertex
     * list and indices to buffers.
     * @param _material Material from the GltfWriter
     * @see MeshGltfWriter#newTextureMaterial(String)
     * @deprecated Use {@link #setMaterial(Material)} instead
     */
    @Deprecated
    public Node build(MeshGltfWriter _geoWriter, Material _material) throws Exception {
        this.setMaterial(_material);
        return build(_geoWriter);
    }
    
    /**
     * Add a 3D triangle specified by 3 vertices. All triangles should be added through this
     * method so that normals can be calculated.
     * @see TopologyBuilder#newVertex
     */
    public void addTriangle(MeshVertex _vtx0, MeshVertex _vtx1, MeshVertex _vtx2) {
        // add indices
        this._indicesList.add(_vtx0.getIndex());
        this._indicesList.add(_vtx1.getIndex());
        this._indicesList.add(_vtx2.getIndex());
        
        if(!this._supressNormals) {
            // calculate normal with cross product
            final Vector3f _vec01 = new Vector3f();
            _vec01.sub(_vtx0.getVertex(), _vtx1.getVertex());
            
            final Vector3f _vec21 = new Vector3f();
            _vec21.sub(_vtx2.getVertex(), _vtx1.getVertex());
            
            Vector3f _normal = new Vector3f();
            _normal.cross(_vec21, _vec01);
            _normal.normalize();
            
            if(Float.isNaN(_normal.x) || Float.isNaN(_normal.y) || Float.isNaN(_normal.z)) {
                LOG.debug("Could not calculate normal for triangle: {},{},{}", 
                        _vtx0.getIndex(), _vtx1.getIndex(), _vtx2.getIndex());
                // create a fake normal
                _normal = new Vector3f(1f, 1f, 1f);
                _normal.normalize();
            }
            
            // add this normal to each vertex
            _vtx0.addNormal(_normal);
            _vtx1.addNormal(_normal);
            _vtx2.addNormal(_normal);
        }
    }

    /**
     * Add a 3D square represented by 4 vertices specified counter clockwise. 
     * All squares should be added though this method so that normals can be calculated.
     * @param _vtx0 Start of square
     * @param _vtx1 common to both triangles
     * @param _vtx2 common to both triangles
     * @param _vtx3 End of square
     * @see TopologyBuilder#newVertex
     */
    public void addSquare(MeshVertex _vtx0, MeshVertex _vtx1, MeshVertex _vtx2, MeshVertex _vtx3) {
        // We need to connect the points with counter-clockwise triangles.
        // Any triangles will do as long as the are CC.
        
        if(_vtx0 != null && _vtx1 != null && _vtx2 != null) {
            addTriangle(_vtx0, _vtx1, _vtx2);
            
            // calculate tangents
            Vector3f _vec01 = new Vector3f();
            _vec01.sub(_vtx0.getVertex(), _vtx1.getVertex());
            _vtx0.addTangent(_vec01);
            _vtx1.addTangent(_vec01);
        }
        
        if(_vtx2 != null && _vtx1 != null && _vtx3 != null) {
            addTriangle(_vtx2, _vtx1, _vtx3);

            // calculate tangents
            Vector3f _vec23 = new Vector3f();
            _vec23.sub(_vtx2.getVertex(), _vtx3.getVertex());
            _vtx2.addTangent(_vec23);
            _vtx3.addTangent(_vec23);
        }
    }
    
    protected BufferVecFloat3 _normals = null;

    @Override
    protected void buildBuffers(MeshGltfWriter _geoWriter, MeshPrimitive _meshPrimitive) throws Exception {
        super.buildBuffers(_geoWriter, _meshPrimitive);
        
        if(this._material != null) {
            int _materialIdx = _geoWriter.getGltf().getMaterials().indexOf(this._material);
            _meshPrimitive.setMaterial(_materialIdx);
        }

        if(this._indicesList.size() == 0) {
            throw new Exception("Mesh has no indices: " + this.getName());
        }
        
        BufferVecFloat2 _texCoords = new BufferVecFloat2(this.getName() + "-texCoords");
        this._normals = new BufferVecFloat3(this.getName() + "-normals");
        //BufferFloat4 _tangents = new BufferFloat4(this.getName(), "tangents");
        
        for(MeshVertex _meshVertex : this._vertexList) {
            Point2f _texCoord = _meshVertex.getTexCoord();
            if(_texCoord != null) {
                _texCoords.add(_texCoord);
            }
            
            if(_texCoords.size() > 0 && _texCoord == null) {
                throw new Exception("Each Vertex must have a texCoord: " + _meshVertex.toString());
            }
            
            this._normals.add(_meshVertex.getNormal());
            
            // leave out tangents for now.
            //this._tangents.add(_meshVertex.getTangent());
        }
        
        // copy triangles to the buffer
        TriangleIndices indices  = new TriangleIndices(this.getName());
        Iterator<Integer> iter = this._indicesList.iterator();
        while(iter.hasNext()) {
            indices.add(iter.next(), iter.next(), iter.next());
        }
        
        // flush all buffers to the primitive
        indices.build(_geoWriter, _meshPrimitive);
        _texCoords.buildAttrib(_geoWriter, _meshPrimitive, "TEXCOORD_0");
        this._normals.buildAttrib(_geoWriter, _meshPrimitive, "NORMAL");
        //_tangents.build(_geoWriter, _meshPrimitive);
        
        this._indicesList.clear();
    }
    
    /**
     * Add lines to indicate direction of normals.
     * @param _geoWriter
     * @param _size
     * @throws Exception
     */
    public void debugNormals(MeshGltfWriter _geoWriter, float _size) throws Exception {
        TopologyBuilder _builder = new TopologyBuilder("debug_normals", TopologyMode.LINES);
        
        for(int idx = 0; idx <  this._vertices.size(); idx++) {
             Tuple3f _lineStart = this._vertices.get(idx);
             Tuple3f _normal = this._normals.get(idx);
             
             Point3f _lineEnd = new Point3f();
             _lineEnd.scaleAdd(_size, _normal, _lineStart);
             
             MeshVertex _vStart = _builder.newVertex(_lineStart);
             _vStart.setColor(new Color(1f,1f,1f,1f));
             
             MeshVertex _vEnd = _builder.newVertex(_lineEnd);
             _vEnd.setColor(new Color(1f,1f,1f,1f));
        }
        
        _builder.build(_geoWriter);
    }
}
