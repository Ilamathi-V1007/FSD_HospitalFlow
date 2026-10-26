package com.hospitalflow.inventory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MedicineMatcherTest {

    private static Medicine med(String name, String strength) {
        Medicine m = new Medicine();
        m.setName(name);
        m.setStrength(strength);
        return m;
    }

    @Test
    void matchesNameAndStrengthIgnoringCaseAndSpacing() {
        String rx = "Paracetamol 650mg TDS, Amoxicillin 500mg BD x 5 days";
        assertTrue(MedicineMatcher.matches(rx, med("Paracetamol", "650 mg")));
        assertTrue(MedicineMatcher.matches(rx, med("AMOXICILLIN", "500MG")));
    }

    @Test
    void strengthFromAnotherItemDoesNotValidateADifferentDrug() {
        String rx = "Paracetamol 650mg TDS, Amoxicillin 500mg BD x 5 days";
        assertFalse(MedicineMatcher.matches(rx, med("Paracetamol", "500mg")));
        assertFalse(MedicineMatcher.matches("Paracetamol 650mg and Amoxicillin 500mg", med("Paracetamol", "500mg")));
    }

    @Test
    void decimalStrengthIsNotConfusedWithSmallerNumber() {
        String rx = "Supplemental O2 (4L/min nasal cannula), Salbutamol 2.5mg nebulization";
        assertTrue(MedicineMatcher.matches(rx, med("Salbutamol", "2.5mg")));
        assertFalse(MedicineMatcher.matches(rx, med("Salbutamol", "5mg")));
    }

    @Test
    void unrelatedOrPartialNamesAreRejected() {
        assertFalse(MedicineMatcher.matches("Amoxicillin 500mg BD", med("Ibuprofen", "400mg")));
        assertFalse(MedicineMatcher.matches("Amoxicillin 500mg BD", med("Amox", "500mg")));
        assertFalse(MedicineMatcher.matches(null, med("Amoxicillin", "500mg")));
    }

    @Test
    void medicineWithoutStrengthMatchesOnNameOnly() {
        assertTrue(MedicineMatcher.matches("ORS sachet after each loose stool", med("ORS", null)));
    }

    @Test
    void commonWaysOfWritingTheSameMedicineMatch() {
        String rx = "Paracetamol 650mg TDS";
        assertTrue(MedicineMatcher.matches(rx, med("Paracetamol Tablet", "650mg")));       // form word in name
        assertTrue(MedicineMatcher.matches(rx, med("Tab Paracetamol", "650mg")));
        assertTrue(MedicineMatcher.matches(rx, med("Paracetamol 650mg", null)));           // strength in name
        assertTrue(MedicineMatcher.matches(rx, med("Paracetamol", "650 mg tablet")));      // form word in strength
        assertTrue(MedicineMatcher.matches("Paracetamol 650 TDS", med("Paracetamol", "650mg")));   // no unit
        assertTrue(MedicineMatcher.matches("Paracetamol650mg TDS", med("Paracetamol", "650mg")));  // no space
        assertTrue(MedicineMatcher.matches("Paracetamol 0.65g TDS", med("Paracetamol", "650mg"))); // g vs mg
        assertTrue(MedicineMatcher.matches("Tab. Paracetamol 650mg 1-0-1", med("Paracetamol", "650mg")));
        assertTrue(MedicineMatcher.matches("Vitamin D3 1000 IU weekly", med("Vitamin D3", "1000IU")));
        assertTrue(MedicineMatcher.matches("Amoxicillin syrup 250mg/5ml TDS", med("Amoxicillin", "250mg/5ml")));
    }

    @Test
    void prescriptionWithoutStrengthOffersEveryStrengthForThePharmacistToChoose() {
        assertTrue(MedicineMatcher.matches("Paracetamol TDS x 3 days", med("Paracetamol", "650mg")));
        assertTrue(MedicineMatcher.matches("Paracetamol TDS x 3 days", med("Paracetamol", "500mg")));
        assertTrue(MedicineMatcher.matches("Paracetamol 2 tabs TDS", med("Paracetamol", "650mg"))); // 2 is a count
    }

    @Test
    void aStatedDifferentStrengthIsStillRejected() {
        assertFalse(MedicineMatcher.matches("Paracetamol 650mg TDS", med("Paracetamol", "500mg")));
        assertFalse(MedicineMatcher.matches("Paracetamol 650 TDS", med("Paracetamol", "500mg")));
        assertFalse(MedicineMatcher.matches("Paracetamol 0.5g TDS", med("Paracetamol", "650mg")));
        assertFalse(MedicineMatcher.matches("Amoxicillin syrup 125mg/5ml", med("Amoxicillin", "250mg/5ml")));
        assertFalse(MedicineMatcher.matches("Paracetamol 650mg TDS", med("Tablet", null)));
    }

    @Test
    void keyNormalisesCaseAndWhitespace() {
        assertEquals(MedicineMatcher.key("Paracetamol", "500 mg", "Tablet"),
                MedicineMatcher.key("  paracetamol ", "500MG", "tablet"));
        assertNotEquals(MedicineMatcher.key("Paracetamol", "500mg", "Tablet"),
                MedicineMatcher.key("Paracetamol", "650mg", "Tablet"));
        assertNotEquals(MedicineMatcher.key("Paracetamol", "500mg", "Tablet"),
                MedicineMatcher.key("Paracetamol", "500mg", "Syrup"));
    }

    @Test
    void stockStatusIsDerivedFromQuantityAndThreshold() {
        assertEquals(StockStatus.OUT_OF_STOCK, StockStatus.of(0, 10));
        assertEquals(StockStatus.LOW_STOCK, StockStatus.of(1, 10));
        assertEquals(StockStatus.LOW_STOCK, StockStatus.of(10, 10));
        assertEquals(StockStatus.IN_STOCK, StockStatus.of(11, 10));
        assertEquals(StockStatus.IN_STOCK, StockStatus.of(1, 0));
    }
}
