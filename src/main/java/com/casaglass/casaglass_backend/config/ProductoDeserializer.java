package com.casaglass.casaglass_backend.config;

import com.casaglass.casaglass_backend.model.Producto;
import com.casaglass.casaglass_backend.model.ProductoVidrio;
import com.casaglass.casaglass_backend.model.ColorProducto;
import com.casaglass.casaglass_backend.model.TipoProducto;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 🔧 DESERIALIZADOR PERSONALIZADO PARA PRODUCTO
 * 
 * Detecta automáticamente si un JSON debe deserializarse como ProductoVidrio
 * basándose en la presencia de los campos mm, m1, m2.
 * 
 * Lógica:
 * - Si el JSON tiene los campos mm, m1, m2 → ProductoVidrio
 * - Si no los tiene → Producto base
 * 
 * IMPORTANTE: Usa un ObjectMapper sin el deserializador para evitar recursión infinita
 */
public class ProductoDeserializer extends JsonDeserializer<Producto> {

    @Override
    public Producto deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);
        
        // ✅ Verificar si tiene los campos específicos de ProductoVidrio
        boolean tieneCamposVidrio = node.has("mm") && node.has("m1") && node.has("m2");
        
        if (tieneCamposVidrio) {
            
            // ✅ Crear un ObjectMapper sin el deserializador para evitar recursión
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            
            // Deserializar manualmente los campos
            ProductoVidrio pv = new ProductoVidrio();
            
            // Campos de Producto base
            if (node.has("id")) pv.setId(node.get("id").asLong());
            if (node.has("codigo")) pv.setCodigo(node.get("codigo").asText());
            if (node.has("nombre")) pv.setNombre(node.get("nombre").asText());
            if (node.has("color") && !node.get("color").isNull()) {
                pv.setColor(ColorProducto.valueOf(node.get("color").asText()));
            }
            if (node.has("tipo") && !node.get("tipo").isNull()) {
                pv.setTipo(TipoProducto.valueOf(node.get("tipo").asText()));
            }
            if (node.has("cantidad") && !node.get("cantidad").isNull()) {
                pv.setCantidad(node.get("cantidad").asInt());
            }
            if (node.has("costo") && !node.get("costo").isNull()) {
                pv.setCosto(node.get("costo").asDouble());
            }
            if (node.has("precio1") && !node.get("precio1").isNull()) {
                pv.setPrecio1(node.get("precio1").asDouble());
            }
            if (node.has("precio2") && !node.get("precio2").isNull()) {
                pv.setPrecio2(node.get("precio2").asDouble());
            }
            if (node.has("precio3") && !node.get("precio3").isNull()) {
                pv.setPrecio3(node.get("precio3").asDouble());
            }
            if (node.has("descripcion") && !node.get("descripcion").isNull()) {
                pv.setDescripcion(node.get("descripcion").asText());
            }
            if (node.has("posicion") && !node.get("posicion").isNull()) {
                pv.setPosicion(node.get("posicion").asText());
            }
            
            // Categoría (si viene)
            if (node.has("categoria") && node.get("categoria").has("id")) {
                com.casaglass.casaglass_backend.model.Categoria cat = new com.casaglass.casaglass_backend.model.Categoria();
                cat.setId(node.get("categoria").get("id").asLong());
                pv.setCategoria(cat);
            }
            
            // Campos específicos de ProductoVidrio
            pv.setMm(node.get("mm").asDouble());
            pv.setM1(node.get("m1").asDouble());
            pv.setM2(node.get("m2").asDouble());
            // m1m2 se calculará automáticamente con @PrePersist
            
            return pv;
        } else {
            // Deserializar como Producto base
            
            // ✅ Crear un ObjectMapper sin el deserializador personalizado para evitar recursión
            ObjectMapper mapper = new ObjectMapper();
            mapper.findAndRegisterModules();
            
            // Deserializar manualmente los campos de Producto base
            Producto producto = new Producto();
            
            // Campos básicos
            if (node.has("id")) producto.setId(node.get("id").asLong());
            if (node.has("codigo")) producto.setCodigo(node.get("codigo").asText());
            if (node.has("nombre")) producto.setNombre(node.get("nombre").asText());
            if (node.has("color") && !node.get("color").isNull()) {
                producto.setColor(ColorProducto.valueOf(node.get("color").asText()));
            }
            if (node.has("tipo") && !node.get("tipo").isNull()) {
                producto.setTipo(TipoProducto.valueOf(node.get("tipo").asText()));
            }
            if (node.has("cantidad") && !node.get("cantidad").isNull()) {
                producto.setCantidad(node.get("cantidad").asInt());
            }
            if (node.has("costo") && !node.get("costo").isNull()) {
                producto.setCosto(node.get("costo").asDouble());
            }
            if (node.has("precio1") && !node.get("precio1").isNull()) {
                producto.setPrecio1(node.get("precio1").asDouble());
            }
            if (node.has("precio2") && !node.get("precio2").isNull()) {
                producto.setPrecio2(node.get("precio2").asDouble());
            }
            if (node.has("precio3") && !node.get("precio3").isNull()) {
                producto.setPrecio3(node.get("precio3").asDouble());
            }
            if (node.has("descripcion") && !node.get("descripcion").isNull()) {
                producto.setDescripcion(node.get("descripcion").asText());
            }
            if (node.has("posicion") && !node.get("posicion").isNull()) {
                producto.setPosicion(node.get("posicion").asText());
            }
            
            // Categoría (si viene)
            if (node.has("categoria") && node.get("categoria").has("id")) {
                com.casaglass.casaglass_backend.model.Categoria cat = new com.casaglass.casaglass_backend.model.Categoria();
                cat.setId(node.get("categoria").get("id").asLong());
                producto.setCategoria(cat);
            }
            
            return producto;
        }
    }
}

