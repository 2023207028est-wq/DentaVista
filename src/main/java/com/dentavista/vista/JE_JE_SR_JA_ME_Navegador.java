package com.dentavista.vista;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;

/** Navegación entre pantallas del panel principal. */
public interface JE_JE_SR_JA_ME_Navegador {

    enum JE_JE_SR_JA_ME_Seccion { INICIO, PACIENTES, AGENDA, TRATAMIENTOS, USUARIOS, MI_ODONTOGRAMA, MIS_CITAS }

    void JE_JE_SR_JA_ME_irA(JE_JE_SR_JA_ME_Seccion seccion);

    void JE_JE_SR_JA_ME_abrirFicha(JE_JE_SR_JA_ME_Paciente paciente);
}
