package com.dentavista.datos;

import com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Odontograma;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Pieza;
import com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Consulta;
import com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoItem;
import com.dentavista.modelo.JE_JE_SR_JA_ME_ItemPlan;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Paciente;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Rol;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Tratamiento;
import com.dentavista.modelo.JE_JE_SR_JA_ME_Usuario;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_ConsultaRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_PacienteRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_TratamientoRepository;
import com.dentavista.repositorio.JE_JE_SR_JA_ME_UsuarioRepository;
import com.dentavista.servicio.JE_JE_SR_JA_ME_ClinicaService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_PlanService;
import com.dentavista.servicio.JE_JE_SR_JA_ME_Seguridad;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

import static com.dentavista.dominio.JE_JE_SR_JA_ME_EstadoDental.*;
import static com.dentavista.dominio.JE_JE_SR_JA_ME_Superficie.*;

/**
 * Datos de demostración: usuarios, catálogo de precios y pacientes con historia
 * clínica real de varios meses (consultas, hallazgos y tratamientos realizados),
 * para que la línea de tiempo del odontograma muestre la evolución de cada boca.
 * Las fechas son relativas a hoy, así la demo nunca envejece.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true", matchIfMissing = true)
public class JE_JE_SR_JA_ME_DatosDemo implements CommandLineRunner {

    private final JE_JE_SR_JA_ME_UsuarioRepository usuarios;
    private final JE_JE_SR_JA_ME_PacienteRepository pacientes;
    private final JE_JE_SR_JA_ME_TratamientoRepository tratamientos;
    private final JE_JE_SR_JA_ME_ConsultaRepository consultas;
    private final JE_JE_SR_JA_ME_ClinicaService clinica;
    private final JE_JE_SR_JA_ME_PlanService plan;
    private final TransactionTemplate tx;
    private final com.dentavista.repositorio.JE_JE_SR_JA_ME_CitaRepository citas;
    private final com.dentavista.repositorio.JE_JE_SR_JA_ME_ItemPlanRepository itemsPlan;
    private final com.dentavista.servicio.JE_JE_SR_JA_ME_PagoService pagosService;
    private final com.dentavista.repositorio.JE_JE_SR_JA_ME_PagoRepository pagosRepo;
    private final com.dentavista.servicio.JE_JE_SR_JA_ME_RecetaService recetasService;

    private final Map<String, JE_JE_SR_JA_ME_Tratamiento> catalogo = new TreeMap<>();
    private JE_JE_SR_JA_ME_Usuario draParedes;
    private JE_JE_SR_JA_ME_Usuario drMamani;

    @Override
    public void run(String... args) {
        if (usuarios.count() > 0) {
            JE_JE_SR_JA_ME_completarVersionesAnteriores();
            return;
        }
        tx.executeWithoutResult(s -> {
            JE_JE_SR_JA_ME_crearUsuarios();
            JE_JE_SR_JA_ME_crearCatalogo();
            JE_JE_SR_JA_ME_crearPacientes();
            JE_JE_SR_JA_ME_crearAccesoPaciente();
            JE_JE_SR_JA_ME_crearCitas();
            JE_JE_SR_JA_ME_crearPagosYRecetas();
        });
        log.info("Datos de demostración cargados: {} pacientes", pacientes.count());
    }

    /**
     * Bases creadas con una versión anterior: se agregan solo los módulos nuevos
     * (acceso del paciente, agenda, pagos y recetas) sin tocar lo que el usuario cargó.
     */
    private void JE_JE_SR_JA_ME_completarVersionesAnteriores() {
        if (pacientes.findByDni("72845163").isEmpty()) {
            return; // no es la base de demostración
        }
        tx.executeWithoutResult(s -> {
            draParedes = usuarios.findByUsuarioIgnoreCase("dra.paredes").orElse(null);
            drMamani = usuarios.findByUsuarioIgnoreCase("dr.mamani").orElse(null);
            if (draParedes == null || drMamani == null) {
                return;
            }
            if (usuarios.findByUsuarioIgnoreCase("72845163").isEmpty()) {
                JE_JE_SR_JA_ME_crearAccesoPaciente();
            }
            if (citas.count() == 0) {
                JE_JE_SR_JA_ME_crearCitas();
            }
            if (pagosRepo.count() == 0) {
                JE_JE_SR_JA_ME_crearPagosYRecetas();
                log.info("Demostración actualizada con pagos y recetas");
            }
        });
    }

    // --------------------------------------------------------------- usuarios

    private void JE_JE_SR_JA_ME_crearUsuarios() {
        usuarios.save(JE_JE_SR_JA_ME_usuario("admin", "admin123", "Administrador del sistema", null, JE_JE_SR_JA_ME_Rol.ADMIN));
        draParedes = usuarios.save(JE_JE_SR_JA_ME_usuario("dra.paredes", "dentista123", "Dra. Lucía Paredes Quispe", "COP 28417", JE_JE_SR_JA_ME_Rol.ODONTOLOGO));
        drMamani = usuarios.save(JE_JE_SR_JA_ME_usuario("dr.mamani", "dentista123", "Dr. Rubén Mamani Condori", "COP 30552", JE_JE_SR_JA_ME_Rol.ODONTOLOGO));
        usuarios.save(JE_JE_SR_JA_ME_usuario("recepcion", "recepcion123", "Milagros Apaza Ticona", null, JE_JE_SR_JA_ME_Rol.ASISTENTE));
    }

    private static JE_JE_SR_JA_ME_Usuario JE_JE_SR_JA_ME_usuario(String login, String clave, String nombre, String cop, JE_JE_SR_JA_ME_Rol rol) {
        return JE_JE_SR_JA_ME_Usuario.builder()
                .usuario(login)
                .contrasena(JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_hashear(clave))
                .nombre(nombre)
                .colegiatura(cop)
                .email(login.replace(".", "") + "@sonrisaandina.pe")
                .rol(rol)
                .activo(true)
                .build();
    }

    // ------------------------------------------------------- acceso y agenda

    /** La paciente de la demo puede entrar con su DNI y ver su propio odontograma. */
    private void JE_JE_SR_JA_ME_crearAccesoPaciente() {
        JE_JE_SR_JA_ME_Paciente valeria = pacientes.findByDni("72845163").orElseThrow();
        usuarios.save(JE_JE_SR_JA_ME_Usuario.builder()
                .usuario("72845163")
                .contrasena(JE_JE_SR_JA_ME_Seguridad.JE_JE_SR_JA_ME_hashear("paciente123"))
                .nombre(valeria.JE_JE_SR_JA_ME_nombreCompleto())
                .email(valeria.getEmail())
                .rol(JE_JE_SR_JA_ME_Rol.PACIENTE)
                .paciente(valeria)
                .activo(true)
                .build());
    }

    /**
     * Agenda de dos semanas: citas atendidas en los días pasados, confirmadas en los
     * próximos y algunas solicitudes de pacientes por confirmar.
     */
    private void JE_JE_SR_JA_ME_crearCitas() {
        Random r = new Random(1510);
        List<JE_JE_SR_JA_ME_Paciente> todos = pacientes.findAll();
        JE_JE_SR_JA_ME_Paciente valeria = pacientes.findByDni("72845163").orElseThrow();
        JE_JE_SR_JA_ME_Usuario[] doctores = {draParedes, drMamani};
        java.time.LocalTime[] manana = {java.time.LocalTime.of(8, 30), java.time.LocalTime.of(9, 30),
                java.time.LocalTime.of(10, 30), java.time.LocalTime.of(11, 30), java.time.LocalTime.of(12, 0)};
        java.time.LocalTime[] tarde = {java.time.LocalTime.of(15, 30), java.time.LocalTime.of(16, 30), java.time.LocalTime.of(17, 30)};
        String[] motivos = {"Control semestral", "Dolor en molar", "Limpieza", "Revisión de restauración", "Sensibilidad al frío"};
        LocalDateTime ahora = LocalDateTime.now();
        int solicitadas = 0;
        for (int dia = -4; dia <= 9; dia++) {
            LocalDate fecha = LocalDate.now().plusDays(dia);
            if (fecha.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                continue;
            }
            java.util.Set<Long> ocupados = new java.util.HashSet<>();
            for (JE_JE_SR_JA_ME_Usuario doc : doctores) {
                List<java.time.LocalTime> horas = new ArrayList<>(List.of(manana));
                if (fecha.getDayOfWeek() != java.time.DayOfWeek.SATURDAY) {
                    horas.addAll(List.of(tarde));
                }
                java.util.Collections.shuffle(horas, r);
                int n = 2 + r.nextInt(3);
                for (java.time.LocalTime h : horas.subList(0, Math.min(n, horas.size()))) {
                    JE_JE_SR_JA_ME_Paciente p = todos.get(r.nextInt(todos.size()));
                    if (p.equals(valeria) || !ocupados.add(p.getId())) {
                        continue;
                    }
                    LocalDateTime inicio = fecha.atTime(h);
                    // Quien ya terminó su plan no tiene citas por venir: queda "para dar de alta".
                    if (inicio.isAfter(ahora) && itemsPlan.findByPacienteAndEstado(p, JE_JE_SR_JA_ME_EstadoItem.PENDIENTE).isEmpty()) {
                        continue;
                    }
                    JE_JE_SR_JA_ME_ItemPlan item = r.nextBoolean() ? itemsPlan.findByPacienteAndEstado(p, JE_JE_SR_JA_ME_EstadoItem.PENDIENTE).stream()
                            .findFirst().orElse(null) : null;
                    com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita estado;
                    if (inicio.isBefore(ahora)) {
                        estado = r.nextInt(10) == 0 ? com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.NO_ASISTIO
                                : com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.ATENDIDA;
                    } else if (solicitadas < 2 && dia >= 2 && r.nextBoolean()) {
                        estado = com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.SOLICITADA;
                        solicitadas++;
                    } else {
                        estado = com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.CONFIRMADA;
                    }
                    JE_JE_SR_JA_ME_cita(p, doc, inicio, h.getHour() == 12 ? 30 : (item != null && item.getPrecio().intValue() > 300 ? 60 : 30),
                            item == null ? motivos[r.nextInt(motivos.length)] : null, item, estado,
                            estado == com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.SOLICITADA ? p.JE_JE_SR_JA_ME_nombreCompleto() + " (portal)" : "Milagros Apaza Ticona");
                }
            }
        }
        // Valeria: su última consulta y su próxima cita (para el portal del paciente).
        LocalDate proximo = LocalDate.now().plusDays(2);
        if (proximo.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            proximo = proximo.plusDays(1);
        }
        JE_JE_SR_JA_ME_ItemPlan pendiente = itemsPlan.findByPacienteAndEstado(valeria, JE_JE_SR_JA_ME_EstadoItem.PENDIENTE).stream()
                .filter(i -> i.getPieza() != null).findFirst().orElse(null);
        JE_JE_SR_JA_ME_cita(valeria, draParedes, proximo.atTime(9, 0), 30, null, pendiente, com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.CONFIRMADA,
                "Milagros Apaza Ticona");
        JE_JE_SR_JA_ME_cita(valeria, draParedes, LocalDate.now().minusDays(6).atTime(10, 0), 30, "Control: sensibilidad al frío", null,
                com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita.ATENDIDA, "Valeria Quispe Huanca (portal)");
    }

    /**
     * Caja y recetas de la demo: la mayoría de pacientes pagó parte de lo realizado
     * (unos deben, otros pagaron por adelantado) y algunas consultas tienen receta.
     */
    private void JE_JE_SR_JA_ME_crearPagosYRecetas() {
        Random r = new Random(77);
        com.dentavista.modelo.JE_JE_SR_JA_ME_MedioPago[] medios = com.dentavista.modelo.JE_JE_SR_JA_ME_MedioPago.values();
        for (JE_JE_SR_JA_ME_Paciente p : pacientes.findAll()) {
            java.math.BigDecimal realizado = itemsPlan.findByPacienteAndEstado(p, JE_JE_SR_JA_ME_EstadoItem.REALIZADO).stream()
                    .map(JE_JE_SR_JA_ME_ItemPlan::getPrecio).reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            if (realizado.signum() == 0) {
                continue;
            }
            // Quien terminó todo su plan lo pagó completo (queda "para dar de alta");
            // el resto, entre el 60 % y el 100 % de lo realizado, en dos abonos.
            boolean terminado = itemsPlan.findByPacienteAndEstado(p, JE_JE_SR_JA_ME_EstadoItem.PENDIENTE).isEmpty();
            double fraccion = terminado ? 1.0 : 0.6 + r.nextInt(5) * 0.1;
            java.math.BigDecimal total = realizado.multiply(java.math.BigDecimal.valueOf(fraccion))
                    .setScale(0, java.math.RoundingMode.DOWN);
            java.math.BigDecimal primero = total.multiply(new java.math.BigDecimal("0.6")).setScale(0, java.math.RoundingMode.DOWN);
            List<JE_JE_SR_JA_ME_Consulta> cs = consultas.findByPacienteOrderByFechaAsc(p);
            LocalDateTime f1 = cs.get(0).getFecha().plusHours(1);
            LocalDateTime f2 = cs.get(cs.size() - 1).getFecha().plusHours(1);
            JE_JE_SR_JA_ME_pago(p, primero, medios[r.nextInt(medios.length)], f1, "Primer abono");
            JE_JE_SR_JA_ME_pago(p, total.subtract(primero), medios[r.nextInt(medios.length)], f2, null);
        }
        // Recetas en consultas con endodoncia, extracción o dolor.
        JE_JE_SR_JA_ME_receta(pacientes.findByDni("72845163").orElseThrow(), "Endodoncia pieza 46",
                List.of(new com.dentavista.modelo.JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item("Clindamicina 300 mg", "1 cápsula cada 8 horas por 7 días"),
                        new com.dentavista.modelo.JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item("Paracetamol 500 mg", "1 tableta cada 8 horas si hay dolor")),
                "Alérgica a penicilina: se indica clindamicina. Evitar masticar del lado tratado 24 horas.");
        JE_JE_SR_JA_ME_receta(pacientes.findByDni("09214587").orElseThrow(), "Extracción 17",
                List.of(new com.dentavista.modelo.JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item("Amoxicilina 500 mg", "1 cápsula cada 8 horas por 7 días"),
                        new com.dentavista.modelo.JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item("Paracetamol 500 mg", "1 tableta cada 8 horas por 3 días"),
                        new com.dentavista.modelo.JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item("Clorhexidina 0.12 % enjuague bucal",
                                "Enjuagar 15 ml por 30 segundos, 2 veces al día por 10 días")),
                "Paciente anticoagulado: se evita ibuprofeno. Compresas frías 24 horas, dieta blanda.");
    }

    private void JE_JE_SR_JA_ME_pago(JE_JE_SR_JA_ME_Paciente p, java.math.BigDecimal monto, com.dentavista.modelo.JE_JE_SR_JA_ME_MedioPago medio, LocalDateTime fecha,
                      String nota) {
        if (monto.signum() <= 0) {
            return;
        }
        pagosService.JE_JE_SR_JA_ME_guardar(com.dentavista.modelo.JE_JE_SR_JA_ME_Pago.builder().paciente(p).monto(monto).medio(medio).fecha(fecha)
                .nota(nota).registradoPor("Milagros Apaza Ticona").build());
    }

    /** Receta en la consulta cuyo motivo coincide (la de la endodoncia, la de la extracción…). */
    private void JE_JE_SR_JA_ME_receta(JE_JE_SR_JA_ME_Paciente p, String motivo, List<com.dentavista.modelo.JE_JE_SR_JA_ME_Receta.JE_JE_SR_JA_ME_Item> items, String indicaciones) {
        consultas.findByPacienteOrderByFechaAsc(p).stream()
                .filter(c -> c.getMotivo() != null && c.getMotivo().contains(motivo))
                .findFirst()
                .ifPresent(c -> recetasService.JE_JE_SR_JA_ME_guardar(com.dentavista.modelo.JE_JE_SR_JA_ME_Receta.builder()
                        .paciente(p).odontologo(c.getOdontologo()).consulta(c).fecha(c.getFecha().plusMinutes(40))
                        .items(new ArrayList<>(items)).indicaciones(indicaciones).build()));
    }

    private void JE_JE_SR_JA_ME_cita(JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Usuario doc, LocalDateTime inicio, int duracion, String motivo, JE_JE_SR_JA_ME_ItemPlan item,
                      com.dentavista.modelo.JE_JE_SR_JA_ME_EstadoCita estado, String creadaPor) {
        citas.save(com.dentavista.modelo.JE_JE_SR_JA_ME_Cita.builder()
                .paciente(p).odontologo(doc).inicio(inicio).duracion(duracion).motivo(motivo).itemPlan(item)
                .estado(estado).creadaPor(creadaPor).creadaEn(inicio.minusDays(3)).build());
    }

    // --------------------------------------------------------------- catalogo

    private void JE_JE_SR_JA_ME_crearCatalogo() {
        JE_JE_SR_JA_ME_tratamiento("PRO", "Profilaxis y destartraje", "Limpieza profesional con ultrasonido y pulido", "120", null, false, false);
        JE_JE_SR_JA_ME_tratamiento("OBT", "Obturación con resina", "Restauración estética por cara afectada", "90", OBTURADO, true, true);
        JE_JE_SR_JA_ME_tratamiento("SEL", "Sellante de fosas y fisuras", "Prevención de caries en caras oclusales", "45", SELLANTE, true, true);
        JE_JE_SR_JA_ME_tratamiento("REC", "Reconstrucción por fractura", "Reconstrucción con resina de alta resistencia", "150", OBTURADO, true, true);
        JE_JE_SR_JA_ME_tratamiento("END", "Endodoncia", "Tratamiento de conductos (unirradicular / multirradicular)", "480", ENDODONCIA, false, true);
        JE_JE_SR_JA_ME_tratamiento("EXO", "Extracción simple", "Exodoncia con anestesia local", "150", AUSENTE, false, true);
        JE_JE_SR_JA_ME_tratamiento("COR", "Corona de porcelana", "Corona metal-porcelana o zirconio", "950", CORONA, false, true);
        JE_JE_SR_JA_ME_tratamiento("IMP", "Implante dental", "Implante de titanio con corona", "3200", IMPLANTE, false, true);
        JE_JE_SR_JA_ME_tratamiento("RXP", "Radiografía periapical", "Radiografía digital de una pieza", "30", null, false, true);
        JE_JE_SR_JA_ME_tratamiento("BLA", "Blanqueamiento dental", "Blanqueamiento en consultorio (2 sesiones)", "600", null, false, false);
    }

    private void JE_JE_SR_JA_ME_tratamiento(String codigo, String nombre, String desc, String precio, JE_JE_SR_JA_ME_EstadoDental resultado,
                             boolean porCara, boolean requierePieza) {
        catalogo.put(codigo, tratamientos.save(JE_JE_SR_JA_ME_Tratamiento.builder()
                .codigo(codigo)
                .nombre(nombre)
                .descripcion(desc)
                .precio(new BigDecimal(precio))
                .estadoResultante(resultado)
                .porCara(porCara)
                .requierePieza(requierePieza)
                .activo(true)
                .build()));
    }

    // --------------------------------------------------------------- pacientes

    private void JE_JE_SR_JA_ME_crearPacientes() {
        JE_JE_SR_JA_ME_valeria();
        Random r = new Random(20261014);
        JE_JE_SR_JA_ME_aleatorio(r, "41258963", "Jorge Luis", "Mamani Ccama", 1980, "M", null, "Hipertensión arterial controlada (losartán)", drMamani);
        JE_JE_SR_JA_ME_aleatorio(r, "70125478", "Ana Lucía", "Flores Torres", 1995, "F", null, null, draParedes);
        JE_JE_SR_JA_ME_carlos();
        JE_JE_SR_JA_ME_aleatorio(r, "43698521", "Rosa Elena", "Apaza Ticona", 1987, "F", null, "Diabetes tipo 2 (metformina)", draParedes);
        JE_JE_SR_JA_ME_aleatorio(r, "74521036", "Diego", "Fernández Rojas", 2003, "M", null, null, drMamani);
        JE_JE_SR_JA_ME_aleatorio(r, "78963210", "Lucía", "Huamán Choque", 2010, "F", null, null, draParedes);
        JE_JE_SR_JA_ME_aleatorio(r, "29874563", "Miguel Ángel", "Ccallo Yupanqui", 1973, "M", "Látex", null, drMamani);
        JE_JE_SR_JA_ME_aleatorio(r, "72036985", "Sofía", "Ramírez Vilca", 1998, "F", "Ibuprofeno", null, draParedes);
        JE_JE_SR_JA_ME_aleatorio(r, "45632178", "Fernando", "Tito Calla", 1991, "M", null, null, drMamani);
        JE_JE_SR_JA_ME_aleatorio(r, "47125896", "Gabriela", "Choque Mendoza", 1985, "F", null, "Embarazo 2.º trimestre", draParedes);
        JE_JE_SR_JA_ME_aleatorio(r, "71458236", "Renzo", "Vargas Pilco", 1999, "M", null, null, drMamani);
    }

    private JE_JE_SR_JA_ME_Paciente JE_JE_SR_JA_ME_paciente(String dni, String nombres, String apellidos, LocalDate nacimiento, String sexo,
                              String alergias, String antecedentes, String telefono) {
        return pacientes.save(JE_JE_SR_JA_ME_Paciente.builder()
                .dni(dni)
                .nombres(nombres)
                .apellidos(apellidos)
                .fechaNacimiento(nacimiento)
                .sexo(sexo)
                .telefono(telefono)
                .email(nombres.split(" ")[0].toLowerCase()
                        .replace("á", "a").replace("é", "e").replace("í", "i").replace("ó", "o").replace("ú", "u")
                        + "." + apellidos.split(" ")[0].toLowerCase().replace("á", "a").replace("é", "e") + "@gmail.com")
                .direccion("Juliaca, Puno")
                .alergias(alergias)
                .antecedentes(antecedentes)
                .fechaRegistro(LocalDateTime.now().minusDays(420))
                .activo(true)
                .build());
    }

    /** Paciente estrella de la demo: historia de un año con todos los tipos de hallazgo. */
    private void JE_JE_SR_JA_ME_valeria() {
        JE_JE_SR_JA_ME_Paciente p = JE_JE_SR_JA_ME_paciente("72845163", "Valeria", "Quispe Huanca", LocalDate.of(1996, 4, 12), "F",
                "Penicilina", null, "951 284 736");

        JE_JE_SR_JA_ME_Visita v1 = JE_JE_SR_JA_ME_visita(p, draParedes, 390, "Primera consulta: dolor al masticar del lado derecho");
        v1.pieza(18, AUSENTE).pieza(28, AUSENTE).pieza(38, AUSENTE).pieza(48, AUSENTE)
                .JE_JE_SR_JA_ME_cara(16, OCLUSAL, CARIES).JE_JE_SR_JA_ME_cara(16, MESIAL, CARIES).JE_JE_SR_JA_ME_cara(15, DISTAL, CARIES)
                .JE_JE_SR_JA_ME_cara(26, OCLUSAL, CARIES).JE_JE_SR_JA_ME_cara(36, OCLUSAL, CARIES)
                .pieza(46, ENDODONCIA_INDICADA).JE_JE_SR_JA_ME_cara(21, OCLUSAL, FRACTURA)
                .JE_JE_SR_JA_ME_realiza("PRO", null, null);
        JE_JE_SR_JA_ME_visita(p, draParedes, 362, "Restauraciones cuadrante 1")
                .JE_JE_SR_JA_ME_realiza("OBT", 16, OCLUSAL).JE_JE_SR_JA_ME_realiza("OBT", 16, MESIAL).JE_JE_SR_JA_ME_realiza("OBT", 15, DISTAL);
        JE_JE_SR_JA_ME_visita(p, drMamani, 334, "Endodoncia pieza 46")
                .JE_JE_SR_JA_ME_realiza("END", 46, PIEZA);
        JE_JE_SR_JA_ME_visita(p, draParedes, 300, "Corona 46 y reconstrucción 21")
                .JE_JE_SR_JA_ME_realiza("COR", 46, PIEZA).JE_JE_SR_JA_ME_realiza("REC", 21, OCLUSAL);
        JE_JE_SR_JA_ME_visita(p, draParedes, 152, "Control semestral")
                .JE_JE_SR_JA_ME_cara(37, OCLUSAL, CARIES).JE_JE_SR_JA_ME_cara(47, OCLUSAL, CARIES).JE_JE_SR_JA_ME_cara(24, MESIAL, CARIES)
                .JE_JE_SR_JA_ME_cara(35, OCLUSAL, SELLANTE).JE_JE_SR_JA_ME_cara(45, OCLUSAL, SELLANTE)
                .JE_JE_SR_JA_ME_realiza("OBT", 26, OCLUSAL).JE_JE_SR_JA_ME_realiza("PRO", null, null);
        JE_JE_SR_JA_ME_Visita v6 = JE_JE_SR_JA_ME_visita(p, draParedes, 6, "Control: sensibilidad al frío");
        v6.JE_JE_SR_JA_ME_cara(14, DISTAL, CARIES).JE_JE_SR_JA_ME_realiza("OBT", 36, OCLUSAL);
        JE_JE_SR_JA_ME_pendientesDesdeOdontograma(p, v6.fecha);
        plan.JE_JE_SR_JA_ME_guardar(plan.JE_JE_SR_JA_ME_nuevo(p, catalogo.get("BLA"), null, null, v6.fecha, "Solicitado por la paciente"));
    }

    /** Adulto mayor con piezas perdidas, implante y prótesis. */
    private void JE_JE_SR_JA_ME_carlos() {
        JE_JE_SR_JA_ME_Paciente p = JE_JE_SR_JA_ME_paciente("09214587", "Carlos Alberto", "Condori Pari", LocalDate.of(1964, 9, 3), "M",
                null, "Anticoagulado (warfarina): coordinar con cardiología antes de extracciones", "954 120 877");
        JE_JE_SR_JA_ME_Visita v1 = JE_JE_SR_JA_ME_visita(p, drMamani, 410, "Evaluación integral y plan de rehabilitación");
        v1.pieza(18, AUSENTE).pieza(28, AUSENTE).pieza(38, AUSENTE).pieza(48, AUSENTE)
                .pieza(36, AUSENTE).pieza(46, AUSENTE).pieza(17, EXTRACCION_INDICADA)
                .pieza(26, ENDODONCIA_INDICADA).JE_JE_SR_JA_ME_cara(25, MESIAL, CARIES).JE_JE_SR_JA_ME_cara(14, VESTIBULAR, CARIES)
                .pieza(11, CORONA).pieza(21, CORONA).JE_JE_SR_JA_ME_cara(34, OCLUSAL, OBTURADO).JE_JE_SR_JA_ME_cara(44, OCLUSAL, OBTURADO)
                .JE_JE_SR_JA_ME_realiza("PRO", null, null);
        JE_JE_SR_JA_ME_visita(p, drMamani, 380, "Extracción 17").JE_JE_SR_JA_ME_realiza("EXO", 17, PIEZA);
        JE_JE_SR_JA_ME_visita(p, drMamani, 300, "Endodoncia 26").JE_JE_SR_JA_ME_realiza("END", 26, PIEZA);
        JE_JE_SR_JA_ME_visita(p, drMamani, 200, "Colocación de implante 46").JE_JE_SR_JA_ME_realiza("IMP", 46, PIEZA);
        JE_JE_SR_JA_ME_Visita v5 = JE_JE_SR_JA_ME_visita(p, drMamani, 45, "Control de implante");
        v5.JE_JE_SR_JA_ME_cara(25, MESIAL, OBTURADO).JE_JE_SR_JA_ME_cara(13, VESTIBULAR, CARIES).JE_JE_SR_JA_ME_realiza("PRO", null, null);
        JE_JE_SR_JA_ME_pendientesDesdeOdontograma(p, v5.fecha);
        plan.JE_JE_SR_JA_ME_guardar(plan.JE_JE_SR_JA_ME_nuevo(p, catalogo.get("IMP"), 36, PIEZA, v5.fecha, "Rehabilitación de pieza perdida"));
        plan.JE_JE_SR_JA_ME_guardar(plan.JE_JE_SR_JA_ME_nuevo(p, catalogo.get("COR"), 26, PIEZA, v5.fecha, "Proteger pieza endodonciada"));
    }

    /** Historia verosímil generada con una semilla fija (siempre la misma). */
    private void JE_JE_SR_JA_ME_aleatorio(Random r, String dni, String nombres, String apellidos, int anio, String sexo,
                           String alergias, String antecedentes, JE_JE_SR_JA_ME_Usuario doctor) {
        JE_JE_SR_JA_ME_Paciente p = JE_JE_SR_JA_ME_paciente(dni, nombres, apellidos,
                LocalDate.of(anio, 1 + r.nextInt(12), 1 + r.nextInt(27)), sexo, alergias, antecedentes,
                "9" + (10 + r.nextInt(89)) + " " + (100 + r.nextInt(899)) + " " + (100 + r.nextInt(899)));
        int edad = LocalDate.now().getYear() - anio;
        int inicio = 120 + r.nextInt(300);
        int dias3 = 2 + r.nextInt(12);
        JE_JE_SR_JA_ME_Visita v1 = JE_JE_SR_JA_ME_visita(p, doctor, inicio, edad < 18 ? "Evaluación y prevención" : "Primera consulta");

        // Terceros molares: en adolescentes aún no erupcionan; en adultos, a veces extraídos.
        for (int m : new int[]{18, 28, 38, 48}) {
            if (edad < 18 || r.nextDouble() < 0.55) {
                v1.pieza(m, AUSENTE);
            }
        }
        List<int[]> caries = new ArrayList<>();
        int[] posteriores = {14, 15, 16, 17, 24, 25, 26, 27, 34, 35, 36, 37, 44, 45, 46, 47};
        int nCaries = 2 + r.nextInt(edad > 40 ? 5 : 4);
        for (int k = 0; k < nCaries; k++) {
            int pieza = posteriores[r.nextInt(posteriores.length)];
            JE_JE_SR_JA_ME_Superficie cara = r.nextDouble() < 0.6 ? OCLUSAL : (r.nextBoolean() ? MESIAL : DISTAL);
            v1.JE_JE_SR_JA_ME_cara(pieza, cara, CARIES);
            caries.add(new int[]{pieza, cara.ordinal()});
        }
        if (edad < 25) {
            v1.JE_JE_SR_JA_ME_cara(36, OCLUSAL, SELLANTE).JE_JE_SR_JA_ME_cara(46, OCLUSAL, SELLANTE);
        }
        if (edad > 40 && r.nextBoolean()) {
            v1.pieza(posteriores[r.nextInt(posteriores.length)], ENDODONCIA_INDICADA);
        }
        if (r.nextDouble() < 0.3) {
            v1.JE_JE_SR_JA_ME_cara(11 + r.nextInt(2), OCLUSAL, FRACTURA);
        }
        v1.JE_JE_SR_JA_ME_realiza("PRO", null, null);

        // Segunda visita: se obturan algunas caries.
        int dias2 = Math.max(30, inicio - 25 - r.nextInt(40));
        JE_JE_SR_JA_ME_Visita v2 = JE_JE_SR_JA_ME_visita(p, doctor, dias2, "Restauraciones");
        for (int k = 0; k < caries.size(); k++) {
            if (r.nextDouble() < 0.6) {
                int[] c = caries.get(k);
                v2.JE_JE_SR_JA_ME_realizaSiPendiente("OBT", c[0], JE_JE_SR_JA_ME_Superficie.values()[c[1]]);
            }
        }
        // Control reciente con algún hallazgo nuevo.
        if (r.nextDouble() < 0.7) {
            JE_JE_SR_JA_ME_Visita v3 = JE_JE_SR_JA_ME_visita(p, doctor, dias3, "Control");
            // En el control se resuelve alguna caries que quedó pendiente.
            for (int[] c : caries) {
                if (clinica.JE_JE_SR_JA_ME_odontograma(p).JE_JE_SR_JA_ME_pieza(c[0]).JE_JE_SR_JA_ME_cara(JE_JE_SR_JA_ME_Superficie.values()[c[1]]) == CARIES) {
                    v3.JE_JE_SR_JA_ME_realiza("OBT", c[0], JE_JE_SR_JA_ME_Superficie.values()[c[1]]);
                    break;
                }
            }
            v3.JE_JE_SR_JA_ME_cara(posteriores[r.nextInt(posteriores.length)], OCLUSAL, CARIES);
            JE_JE_SR_JA_ME_pendientesDesdeOdontograma(p, v3.fecha);
        } else {
            JE_JE_SR_JA_ME_pendientesDesdeOdontograma(p, v2.fecha);
        }
    }

    // ------------------------------------------------------------ utilidades

    private JE_JE_SR_JA_ME_Visita JE_JE_SR_JA_ME_visita(JE_JE_SR_JA_ME_Paciente p, JE_JE_SR_JA_ME_Usuario doctor, int diasAtras, String motivo) {
        LocalDateTime fecha = LocalDate.now().minusDays(diasAtras)
                .atTime(LocalTime.of(9 + (diasAtras % 8), (diasAtras * 7) % 60));
        JE_JE_SR_JA_ME_Consulta c = consultas.save(JE_JE_SR_JA_ME_Consulta.builder()
                .paciente(p)
                .odontologo(doctor)
                .fecha(fecha)
                .motivo(motivo)
                .build());
        return new JE_JE_SR_JA_ME_Visita(p, c, fecha);
    }

    /** Crea tratamientos pendientes para todas las patologías abiertas (como el botón "Generar plan"). */
    private void JE_JE_SR_JA_ME_pendientesDesdeOdontograma(JE_JE_SR_JA_ME_Paciente p, LocalDateTime fecha) {
        JE_JE_SR_JA_ME_Odontograma o = clinica.JE_JE_SR_JA_ME_odontograma(p);
        for (JE_JE_SR_JA_ME_Odontograma.JE_JE_SR_JA_ME_Patologia pat : o.JE_JE_SR_JA_ME_patologias()) {
            JE_JE_SR_JA_ME_Tratamiento t = catalogo.get(pat.estado().JE_JE_SR_JA_ME_getTratamientoSugerido());
            if (t != null) {
                JE_JE_SR_JA_ME_Superficie s = t.isPorCara() ? pat.superficie() : PIEZA;
                plan.JE_JE_SR_JA_ME_guardar(plan.JE_JE_SR_JA_ME_nuevo(p, t, pat.pieza(), s, fecha,
                        pat.estado().JE_JE_SR_JA_ME_getEtiqueta() + " en " + pat.superficie().JE_JE_SR_JA_ME_etiqueta(pat.pieza()).toLowerCase()));
            }
        }
    }

    /** Una visita: registra hallazgos y tratamientos realizados en orden, minuto a minuto. */
    private final class JE_JE_SR_JA_ME_Visita {
        private final JE_JE_SR_JA_ME_Paciente paciente;
        private final JE_JE_SR_JA_ME_Consulta consulta;
        private final LocalDateTime fecha;
        private int minuto;

        private JE_JE_SR_JA_ME_Visita(JE_JE_SR_JA_ME_Paciente paciente, JE_JE_SR_JA_ME_Consulta consulta, LocalDateTime fecha) {
            this.paciente = paciente;
            this.consulta = consulta;
            this.fecha = fecha;
        }

        private LocalDateTime JE_JE_SR_JA_ME_siguiente() {
            return fecha.plusMinutes(minuto++);
        }

        JE_JE_SR_JA_ME_Visita JE_JE_SR_JA_ME_cara(int pieza, JE_JE_SR_JA_ME_Superficie s, JE_JE_SR_JA_ME_EstadoDental e) {
            JE_JE_SR_JA_ME_Pieza.JE_JE_SR_JA_ME_validar(pieza);
            clinica.JE_JE_SR_JA_ME_registrarEn(paciente, consulta, JE_JE_SR_JA_ME_siguiente(), pieza, s, e, null);
            return this;
        }

        JE_JE_SR_JA_ME_Visita pieza(int pieza, JE_JE_SR_JA_ME_EstadoDental e) {
            return JE_JE_SR_JA_ME_cara(pieza, PIEZA, e);
        }

        JE_JE_SR_JA_ME_Visita JE_JE_SR_JA_ME_realiza(String codigo, Integer pieza, JE_JE_SR_JA_ME_Superficie s) {
            JE_JE_SR_JA_ME_Tratamiento t = catalogo.get(codigo);
            JE_JE_SR_JA_ME_ItemPlan item = plan.JE_JE_SR_JA_ME_guardar(plan.JE_JE_SR_JA_ME_nuevo(paciente, t, pieza, pieza == null ? null : s, fecha.minusDays(2), null));
            plan.JE_JE_SR_JA_ME_aplicarRealizacion(item, consulta, JE_JE_SR_JA_ME_siguiente());
            return this;
        }

        /** Solo si la cara sigue con caries (el generador puede repetir una pieza). */
        JE_JE_SR_JA_ME_Visita JE_JE_SR_JA_ME_realizaSiPendiente(String codigo, int pieza, JE_JE_SR_JA_ME_Superficie s) {
            JE_JE_SR_JA_ME_EstadoDental actual = clinica.JE_JE_SR_JA_ME_odontograma(paciente).JE_JE_SR_JA_ME_pieza(pieza).JE_JE_SR_JA_ME_cara(s);
            if (actual == CARIES) {
                JE_JE_SR_JA_ME_realiza(codigo, pieza, s);
            }
            return this;
        }
    }
}
