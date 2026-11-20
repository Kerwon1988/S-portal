package com.simbio.core.simbiosphere.api;

import java.util.UUID;

public record AccountLinkResult(UUID linkPublicId, String externalAccountId) {
}
