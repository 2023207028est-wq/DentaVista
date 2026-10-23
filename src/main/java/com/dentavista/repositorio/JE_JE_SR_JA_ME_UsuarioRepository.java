package com.dentavista.repositorio;

import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JE_JE_SR_JA_ME_UsuarioRepository extends JpaRepository<JE_JE_SR_JA_ME_Usuario, Long> {

    Optional<JE_JE_SR_JA_ME_Usuario> findByUsuarioIgnoreCase(String usuario);

    long countByRolAndActivoTrue(JE_JE_SR_JA_ME_Rol rol);

    List<JE_JE_SR_JA_ME_Usuario> findAllByOrderByNombreAsc();

    List<JE_JE_SR_JA_ME_Usuario> findByRolAndActivoTrueOrderByNombreAsc(JE_JE_SR_JA_ME_Rol rol);

    List<JE_JE_SR_JA_ME_Usuario> findByRolNotOrderByNombreAsc(JE_JE_SR_JA_ME_Rol rol);

    Optional<JE_JE_SR_JA_ME_Usuario> findByPaciente(com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente paciente);
}
