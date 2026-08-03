package com.ledgerbook.lite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import org.junit.Test;

public final class PinSecurityTest {
    @Test
    public void pinUsesSaltedHashAndConstantTimeVerification() {
        byte[] salt = "0123456789abcdef".getBytes(StandardCharsets.UTF_8);
        PinSecurity.Credential credential = PinSecurity.create("1234".toCharArray(), salt);
        assertFalse(credential.encodedHash.contains("1234"));
        assertTrue(PinSecurity.verify("1234".toCharArray(), credential));
        assertFalse(PinSecurity.verify("4321".toCharArray(), credential));
        PinSecurity.Credential differentSalt = PinSecurity.create("1234".toCharArray(),
                "fedcba9876543210".getBytes(StandardCharsets.UTF_8));
        assertFalse(credential.encodedHash.equals(differentSalt.encodedHash));
    }

    @Test(expected = IllegalArgumentException.class)
    public void pinMustContainFourToEightDigits() {
        PinSecurity.create("12a4".toCharArray(), new byte[16]);
    }

    @Test
    public void fifthFailureStartsThirtySecondLockout() {
        long now = 1_000L;
        for (int failures = 1; failures < 5; failures++) {
            PinSecurity.AttemptState state = PinSecurity.afterFailure(failures, now);
            assertFalse(state.isLocked(now));
        }
        PinSecurity.AttemptState locked = PinSecurity.afterFailure(5, now);
        assertTrue(locked.isLocked(now));
        assertTrue(locked.isLocked(now + 29_999L));
        assertFalse(locked.isLocked(now + 30_000L));
    }
}
