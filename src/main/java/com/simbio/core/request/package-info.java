@org.springframework.modulith.ApplicationModule(
        displayName = "request",
        allowedDependencies = {"shared", "audit :: api", "customer :: api", "files :: api",
                "messaging :: api", "staff :: api", "notification :: api", "simbiosphere :: api"})
package com.simbio.core.request;
