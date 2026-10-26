package com.hospitalflow.inventory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospitalflow.allocation.AppointmentRepository;
import com.hospitalflow.inventory.InventoryDtos.DispenseItem;
import com.hospitalflow.inventory.InventoryDtos.DispenseRequest;
import com.hospitalflow.labpharmacy.Prescription;
import com.hospitalflow.labpharmacy.PrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end inventory + dispensing tests against an in-memory database in PostgreSQL mode.
 * Not @Transactional: every request commits, so rollbacks and lock contention are real.
 * Each test uses its own medicine names so tests don't interfere through the shared database.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:inventorytest;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;LOCK_TIMEOUT=10000;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
})
@AutoConfigureMockMvc
class InventoryIntegrationTest {

    private static final AtomicInteger SEQ = new AtomicInteger();

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired InventoryService inventoryService;
    @Autowired MedicineRepository medicineRepository;
    @Autowired StockMovementRepository stockMovementRepository;
    @Autowired AppointmentRepository appointmentRepository;
    @SpyBean PrescriptionRepository prescriptionRepository;

    private String pharmacistToken;

    @BeforeEach
    void setUp() throws Exception {
        reset(prescriptionRepository);
        pharmacistToken = login("pharmacy@patientflow.local");
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────────

    private String login(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", email, "password", "Demo@1234"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("token").asText();
    }

    private ResultActions postJson(String url, String token, Object body) throws Exception {
        var req = post(url).header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
        if (body != null) req = req.content(json.writeValueAsString(body));
        return mvc.perform(req);
    }

    private String uniqueName(String base) {
        return base + "x" + SEQ.incrementAndGet();
    }

    /** Adds a medicine through the API and returns its id. */
    private long addMedicine(String name, String strength, int qty, int threshold) throws Exception {
        String body = postJson("/api/inventory", pharmacistToken, Map.of(
                "name", name, "strength", strength, "dosageForm", "Tablet", "unit", "tablets",
                "initialQuantity", qty, "reorderThreshold", threshold))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private Prescription pendingPrescription(String details) {
        Prescription p = new Prescription();
        p.setAppointmentId(appointmentRepository.findAll().get(0).getId());
        p.setMedicineDetails(details);
        p.setStatus(Prescription.Status.PENDING);
        return prescriptionRepository.save(p);
    }

    private ResultActions dispense(long rxId, long medicineId, long qty) throws Exception {
        return postJson("/api/labpharmacy/prescriptions/" + rxId + "/dispense", pharmacistToken,
                Map.of("items", List.of(Map.of("medicineId", medicineId, "quantity", qty))));
    }

    private int stock(long medicineId) {
        return medicineRepository.findById(medicineId).orElseThrow().getQuantity();
    }

    private Prescription.Status rxStatus(long rxId) {
        return prescriptionRepository.findById(rxId).orElseThrow().getStatus();
    }

    // ─── Regression: existing logins and protected routes ──────────────────────

    @Test
    void existingStaffCanStillLogInAndReachTheirEndpoints() throws Exception {
        String admin = login("admin@patientflow.local");
        String doctor = login("doctor1@patientflow.local");
        String reception = login("reception@patientflow.local");
        String lab = login("lab@patientflow.local");

        mvc.perform(get("/api/labpharmacy/prescriptions/pending").header("Authorization", "Bearer " + pharmacistToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/labpharmacy/prescriptions/dispensed").header("Authorization", "Bearer " + pharmacistToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/labpharmacy/lab-requests/pending").header("Authorization", "Bearer " + lab))
                .andExpect(status().isOk());
        mvc.perform(get("/api/allocation/doctor/me").header("Authorization", "Bearer " + doctor))
                .andExpect(status().isOk());
        mvc.perform(get("/api/allocation/beds").header("Authorization", "Bearer " + doctor))
                .andExpect(status().isOk());
        mvc.perform(get("/api/queue").header("Authorization", "Bearer " + reception))
                .andExpect(status().isOk());
        mvc.perform(get("/api/labpharmacy/discharge/ready").header("Authorization", "Bearer " + reception))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/staff").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk());
        // role boundaries unchanged
        mvc.perform(get("/api/labpharmacy/lab-requests/pending").header("Authorization", "Bearer " + pharmacistToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void inventoryIsPharmacistOnlyForWritesAndRequiresAuth() throws Exception {
        String doctor = login("doctor1@patientflow.local");
        String lab = login("lab@patientflow.local");
        String admin = login("admin@patientflow.local");

        mvc.perform(get("/api/inventory")).andExpect(status().isForbidden());
        mvc.perform(get("/api/inventory").header("Authorization", "Bearer " + doctor)).andExpect(status().isForbidden());
        mvc.perform(get("/api/inventory").header("Authorization", "Bearer " + admin)).andExpect(status().isOk());
        mvc.perform(get("/api/inventory").header("Authorization", "Bearer " + pharmacistToken)).andExpect(status().isOk());

        Map<String, Object> body = Map.of("name", uniqueName("Cetirizine"), "unit", "tablets",
                "initialQuantity", 5, "reorderThreshold", 1);
        postJson("/api/inventory", doctor, body).andExpect(status().isForbidden());
        postJson("/api/inventory", admin, body).andExpect(status().isForbidden());
        long id = addMedicine(uniqueName("Cetirizine"), "10mg", 5, 1);
        postJson("/api/inventory/" + id + "/restock", lab, Map.of("quantity", 5)).andExpect(status().isForbidden());
        assertEquals(5, stock(id));
    }

    // ─── Add / duplicate / validation ───────────────────────────────────────────

    @Test
    void addMedicineThenDuplicateWithDifferentCaseOrSpacingIsRejected() throws Exception {
        String name = uniqueName("Metformin");
        long id = addMedicine(name, "500mg", 40, 10);

        mvc.perform(get("/api/inventory/" + id).header("Authorization", "Bearer " + pharmacistToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(40))
                .andExpect(jsonPath("$.status").value("IN_STOCK"));

        postJson("/api/inventory", pharmacistToken, Map.of(
                "name", "  " + name.toUpperCase() + " ", "strength", "500 MG", "dosageForm", "tablet",
                "unit", "tablets", "initialQuantity", 1, "reorderThreshold", 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").exists());

        // a different strength is a different medicine
        addMedicine(name, "850mg", 10, 2);
        assertEquals(40, stock(id));
    }

    @Test
    void invalidMedicineInputIsRejected() throws Exception {
        String name = uniqueName("Invalid");
        postJson("/api/inventory", pharmacistToken, Map.of("name", name, "unit", "tablets",
                "initialQuantity", -1, "reorderThreshold", 5))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.initialQuantity").exists());
        postJson("/api/inventory", pharmacistToken, Map.of("name", name, "unit", "tablets",
                "initialQuantity", 5, "reorderThreshold", -2))
                .andExpect(status().isBadRequest());
        postJson("/api/inventory", pharmacistToken, Map.of("name", " ", "unit", "tablets",
                "initialQuantity", 5, "reorderThreshold", 2))
                .andExpect(status().isBadRequest());
        postJson("/api/inventory", pharmacistToken, Map.of("name", name, "unit", "",
                "initialQuantity", 5, "reorderThreshold", 2))
                .andExpect(status().isBadRequest());
        postJson("/api/inventory", pharmacistToken, Map.of("name", name, "unit", "tablets",
                "initialQuantity", 5_000_000_000L, "reorderThreshold", 2))
                .andExpect(status().isBadRequest());
    }

    // ─── Restock and status ─────────────────────────────────────────────────────

    @Test
    void restockAddsToExistingStockAndStatusFollowsQuantity() throws Exception {
        long id = addMedicine(uniqueName("Omeprazole"), "20mg", 0, 10);
        mvc.perform(get("/api/inventory/" + id).header("Authorization", "Bearer " + pharmacistToken))
                .andExpect(jsonPath("$.status").value("OUT_OF_STOCK"));

        postJson("/api/inventory/" + id + "/restock", pharmacistToken, Map.of("quantity", 6))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(6))
                .andExpect(jsonPath("$.status").value("LOW_STOCK"));
        postJson("/api/inventory/" + id + "/restock", pharmacistToken, Map.of("quantity", 5))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(11))
                .andExpect(jsonPath("$.status").value("IN_STOCK"));

        postJson("/api/inventory/" + id + "/restock", pharmacistToken, Map.of("quantity", 0))
                .andExpect(status().isBadRequest());
        postJson("/api/inventory/" + id + "/restock", pharmacistToken, Map.of("quantity", -4))
                .andExpect(status().isBadRequest());
        postJson("/api/inventory/" + id + "/restock", pharmacistToken, Map.of("quantity", InventoryService.MAX_STOCK))
                .andExpect(status().isBadRequest()); // would exceed the ceiling
        postJson("/api/inventory/" + id + "/restock", pharmacistToken, Map.of("quantity", 5_000_000_000L))
                .andExpect(status().isBadRequest());
        postJson("/api/inventory/999999/restock", pharmacistToken, Map.of("quantity", 1))
                .andExpect(status().isNotFound());
        assertEquals(11, stock(id));

        long restocks = stockMovementRepository.findAll().stream()
                .filter(s -> s.getMedicine().getId() == id && s.getType() == StockMovement.Type.RESTOCK).count();
        assertEquals(2, restocks);
    }

    // ─── Dispensing ─────────────────────────────────────────────────────────────

    @Test
    void dispenseDeductsExactlyOnceAndMarksPrescriptionDispensed() throws Exception {
        String name = uniqueName("Amoxicillin");
        long id = addMedicine(name, "500mg", 30, 5);
        Prescription rx = pendingPrescription("Paracetamol 650mg TDS, " + name + " 500 mg BD x 5 days");

        String matches = mvc.perform(get("/api/inventory/prescriptions/pending/matches")
                        .header("Authorization", "Bearer " + pharmacistToken))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        boolean suggested = false;
        for (JsonNode n : json.readTree(matches)) {
            if (n.get("prescriptionId").asLong() == rx.getId()) {
                suggested = n.get("matches").size() == 1 && n.get("matches").get(0).get("id").asLong() == id;
            }
        }
        assertTrue(suggested, "matching medicine should be suggested for the prescription");

        dispense(rx.getId(), id, 10)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DISPENSED"))
                .andExpect(jsonPath("$.medicineDetails").value(rx.getMedicineDetails()));
        assertEquals(20, stock(id));

        dispense(rx.getId(), id, 10).andExpect(status().isConflict());
        assertEquals(20, stock(id));
        assertEquals(1, stockMovementRepository.findByPrescriptionId(rx.getId()).size());

        Prescription saved = prescriptionRepository.findById(rx.getId()).orElseThrow();
        assertEquals("pharmacy@patientflow.local", saved.getDispensedBy());
        assertTrue(saved.getDispensedAt() != null);
    }

    @Test
    void outOfStockAndInsufficientStockBlockDispensing() throws Exception {
        String empty = uniqueName("Azithromycin");
        String low = uniqueName("Ibuprofen");
        long emptyId = addMedicine(empty, "500mg", 0, 3);
        long lowId = addMedicine(low, "400mg", 4, 3);
        Prescription rx1 = pendingPrescription(empty + " 500mg OD x 3 days");
        Prescription rx2 = pendingPrescription(low + " 400mg TDS");

        dispense(rx1.getId(), emptyId, 3)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("out of stock")));
        dispense(rx2.getId(), lowId, 5)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Insufficient stock")));

        assertEquals(0, stock(emptyId));
        assertEquals(4, stock(lowId));
        assertEquals(Prescription.Status.PENDING, rxStatus(rx1.getId()));
        assertEquals(Prescription.Status.PENDING, rxStatus(rx2.getId()));

        dispense(rx2.getId(), lowId, 4).andExpect(status().isOk());
        assertEquals(0, stock(lowId));
    }

    @Test
    void multiItemDispenseIsAllOrNothing() throws Exception {
        String a = uniqueName("Paracetamol");
        String b = uniqueName("Amoxicillin");
        long aId = addMedicine(a, "650mg", 20, 2);
        long bId = addMedicine(b, "500mg", 3, 2);
        Prescription rx = pendingPrescription(a + " 650mg TDS, " + b + " 500mg BD x 5 days");

        postJson("/api/labpharmacy/prescriptions/" + rx.getId() + "/dispense", pharmacistToken,
                Map.of("items", List.of(Map.of("medicineId", aId, "quantity", 9),
                        Map.of("medicineId", bId, "quantity", 10))))
                .andExpect(status().isConflict());
        assertEquals(20, stock(aId)); // first item not deducted even though it had enough
        assertEquals(3, stock(bId));
        assertEquals(Prescription.Status.PENDING, rxStatus(rx.getId()));
    }

    @Test
    void invalidOrUnmatchedMappingsAreRejected() throws Exception {
        String prescribed = uniqueName("Amlodipine");
        String unrelated = uniqueName("Atorvastatin");
        long prescribedId = addMedicine(prescribed, "5mg", 50, 5);
        long otherStrengthId = addMedicine(prescribed, "10mg", 50, 5);
        long unrelatedId = addMedicine(unrelated, "10mg", 50, 5);
        Prescription rx = pendingPrescription(prescribed + " 5mg OD x 30 days");

        dispense(rx.getId(), unrelatedId, 1).andExpect(status().isBadRequest());
        dispense(rx.getId(), otherStrengthId, 1).andExpect(status().isBadRequest());
        dispense(rx.getId(), 999_999, 1).andExpect(status().isBadRequest());
        dispense(rx.getId(), prescribedId, 0).andExpect(status().isBadRequest());
        dispense(rx.getId(), prescribedId, -3).andExpect(status().isBadRequest());
        postJson("/api/labpharmacy/prescriptions/" + rx.getId() + "/dispense", pharmacistToken, null)
                .andExpect(status().isBadRequest()); // legacy no-body call can no longer bypass inventory
        postJson("/api/labpharmacy/prescriptions/" + rx.getId() + "/dispense", pharmacistToken,
                Map.of("items", List.of(Map.of("medicineId", prescribedId, "quantity", 1),
                        Map.of("medicineId", prescribedId, "quantity", 1))))
                .andExpect(status().isBadRequest());
        dispense(999_999, prescribedId, 1).andExpect(status().isNotFound());

        assertEquals(50, stock(prescribedId));
        assertEquals(50, stock(otherStrengthId));
        assertEquals(50, stock(unrelatedId));
        assertEquals(Prescription.Status.PENDING, rxStatus(rx.getId()));

        String doctor = login("doctor1@patientflow.local");
        postJson("/api/labpharmacy/prescriptions/" + rx.getId() + "/dispense", doctor,
                Map.of("items", List.of(Map.of("medicineId", prescribedId, "quantity", 1))))
                .andExpect(status().isForbidden());
        assertEquals(50, stock(prescribedId));
    }

    @Test
    void failedPrescriptionUpdateRollsBackStockDeduction() throws Exception {
        String name = uniqueName("Losartan");
        long id = addMedicine(name, "50mg", 10, 2);
        Prescription rx = pendingPrescription(name + " 50mg OD");

        doThrow(new RuntimeException("simulated database failure"))
                .when(prescriptionRepository).saveAndFlush(any(Prescription.class));

        dispense(rx.getId(), id, 4).andExpect(status().isInternalServerError());

        reset(prescriptionRepository);
        assertEquals(10, stock(id));
        assertEquals(Prescription.Status.PENDING, rxStatus(rx.getId()));
        assertTrue(stockMovementRepository.findByPrescriptionId(rx.getId()).isEmpty());
        assertNull(prescriptionRepository.findById(rx.getId()).orElseThrow().getDispensedAt());

        dispense(rx.getId(), id, 4).andExpect(status().isOk()); // works once the failure is gone
        assertEquals(6, stock(id));
    }

    // ─── Concurrency ────────────────────────────────────────────────────────────

    private List<Object> runConcurrently(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> t : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        return t.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            start.countDown();
            List<Object> results = new ArrayList<>();
            for (Future<Object> f : futures) results.add(f.get(30, TimeUnit.SECONDS));
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    private static DispenseRequest request(long medicineId, long qty) {
        return new DispenseRequest(List.of(new DispenseItem(medicineId, qty)));
    }

    @Test
    void simultaneousDispensesOfDifferentPrescriptionsCannotOversell() throws Exception {
        String name = uniqueName("Prednisolone");
        long id = addMedicine(name, "5mg", 10, 2);
        List<Callable<Object>> tasks = new ArrayList<>();
        List<Long> rxIds = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            long rxId = pendingPrescription(name + " 5mg OD").getId();
            rxIds.add(rxId);
            tasks.add(() -> inventoryService.dispense(rxId, request(id, 6), "pharmacy@patientflow.local"));
        }

        List<Object> results = runConcurrently(tasks);

        long ok = results.stream().filter(r -> r instanceof Prescription).count();
        long rejected = results.stream().filter(r -> r instanceof InventoryException ie
                && ie.getStatus().value() == 409).count();
        assertEquals(1, ok, "only one 6-unit dispense fits in 10 units: " + results);
        assertEquals(3, rejected, "the rest must be rejected as insufficient: " + results);
        assertEquals(4, stock(id));
        long dispensed = rxIds.stream().filter(r -> rxStatus(r) == Prescription.Status.DISPENSED).count();
        assertEquals(1, dispensed);
    }

    @Test
    void simultaneousDispensesOfSamePrescriptionDeductOnce() throws Exception {
        String name = uniqueName("Cefixime");
        long id = addMedicine(name, "200mg", 50, 5);
        long rxId = pendingPrescription(name + " 200mg BD").getId();
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            tasks.add(() -> inventoryService.dispense(rxId, request(id, 10), "pharmacy@patientflow.local"));
        }

        List<Object> results = runConcurrently(tasks);

        assertEquals(1, results.stream().filter(r -> r instanceof Prescription).count(), results.toString());
        assertEquals(40, stock(id));
        assertEquals(1, stockMovementRepository.findByPrescriptionId(rxId).size());
    }
}
