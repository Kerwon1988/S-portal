@org.springframework.modulith.ApplicationModule(
        displayName = "customer",
        allowedDependencies = {"shared", "identity :: api", "legal :: api", "staff :: api", "audit :: api"})
package com.simbio.core.customer;
