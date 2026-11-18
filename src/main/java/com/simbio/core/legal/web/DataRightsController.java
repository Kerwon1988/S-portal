package com.simbio.core.legal.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DataRightsController {

    @GetMapping("/cuenta/datos")
    public String dataRights() {
        return "legal/data-rights";
    }
}
