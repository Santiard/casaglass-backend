package com.casaglass.casaglass_backend.dto;

public class DashboardVentasPorSedeDTO {
    private Long sedeId;
    private String sede;
    private Integer ordenes;
    private Double monto;

    public DashboardVentasPorSedeDTO() {}

    public DashboardVentasPorSedeDTO(Long sedeId, String sede, Integer ordenes, Double monto) {
        this.sedeId = sedeId;
        this.sede = sede;
        this.ordenes = ordenes;
        this.monto = monto;
    }

    public Long getSedeId() {
        return sedeId;
    }

    public void setSedeId(Long sedeId) {
        this.sedeId = sedeId;
    }

    public String getSede() {
        return sede;
    }

    public void setSede(String sede) {
        this.sede = sede;
    }

    public Integer getOrdenes() {
        return ordenes;
    }

    public void setOrdenes(Integer ordenes) {
        this.ordenes = ordenes;
    }

    public Double getMonto() {
        return monto;
    }

    public void setMonto(Double monto) {
        this.monto = monto;
    }
}


