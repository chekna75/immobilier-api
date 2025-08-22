package com.ditsolution.features.auth.service;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PhoneService {
    private static final PhoneNumberUtil PNU = PhoneNumberUtil.getInstance();
    // Par défaut, on suppose Côte d’Ivoire (CI). Tu peux rendre ça configurable.
    private static final String DEFAULT_REGION = "CI"; // "FR" pour France, etc.

    /** Normalise vers E.164 (+225XXXXXXXXXX). Retourne null si invalide. */
    public String normalizeE164(String raw) {
        return normalizeE164(raw, DEFAULT_REGION);
    }

    /** Normalise vers E.164 avec région par défaut paramétrable. */
    public String normalizeE164(String raw, String defaultRegion) {
        if (raw == null || raw.isBlank()) return null;
        var cleaned = raw.replaceAll("[^\\d+]", ""); // garde chiffres et '+'
        try {
            PhoneNumber proto = PNU.parse(cleaned, defaultRegion);
            if (!PNU.isValidNumber(proto)) return null;
            return PNU.format(proto, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException e) {
            return null;
        }
    }

    /** Vérifie qu’un numéro est valide (selon pays par défaut). */
    public boolean isValid(String raw) {
        return normalizeE164(raw) != null;
    }

    /** Masque pour affichage: +225 XX XX XX ** XX */
    public String maskForDisplay(String raw) {
        String e164 = normalizeE164(raw);
        if (e164 == null) return raw;
        String digits = e164.substring(1); // sans '+'
        if (digits.length() <= 6) return "+" + digits; // petit filet de sécurité
        return "+" + digits.substring(0, digits.length()-4).replaceAll(".", "X")
               + digits.substring(digits.length()-4);
    }
}


