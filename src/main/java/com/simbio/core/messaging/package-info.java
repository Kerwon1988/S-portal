@org.springframework.modulith.ApplicationModule(
        displayName = "messaging",
        allowedDependencies = {"shared", "files :: api", "audit :: api"})
package com.simbio.core.messaging;
