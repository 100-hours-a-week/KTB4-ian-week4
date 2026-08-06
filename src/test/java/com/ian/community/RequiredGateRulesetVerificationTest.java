package com.ian.community;

import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

class RequiredGateRulesetVerificationTest {

	@Test
	void deliberatelyFailsWhileVerifyingTheRequiredGateRuleset() {
		fail("Intentional failure used to verify that the required-gate Ruleset blocks merging");
	}
}
