package com.hospitalflow.inventory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Links free-text prescription details to inventory medicines.
 *
 * <p>Prescriptions are free text such as "Paracetamol 650mg TDS, Amoxicillin 500mg BD x 5 days".
 * The text is split into items at , ; + &amp; "and" or new lines, and a medicine matches an item when:
 * <ol>
 *   <li>its name appears in the item (dosage-form words like "Tablet" in the inventory name are ignored), and</li>
 *   <li>the item does not state a different strength. The strength may be written "650mg", "650 mg",
 *       "0.65g" or as a bare number right after the name ("Paracetamol 650"). If the item gives no
 *       strength at all, every strength of that medicine is offered and the pharmacist chooses.</li>
 * </ol>
 * A stated strength that differs (650mg prescribed, 500mg stocked) never matches, and the strength of
 * one item can never validate another item's drug.
 */
public final class MedicineMatcher {

    private static final Pattern ITEM_SEPARATOR = Pattern.compile("[,;+&\\n]|\\band\\b");
    // "500 mg" -> "500mg" so spacing differences don't matter
    private static final Pattern NUMBER_UNIT_GAP = Pattern.compile("(\\d)\\s+(?=[a-z%])");
    // "paracetamol650mg" -> "paracetamol 650mg"
    private static final Pattern LETTER_DIGIT = Pattern.compile("([a-z])(?=\\d)");
    private static final Pattern DOSE_UNIT_GAP = Pattern.compile("(\\d)\\s+(?=(?:mg|g|gm|gms|mcg|ug|ml|iu|%|units?)(?![a-z]))");
    private static final Pattern NON_TOKEN = Pattern.compile("[^a-z0-9.%]+");
    private static final Pattern DOSE = Pattern.compile("^(\\d+(?:\\.\\d+)?)(mg|g|gm|gms|mcg|ug|ml|iu|%|units?)$");
    private static final Pattern BARE_NUMBER = Pattern.compile("^\\d+(?:\\.\\d+)?$");

    /** Dosage-form words ignored in medicine names and strengths when matching. */
    private static final Set<String> FORM_WORDS = Set.of(
            "tab", "tabs", "tablet", "tablets", "cap", "caps", "capsule", "capsules",
            "syp", "syrup", "susp", "suspension", "inj", "injection", "injections", "vial", "vials",
            "amp", "ampoule", "ampoules", "inhaler", "cream", "ointment", "oint", "gel", "drop", "drops",
            "sachet", "sachets", "solution", "soln", "nebuliser", "nebulizer", "nebulisation", "nebulization",
            "lotion", "spray", "patch", "strip", "strips");

    private MedicineMatcher() {}

    public static boolean matches(String prescriptionDetails, Medicine medicine) {
        if (prescriptionDetails == null) return false;

        List<String> medTokens = matchTokens(medicine.getName());
        List<String> name = new ArrayList<>();
        String strength = null; // canonical primary strength of the medicine, e.g. "650mg"
        for (String t : medTokens) {
            String dose = canonicalDose(t);
            if (dose != null) { if (strength == null) strength = dose; }
            else if (!FORM_WORDS.contains(t)) name.add(t);
        }
        if (name.isEmpty()) return false;
        for (String t : matchTokens(medicine.getStrength())) {
            if (strength != null) break;
            strength = canonicalDose(t);
        }

        for (String item : ITEM_SEPARATOR.split(prescriptionDetails.toLowerCase(Locale.ROOT))) {
            List<String> tokens = matchTokens(item);
            int at = indexOfSequence(tokens, name);
            if (at < 0) continue;
            if (strength == null) return true;

            List<String> stated = new ArrayList<>();
            for (String t : tokens) {
                String dose = canonicalDose(t);
                if (dose != null && !dose.endsWith("ml")) stated.add(dose);
            }
            int next = at + name.size();
            boolean bareStrength = next < tokens.size() && BARE_NUMBER.matcher(tokens.get(next)).matches()
                    // "Paracetamol 2 tabs" is a count, not a strength
                    && !(next + 1 < tokens.size() && FORM_WORDS.contains(tokens.get(next + 1)));
            if (stated.isEmpty() && bareStrength) {
                // "Paracetamol 650": a bare number right after the name is the strength in the stocked unit
                if (numberPart(strength).compareTo(new BigDecimal(tokens.get(next))) == 0) return true;
                continue;
            }
            if (stated.isEmpty() || stated.contains(strength)) return true;
        }
        return false;
    }

    /** Normalised identity used for duplicate detection: name|strength|form. Unchanged so stored keys stay valid. */
    public static String key(String name, String strength, String dosageForm) {
        return String.join(" ", tokens(name)) + "|"
                + String.join(" ", tokens(strength)) + "|"
                + String.join(" ", tokens(dosageForm));
    }

    static List<String> tokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        String normalised = NUMBER_UNIT_GAP.matcher(text.toLowerCase(Locale.ROOT)).replaceAll("$1");
        List<String> out = new ArrayList<>();
        for (String t : NON_TOKEN.split(normalised)) {
            String trimmed = t.replaceAll("^\\.+|\\.+$", "");
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    /**
     * Tokens for matching: separates a name glued to its strength, normalises µg, and joins a number
     * to the following word only when that word is a dose unit ("650 mg" -> "650mg", "650 TDS" stays two tokens).
     */
    private static List<String> matchTokens(String text) {
        if (text == null || text.isBlank()) return List.of();
        String lower = text.toLowerCase(Locale.ROOT).replace("µg", "mcg").replace("μg", "mcg");
        lower = LETTER_DIGIT.matcher(lower).replaceAll("$1 ");
        lower = DOSE_UNIT_GAP.matcher(lower).replaceAll("$1");
        List<String> out = new ArrayList<>();
        for (String t : NON_TOKEN.split(lower)) {
            String trimmed = t.replaceAll("^\\.+|\\.+$", "");
            if (!trimmed.isEmpty()) out.add(trimmed);
        }
        return out;
    }

    /** "650mg", "0.65g" and "650000mcg" all become "650mg"; ml/iu/% keep their unit. Null if not a dose. */
    private static String canonicalDose(String token) {
        Matcher m = DOSE.matcher(token);
        if (!m.matches()) return null;
        BigDecimal value = new BigDecimal(m.group(1));
        String unit = m.group(2);
        switch (unit) {
            case "g", "gm", "gms" -> { value = value.movePointRight(3); unit = "mg"; }
            case "mcg", "ug" -> { value = value.movePointLeft(3); unit = "mg"; }
            case "unit", "units" -> unit = "iu";
            default -> { }
        }
        return value.stripTrailingZeros().toPlainString() + unit;
    }

    private static BigDecimal numberPart(String canonicalDose) {
        Matcher m = Pattern.compile("^(\\d+(?:\\.\\d+)?)").matcher(canonicalDose);
        return m.find() ? new BigDecimal(m.group(1)) : BigDecimal.valueOf(-1);
    }

    private static int indexOfSequence(List<String> haystack, List<String> needle) {
        outer:
        for (int i = 0; i + needle.size() <= haystack.size(); i++) {
            for (int j = 0; j < needle.size(); j++) {
                if (!haystack.get(i + j).equals(needle.get(j))) continue outer;
            }
            return i;
        }
        return -1;
    }
}
