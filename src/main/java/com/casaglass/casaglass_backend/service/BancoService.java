package com.casaglass.casaglass_backend.service;

import com.casaglass.casaglass_backend.model.Banco;
import com.casaglass.casaglass_backend.repository.BancoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class BancoService {

    private final BancoRepository bancoRepository;

    public BancoService(BancoRepository bancoRepository) {
        this.bancoRepository = bancoRepository;
    }

    /**
     * 📋 LISTAR TODOS LOS BANCOS
     */
    @Transactional(readOnly = true)
    public List<Banco> listarTodos() {
        return bancoRepository.findAll();
    }

    /**
     * 🔍 OBTENER BANCO POR ID
     */
    @Transactional(readOnly = true)
    public Optional<Banco> obtenerPorId(Long id) {
        return bancoRepository.findById(id);
    }

    /**
     * ➕ CREAR NUEVO BANCO
     */
    @Transactional
    public Banco crear(Banco banco) {
        // Validar que no exista un banco con el mismo nombre
        if (bancoRepository.existsByNombreIgnoreCase(banco.getNombre())) {
            throw new IllegalArgumentException("Ya existe un banco con el nombre: " + banco.getNombre());
        }
        
        return bancoRepository.save(banco);
    }

    /**
     * ✏️ ACTUALIZAR BANCO
     */
    @Transactional
    public Banco actualizar(Long id, Banco bancoActualizado) {
        Banco bancoExistente = bancoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Banco no encontrado con ID: " + id));

        // Validar nombre duplicado (excluyendo el banco actual)
        Optional<Banco> bancoConMismoNombre = bancoRepository.findByNombre(bancoActualizado.getNombre());
        if (bancoConMismoNombre.isPresent() && !bancoConMismoNombre.get().getId().equals(id)) {
            throw new IllegalArgumentException("Ya existe otro banco con el nombre: " + bancoActualizado.getNombre());
        }

        bancoExistente.setNombre(bancoActualizado.getNombre());

        return bancoRepository.save(bancoExistente);
    }

    /**
     * 🗑️ ELIMINAR BANCO
     */
    @Transactional
    public void eliminar(Long id) {
        if (!bancoRepository.existsById(id)) {
            throw new IllegalArgumentException("Banco no encontrado con ID: " + id);
        }
        bancoRepository.deleteById(id);
    }
}
