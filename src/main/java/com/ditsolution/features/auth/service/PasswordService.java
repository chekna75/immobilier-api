package com.ditsolution.features.auth.service;

import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordService {
    public String hash(String raw) { return BcryptUtil.bcryptHash(raw); }
    public boolean matches(String raw, String hash) { return BcryptUtil.matches(raw, hash); }
}


