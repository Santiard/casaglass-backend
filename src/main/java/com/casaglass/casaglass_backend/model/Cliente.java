package com.casaglass.casaglass_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String nit;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(length = 150)
    private String direccion;

    @Column(length = 15)
    private String telefono;

    @Column(length = 50)
    private String ciudad;

    @Column(length = 100, unique = true)
    private String correo;

    private Boolean credito;  // true = tiene crédito, false = no
}
